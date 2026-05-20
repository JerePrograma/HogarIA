package com.hogaria.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hogaria.dto.TransactionCreateRequest;
import com.hogaria.dto.TransactionImportDtos.*;
import com.hogaria.entity.Category;
import com.hogaria.entity.ExcelImportBatch;
import com.hogaria.entity.ExcelImportRow;
import com.hogaria.entity.ImportBatchStatus;
import com.hogaria.entity.ImportRowStatus;
import com.hogaria.entity.MoneyTransaction;
import com.hogaria.exception.BadRequestException;
import com.hogaria.exception.ForbiddenException;
import com.hogaria.exception.NotFoundException;
import com.hogaria.repository.AccountRepository;
import com.hogaria.repository.CategoryRepository;
import com.hogaria.repository.ExcelImportBatchRepository;
import com.hogaria.repository.ExcelImportRowRepository;
import com.hogaria.repository.FinancialProfileRepository;
import com.hogaria.repository.MoneyTransactionRepository;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.text.Normalizer;
import java.time.DateTimeException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.regex.Pattern;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import java.io.ByteArrayInputStream;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.CodingErrorAction;
import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;

@Service
public class TransactionImportService {

  private static final Locale ES_AR = new Locale("es", "AR");
  private static final String DEFAULT_CURRENCY = "ARS";

  private static final String CAT_COMISIONES = "Comisiones y cargos";
  private static final String CAT_IMPUESTOS = "Impuestos";
  private static final String CAT_TARJETA_CREDITO = "Tarjeta de crédito";
  private static final String CAT_RETIROS = "Retiros de efectivo";
  private static final String CAT_COMPRAS_TARJETA = "Compras con tarjeta";
  private static final String CAT_CUENTA_DNI_DEBIN = "Cuenta DNI / DEBIN";
  private static final String CAT_SUELDO = "Sueldo / Ingresos laborales";
  private static final String CAT_TRANSFERENCIAS_RECIBIDAS = "Transferencias recibidas";
  private static final String CAT_TRANSFERENCIAS_ENVIADAS = "Transferencias enviadas";
  private static final String CAT_DEVOLUCIONES = "Devoluciones y reintegros";
  private static final String CAT_BENEFICIOS = "Beneficios y promociones";
  private static final String CAT_COMIDAS = "Comidas y bebidas";
  private static final String CAT_SUPERMERCADO = "Supermercado";
  private static final String CAT_TRANSPORTE = "Transporte";
  private static final String CAT_SUSCRIPCIONES = "Suscripciones";
  private static final String CAT_SALUD = "Salud y cuidado personal";
  private static final String CAT_EDUCACION = "Educación";
  private static final String CAT_SHOPPING = "Shopping";
  private static final String CAT_VIAJES = "Viajes";
  private static final String CAT_CREDITOS = "Créditos y financiación";
  private static final String CAT_GASTOS_GENERALES = "Gastos generales";
  private static final String CAT_CJ_CAPITAL_RECUPERADO = "CJ - Capital recuperado";
  private static final String CAT_FONDEO_MERCADO_PAGO = "Fondeo MercadoPago / transferencias internas";
  private static final String CAT_AJUSTES_MERCADO_PAGO = "Ajustes MercadoPago";

  private static final List<CategoryRule> CATEGORY_RULES = List.of(
          rule("\\b(COMISION|COMISIÓN|CARGO|PUNTO\\s+EFECTIVO)\\b",
                  CAT_COMISIONES,
                  MoneyTransaction.MovementType.EXPENSE,
                  Category.Type.VARIABLE_EXPENSE,
                  Confidence.HIGH),

          rule("\\b(IMPUESTO|RG\\s*4815|IIBB|RETENCION|RETENCIÓN)\\b",
                  CAT_IMPUESTOS,
                  MoneyTransaction.MovementType.EXPENSE,
                  Category.Type.FIXED_EXPENSE,
                  Confidence.HIGH),

          rule("\\b(PAGO\\s+MASTERCARD|PAGO\\s+VISA|TARJETA\\s+DE\\s+CREDITO|TARJETA\\s+DE\\s+CRÉDITO)\\b",
                  CAT_TARJETA_CREDITO,
                  MoneyTransaction.MovementType.EXPENSE,
                  Category.Type.DEBT,
                  Confidence.HIGH),

          rule("\\b(EXTRACCION\\s+CAJERO|EXTRACCIÓN\\s+CAJERO|RETIRO\\s+CAJERO)\\b",
                  CAT_RETIROS,
                  MoneyTransaction.MovementType.EXPENSE,
                  Category.Type.VARIABLE_EXPENSE,
                  Confidence.HIGH),

          rule("\\b(COMPRA\\s+TARJETA|COMPRA\\s+TARJETA\\s+M\\.E\\.)\\b",
                  CAT_COMPRAS_TARJETA,
                  MoneyTransaction.MovementType.EXPENSE,
                  Category.Type.VARIABLE_EXPENSE,
                  Confidence.MEDIUM),

          rule("\\b(DB\\.DEBIN|DEBIN|CDNI|CUENTA\\s+DNI|PAGO\\s+DEBIN)\\b",
                  CAT_CUENTA_DNI_DEBIN,
                  MoneyTransaction.MovementType.EXPENSE,
                  Category.Type.VARIABLE_EXPENSE,
                  Confidence.MEDIUM),

          rule("\\b(TRANSF\\s+DE\\s+SYSTEMSCORP|SYSTEMSCORP|SUELDO|HABERES)\\b",
                  CAT_SUELDO,
                  MoneyTransaction.MovementType.INCOME,
                  Category.Type.INCOME,
                  Confidence.HIGH),

          rule("\\b(TRANSF\\s+DE|TRANSFERENCIA\\s+RECIBIDA|CR\\.TRAN\\.|BANK\\s+TRANSFER|CUENTA\\s+BANCARIA\\s+DIGITAL|DÉBITO\\s+INMEDIATO|DEBITO\\s+INMEDIATO)\\b",
                  CAT_TRANSFERENCIAS_RECIBIDAS,
                  MoneyTransaction.MovementType.INCOME,
                  Category.Type.INCOME,
                  Confidence.MEDIUM),

          rule("\\b(TRANSF\\s+A|TRANSFERENCIA\\s+ENVIADA|ENVIO\\s+DINERO|ENVÍO\\s+DINERO)\\b",
                  CAT_TRANSFERENCIAS_ENVIADAS,
                  MoneyTransaction.MovementType.EXPENSE,
                  Category.Type.VARIABLE_EXPENSE,
                  Confidence.MEDIUM),

          rule("\\b(DEVO\\.|DEVOLUCION|DEVOLUCIÓN|REINTEGRO|REVERSA|ANULACION|ANULACIÓN)\\b",
                  CAT_DEVOLUCIONES,
                  MoneyTransaction.MovementType.INCOME,
                  Category.Type.INCOME,
                  Confidence.HIGH),

          rule("\\b(BENEF|BENEFICIO|PROMO|PROMOCION|PROMOCIÓN|PEI-CUENTA\\s+DNI)\\b",
                  CAT_BENEFICIOS,
                  MoneyTransaction.MovementType.INCOME,
                  Category.Type.INCOME,
                  Confidence.HIGH),

          rule("\\b(SUBE)\\b",
                  CAT_TRANSPORTE,
                  MoneyTransaction.MovementType.EXPENSE,
                  Category.Type.VARIABLE_EXPENSE,
                  Confidence.HIGH),

          rule("\\b(PASAJES)\\b",
                  CAT_VIAJES,
                  MoneyTransaction.MovementType.EXPENSE,
                  Category.Type.VARIABLE_EXPENSE,
                  Confidence.HIGH),

          rule("\\b(MELI\\+|SUSCRIPCION\\s+A\\s+MELI|SUSCRIPCIÓN\\s+A\\s+MELI)\\b",
                  CAT_SUSCRIPCIONES,
                  MoneyTransaction.MovementType.EXPENSE,
                  Category.Type.FIXED_EXPENSE,
                  Confidence.HIGH),

          rule("\\b(LINK\\s+DE\\s+PAGO|PRESTAMOS|PRÉSTAMOS)\\b",
                  CAT_CJ_CAPITAL_RECUPERADO,
                  MoneyTransaction.MovementType.SAVING,
                  Category.Type.INVESTMENT,
                  Confidence.MEDIUM),

          rule("\\b(PAYMENT\\s+LINKED\\s+TO\\s+A\\s+LOAN\\s+ORIGINATION|MERCADOCREDITO|MERCADOCRÉDITO)\\b",
                  CAT_CREDITOS,
                  MoneyTransaction.MovementType.ADJUSTMENT,
                  Category.Type.DEBT,
                  Confidence.MEDIUM),

          rule("\\b(RAPPI|PEDIDOSYA)\\b",
                  CAT_DEVOLUCIONES,
                  MoneyTransaction.MovementType.INCOME,
                  Category.Type.INCOME,
                  Confidence.MEDIUM),

          rule("\\b(RAPPI|PEDIDOSYA|MOSTAZA|MCDONALD|BURGER|CAF[EÉ]|RESTAURANT|BAR)\\b",
                  CAT_COMIDAS,
                  MoneyTransaction.MovementType.EXPENSE,
                  Category.Type.VARIABLE_EXPENSE,
                  Confidence.MEDIUM),

          rule("\\b(CARREFOUR|COTO|DIA|DÍA|CHANGO|TOLEDO|SUPERMERCADO|SERENISIMA|SERENÍSIMA|LECHE)\\b",
                  CAT_SUPERMERCADO,
                  MoneyTransaction.MovementType.EXPENSE,
                  Category.Type.VARIABLE_EXPENSE,
                  Confidence.MEDIUM),

          rule("\\b(YPF|SHELL|AXION|UBER|CABIFY|DIDI|TAXI|COLECTIVO|TREN)\\b",
                  CAT_TRANSPORTE,
                  MoneyTransaction.MovementType.EXPENSE,
                  Category.Type.VARIABLE_EXPENSE,
                  Confidence.MEDIUM),

          rule("\\b(NETFLIX|SPOTIFY|YOUTUBE|GOOGLE|APPLE|MICROSOFT|OPENAI|AMAZON|DISNEY|MAX|PARAMOUNT)\\b",
                  CAT_SUSCRIPCIONES,
                  MoneyTransaction.MovementType.EXPENSE,
                  Category.Type.FIXED_EXPENSE,
                  Confidence.MEDIUM),

          rule("\\b(FARMACIA|OSDE|SWISS|MEDIC|SALUD|ODONTO|CLINICA|CLÍNICA)\\b",
                  CAT_SALUD,
                  MoneyTransaction.MovementType.EXPENSE,
                  Category.Type.VARIABLE_EXPENSE,
                  Confidence.MEDIUM),

          rule("\\b(EDUCACION|EDUCACIÓN|CURSO|UNIVERSIDAD|FACULTAD|LIBRERIA|LIBRERÍA)\\b",
                  CAT_EDUCACION,
                  MoneyTransaction.MovementType.EXPENSE,
                  Category.Type.VARIABLE_EXPENSE,
                  Confidence.MEDIUM),

          rule("\\b(MERCADOLIBRE|MERCADO\\s+LIBRE|SHOPPING)\\b",
                  CAT_SHOPPING,
                  MoneyTransaction.MovementType.EXPENSE,
                  Category.Type.VARIABLE_EXPENSE,
                  Confidence.MEDIUM),

          rule(".*",
                  CAT_GASTOS_GENERALES,
                  MoneyTransaction.MovementType.EXPENSE,
                  Category.Type.VARIABLE_EXPENSE,
                  Confidence.LOW),

          rule(".*",
                  CAT_TRANSFERENCIAS_RECIBIDAS,
                  MoneyTransaction.MovementType.INCOME,
                  Category.Type.INCOME,
                  Confidence.LOW)
  );

  private final FinancialProfileRepository profileRepository;
  private final AccountRepository accountRepository;
  private final CategoryRepository categoryRepository;
  private final MoneyTransactionRepository txRepository;
  private final ExcelImportBatchRepository batchRepository;
  private final ExcelImportRowRepository rowRepository;
  private final TransactionService txService;
  private final TransactionCategorySuggestionService suggestionService;
  private final ObjectMapper objectMapper;

  public TransactionImportService(
          FinancialProfileRepository profileRepository,
          AccountRepository accountRepository,
          CategoryRepository categoryRepository,
          MoneyTransactionRepository txRepository,
          ExcelImportBatchRepository batchRepository,
          ExcelImportRowRepository rowRepository,
          TransactionService txService,
          TransactionCategorySuggestionService suggestionService,
          ObjectMapper objectMapper
  ) {
    this.profileRepository = profileRepository;
    this.accountRepository = accountRepository;
    this.categoryRepository = categoryRepository;
    this.txRepository = txRepository;
    this.batchRepository = batchRepository;
    this.rowRepository = rowRepository;
    this.txService = txService;
    this.suggestionService = suggestionService;
    this.objectMapper = objectMapper;
  }

  @Transactional
  public TransactionImportPreviewResponse preview(
          UUID userId,
          UUID profileId,
          UUID accountId,
          TransactionImportSource source,
          MultipartFile file,
          Integer year,
          Integer month
  ) {
    ensureProfileBelongsToUser(profileId, userId);
    ensureAccountBelongsToProfile(accountId, profileId);

    var rows = parse(source, file, profileId, accountId, year, month);
    rows = applyDuplicateStatus(profileId, accountId, rows);

    var batch = batchRepository.save(
            ExcelImportBatch.builder()
                    .profileId(profileId)
                    .originalFileName(file.getOriginalFilename())
                    .currency(DEFAULT_CURRENCY)
                    .status(ImportBatchStatus.PREVIEWED)
                    .year(year)
                    .summaryJson("{}")
                    .warningsJson("[]")
                    .errorsJson("[]")
                    .build()
    );

    for (var row : rows) {
      savePreviewRow(batch.getId(), source, row);
    }

    return summarize(batch.getId(), source, accountId, rows);
  }

  @Transactional(readOnly = true)
  public TransactionImportPreviewResponse getBatch(UUID userId, UUID profileId, UUID batchId) {
    ensureProfileBelongsToUser(profileId, userId);

    var batch = batchRepository
            .findById(batchId)
            .orElseThrow(() -> new NotFoundException("Batch not found"));

    if (!batch.getProfileId().equals(profileId)) {
      throw new ForbiddenException("Batch does not belong to profile");
    }

    var rows = loadRows(batchId);

    if (rows.isEmpty()) {
      throw new NotFoundException("Batch has no readable rows");
    }

    return summarize(batchId, rows.get(0).source(), null, rows);
  }

  @Transactional
  public TransactionImportCommitResponse commit(
          UUID userId,
          UUID profileId,
          UUID batchId,
          TransactionImportCommitRequest request
  ) {
    ensureProfileBelongsToUser(profileId, userId);

    var previewRows = loadRows(batchId);

    if (previewRows.isEmpty()) {
      throw new BadRequestException("El batch no tiene filas importables.");
    }

    var rowsByNumber = new HashMap<Integer, TransactionImportPreviewRow>();

    for (var row : previewRows) {
      rowsByNumber.put(row.rowNumber(), row);
    }

    int created = 0;
    int skipped = 0;
    int duplicates = 0;
    int failed = 0;

    var createdIds = new ArrayList<UUID>();
    var warnings = new ArrayList<String>();
    var errors = new ArrayList<String>();

    for (var commitRow : request.rows()) {
      var result = commitSingleRow(userId, profileId, batchId, rowsByNumber, commitRow, request, warnings, errors);

      created += result.createdCount();
      skipped += result.skippedCount();
      duplicates += result.duplicateCount();
      failed += result.failedCount();

      if (result.createdTransactionId() != null) {
        createdIds.add(result.createdTransactionId());
      }
    }

    return new TransactionImportCommitResponse(
            created,
            skipped,
            duplicates,
            failed,
            createdIds,
            warnings,
            errors
    );
  }

  private CommitRowResult commitSingleRow(
          UUID userId,
          UUID profileId,
          UUID batchId,
          Map<Integer, TransactionImportPreviewRow> rowsByNumber,
          TransactionImportCommitRow commitRow,
          TransactionImportCommitRequest request,
          List<String> warnings,
          List<String> errors
  ) {
    var previewRow = rowsByNumber.get(commitRow.rowNumber());

    if (previewRow == null) {
      errors.add("Fila " + commitRow.rowNumber() + ": no existe en el batch.");
      return failedResult();
    }

    if (commitRow.status() == RowStatus.SKIPPED || previewRow.status() == RowStatus.SKIPPED) {
      return skippedResult();
    }
    if (commitRow.status() == RowStatus.DUPLICATE || previewRow.status() == RowStatus.DUPLICATE) {
      if (request.skipDuplicates()) {
        return duplicateResult();
      }
      warnings.add("Fila " + commitRow.rowNumber() + ": marcada como duplicada, se intenta importar.");
    }
    if (commitRow.status() == RowStatus.ERROR || previewRow.status() == RowStatus.ERROR) {
      errors.add("Fila " + commitRow.rowNumber() + ": fila inválida en preview.");
      return failedResult();
    }

    var amount = commitRow.amount() != null ? commitRow.amount() : previewRow.amount();

    if (amount == null || amount.signum() <= 0) {
      errors.add("Fila " + commitRow.rowNumber() + ": monto inválido.");
      return failedResult();
    }

    var accountId = commitRow.accountId();

    if (accountId == null) {
      errors.add("Fila " + commitRow.rowNumber() + ": falta accountId.");
      return failedResult();
    }

    try {
      ensureAccountBelongsToProfile(accountId, profileId);
    } catch (Exception ex) {
      errors.add("Fila " + commitRow.rowNumber() + ": la cuenta no pertenece al perfil.");
      return failedResult();
    }

    var movementType = commitRow.movementType() != null
            ? commitRow.movementType()
            : previewRow.movementType();

    UUID categoryId;

    try {
      categoryId = resolveCategoryId(profileId, commitRow, previewRow, request, movementType);
    } catch (Exception ex) {
      errors.add("Fila " + commitRow.rowNumber() + ": " + ex.getMessage());
      return failedResult();
    }

    if (categoryId == null) {
      errors.add("Fila " + commitRow.rowNumber() + ": falta categoría.");
      return failedResult();
    }
    var category = categoryRepository.findById(categoryId).orElse(null);
    if (category == null) {
      errors.add("Fila " + commitRow.rowNumber() + ": categoría inexistente.");
      return failedResult();
    }
    if (!isMovementCategoryCompatible(movementType, category.getType())) {
      errors.add("Fila " + commitRow.rowNumber() + ": tipo de movimiento/categoría incompatible.");
      return failedResult();
    }

    var description = firstNonBlank(
            commitRow.description(),
            previewRow.rawDescription(),
            previewRow.normalizedDescription(),
            "Movimiento importado"
    );

    if (isDuplicate(profileId, accountId, previewRow.realDate(), amount, description)) {
      if (request.skipDuplicates()) {
        warnings.add("Fila " + commitRow.rowNumber() + ": omitida por duplicado.");
        return duplicateResult();
      }

      warnings.add("Fila " + commitRow.rowNumber() + ": posible duplicado importado igualmente.");
    }

    try {
      var response = txService.create(
              new TransactionCreateRequest(
                      profileId,
                      accountId,
                      categoryId,
                      movementType,
                      previewRow.realDate(),
                      previewRow.budgetDate(),
                      amount,
                      firstNonBlank(previewRow.currency(), DEFAULT_CURRENCY).toUpperCase(Locale.ROOT),
                      description,
                      MoneyTransaction.Origin.IMPORT,
                      MoneyTransaction.Status.CONFIRMED
              ),
              userId,
              new TransactionService.TransactionMetadata(
                      previewRow.source().name(),
                      previewRow.sourceOperationId(),
                      previewRow.sourceHash(),
                      inferPaymentChannel(previewRow.source(), description),
                      null,
                      categoryId == null
                              ? MoneyTransaction.ClassificationStatus.NEEDS_CATEGORY
                              : MoneyTransaction.ClassificationStatus.CLASSIFIED,
                      previewRow.skipReason(),
                      batchId,
                      null
              )
      );

      return createdResult(response.id());
    } catch (Exception ex) {
      errors.add("Fila " + commitRow.rowNumber() + ": " + ex.getMessage());
      return failedResult();
    }
  }

  private List<TransactionImportPreviewRow> parse(
          TransactionImportSource source,
          MultipartFile file,
          UUID profileId,
          UUID accountId,
          Integer year,
          Integer month
  ) {
    if (file == null || file.isEmpty()) {
      throw new BadRequestException("El archivo de importación está vacío.");
    }

    try {
      return switch (source) {
        case BANCO_PROVINCIA -> parseBancoProvincia(file, profileId, accountId, year, month);
        case MERCADO_PAGO -> parseMercadoPago(file, profileId, accountId, year, month);
      };
    } catch (BadRequestException ex) {
      throw ex;
    } catch (Exception ex) {
      throw new BadRequestException("Cannot parse file: " + ex.getMessage());
    }
  }

  private List<TransactionImportPreviewRow> parseBancoProvincia(
          MultipartFile file,
          UUID profileId,
          UUID accountId,
          Integer year,
          Integer month
  ) throws Exception {
    var rows = new ArrayList<TransactionImportPreviewRow>();
    var formatter = new DataFormatter(ES_AR);
    var categories = loadVisibleCategories(profileId);

    try (var workbook = WorkbookFactory.create(file.getInputStream())) {
      var sheet = workbook.getSheet("report");

      if (sheet == null && workbook.getNumberOfSheets() > 0) {
        sheet = workbook.getSheetAt(0);
      }

      if (sheet == null) {
        throw new BadRequestException("No se encontró una hoja válida en el archivo Banco Provincia.");
      }

      BancoProvinciaHeaderIndexes header = null;
      int outputRowNumber = 0;

      for (var sheetRow : sheet) {
        if (header == null) {
          header = detectBancoProvinciaHeader(sheetRow, formatter);
          continue;
        }

        var dateText = cell(sheetRow, header.fechaCol(), formatter);
        var description = cell(sheetRow, header.descripcionCol(), formatter);
        var amountText = cell(sheetRow, header.importeCol(), formatter);
        var saldoText = header.saldoCol() == null ? "" : cell(sheetRow, header.saldoCol(), formatter);

        if (allBlank(dateText, description, amountText)) {
          continue;
        }

        if (anyBlank(dateText, description, amountText)) {
          continue;
        }

        LocalDate date;
        BigDecimal signedAmount;

        try {
          date = parseBancoProvinciaDate(dateText);
          signedAmount = parseLocalizedAmount(amountText);
        } catch (Exception ignored) {
          continue;
        }

        if (signedAmount.signum() == 0) {
          continue;
        }

        var movementType = inferMovementType(signedAmount);
        var normalizedDescription = normalizeDescription(description);
        var budgetDate = resolveBudgetDate(date, year, month);
        var categorySuggestion = suggestCategory(profileId, categories, normalizedDescription, movementType);

        outputRowNumber++;

        rows.add(
                new TransactionImportPreviewRow(
                        outputRowNumber,
                        TransactionImportSource.BANCO_PROVINCIA,
                        null,
                        buildSourceHash("BP", profileId, accountId, null, date, normalizedDescription, signedAmount),
                        date,
                        budgetDate,
                        description,
                        normalizedDescription,
                        signedAmount,
                        signedAmount.abs(),
                        DEFAULT_CURRENCY,
                        movementType,
                        categorySuggestion.categoryId(),
                        categorySuggestion.categoryName(),
                        categorySuggestion.confidence(),
                        categorySuggestion.status(),
                        "",
                        String.format("{\"saldo\":\"%s\"}", escapeJson(saldoText))
                )
        );
      }
    }

    if (rows.isEmpty()) {
      throw new BadRequestException(
              "No se detectaron movimientos en el archivo Banco Provincia. "
                      + "Verificá que tenga columnas Fecha, Descripción e Importe."
      );
    }

    return rows;
  }

  private List<TransactionImportPreviewRow> parseMercadoPago(
          MultipartFile file,
          UUID profileId,
          UUID accountId,
          Integer year,
          Integer month
  ) throws Exception {
    var bytes = file.getBytes();

    if (bytes.length == 0) {
      throw new BadRequestException("El archivo de MercadoPago está vacío.");
    }

    if (looksLikeExcelFile(bytes, file.getOriginalFilename())) {
      return parseMercadoPagoWorkbook(bytes, profileId, accountId, year, month);
    }

    return parseMercadoPagoDelimited(bytes, profileId, accountId, year, month);
  }

  private List<TransactionImportPreviewRow> parseMercadoPagoWorkbook(
          byte[] bytes,
          UUID profileId,
          UUID accountId,
          Integer year,
          Integer month
  ) throws Exception {
    var rows = new ArrayList<TransactionImportPreviewRow>();
    var categories = loadVisibleCategories(profileId);
    var formatter = new DataFormatter(ES_AR);

    try (var workbook = WorkbookFactory.create(new ByteArrayInputStream(bytes))) {
      if (workbook.getNumberOfSheets() == 0) {
        throw new BadRequestException("El Excel de MercadoPago no tiene hojas.");
      }

      var sheet = workbook.getSheetAt(0);

      Map<String, Integer> headers = null;
      List<String> headerValues = List.of();
      int headerRowIndex = -1;

      for (var sheetRow : sheet) {
        var values = readExcelRowValues(sheetRow, formatter);

        if (values.isEmpty()) {
          continue;
        }

        var candidateHeaders = buildMercadoPagoHeaderIndex(values);

        if (hasMercadoPagoRequiredHeaders(candidateHeaders)) {
          headers = candidateHeaders;
          headerValues = values;
          headerRowIndex = sheetRow.getRowNum();
          break;
        }
      }

      if (headers == null) {
        throw new BadRequestException(
                "No se encontró el encabezado de MercadoPago. "
                        + "El archivo debe tener FECHA DE ORIGEN / FECHA DE APROBACIÓN "
                        + "y MONTO NETO DE LA OPERACIÓN QUE IMPACTÓ TU DINERO."
        );
      }

      int outputRowNumber = 0;

      for (int rowIndex = headerRowIndex + 1; rowIndex <= sheet.getLastRowNum(); rowIndex++) {
        var sheetRow = sheet.getRow(rowIndex);

        if (sheetRow == null) {
          continue;
        }

        var cols = readExcelRowValues(sheetRow, formatter, headerValues.size());

        if (isBlankMercadoPagoRow(cols)) {
          continue;
        }

        var previewRow = buildMercadoPagoPreviewRow(
                outputRowNumber + 1,
                cols,
                headers,
                String.join("\t", cols),
                profileId,
                accountId,
                year,
                month,
                categories
        );

        if (previewRow == null) {
          continue;
        }

        outputRowNumber++;
        rows.add(previewRow);
      }
    }

    if (rows.isEmpty()) {
      throw new BadRequestException(
              "No se detectaron movimientos en el Excel de MercadoPago. "
                      + "Revisá que las filas tengan fecha y monto neto impactado."
      );
    }

    return rows;
  }

  private List<TransactionImportPreviewRow> parseMercadoPagoDelimited(
          byte[] bytes,
          UUID profileId,
          UUID accountId,
          Integer year,
          Integer month
  ) {
    var rows = new ArrayList<TransactionImportPreviewRow>();
    var categories = loadVisibleCategories(profileId);

    var content = decodeTextFile(bytes)
            .replace("\uFEFF", "")
            .replace("\u0000", "");

    if (content.isBlank()) {
      throw new BadRequestException("El archivo de MercadoPago está vacío.");
    }

    var records = splitDelimitedRecords(content)
            .stream()
            .map(record -> record == null ? "" : record.replace("\uFEFF", ""))
            .filter(record -> !record.trim().isBlank())
            .toList();

    if (records.size() < 2) {
      throw new BadRequestException(
              "El archivo de MercadoPago no tiene filas suficientes. Primeros caracteres: "
                      + content.substring(0, Math.min(content.length(), 300))
      );
    }

    int headerRecordIndex = 0;
    Character forcedDelimiter = null;

    var firstRecord = records.get(0).trim();

    if (firstRecord.toLowerCase(Locale.ROOT).startsWith("sep=")) {
      forcedDelimiter = firstRecord.length() >= 5 ? firstRecord.charAt(4) : null;
      headerRecordIndex = 1;
    }

    if (records.size() <= headerRecordIndex) {
      throw new BadRequestException("El archivo de MercadoPago no contiene encabezado luego de la línea sep=.");
    }

    var delimiter = forcedDelimiter != null
            ? forcedDelimiter
            : detectDelimiter(records.get(headerRecordIndex));

    var headerValues = splitMercadoPagoLine(records.get(headerRecordIndex), delimiter);
    var headers = buildMercadoPagoHeaderIndex(headerValues);

    if (!hasMercadoPagoRequiredHeaders(headers)) {
      throw new BadRequestException(
              "El archivo no parece ser un export de MercadoPago válido. "
                      + "Separador detectado: [" + printableDelimiter(delimiter) + "]. "
                      + "Headers detectados: [" + String.join(" | ", headerValues) + "]."
      );
    }

    int outputRowNumber = 0;

    for (int recordIndex = headerRecordIndex + 1; recordIndex < records.size(); recordIndex++) {
      var rawRecord = records.get(recordIndex);

      if (rawRecord == null || rawRecord.trim().isBlank()) {
        continue;
      }

      var cols = splitMercadoPagoLine(rawRecord, delimiter);

      if (isBlankMercadoPagoRow(cols)) {
        continue;
      }

      var previewRow = buildMercadoPagoPreviewRow(
              outputRowNumber + 1,
              cols,
              headers,
              rawRecord,
              profileId,
              accountId,
              year,
              month,
              categories
      );

      if (previewRow == null) {
        continue;
      }

      outputRowNumber++;
      rows.add(previewRow);
    }

    if (rows.isEmpty()) {
      var headerDebug = String.join(" | ", headerValues);
      var firstDataRow = records.size() > headerRecordIndex + 1 ? records.get(headerRecordIndex + 1) : "";

      throw new BadRequestException(
              "No se detectaron movimientos en el archivo de MercadoPago. "
                      + "Separador detectado: [" + printableDelimiter(delimiter) + "]. "
                      + "Headers detectados: [" + headerDebug + "]. "
                      + "Primera fila de datos: ["
                      + firstDataRow.substring(0, Math.min(firstDataRow.length(), 500))
                      + "]."
      );
    }

    return rows;
  }

  private TransactionImportPreviewRow buildMercadoPagoPreviewRow(
          int outputRowNumber,
          List<String> cols,
          Map<String, Integer> headers,
          String rawPayload,
          UUID profileId,
          UUID accountId,
          Integer year,
          Integer month,
          List<Category> categories
  ) {
    var dateText = mpFirstValue(
            cols,
            headers,
            "FECHA DE APROBACION",
            "FECHA DE APROBACIÓN",
            "FECHA DE ORIGEN"
    );

    var amountText = mpFirstValue(
            cols,
            headers,
            "MONTO NETO DE LA OPERACION QUE IMPACTO TU DINERO",
            "MONTO NETO DE LA OPERACIÓN QUE IMPACTÓ TU DINERO",
            "MONTO NETO DE LA OPERACION QUE IMPACTO EN TU DINERO",
            "MONTO NETO DE LA OPERACIÓN QUE IMPACTÓ EN TU DINERO",
            "MONTO NETO DE LA OPERACION",
            "MONTO NETO DE LA OPERACIÓN",
            "VALOR DE LA COMPRA"
    );

    if (dateText.isBlank() || amountText.isBlank()) {
      return null;
    }

    LocalDate date;
    BigDecimal signedAmount;

    try {
      date = parseMercadoPagoDate(dateText);
      signedAmount = parseLocalizedAmount(amountText);
    } catch (Exception ignored) {
      return null;
    }

    if (signedAmount.signum() == 0) {
      return null;
    }

    var operationId = mpFirstValue(
            cols,
            headers,
            "ID DE OPERACION EN MERCADO PAGO",
            "ID DE OPERACIÓN EN MERCADO PAGO"
    );

    var reference = mpFirstValue(
            cols,
            headers,
            "CODIGO DE REFERENCIA",
            "CÓDIGO DE REFERENCIA",
            "CODIGO DE PRODUCTO SKU",
            "CÓDIGO DE PRODUCTO SKU"
    );

    var purchaseId = mpFirstValue(cols, headers, "ID DE LA COMPRA");
    var orderId = mpFirstValue(cols, headers, "ID DE LA ORDEN");

    var operationType = mpFirstValue(
            cols,
            headers,
            "TIPO DE OPERACION",
            "TIPO DE OPERACIÓN"
    );

    var paymentMethodType = mpFirstValue(cols, headers, "TIPO DE MEDIO DE PAGO");
    var paymentMethod = mpFirstValue(cols, headers, "MEDIO DE PAGO");

    var currency = firstNonBlank(
            mpFirstValue(cols, headers, "MONEDA"),
            DEFAULT_CURRENCY
    );

    var detail = mpFirstValue(cols, headers, "DETALLE DE LA VENTA");

    var payer = mpFirstValue(
            cols,
            headers,
            "PAGADOR",
            "NOMBRE DE QUIEN HACE EL PAGO"
    );

    var installments = mpFirstValue(cols, headers, "CUOTAS");
    var liquidated = mpFirstValue(cols, headers, "LIQUIDADO");

    var classification = classifyMercadoPagoMovement(
            signedAmount,
            detail,
            operationType,
            paymentMethodType,
            paymentMethod,
            payer,
            purchaseId,
            orderId,
            installments,
            liquidated
    );

    var description = buildMercadoPagoDescription(
            detail,
            operationType,
            paymentMethodType,
            paymentMethod,
            payer,
            purchaseId,
            orderId,
            installments,
            liquidated
    );

    var normalizedDescription = normalizeDescription(description);
    var sourceOperationId = firstNonBlank(operationId, reference, purchaseId, orderId);
    var budgetDate = resolveBudgetDate(date, year, month);

    var categorySuggestion = classification.categoryName() != null
            ? suggestForcedCategory(
            categories,
            classification.categoryName(),
            classification.confidence(),
            classification.status()
    )
            : suggestCategory(profileId, categories, normalizedDescription, classification.movementType());

    var rowStatus = classification.status() != null
            ? classification.status()
            : categorySuggestion.status();

    var warning = firstNonBlank(
            classification.warning(),
            rowStatus == RowStatus.SKIPPED ? "Movimiento omitido por regla de importación MercadoPago." : ""
    );

    return new TransactionImportPreviewRow(
            outputRowNumber,
            TransactionImportSource.MERCADO_PAGO,
            sourceOperationId,
            buildSourceHash(
                    "MP",
                    profileId,
                    accountId,
                    sourceOperationId,
                    date,
                    normalizedDescription,
                    signedAmount
            ),
            date,
            budgetDate,
            description,
            normalizedDescription,
            signedAmount,
            signedAmount.abs(),
            currency.toUpperCase(Locale.ROOT),
            classification.movementType(),
            categorySuggestion.categoryId(),
            categorySuggestion.categoryName(),
            categorySuggestion.confidence(),
            rowStatus,
            warning,
            rawPayload
    );
  }

  private boolean looksLikeExcelFile(byte[] bytes, String originalFilename) {
    var filename = originalFilename == null
            ? ""
            : originalFilename.toLowerCase(Locale.ROOT);

    if (filename.endsWith(".xlsx") || filename.endsWith(".xls")) {
      return true;
    }

    // XLSX = ZIP
    if (bytes.length >= 2
            && bytes[0] == 'P'
            && bytes[1] == 'K') {
      return true;
    }

    // XLS antiguo OLE2
    return bytes.length >= 8
            && (bytes[0] & 0xFF) == 0xD0
            && (bytes[1] & 0xFF) == 0xCF
            && (bytes[2] & 0xFF) == 0x11
            && (bytes[3] & 0xFF) == 0xE0
            && (bytes[4] & 0xFF) == 0xA1
            && (bytes[5] & 0xFF) == 0xB1
            && (bytes[6] & 0xFF) == 0x1A
            && (bytes[7] & 0xFF) == 0xE1;
  }

  private boolean hasMercadoPagoRequiredHeaders(Map<String, Integer> headers) {
    return hasAnyHeader(headers, "FECHA DE APROBACION", "FECHA DE APROBACIÓN", "FECHA DE ORIGEN")
            && hasAnyHeader(
            headers,
            "MONTO NETO DE LA OPERACION QUE IMPACTO TU DINERO",
            "MONTO NETO DE LA OPERACIÓN QUE IMPACTÓ TU DINERO",
            "MONTO NETO DE LA OPERACION QUE IMPACTO EN TU DINERO",
            "MONTO NETO DE LA OPERACIÓN QUE IMPACTÓ EN TU DINERO",
            "MONTO NETO DE LA OPERACION",
            "MONTO NETO DE LA OPERACIÓN",
            "VALOR DE LA COMPRA"
    );
  }

  private boolean hasAnyHeader(Map<String, Integer> headers, String... names) {
    for (var name : names) {
      if (headers.containsKey(normalizeMercadoPagoHeader(name))) {
        return true;
      }
    }

    return false;
  }

  private String mpFirstValue(List<String> cols, Map<String, Integer> indexes, String... keys) {
    for (var key : keys) {
      var value = mpValue(cols, indexes, key);

      if (value != null && !value.trim().isBlank()) {
        return value.trim();
      }
    }

    return "";
  }

  private boolean isBlankMercadoPagoRow(List<String> cols) {
    if (cols == null || cols.isEmpty()) {
      return true;
    }

    for (var col : cols) {
      if (col != null && !cleanDelimitedValue(col).isBlank()) {
        return false;
      }
    }

    return true;
  }

  private List<String> readExcelRowValues(
          org.apache.poi.ss.usermodel.Row row,
          DataFormatter formatter
  ) {
    if (row == null || row.getFirstCellNum() < 0 || row.getLastCellNum() < 0) {
      return List.of();
    }

    return readExcelRowValues(row, formatter, row.getLastCellNum());
  }

  private List<String> readExcelRowValues(
          org.apache.poi.ss.usermodel.Row row,
          DataFormatter formatter,
          int expectedSize
  ) {
    var values = new ArrayList<String>();

    if (row == null) {
      return values;
    }

    var lastCell = Math.max(expectedSize, row.getLastCellNum());

    for (int i = 0; i < lastCell; i++) {
      values.add(excelCell(row, i, formatter));
    }

    return values;
  }

  private String excelCell(
          org.apache.poi.ss.usermodel.Row row,
          int index,
          DataFormatter formatter
  ) {
    if (row == null || index < 0) {
      return "";
    }

    var cell = row.getCell(index);

    if (cell == null) {
      return "";
    }

    if (cell.getCellType() == CellType.NUMERIC && DateUtil.isCellDateFormatted(cell)) {
      return cell.getLocalDateTimeCellValue().toLocalDate().toString();
    }

    return cleanDelimitedValue(formatter.formatCellValue(cell));
  }

  private LocalDate parseMercadoPagoDate(String value) {
    var clean = cleanDelimitedValue(value);

    if (clean.isBlank()) {
      throw new IllegalArgumentException("Fecha MercadoPago vacía.");
    }

    try {
      return OffsetDateTime.parse(clean).toLocalDate();
    } catch (DateTimeParseException ignored) {
    }

    if (clean.length() >= 10 && clean.charAt(4) == '-' && clean.charAt(7) == '-') {
      return LocalDate.parse(clean.substring(0, 10));
    }

    var formatters = List.of(
            DateTimeFormatter.ofPattern("d/M/uuuu", ES_AR),
            DateTimeFormatter.ofPattern("dd/MM/uuuu", ES_AR),
            DateTimeFormatter.ofPattern("d-M-uuuu", ES_AR),
            DateTimeFormatter.ofPattern("dd-MM-uuuu", ES_AR),
            DateTimeFormatter.ISO_LOCAL_DATE
    );

    for (var formatter : formatters) {
      try {
        return LocalDate.parse(clean, formatter);
      } catch (DateTimeException ignored) {
      }
    }

    throw new IllegalArgumentException("Fecha MercadoPago inválida: " + value);
  }

  private List<TransactionImportPreviewRow> applyDuplicateStatus(
          UUID profileId,
          UUID accountId,
          List<TransactionImportPreviewRow> rows
  ) {
    return rows
            .stream()
            .map(row -> {
              if (isDuplicate(profileId, accountId, row.realDate(), row.amount(), row.normalizedDescription())) {
                return copyWithStatus(row);
              }

              return row;
            })
            .toList();
  }

  private TransactionImportPreviewRow copyWithStatus(
          TransactionImportPreviewRow row
  ) {
    return new TransactionImportPreviewRow(
            row.rowNumber(),
            row.source(),
            row.sourceOperationId(),
            row.sourceHash(),
            row.realDate(),
            row.budgetDate(),
            row.rawDescription(),
            row.normalizedDescription(),
            row.rawSignedAmount(),
            row.amount(),
            row.currency(),
            row.movementType(),
            row.suggestedCategoryId(),
            row.suggestedCategoryName(),
            row.confidence(),
            RowStatus.DUPLICATE,
            "Movimiento posiblemente duplicado.",
            row.rawPayload()
    );
  }

  private void savePreviewRow(UUID batchId, TransactionImportSource source, TransactionImportPreviewRow row) {
    try {
      rowRepository.save(
              ExcelImportRow.builder()
                      .batchId(batchId)
                      .sheetName(source.name())
                      .rowNumber(row.rowNumber())
                      .concept(row.rawDescription())
                      .month(row.budgetDate() == null ? null : row.budgetDate().getMonthValue())
                      .realDate(row.realDate())
                      .budgetDate(row.budgetDate())
                      .amount(row.amount())
                      .movementType(row.movementType())
                      .sourceOperationId(row.sourceOperationId())
                      .sourceHash(row.sourceHash())
                      .rawDescription(row.rawDescription())
                      .normalizedDescription(row.normalizedDescription())
                      .status(toImportRowStatus(row.status()))
                      .errorMessage(row.skipReason())
                      .rawJson(objectMapper.writeValueAsString(row))
                      .build()
      );
    } catch (Exception ex) {
      throw new BadRequestException(
              "No se pudo guardar la fila de preview " + row.rowNumber() + ": " + ex.getMessage()
      );
    }
  }

  private ImportRowStatus toImportRowStatus(RowStatus status) {
    if (status == null) {
      return ImportRowStatus.WARNING;
    }

    return switch (status) {
      case READY -> ImportRowStatus.READY;
      case SKIPPED -> ImportRowStatus.SKIPPED;
      case ERROR -> ImportRowStatus.ERROR;
      case NEEDS_CATEGORY, DUPLICATE -> ImportRowStatus.WARNING;
    };
  }

  private List<TransactionImportPreviewRow> loadRows(UUID batchId) {
    return rowRepository
            .findByBatchIdOrderByRowNumber(batchId)
            .stream()
            .map(row -> {
              try {
                return objectMapper.readValue(row.getRawJson(), TransactionImportPreviewRow.class);
              } catch (Exception ignored) {
                return null;
              }
            })
            .filter(Objects::nonNull)
            .toList();
  }

  private TransactionImportPreviewResponse summarize(
          UUID batchId,
          TransactionImportSource source,
          UUID accountId,
          List<TransactionImportPreviewRow> rows
  ) {
    int duplicates = 0;
    int skipped = 0;
    int unresolved = 0;
    int importable = 0;

    for (var row : rows) {
      if (row.status() == RowStatus.DUPLICATE) {
        duplicates++;
      } else if (row.status() == RowStatus.SKIPPED) {
        skipped++;
      } else if (row.status() == RowStatus.NEEDS_CATEGORY) {
        unresolved++;
      } else if (row.status() == RowStatus.READY) {
        importable++;
      }
    }

    return new TransactionImportPreviewResponse(
            batchId,
            source,
            accountId,
            rows.size(),
            importable,
            duplicates,
            skipped,
            unresolved,
            rows,
            List.of(),
            List.of()
    );
  }

  private UUID resolveCategoryId(
          UUID profileId,
          TransactionImportCommitRow commitRow,
          TransactionImportPreviewRow previewRow,
          TransactionImportCommitRequest request,
          MoneyTransaction.MovementType movementType
  ) {
    if (commitRow.categoryId() != null) {
      ensureCategoryBelongsToProfileOrIsGlobal(profileId, commitRow.categoryId());
      return commitRow.categoryId();
    }

    if (previewRow.suggestedCategoryId() != null) {
      ensureCategoryBelongsToProfileOrIsGlobal(profileId, previewRow.suggestedCategoryId());
      return previewRow.suggestedCategoryId();
    }

    if (!request.createMissingFallbackCategory()) {
      return null;
    }

    var categoryName = firstNonBlank(
            previewRow.suggestedCategoryName(),
            fallbackCategoryName(movementType)
    );

    var categoryType = inferCategoryType(categoryName, movementType);

    return getOrCreateCategory(profileId, categoryName, categoryType).getId();
  }

  private Category getOrCreateCategory(UUID profileId, String name, Category.Type type) {
    var existing = loadVisibleCategories(profileId)
            .stream()
            .filter(category -> sameCategoryName(category.getName(), name))
            .filter(category -> isMovementCategoryCompatible(toMovementType(type), category.getType()))
            .findFirst();

      return existing.orElseGet(() -> categoryRepository.save(
              Category.builder()
                      .profileId(profileId)
                      .name(name)
                      .type(type)
                      .scope(Category.Scope.PERSONAL)
                      .active(true)
                      .build()
      ));

  }

  private MoneyTransaction.MovementType toMovementType(Category.Type type) {
    if (type == Category.Type.INCOME) return MoneyTransaction.MovementType.INCOME;
    if (type == Category.Type.SAVING || type == Category.Type.INVESTMENT) return MoneyTransaction.MovementType.SAVING;
    return MoneyTransaction.MovementType.EXPENSE;
  }

  private void ensureProfileBelongsToUser(UUID profileId, UUID userId) {
    profileRepository
            .findByIdAndUserId(profileId, userId)
            .orElseThrow(() -> new ForbiddenException("Profile does not belong to user"));
  }

  private void ensureAccountBelongsToProfile(UUID accountId, UUID profileId) {
    if (accountId == null || !accountRepository.existsByIdAndProfileId(accountId, profileId)) {
      throw new BadRequestException("Account does not belong to profile");
    }
  }

  private void ensureCategoryBelongsToProfileOrIsGlobal(UUID profileId, UUID categoryId) {
    var category = categoryRepository
            .findById(categoryId)
            .orElseThrow(() -> new NotFoundException("Category not found"));

    if (category.getProfileId() != null && !category.getProfileId().equals(profileId)) {
      throw new ForbiddenException("Category does not belong to profile");
    }
  }

  private boolean isDuplicate(
          UUID profileId,
          UUID accountId,
          LocalDate realDate,
          BigDecimal amount,
          String description
  ) {
    return txRepository
            .findByProfileId(profileId)
            .stream()
            .anyMatch(transaction ->
                    Objects.equals(transaction.getAccountId(), accountId)
                            && Objects.equals(transaction.getRealDate(), realDate)
                            && transaction.getAmount().compareTo(amount) == 0
                            && Objects.equals(
                            normalizeDescription(transaction.getDescription()),
                            normalizeDescription(description)
                    )
            );
  }

  private List<Category> loadVisibleCategories(UUID profileId) {
    var categories = new ArrayList<Category>();
    categories.addAll(categoryRepository.findByProfileIdAndActiveTrue(profileId));
    categories.addAll(categoryRepository.findByProfileIdIsNullAndActiveTrue());
    return categories;
  }

  private CategorySuggestion suggestCategory(
          UUID profileId,
          List<Category> categories,
          String normalizedDescription,
          MoneyTransaction.MovementType movementType
  ) {
    var suggestion = suggestionService.suggest(
            profileId,
            normalizedDescription,
            movementType,
            null,
            null
    );
    if (suggestion.status() == TransactionCategorySuggestionService.Status.READY && suggestion.suggestedCategoryId() != null) {
      return new CategorySuggestion(suggestion.suggestedCategoryId(), suggestion.suggestedCategoryName(), Confidence.valueOf(suggestion.confidence().name()), RowStatus.READY, suggestion.suggestedCategoryType());
    }
    if (suggestion.status() == TransactionCategorySuggestionService.Status.NEEDS_CATEGORY) {
      return new CategorySuggestion(null, suggestion.suggestedCategoryName(), Confidence.LOW, RowStatus.NEEDS_CATEGORY, suggestion.suggestedCategoryType());
    }
    var fallbackName = fallbackCategoryName(movementType);
    var fallback = findCategoryByName(categories, fallbackName);
    if (fallback != null) return new CategorySuggestion(fallback.getId(), fallback.getName(), Confidence.LOW, RowStatus.READY, fallback.getType());
    return new CategorySuggestion(null, fallbackName, Confidence.LOW, RowStatus.NEEDS_CATEGORY, inferCategoryType(fallbackName, movementType));
  }

  private Category findCategoryByName(List<Category> categories, String name) {
    return categories
            .stream()
            .filter(category -> sameCategoryName(category.getName(), name))
            .findFirst()
            .orElse(null);
  }

  private Category findCategoryByNameAndCompatibleType(List<Category> categories, String name, MoneyTransaction.MovementType movementType, Category.Type preferredType) {
    return categories
            .stream()
            .filter(category -> sameCategoryName(category.getName(), name))
            .filter(category -> isMovementCategoryCompatible(movementType, category.getType()))
            .filter(category -> preferredType == null || category.getType() == preferredType)
            .findFirst()
            .orElse(null);
  }

  private boolean sameCategoryName(String left, String right) {
    return normalizeCategoryName(left).equals(normalizeCategoryName(right));
  }

  private String normalizeCategoryName(String value) {
    return value == null
            ? ""
            : value.replace('\u00A0', ' ')
              .trim()
              .toLowerCase(Locale.ROOT);
  }

  private Category.Type inferCategoryType(String categoryName, MoneyTransaction.MovementType movementType) {
    var normalized = normalizeText(categoryName);

    if (normalized.contains("FONDEO MERCADOPAGO")
            || normalized.contains("TRANSFERENCIAS INTERNAS")
            || normalized.contains("CUENTA DNI")
            || normalized.contains("DEBIN")) {
      return Category.Type.SAVING;
    }

    if (normalized.contains("AJUSTES MERCADOPAGO")) {
      return Category.Type.VARIABLE_EXPENSE;
    }

    if (normalized.contains("TARJETA DE CREDITO") || normalized.contains("CREDITOS")) {
      return Category.Type.DEBT;
    }

    if (normalized.contains("INVERSION") || normalized.contains("CAPITAL RECUPERADO")) {
      return Category.Type.INVESTMENT;
    }

    if (normalized.contains("AHORRO") || normalized.contains("CUENTAS PROPIAS")) {
      return Category.Type.SAVING;
    }

    if (movementType == MoneyTransaction.MovementType.INCOME) {
      return Category.Type.INCOME;
    }

    if (movementType == MoneyTransaction.MovementType.SAVING) {
      return Category.Type.SAVING;
    }

    return Category.Type.VARIABLE_EXPENSE;
  }

  private String fallbackCategoryName(MoneyTransaction.MovementType movementType) {
    if (movementType == MoneyTransaction.MovementType.INCOME) {
      return "Falta de categoría - Ingreso";
    }

    if (movementType == MoneyTransaction.MovementType.SAVING) {
      return "Falta de categoría - Ahorro";
    }

    if (movementType == MoneyTransaction.MovementType.TRANSFER) {
      return "Falta de categoría - Transferencia";
    }

    if (movementType == MoneyTransaction.MovementType.ADJUSTMENT) {
      return "Falta de categoría - Ajuste";
    }

    return "Falta de categoría - Gasto";
  }

  private BancoProvinciaHeaderIndexes detectBancoProvinciaHeader(
          org.apache.poi.ss.usermodel.Row row,
          DataFormatter formatter
  ) {
    if (row == null || row.getFirstCellNum() < 0 || row.getLastCellNum() < 0) {
      return null;
    }

    Integer fechaCol = null;
    Integer descripcionCol = null;
    Integer importeCol = null;
    Integer saldoCol = null;

    for (int i = row.getFirstCellNum(); i < row.getLastCellNum(); i++) {
      var value = normalizeHeader(cell(row, i, formatter));

      switch (value) {
        case "fecha" -> fechaCol = i;
        case "descripcion", "descripción" -> descripcionCol = i;
        case "importe" -> importeCol = i;
        case "saldo" -> saldoCol = i;
        default -> {
        }
      }
    }

    if (fechaCol == null || descripcionCol == null || importeCol == null) {
      return null;
    }

    return new BancoProvinciaHeaderIndexes(fechaCol, descripcionCol, importeCol, saldoCol);
  }

  private Map<String, Integer> buildMercadoPagoHeaderIndex(List<String> headers) {
    var indexes = new HashMap<String, Integer>();

    for (int i = 0; i < headers.size(); i++) {
      indexes.put(normalizeMercadoPagoHeader(headers.get(i)), i);
    }

    return indexes;
  }

  private char detectDelimiter(String headerLine) {
    int tabs = countChar(headerLine, '\t');
    int semicolons = countChar(headerLine, ';');
    int commas = countChar(headerLine, ',');

    if (tabs >= semicolons && tabs >= commas) {
      return '\t';
    }

    if (semicolons >= commas) {
      return ';';
    }

    return ',';
  }

  private int countChar(String value, char target) {
    int count = 0;

    if (value == null) {
      return count;
    }

    for (int i = 0; i < value.length(); i++) {
      if (value.charAt(i) == target) {
        count++;
      }
    }

    return count;
  }

  private List<String> splitDelimitedRecords(String content) {
    var records = new ArrayList<String>();
    var current = new StringBuilder();
    boolean inQuotes = false;

    for (int i = 0; i < content.length(); i++) {
      char currentChar = content.charAt(i);

      if (currentChar == '"') {
        if (inQuotes && i + 1 < content.length() && content.charAt(i + 1) == '"') {
          current.append('"');
          i++;
        } else {
          inQuotes = !inQuotes;
          current.append(currentChar);
        }

        continue;
      }

      if ((currentChar == '\n' || currentChar == '\r') && !inQuotes) {
        if (current.length() > 0) {
          records.add(current.toString());
          current.setLength(0);
        }

        if (currentChar == '\r' && i + 1 < content.length() && content.charAt(i + 1) == '\n') {
          i++;
        }

        continue;
      }

      current.append(currentChar);
    }

    if (current.length() > 0) {
      records.add(current.toString());
    }

    return records;
  }

  private List<String> splitDelimitedLine(String line, char delimiter) {
    var result = new ArrayList<String>();
    var current = new StringBuilder();
    boolean inQuotes = false;

    for (int i = 0; i < line.length(); i++) {
      char currentChar = line.charAt(i);

      if (currentChar == '"') {
        if (inQuotes && i + 1 < line.length() && line.charAt(i + 1) == '"') {
          current.append('"');
          i++;
        } else {
          inQuotes = !inQuotes;
          current.append(currentChar);
        }

        continue;
      }

      if (currentChar == delimiter && !inQuotes) {
        result.add(cleanDelimitedValue(current.toString()));
        current.setLength(0);
        continue;
      }

      current.append(currentChar);
    }

    result.add(cleanDelimitedValue(current.toString()));
    return result;
  }

  private String cleanDelimitedValue(String value) {
    if (value == null) {
      return "";
    }

    var clean = value
            .replace('\u00A0', ' ')
            .replace("\u0000", "")
            .trim();

    while (clean.length() >= 2 && clean.startsWith("\"") && clean.endsWith("\"")) {
      clean = clean.substring(1, clean.length() - 1).trim();
    }

    return clean
            .replace("\"\"", "\"")
            .trim();
  }

  private String mpValue(List<String> cols, Map<String, Integer> indexes, String key) {
    var index = indexes.get(normalizeMercadoPagoHeader(key));

    if (index == null || index < 0 || index >= cols.size()) {
      return "";
    }

    return cleanDelimitedValue(cols.get(index));
  }

  private String buildMercadoPagoDescription(
          String detail,
          String operationType,
          String paymentMethodType,
          String paymentMethod,
          String payer,
          String purchaseId,
          String orderId,
          String installments,
          String liquidated
  ) {
    var parts = new ArrayList<String>();

    addIfNotBlank(parts, cleanDelimitedValue(detail));

    if (!sameNormalizedText(operationType, "Pago aprobado")) {
      addIfNotBlank(parts, cleanDelimitedValue(operationType));
    }

    addIfNotBlank(parts, cleanDelimitedValue(paymentMethodType));
    addIfNotBlank(parts, cleanDelimitedValue(paymentMethod));
    addIfNotBlank(parts, cleanDelimitedValue(payer));

    if (!cleanDelimitedValue(installments).isBlank()
            && !"1".equals(cleanDelimitedValue(installments))) {
      parts.add("Cuotas: " + cleanDelimitedValue(installments));
    }

    if (!cleanDelimitedValue(liquidated).isBlank()
            && "false".equalsIgnoreCase(cleanDelimitedValue(liquidated))) {
      parts.add("Pendiente de liquidación");
    }

    if (!cleanDelimitedValue(purchaseId).isBlank()) {
      parts.add("Compra: " + cleanDelimitedValue(purchaseId));
    }

    if (!cleanDelimitedValue(orderId).isBlank()) {
      parts.add("Orden: " + cleanDelimitedValue(orderId));
    }

    if (parts.isEmpty()) {
      return "Movimiento MercadoPago";
    }

    return String.join(" | ", parts);
  }

  private boolean sameNormalizedText(String left, String right) {
    return normalizeDescription(left).equals(normalizeDescription(right));
  }

  private void addIfNotBlank(List<String> values, String value) {
    if (value != null && !value.trim().isBlank()) {
      values.add(value.trim());
    }
  }

  private LocalDate parseBancoProvinciaDate(String value) {
    var clean = value
            .replace('\u00A0', ' ')
            .trim()
            .toLowerCase(ES_AR);

    var formatters = List.of(
            DateTimeFormatter.ofPattern("d-MMM-uuuu", ES_AR),
            DateTimeFormatter.ofPattern("dd-MMM-uuuu", ES_AR),
            DateTimeFormatter.ofPattern("d/MM/uuuu", ES_AR),
            DateTimeFormatter.ofPattern("dd/MM/uuuu", ES_AR),
            DateTimeFormatter.ISO_LOCAL_DATE
    );

    for (var formatter : formatters) {
      try {
        return LocalDate.parse(clean, formatter);
      } catch (DateTimeException ignored) {
      }
    }

    throw new IllegalArgumentException("Fecha Banco Provincia inválida: " + value);
  }

  private LocalDate parseIsoDatePrefix(String value) {
    var clean = cleanDelimitedValue(value);

    if (clean.length() >= 10) {
      return LocalDate.parse(clean.substring(0, 10));
    }

    throw new IllegalArgumentException("Fecha inválida: " + value);
  }

  private BigDecimal parseLocalizedAmount(String value) {
    var clean = value
            .replace('\u00A0', ' ')
            .replace("$", "")
            .replace("\"", "")
            .replace(" ", "")
            .trim();

    if (clean.isBlank()) {
      throw new IllegalArgumentException("Importe vacío.");
    }

    boolean negativeByParentheses = clean.startsWith("(") && clean.endsWith(")");

    if (negativeByParentheses) {
      clean = clean.substring(1, clean.length() - 1);
    }

    clean = clean.replace("+", "");

    // Formato argentino: 1.234,56
    if (clean.contains(",")) {
      clean = clean.replace(".", "").replace(",", ".");
    }

    // Formato MercadoPago: 12500.00 queda intacto.
    var amount = new BigDecimal(clean);

    return negativeByParentheses ? amount.negate() : amount;
  }

  private MoneyTransaction.MovementType inferMovementType(BigDecimal signedAmount) {
    return signedAmount.signum() < 0
            ? MoneyTransaction.MovementType.EXPENSE
            : MoneyTransaction.MovementType.INCOME;
  }

  private LocalDate resolveBudgetDate(LocalDate realDate, Integer year, Integer month) {
    return LocalDate.of(
            year != null ? year : realDate.getYear(),
            month != null ? month : realDate.getMonthValue(),
            1
    );
  }

  private String cell(org.apache.poi.ss.usermodel.Row row, int index, DataFormatter formatter) {
    if (row == null || index < 0) {
      return "";
    }

    var cell = row.getCell(index);

    if (cell == null) {
      return "";
    }

    if (cell.getCellType() == CellType.NUMERIC && DateUtil.isCellDateFormatted(cell)) {
      return cell.getLocalDateTimeCellValue().toLocalDate().toString();
    }

    return formatter.formatCellValue(cell).trim();
  }

  private String normalizeHeader(String value) {
    return value == null
            ? ""
            : value
              .replace('\u00A0', ' ')
              .trim()
              .toLowerCase(Locale.ROOT);
  }

  private String normalizeMercadoPagoHeader(String value) {
    return normalizeText(value);
  }

  private String normalizeDescription(String value) {
    return normalizeText(value)
            .replaceAll("\\s+", " ")
            .trim();
  }

  private String normalizeText(String value) {
    if (value == null) {
      return "";
    }

    var clean = value
            .replace('\u00A0', ' ')
            .trim();

    clean = Normalizer.normalize(clean, Normalizer.Form.NFD)
            .replaceAll("\\p{M}", "");

    return clean.toUpperCase(Locale.ROOT);
  }

  private String firstNonBlank(String... values) {
    for (var value : values) {
      if (value != null && !value.trim().isBlank()) {
        return value.trim();
      }
    }

    return "";
  }

  private boolean allBlank(String... values) {
    for (var value : values) {
      if (value != null && !value.trim().isBlank()) {
        return false;
      }
    }

    return true;
  }

  private boolean anyBlank(String... values) {
    for (var value : values) {
      if (value == null || value.trim().isBlank()) {
        return true;
      }
    }

    return false;
  }

  private String escapeJson(String value) {
    return value == null
            ? ""
            : value
              .replace("\\", "\\\\")
              .replace("\"", "\\\"");
  }

  private String buildSourceHash(
          String source,
          UUID profileId,
          UUID accountId,
          String sourceOperationId,
          LocalDate realDate,
          String normalizedDescription,
          BigDecimal signedAmount
  ) {
    return hash(
            source + "|"
                    + profileId + "|"
                    + accountId + "|"
                    + firstNonBlank(sourceOperationId, "") + "|"
                    + realDate + "|"
                    + normalizedDescription + "|"
                    + signedAmount
    );
  }

  private String hash(String value) {
    try {
      var digest = MessageDigest.getInstance("SHA-256");
      var bytes = digest.digest(value.getBytes(StandardCharsets.UTF_8));
      var builder = new StringBuilder();

      for (byte current : bytes) {
        builder.append(String.format("%02x", current));
      }

      return builder.toString();
    } catch (Exception ignored) {
      return UUID.randomUUID().toString();
    }
  }

  private static CategoryRule rule(
          String regex,
          String categoryName,
          MoneyTransaction.MovementType movementType,
          Category.Type categoryType,
          Confidence confidence
  ) {
    return new CategoryRule(
            Pattern.compile(regex, Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE),
            categoryName,
            movementType,
            categoryType,
            confidence
    );
  }

  private record BancoProvinciaHeaderIndexes(
          int fechaCol,
          int descripcionCol,
          int importeCol,
          Integer saldoCol
  ) {
  }

  private record CategorySuggestion(
          UUID categoryId,
          String categoryName,
          Confidence confidence,
          RowStatus status,
          Category.Type categoryType
  ) {
  }

  private record CategoryRule(
          Pattern pattern,
          String categoryName,
          MoneyTransaction.MovementType movementType,
          Category.Type categoryType,
          Confidence confidence
  ) {
    boolean matches(String description, MoneyTransaction.MovementType currentMovementType) {
      if (movementType != null && movementType != currentMovementType) {
        return false;
      }

      return pattern.matcher(description).find();
    }
  }

  private record CommitRowResult(
          int createdCount,
          int skippedCount,
          int duplicateCount,
          int failedCount,
          UUID createdTransactionId
  ) {
  }

  private CommitRowResult createdResult(UUID id) {
    return new CommitRowResult(1, 0, 0, 0, id);
  }

  private CommitRowResult skippedResult() {
    return new CommitRowResult(0, 1, 0, 0, null);
  }

  private CommitRowResult duplicateResult() {
    return new CommitRowResult(0, 0, 1, 0, null);
  }

  private CommitRowResult failedResult() {
    return new CommitRowResult(0, 0, 0, 1, null);
  }

  private List<String> splitMercadoPagoLine(String line, char delimiter) {
    if (line == null || line.isBlank()) {
      return List.of();
    }

    if (delimiter == '\t') {
      var parts = line.split("\t", -1);
      var values = new ArrayList<String>();

      for (var part : parts) {
        values.add(cleanDelimitedValue(part));
      }

      return values;
    }

    return splitDelimitedLine(line, delimiter);
  }

  private String readTextFile(MultipartFile file) throws Exception {
    return decodeTextFile(file.getBytes());
  }

  private String decodeTextFile(byte[] bytes) {
    if (bytes.length == 0) {
      throw new BadRequestException("El archivo está vacío.");
    }

    // UTF-8 BOM
    if (bytes.length >= 3
            && (bytes[0] & 0xFF) == 0xEF
            && (bytes[1] & 0xFF) == 0xBB
            && (bytes[2] & 0xFF) == 0xBF) {
      return new String(bytes, 3, bytes.length - 3, StandardCharsets.UTF_8);
    }

    // UTF-16 LE BOM
    if (bytes.length >= 2
            && (bytes[0] & 0xFF) == 0xFF
            && (bytes[1] & 0xFF) == 0xFE) {
      return new String(bytes, 2, bytes.length - 2, StandardCharsets.UTF_16LE);
    }

    // UTF-16 BE BOM
    if (bytes.length >= 2
            && (bytes[0] & 0xFF) == 0xFE
            && (bytes[1] & 0xFF) == 0xFF) {
      return new String(bytes, 2, bytes.length - 2, StandardCharsets.UTF_16BE);
    }

    int inspected = Math.min(bytes.length, 400);
    int nullsInOddPositions = 0;

    for (int i = 1; i < inspected; i += 2) {
      if (bytes[i] == 0) {
        nullsInOddPositions++;
      }
    }

    // UTF-16LE sin BOM
    if (nullsInOddPositions > inspected / 8) {
      return new String(bytes, StandardCharsets.UTF_16LE);
    }

    try {
      return decodeStrict(bytes, StandardCharsets.UTF_8);
    } catch (CharacterCodingException ignored) {
      // Exportaciones locales de Excel / bancos / MercadoPago suelen venir en Windows-1252.
      return new String(bytes, Charset.forName("windows-1252"));
    }
  }

  private String decodeStrict(byte[] bytes, Charset charset) throws CharacterCodingException {
    var decoder = charset
            .newDecoder()
            .onMalformedInput(CodingErrorAction.REPORT)
            .onUnmappableCharacter(CodingErrorAction.REPORT);

    return decoder.decode(ByteBuffer.wrap(bytes)).toString();
  }

  private String printableDelimiter(char delimiter) {
    if (delimiter == '\t') {
      return "TAB";
    }

    if (delimiter == ';') {
      return "PUNTO_Y_COMA";
    }

    if (delimiter == ',') {
      return "COMA";
    }

    return String.valueOf(delimiter);
  }

  private record MercadoPagoClassification(
          MoneyTransaction.MovementType movementType,
          String categoryName,
          Confidence confidence,
          RowStatus status,
          String warning
  ) {
  }

  private MercadoPagoClassification classifyMercadoPagoMovement(
          BigDecimal signedAmount,
          String detail,
          String operationType,
          String paymentMethodType,
          String paymentMethod,
          String payer,
          String purchaseId,
          String orderId,
          String installments,
          String liquidated
  ) {
    var text = normalizeDescription(String.join(" ",
            firstNonBlank(detail),
            firstNonBlank(operationType),
            firstNonBlank(paymentMethodType),
            firstNonBlank(paymentMethod),
            firstNonBlank(payer),
            firstNonBlank(purchaseId),
            firstNonBlank(orderId),
            firstNonBlank(installments),
            firstNonBlank(liquidated)
    ));

    if (isMercadoPagoNoiseMovement(signedAmount, detail, operationType, paymentMethodType, paymentMethod, payer, purchaseId, orderId)) {
      return new MercadoPagoClassification(
              inferMovementType(signedAmount),
              CAT_AJUSTES_MERCADO_PAGO,
              Confidence.LOW,
              RowStatus.SKIPPED,
              "Movimiento MercadoPago sin detalle suficiente. Se omite para no contaminar ingresos/gastos."
      );
    }

    if (containsAny(text,
            "PAYMENT LINKED TO A LOAN ORIGINATION",
            "MERCADOCREDITO",
            "MERCADO CREDITO"
    )) {
      return new MercadoPagoClassification(
              MoneyTransaction.MovementType.ADJUSTMENT,
              CAT_CREDITOS,
              Confidence.HIGH,
              null,
              "Entrada por financiación/deuda. No debería tratarse como ingreso operativo."
      );
    }

    if (isMercadoPagoInternalFunding(text)) {
      return new MercadoPagoClassification(
              MoneyTransaction.MovementType.TRANSFER,
              CAT_FONDEO_MERCADO_PAGO,
              Confidence.HIGH,
              null,
              "Fondeo o movimiento interno de MercadoPago. No debería contar como ingreso real."
      );
    }

    if (signedAmount.signum() > 0 && containsAny(text,
            "LINK DE PAGO",
            "PRESTAMOS",
            "PRESTAMOS",
            "PRÉSTAMOS",
            "MONEDA DIGITAL"
    )) {
      return new MercadoPagoClassification(
              MoneyTransaction.MovementType.SAVING,
              CAT_CJ_CAPITAL_RECUPERADO,
              Confidence.HIGH,
              null,
              "Recupero/inversión identificado. Se clasifica como ahorro/inversión para no inflar ingresos operativos."
      );
    }

    return new MercadoPagoClassification(
            inferMovementType(signedAmount),
            null,
            Confidence.LOW,
            null,
            ""
    );
  }

  private boolean isMercadoPagoInternalFunding(String normalizedText) {
    return containsAny(
            normalizedText,
            "PAGO DEBIN",
            "DEBITO INMEDIATO",
            "DEBITO INMEDIATO",
            "DÉBITO INMEDIATO",
            "BANK TRANSFER",
            "CUENTA BANCARIA DIGITAL",
            "TRANSFERENCIA BANCARIA"
    );
  }

  private boolean isMercadoPagoNoiseMovement(
          BigDecimal signedAmount,
          String detail,
          String operationType,
          String paymentMethodType,
          String paymentMethod,
          String payer,
          String purchaseId,
          String orderId
  ) {
    var amount = signedAmount == null ? BigDecimal.ZERO : signedAmount.abs();

    var hasUsefulDetail = !firstNonBlank(detail).isBlank()
            || !firstNonBlank(paymentMethodType).isBlank()
            || !firstNonBlank(paymentMethod).isBlank()
            || !firstNonBlank(payer).isBlank()
            || !firstNonBlank(purchaseId).isBlank()
            || !firstNonBlank(orderId).isBlank();

    var onlyGenericOperation = normalizeDescription(operationType).equals("PAGO APROBADO");

    return amount.compareTo(new BigDecimal("5.00")) < 0
            && !hasUsefulDetail
            && onlyGenericOperation;
  }

  private boolean containsAny(String value, String... needles) {
    var normalizedValue = normalizeDescription(value);

    for (var needle : needles) {
      if (normalizedValue.contains(normalizeDescription(needle))) {
        return true;
      }
    }

    return false;
  }

  private CategorySuggestion suggestForcedCategory(
          List<Category> categories,
          String categoryName,
          Confidence confidence,
          RowStatus preferredStatus
  ) {
    var category = findCategoryByName(categories, categoryName);

    if (category != null) {
      return new CategorySuggestion(
              category.getId(),
              category.getName(),
              confidence,
              preferredStatus != null ? preferredStatus : RowStatus.READY,
              category.getType()
      );
    }

    if (preferredStatus == RowStatus.SKIPPED) {
      return new CategorySuggestion(
              null,
              categoryName,
              confidence,
              RowStatus.SKIPPED,
              null
      );
    }

    return new CategorySuggestion(
            null,
            categoryName,
            confidence,
            RowStatus.NEEDS_CATEGORY,
            null
    );
  }

  private boolean isMovementCategoryCompatible(
          MoneyTransaction.MovementType movementType,
          Category.Type categoryType
  ) {
    if (movementType == MoneyTransaction.MovementType.INCOME) {
      return categoryType == Category.Type.INCOME;
    }
    if (movementType == MoneyTransaction.MovementType.SAVING) {
      return categoryType == Category.Type.SAVING || categoryType == Category.Type.INVESTMENT;
    }
    if (movementType == MoneyTransaction.MovementType.EXPENSE) {
      return categoryType != Category.Type.INCOME;
    }
    return true;
  }

  private MoneyTransaction.PaymentChannel inferPaymentChannel(
          TransactionImportSource source,
          String description
  ) {
    var text = normalizeDescription(description);

    if (source == TransactionImportSource.MERCADO_PAGO) {
      return MoneyTransaction.PaymentChannel.MERCADO_PAGO;
    }

    if (text.contains("DEBIN")) {
      return MoneyTransaction.PaymentChannel.DEBIN;
    }

    if (text.contains("CUENTA DNI") || text.contains("CDNI")) {
      return MoneyTransaction.PaymentChannel.CUENTA_DNI;
    }

    if (text.contains("TARJETA DEBITO") || text.contains("T.D.")) {
      return MoneyTransaction.PaymentChannel.DEBIT_CARD;
    }

    if (text.contains("MASTERCARD") || text.contains("VISA")) {
      return MoneyTransaction.PaymentChannel.CREDIT_CARD;
    }

    if (text.contains("TRANSFERENCIA") || text.contains("BANK TRANSFER")) {
      return MoneyTransaction.PaymentChannel.BANK_TRANSFER;
    }

    return MoneyTransaction.PaymentChannel.UNKNOWN;
  }
}
