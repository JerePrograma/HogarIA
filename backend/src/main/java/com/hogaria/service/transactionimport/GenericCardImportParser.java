package com.hogaria.service.transactionimport;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hogaria.dto.TransactionImportDtos.Confidence;
import com.hogaria.dto.TransactionImportDtos.RowStatus;
import com.hogaria.dto.TransactionImportDtos.TransactionImportPreviewRow;
import com.hogaria.dto.TransactionImportDtos.TransactionImportSource;
import com.hogaria.entity.ImportTargetEntity;
import com.hogaria.entity.MoneyTransaction;
import com.hogaria.exception.BadRequestException;
import com.hogaria.service.TransactionCategorySuggestionService;
import java.math.BigDecimal;
import java.time.DateTimeException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class GenericCardImportParser {

    private static final Locale ES_AR = new Locale("es", "AR");
    private static final String DEFAULT_CURRENCY = "ARS";

    private final ImportTextNormalizer normalizer;
    private final ImportSourceHashService hashService;
    private final TransactionImportPreviewMapper previewMapper;
    private final TransactionCategorySuggestionService suggestionService;
    private final ObjectMapper objectMapper;

    public GenericCardImportParser(
            ImportTextNormalizer normalizer,
            ImportSourceHashService hashService,
            TransactionImportPreviewMapper previewMapper,
            TransactionCategorySuggestionService suggestionService,
            ObjectMapper objectMapper
    ) {
        this.normalizer = normalizer;
        this.hashService = hashService;
        this.previewMapper = previewMapper;
        this.suggestionService = suggestionService;
        this.objectMapper = objectMapper;
    }

    public List<TransactionImportPreviewRow> parse(
            MultipartFile file,
            UUID profileId,
            UUID accountId,
            Integer year,
            Integer month,
            boolean planningOnly
    ) {
        var candidates = parseCandidates(file, profileId, accountId, year, month, planningOnly);

        return candidates
                .stream()
                .map(candidate -> previewMapper.toPreviewRow(profileId, candidate))
                .toList();
    }

    private List<ImportedMovementCandidate> parseCandidates(
            MultipartFile file,
            UUID profileId,
            UUID accountId,
            Integer year,
            Integer month,
            boolean planningOnly
    ) {
        if (file == null || file.isEmpty()) {
            throw new BadRequestException("El resumen de tarjeta/deudas está vacío.");
        }

        var formatter = new DataFormatter(ES_AR);
        var candidates = new ArrayList<ImportedMovementCandidate>();
        var source = planningOnly
                ? TransactionImportSource.DEUDAS_TARJETA_GENERICA
                : TransactionImportSource.TARJETA_CREDITO_GENERICA;

        try (var workbook = WorkbookFactory.create(file.getInputStream())) {
            if (workbook.getNumberOfSheets() == 0) {
                throw new BadRequestException("El archivo no tiene hojas.");
            }

            var sheet = workbook.getSheetAt(0);
            Map<String, Integer> headers = null;
            List<String> headerValues = List.of();
            int headerRow = -1;

            for (var row : sheet) {
                var values = readExcelRowValues(row, formatter);
                var candidate = buildGenericCardHeaderIndex(values);

                if (hasGenericCardRequiredHeaders(candidate)) {
                    headers = candidate;
                    headerValues = values;
                    headerRow = row.getRowNum();
                    break;
                }
            }

            if (headers == null) {
                throw new BadRequestException("No encontramos columnas de fecha, descripción/comercio y monto.");
            }

            int outputRow = 0;

            for (int rowIndex = headerRow + 1; rowIndex <= sheet.getLastRowNum(); rowIndex++) {
                var sheetRow = sheet.getRow(rowIndex);
                var values = readExcelRowValues(sheetRow, formatter, headerValues.size());

                if (isBlankRow(values)) {
                    continue;
                }

                outputRow++;

                try {
                    var candidate = parseRow(
                            outputRow,
                            source,
                            values,
                            headers,
                            headerValues,
                            profileId,
                            accountId,
                            year,
                            month,
                            planningOnly
                    );

                    candidates.add(candidate);
                } catch (Exception ex) {
                    candidates.add(errorRow(outputRow, source, headerValues, values, ex.getMessage()));
                }
            }
        } catch (BadRequestException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new BadRequestException("No se pudo parsear el resumen: " + ex.getMessage());
        }

        if (candidates.isEmpty()) {
            throw new BadRequestException("No se detectaron consumos en el resumen.");
        }

        return candidates;
    }

    private ImportedMovementCandidate parseRow(
            int rowNumber,
            TransactionImportSource source,
            List<String> values,
            Map<String, Integer> headers,
            List<String> headerValues,
            UUID profileId,
            UUID accountId,
            Integer year,
            Integer month,
            boolean planningOnly
    ) throws Exception {
        var dateText = genericCardValue(
                values,
                headers,
                "fecha",
                "fecha_compra",
                "fecha_de_compra"
        );

        var description = firstNonBlank(
                genericCardValue(values, headers, "descripcion", "descripción", "comercio", "detalle"),
                "Consumo de tarjeta"
        );

        var amountText = genericCardValue(
                values,
                headers,
                "monto",
                "importe",
                "importe_cuota",
                "monto_cuota"
        );

        var date = parseDate(dateText);
        var amount = parseAmount(amountText).abs();

        if (amount.signum() == 0) {
            throw new IllegalArgumentException("Monto cero.");
        }

        var normalizedDescription = normalizer.normalize(description);
        var installmentText = genericCardValue(values, headers, "cuota", "cuotas", "plan");
        var enrichedDescription = installmentText == null || installmentText.isBlank()
                ? description
                : description + " | Cuotas: " + installmentText;

        var extendedDescription = joinUseful(
                installmentText.isBlank() ? "" : "Cuotas: " + installmentText,
                genericCardValue(values, headers, "tarjeta", "nro_tarjeta", "numero_tarjeta"),
                genericCardValue(values, headers, "comprobante", "operacion", "operación", "referencia")
        );

        var budgetDate = resolveBudgetDate(date, year, month);
        var signedAmount = amount.negate();
        var sourceHash = hashService.fromFallback(
                profileId,
                accountId,
                source,
                date + "|" + normalizedDescription + "|" + amount + "|" + installmentText
        );

        var rawJson = objectMapper.writeValueAsString(rawMap(headerValues, values, source.name(), rowNumber));

        if (planningOnly) {
            return ImportedMovementCandidate.builder()
                    .source(source)
                    .detectedFormat("GENERIC_CARD_PLANNING")
                    .sourceHash(sourceHash)
                    .realDate(date)
                    .budgetDate(budgetDate)
                    .operationDateTime(date.atStartOfDay())
                    .operationDateTimePrecision(MoneyTransaction.OperationDateTimePrecision.DATE_ONLY)
                    .signedAmount(signedAmount)
                    .amountAbs(amount)
                    .currency(DEFAULT_CURRENCY)
                    .rawDescription(enrichedDescription)
                    .normalizedDescription(normalizedDescription)
                    .extendedDescription(extendedDescription)
                    .paymentChannel(MoneyTransaction.PaymentChannel.CREDIT_CARD)
                    .movementType(MoneyTransaction.MovementType.EXPENSE)
                    .balanceImpact(MoneyTransaction.BalanceImpact.IGNORED)
                    .categorySuggestionName("Planificación futura de tarjeta")
                    .classificationStatus(MoneyTransaction.ClassificationStatus.IGNORED_BY_RULE)
                    .classificationReason("CARD_PLANNING_ONLY")
                    .confidence(Confidence.LOW)
                    .rawJson(rawJson)
                    .rowNumber(rowNumber)
                    .sheetName(source.name())
                    .targetEntity(ImportTargetEntity.UNKNOWN)
                    .rowStatus(RowStatus.SKIPPED)
                    .warning("Este resumen sirve para planificar cuotas futuras; no se crea un movimiento real confirmado.")
                    .build();
        }

        var suggestion = suggestionService.suggest(
                profileId,
                normalizedDescription,
                MoneyTransaction.MovementType.EXPENSE,
                source.name(),
                amount
        );

        return ImportedMovementCandidate.builder()
                .source(source)
                .detectedFormat("GENERIC_CARD")
                .sourceHash(sourceHash)
                .realDate(date)
                .budgetDate(budgetDate)
                .operationDateTime(date.atStartOfDay())
                .operationDateTimePrecision(MoneyTransaction.OperationDateTimePrecision.DATE_ONLY)
                .signedAmount(signedAmount)
                .amountAbs(amount)
                .currency(DEFAULT_CURRENCY)
                .rawDescription(enrichedDescription)
                .normalizedDescription(normalizedDescription)
                .extendedDescription(extendedDescription)
                .paymentChannel(firstNonNull(suggestion.paymentChannel(), MoneyTransaction.PaymentChannel.CREDIT_CARD))
                .movementType(firstNonNull(suggestion.suggestedMovementType(), MoneyTransaction.MovementType.EXPENSE))
                .balanceImpact(MoneyTransaction.BalanceImpact.CONSUMPTION_EXPENSE)
                .categorySuggestionKey(null)
                .categorySuggestionName(suggestion.suggestedCategoryName())
                .classificationStatus(firstNonNull(
                        suggestion.classificationStatus(),
                        MoneyTransaction.ClassificationStatus.NEEDS_CATEGORY
                ))
                .classificationReason(suggestion.reason())
                .confidence(toImportConfidence(suggestion.confidence()))
                .rawJson(rawJson)
                .rowNumber(rowNumber)
                .sheetName(source.name())
                .targetEntity(ImportTargetEntity.EXPENSE)
                .rowStatus(toRowStatus(suggestion.status()))
                .warning(suggestion.warning())
                .build();
    }

    private ImportedMovementCandidate errorRow(
            int rowNumber,
            TransactionImportSource source,
            List<String> headers,
            List<String> values,
            String message
    ) {
        try {
            var description = firstNonBlank(
                    values == null || values.isEmpty() ? null : String.join(" | ", values),
                    "Fila inválida"
            );

            return ImportedMovementCandidate.builder()
                    .source(source)
                    .detectedFormat(source.name())
                    .rawDescription(description)
                    .normalizedDescription(normalizer.normalize(description))
                    .currency(DEFAULT_CURRENCY)
                    .paymentChannel(MoneyTransaction.PaymentChannel.CREDIT_CARD)
                    .movementType(MoneyTransaction.MovementType.EXPENSE)
                    .targetEntity(ImportTargetEntity.UNKNOWN)
                    .classificationStatus(MoneyTransaction.ClassificationStatus.REVIEW)
                    .classificationReason("PARSE_ERROR")
                    .confidence(Confidence.NONE)
                    .rawJson(objectMapper.writeValueAsString(rawMap(headers, values, source.name(), rowNumber)))
                    .rowNumber(rowNumber)
                    .sheetName(source.name())
                    .rowStatus(RowStatus.ERROR)
                    .warning("No pudimos leer fecha o monto de esta fila: " + message)
                    .build();
        } catch (Exception ex) {
            throw new BadRequestException("No se pudo construir fila de error: " + ex.getMessage());
        }
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

        return normalizer.cleanRaw(formatter.formatCellValue(cell));
    }

    private Map<String, Integer> buildGenericCardHeaderIndex(List<String> headers) {
        var indexes = new LinkedHashMap<String, Integer>();

        for (int i = 0; i < headers.size(); i++) {
            indexes.put(normalizeGenericHeader(headers.get(i)), i);
        }

        return indexes;
    }

    private boolean hasGenericCardRequiredHeaders(Map<String, Integer> headers) {
        return hasAnyGenericHeader(headers, "fecha", "fecha_compra", "fecha_de_compra")
                && hasAnyGenericHeader(headers, "descripcion", "descripción", "comercio", "detalle")
                && hasAnyGenericHeader(headers, "monto", "importe", "importe_cuota", "monto_cuota");
    }

    private boolean hasAnyGenericHeader(Map<String, Integer> headers, String... names) {
        for (var name : names) {
            if (headers.containsKey(normalizeGenericHeader(name))) {
                return true;
            }
        }

        return false;
    }

    private String genericCardValue(
            List<String> values,
            Map<String, Integer> headers,
            String... names
    ) {
        for (var name : names) {
            var index = headers.get(normalizeGenericHeader(name));

            if (index != null && index >= 0 && index < values.size()) {
                return normalizer.cleanRaw(values.get(index));
            }
        }

        return "";
    }

    private String normalizeGenericHeader(String value) {
        return normalizer.canonicalHeader(value).replace(" ", "_");
    }

    private boolean isBlankRow(List<String> values) {
        if (values == null || values.isEmpty()) {
            return true;
        }

        for (var value : values) {
            if (value != null && !normalizer.cleanRaw(value).isBlank()) {
                return false;
            }
        }

        return true;
    }

    private LocalDate parseDate(String value) {
        var clean = normalizer.cleanRaw(value).toLowerCase(ES_AR);

        if (clean.isBlank()) {
            throw new IllegalArgumentException("Fecha vacía.");
        }

        var formatters = List.of(
                DateTimeFormatter.ofPattern("d-MMM-uuuu", ES_AR),
                DateTimeFormatter.ofPattern("dd-MMM-uuuu", ES_AR),
                DateTimeFormatter.ofPattern("d/MM/uuuu", ES_AR),
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

        throw new IllegalArgumentException("Fecha inválida: " + value);
    }

    private BigDecimal parseAmount(String value) {
        var clean = normalizer.cleanRaw(value)
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

        if (clean.contains(",") && clean.contains(".")) {
            var lastComma = clean.lastIndexOf(',');
            var lastDot = clean.lastIndexOf('.');

            if (lastComma > lastDot) {
                clean = clean.replace(".", "").replace(",", ".");
            } else {
                clean = clean.replace(",", "");
            }
        } else if (clean.contains(",")) {
            clean = clean.replace(".", "").replace(",", ".");
        }

        var amount = new BigDecimal(clean);
        return negativeByParentheses ? amount.negate() : amount;
    }

    private LocalDate resolveBudgetDate(LocalDate realDate, Integer year, Integer month) {
        return LocalDate.of(
                year != null ? year : realDate.getYear(),
                month != null ? month : realDate.getMonthValue(),
                1
        );
    }

    private Map<String, String> rawMap(
            List<String> headers,
            List<String> values,
            String detectedFormat,
            int rowNumber
    ) {
        var raw = new LinkedHashMap<String, String>();

        if (headers != null) {
            for (int i = 0; i < headers.size(); i++) {
                raw.put(headers.get(i), values != null && i < values.size() ? normalizer.cleanRaw(values.get(i)) : "");
            }
        }

        raw.put("_detectedFormat", detectedFormat);
        raw.put("_sheetName", detectedFormat);
        raw.put("_rowNumber", String.valueOf(rowNumber));

        return raw;
    }

    private RowStatus toRowStatus(TransactionCategorySuggestionService.Status status) {
        if (status == null) {
            return RowStatus.NEEDS_CATEGORY;
        }

        return switch (status) {
            case READY -> RowStatus.READY;
            case NEEDS_CATEGORY, NO_SUGGESTION -> RowStatus.NEEDS_CATEGORY;
            case SKIPPED -> RowStatus.SKIPPED;
        };
    }

    private Confidence toImportConfidence(TransactionCategorySuggestionService.Confidence confidence) {
        if (confidence == null) {
            return Confidence.LOW;
        }

        return switch (confidence) {
            case HIGH -> Confidence.HIGH;
            case MEDIUM -> Confidence.MEDIUM;
            case LOW -> Confidence.LOW;
        };
    }

    private String joinUseful(String... values) {
        var parts = new ArrayList<String>();

        if (values != null) {
            for (var value : values) {
                var clean = normalizer.cleanRaw(value);

                if (!clean.isBlank() && !parts.contains(clean)) {
                    parts.add(clean);
                }
            }
        }

        return String.join(" | ", parts);
    }

    private String firstNonBlank(String... values) {
        if (values == null) {
            return "";
        }

        for (var value : values) {
            if (value != null && !value.trim().isBlank()) {
                return value.trim();
            }
        }

        return "";
    }

    private <T> T firstNonNull(T first, T second) {
        return first != null ? first : second;
    }
}