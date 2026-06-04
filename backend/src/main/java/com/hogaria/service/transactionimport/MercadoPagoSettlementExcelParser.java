package com.hogaria.service.transactionimport;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hogaria.dto.TransactionImportDtos.Confidence;
import com.hogaria.dto.TransactionImportDtos.RowStatus;
import com.hogaria.dto.TransactionImportDtos.TransactionImportSource;
import com.hogaria.entity.MoneyTransaction;
import com.hogaria.exception.BadRequestException;
import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.springframework.stereotype.Component;

@Component
public class MercadoPagoSettlementExcelParser extends ExcelImportParserSupport implements TransactionExcelMovementParser {

  private static final ZoneId IMPORT_ZONE = ZoneId.of("America/Argentina/Buenos_Aires");

  private final TransactionImportRuleClassifier classifier;
  private final ImportSourceHashService hashService;
  private final ObjectMapper objectMapper;

  public MercadoPagoSettlementExcelParser(
          ImportTextNormalizer normalizer,
          TransactionImportRuleClassifier classifier,
          ImportSourceHashService hashService,
          ObjectMapper objectMapper
  ) {
    super(normalizer);
    this.classifier = classifier;
    this.hashService = hashService;
    this.objectMapper = objectMapper;
  }

  @Override
  public ExcelImportTemplate template() {
    return ExcelImportTemplate.MERCADO_PAGO_SETTLEMENT;
  }

  @Override
  public List<ImportedMovementCandidate> parse(
          byte[] bytes,
          DetectedExcelImportFormat detection,
          UUID profileId,
          UUID accountId
  ) {
    var rows = new ArrayList<ImportedMovementCandidate>();
    var formatter = new DataFormatter(ES_AR);

    try (var workbook = WorkbookFactory.create(new ByteArrayInputStream(bytes))) {
      var sheet = workbook.getSheet(detection.sheetName());

      if (sheet == null) {
        throw new BadRequestException("No se encontró la hoja detectada: " + detection.sheetName());
      }

      for (int rowIndex = detection.headerRowIndex() + 1; rowIndex <= sheet.getLastRowNum(); rowIndex++) {
        var sheetRow = sheet.getRow(rowIndex);
        var values = readRow(sheetRow, formatter, detection.headers().size());
        var rowNumber = rowIndex + 1;

        if (isBlankRow(values)) {
          continue;
        }

        try {
          var candidate = parseRow(values, detection, profileId, accountId, rowNumber);

          if (candidate != null) {
            rows.add(candidate);
          }
        } catch (Exception ex) {
          rows.add(errorRow(values, detection, rowNumber, ex.getMessage()));
        }
      }
    } catch (BadRequestException ex) {
      throw ex;
    } catch (Exception ex) {
      throw new BadRequestException("No se pudo parsear Mercado Pago settlement: " + ex.getMessage());
    }

    if (rows.isEmpty()) {
      throw new BadRequestException("No se detectaron movimientos en Mercado Pago settlement.");
    }

    return rows;
  }

  private ImportedMovementCandidate parseRow(
          List<String> values,
          DetectedExcelImportFormat detection,
          UUID profileId,
          UUID accountId,
          int rowNumber
  ) throws Exception {
    var originDateText = value(values, detection.headerIndexes(), "FECHA DE ORIGEN");
    var approvalDateText = value(values, detection.headerIndexes(), "FECHA DE APROBACIÓN");
    var amountText = value(values, detection.headerIndexes(), "MONTO NETO DE LA OPERACIÓN QUE IMPACTÓ TU DINERO");

    if (originDateText.isBlank() && amountText.isBlank()) {
      return null;
    }

    var parsedDate = parseMercadoPagoDateTime(firstNonBlank(originDateText, approvalDateText));
    var signedAmount = parseAmount(amountText);

    if (signedAmount.signum() == 0) {
      return null;
    }

    var operationId = value(values, detection.headerIndexes(), "ID DE OPERACIÓN EN MERCADO PAGO");
    var operationType = value(values, detection.headerIndexes(), "TIPO DE OPERACIÓN");
    var paymentMethodType = value(values, detection.headerIndexes(), "TIPO DE MEDIO DE PAGO");
    var paymentMethod = value(values, detection.headerIndexes(), "MEDIO DE PAGO");
    var currency = firstNonBlank(value(values, detection.headerIndexes(), "MONEDA"), DEFAULT_CURRENCY).toUpperCase();
    var liquidated = value(values, detection.headerIndexes(), "LIQUIDADO");
    var rawDescription = value(values, detection.headerIndexes(), "DETALLE DE LA VENTA");
    var sku = value(values, detection.headerIndexes(), "CÓDIGO DE PRODUCTO SKU");
    var operationTags = value(values, detection.headerIndexes(), "OPERATION_TAGS");
    var installments = value(values, detection.headerIndexes(), "CUOTAS");
    var identificationNumber = value(values, detection.headerIndexes(), "NÚMERO DE IDENTIFICACIÓN");
    var purchaseId = value(values, detection.headerIndexes(), "ID DE LA COMPRA");
    var orderId = value(values, detection.headerIndexes(), "ID DE LA ORDEN");
    var payer = value(values, detection.headerIndexes(), "PAGADOR");
    var bank = value(values, detection.headerIndexes(), "BANCO DE ORIGEN");
    var wallet = value(values, detection.headerIndexes(), "BILLETERA VIRTUAL");
    var lastFour = value(values, detection.headerIndexes(), "LAST_FOUR_DIGITS");
    var franchise = value(values, detection.headerIndexes(), "FRANCHISE");
    var paymentChannel = classifier.inferMercadoPagoPaymentChannel(paymentMethodType, paymentMethod);

    var extendedDescription = joinUseful(
            operationType,
            paymentMethodType,
            paymentMethod,
            payer,
            bank,
            wallet,
            sku.isBlank() ? "" : "SKU: " + sku,
            operationTags,
            purchaseId.isBlank() ? "" : "Compra: " + purchaseId,
            orderId.isBlank() ? "" : "Orden: " + orderId,
            installments.isBlank() ? "" : "Cuotas: " + installments,
            identificationNumber.isBlank() ? "" : "Identificación: " + identificationNumber,
            lastFour.isBlank() ? "" : "Últimos dígitos: " + lastFour,
            franchise,
            liquidated.isBlank() ? "" : "Liquidado: " + liquidated
    );

    var normalizedDescription = normalizer.normalize(joinUseful(rawDescription, extendedDescription));
    var classification = classifier.classifyMercadoPago(
            signedAmount,
            rawDescription,
            operationType,
            paymentMethodType,
            paymentMethod,
            payer,
            liquidated,
            paymentChannel
    );
    var sourceOperationId = firstNonBlank(operationId, identificationNumber, purchaseId, orderId);
    var sourceHash = sourceOperationId.isBlank()
            ? hashService.fromFallback(
            profileId,
            accountId,
            TransactionImportSource.MERCADO_PAGO,
            parsedDate.realDate() + "|" + signedAmount + "|" + normalizedDescription + "|" + extendedDescription
    )
            : hashService.fromOperationId(
            profileId,
            accountId,
            TransactionImportSource.MERCADO_PAGO,
            sourceOperationId
    );
    var raw = rawMap(detection.headers(), values, detection.displayName(), detection.sheetName(), rowNumber);
    raw.put("_signedAmount", signedAmount.toPlainString());
    raw.put("_sourceHash", sourceHash);

    return ImportedMovementCandidate.builder()
            .source(TransactionImportSource.MERCADO_PAGO)
            .detectedFormat(detection.displayName())
            .sourceOperationId(sourceOperationId.isBlank() ? null : sourceOperationId)
            .sourceHash(sourceHash)
            .externalSequence(null)
            .realDate(parsedDate.realDate())
            .budgetDate(parsedDate.realDate())
            .operationDateTime(parsedDate.operationDateTime())
            .operationDateTimePrecision(MoneyTransaction.OperationDateTimePrecision.DATE_TIME)
            .signedAmount(signedAmount)
            .amountAbs(signedAmount.abs())
            .currency(currency)
            .rawDescription(rawDescription)
            .normalizedDescription(normalizedDescription)
            .extendedDescription(extendedDescription)
            .merchantName(null)
            .counterparty(firstNonBlank(payer, bank, wallet))
            .paymentChannel(classification.paymentChannel())
            .movementType(classification.movementType())
            .balanceImpact(classification.balanceImpact())
            .categorySuggestionKey(classification.categorySuggestionKey())
            .categorySuggestionName(classification.categorySuggestionName())
            .classificationStatus(classification.classificationStatus())
            .classificationReason(classification.classificationReason())
            .confidence(classification.confidence())
            .rawJson(objectMapper.writeValueAsString(raw))
            .rowNumber(rowNumber)
            .sheetName(detection.sheetName())
            .targetEntity(targetEntity(classification.movementType()))
            .rowStatus(classification.rowStatus())
            .warning(classification.warning())
            .build();
  }

  private ImportedMovementCandidate errorRow(
          List<String> values,
          DetectedExcelImportFormat detection,
          int rowNumber,
          String message
  ) {
    try {
      return ImportedMovementCandidate.builder()
              .source(TransactionImportSource.MERCADO_PAGO)
              .detectedFormat(detection.displayName())
              .rawDescription("")
              .normalizedDescription("")
              .currency(DEFAULT_CURRENCY)
              .confidence(Confidence.NONE)
              .rawJson(objectMapper.writeValueAsString(rawMap(detection.headers(), values, detection.displayName(), detection.sheetName(), rowNumber)))
              .rowNumber(rowNumber)
              .sheetName(detection.sheetName())
              .targetEntity(com.hogaria.entity.ImportTargetEntity.UNKNOWN)
              .rowStatus(RowStatus.ERROR)
              .warning("Fila inválida: " + message)
              .classificationStatus(MoneyTransaction.ClassificationStatus.REVIEW)
              .classificationReason("PARSE_ERROR")
              .build();
    } catch (Exception ex) {
      throw new BadRequestException("No se pudo construir fila de error: " + ex.getMessage());
    }
  }

  private ParsedDateTime parseMercadoPagoDateTime(String value) {
    var clean = normalizer.cleanRaw(value);

    if (clean.isBlank()) {
      throw new IllegalArgumentException("Fecha Mercado Pago vacía.");
    }

    try {
      var local = OffsetDateTime.parse(clean).atZoneSameInstant(IMPORT_ZONE).toLocalDateTime();
      return new ParsedDateTime(local.toLocalDate(), local);
    } catch (DateTimeParseException ignored) {
    }

    if (clean.length() >= 10 && clean.charAt(4) == '-' && clean.charAt(7) == '-') {
      var localDate = LocalDate.parse(clean.substring(0, 10));

      try {
        var localDateTime = LocalDateTime.parse(clean.substring(0, Math.min(clean.length(), 19)), DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        return new ParsedDateTime(localDateTime.toLocalDate(), localDateTime);
      } catch (DateTimeParseException ignored) {
      }

      return new ParsedDateTime(localDate, localDate.atStartOfDay());
    }

    throw new IllegalArgumentException("Fecha Mercado Pago inválida: " + value);
  }

  private record ParsedDateTime(LocalDate realDate, LocalDateTime operationDateTime) {
  }
}
