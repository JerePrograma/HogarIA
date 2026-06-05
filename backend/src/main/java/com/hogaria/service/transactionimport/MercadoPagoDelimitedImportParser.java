package com.hogaria.service.transactionimport;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hogaria.dto.TransactionImportDtos.Confidence;
import com.hogaria.dto.TransactionImportDtos.RowStatus;
import com.hogaria.dto.TransactionImportDtos.TransactionImportPreviewRow;
import com.hogaria.dto.TransactionImportDtos.TransactionImportSource;
import com.hogaria.entity.ImportTargetEntity;
import com.hogaria.entity.MoneyTransaction;
import com.hogaria.exception.BadRequestException;
import java.math.BigDecimal;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.time.DateTimeException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class MercadoPagoDelimitedImportParser {

    private static final ZoneId IMPORT_ZONE = ZoneId.of("America/Argentina/Buenos_Aires");
    private static final String DETECTED_FORMAT = "MERCADO_PAGO_DELIMITED";
    private static final String SHEET_NAME = "mercado_pago_delimited";

    private final ImportTextNormalizer normalizer;
    private final TransactionImportRuleClassifier classifier;
    private final ImportSourceHashService hashService;
    private final TransactionImportPreviewMapper previewMapper;
    private final ObjectMapper objectMapper;
    private final PaymentChannelDetector paymentChannelDetector;
    private final MerchantExtractor merchantExtractor;
    private final CounterpartyExtractor counterpartyExtractor;

    public MercadoPagoDelimitedImportParser(
            ImportTextNormalizer normalizer,
            TransactionImportRuleClassifier classifier,
            ImportSourceHashService hashService,
            TransactionImportPreviewMapper previewMapper,
            ObjectMapper objectMapper
    ) {
        this.normalizer = normalizer;
        this.classifier = classifier;
        this.hashService = hashService;
        this.previewMapper = previewMapper;
        this.objectMapper = objectMapper;
        this.paymentChannelDetector = new PaymentChannelDetector(normalizer);
        this.merchantExtractor = new MerchantExtractor(normalizer);
        this.counterpartyExtractor = new CounterpartyExtractor(normalizer);
    }

    public List<TransactionImportPreviewRow> parse(
            byte[] bytes,
            UUID profileId,
            UUID accountId,
            Integer year,
            Integer month
    ) {
        var candidates = parseCandidates(bytes, profileId, accountId, year, month);

        return candidates
                .stream()
                .map(candidate -> previewMapper.toPreviewRow(profileId, candidate))
                .toList();
    }

    private List<ImportedMovementCandidate> parseCandidates(
            byte[] bytes,
            UUID profileId,
            UUID accountId,
            Integer year,
            Integer month
    ) {
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

        var headers = splitMercadoPagoLine(records.get(headerRecordIndex), delimiter);
        var headerIndexes = buildHeaderIndex(headers);

        if (!hasMercadoPagoRequiredHeaders(headerIndexes)) {
            throw new BadRequestException(
                    "El archivo no parece ser un export de MercadoPago válido. "
                            + "Separador detectado: [" + printableDelimiter(delimiter) + "]. "
                            + "Headers detectados: [" + String.join(" | ", headers) + "]."
            );
        }

        var candidates = new ArrayList<ImportedMovementCandidate>();
        int outputRowNumber = 0;

        for (int recordIndex = headerRecordIndex + 1; recordIndex < records.size(); recordIndex++) {
            var rawRecord = records.get(recordIndex);

            if (rawRecord == null || rawRecord.trim().isBlank()) {
                continue;
            }

            var values = splitMercadoPagoLine(rawRecord, delimiter);

            if (isBlankRow(values)) {
                continue;
            }

            try {
                var candidate = parseRow(
                        outputRowNumber + 1,
                        headers,
                        headerIndexes,
                        values,
                        rawRecord,
                        profileId,
                        accountId,
                        year,
                        month
                );

                outputRowNumber++;
                candidates.add(candidate);
            } catch (Exception ex) {
                outputRowNumber++;
                candidates.add(errorRow(outputRowNumber, headers, values, ex.getMessage()));
            }
        }

        if (candidates.isEmpty()) {
            var headerDebug = String.join(" | ", headers);
            var firstDataRow = records.size() > headerRecordIndex + 1
                    ? records.get(headerRecordIndex + 1)
                    : "";

            throw new BadRequestException(
                    "No se detectaron movimientos en el archivo de MercadoPago. "
                            + "Separador detectado: [" + printableDelimiter(delimiter) + "]. "
                            + "Headers detectados: [" + headerDebug + "]. "
                            + "Primera fila de datos: ["
                            + firstDataRow.substring(0, Math.min(firstDataRow.length(), 500))
                            + "]."
            );
        }

        return candidates;
    }

    private ImportedMovementCandidate parseRow(
            int rowNumber,
            List<String> headers,
            Map<String, Integer> headerIndexes,
            List<String> values,
            String rawRecord,
            UUID profileId,
            UUID accountId,
            Integer year,
            Integer month
    ) throws Exception {
        var originDateText = firstHeaderValue(
                values,
                headerIndexes,
                "FECHA DE ORIGEN",
                "FECHA DE APROBACION",
                "FECHA DE APROBACIÓN"
        );

        var amountText = firstHeaderValue(
                values,
                headerIndexes,
                "MONTO NETO DE LA OPERACION QUE IMPACTO TU DINERO",
                "MONTO NETO DE LA OPERACIÓN QUE IMPACTÓ TU DINERO",
                "MONTO NETO DE LA OPERACION QUE IMPACTO EN TU DINERO",
                "MONTO NETO DE LA OPERACIÓN QUE IMPACTÓ EN TU DINERO",
                "MONTO NETO DE LA OPERACION",
                "MONTO NETO DE LA OPERACIÓN",
                "VALOR DE LA COMPRA"
        );

        if (originDateText.isBlank() || amountText.isBlank()) {
            throw new IllegalArgumentException("Faltan fecha o monto neto.");
        }

        var parsedDate = parseMercadoPagoDateTime(originDateText);
        var signedAmount = parseAmount(amountText);

        if (signedAmount.signum() == 0) {
            throw new IllegalArgumentException("Importe cero.");
        }

        var operationId = firstHeaderValue(
                values,
                headerIndexes,
                "ID DE OPERACION EN MERCADO PAGO",
                "ID DE OPERACIÓN EN MERCADO PAGO"
        );

        var reference = firstHeaderValue(
                values,
                headerIndexes,
                "CODIGO DE REFERENCIA",
                "CÓDIGO DE REFERENCIA",
                "CODIGO DE PRODUCTO SKU",
                "CÓDIGO DE PRODUCTO SKU"
        );

        var operationType = firstHeaderValue(values, headerIndexes, "TIPO DE OPERACION", "TIPO DE OPERACIÓN");
        var paymentMethodType = firstHeaderValue(values, headerIndexes, "TIPO DE MEDIO DE PAGO");
        var paymentMethod = firstHeaderValue(values, headerIndexes, "MEDIO DE PAGO");
        var currency = firstNonBlank(firstHeaderValue(values, headerIndexes, "MONEDA"), "ARS").toUpperCase(Locale.ROOT);
        var liquidated = firstHeaderValue(values, headerIndexes, "LIQUIDADO");
        var rawDescription = firstHeaderValue(values, headerIndexes, "DETALLE DE LA VENTA");
        var sku = firstHeaderValue(values, headerIndexes, "CÓDIGO DE PRODUCTO SKU", "CODIGO DE PRODUCTO SKU");
        var operationTags = firstHeaderValue(values, headerIndexes, "OPERATION_TAGS");
        var installments = firstHeaderValue(values, headerIndexes, "CUOTAS");
        var identificationNumber = firstHeaderValue(values, headerIndexes, "NÚMERO DE IDENTIFICACIÓN", "NUMERO DE IDENTIFICACION");
        var purchaseId = firstHeaderValue(values, headerIndexes, "ID DE LA COMPRA");
        var orderId = firstHeaderValue(values, headerIndexes, "ID DE LA ORDEN");
        var payer = firstHeaderValue(values, headerIndexes, "PAGADOR", "NOMBRE DE QUIEN HACE EL PAGO");
        var bank = firstHeaderValue(values, headerIndexes, "BANCO DE ORIGEN");
        var wallet = firstHeaderValue(values, headerIndexes, "BILLETERA VIRTUAL");
        var lastFour = firstHeaderValue(values, headerIndexes, "LAST_FOUR_DIGITS");
        var franchise = firstHeaderValue(values, headerIndexes, "FRANCHISE");

        var paymentChannel = paymentChannelDetector.detectMercadoPago(
                rawDescription,
                operationType,
                paymentMethodType,
                paymentMethod,
                identificationNumber,
                operationTags
        );

        var merchant = merchantExtractor.fromMercadoPago(
                rawDescription,
                identificationNumber,
                operationTags,
                payer
        );

        var counterparty = counterpartyExtractor.fromMercadoPago(
                payer,
                bank,
                wallet,
                identificationNumber
        );

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
        var sourceOperationId = firstNonBlank(operationId, reference, identificationNumber, purchaseId, orderId);

        var sourceHash = sourceOperationId.isBlank()
                ? hashService.fromFallback(
                profileId,
                accountId,
                TransactionImportSource.MERCADO_PAGO,
                parsedDate.realDate() + "|" + signedAmount + "|" + normalizedDescription + "|" + extendedDescription
        )
                : hashService.fromFallback(
                profileId,
                accountId,
                TransactionImportSource.MERCADO_PAGO,
                sourceOperationId + "|" + signedAmount + "|" + parsedDate.realDate()
        );

        var raw = rawMap(headers, values, rowNumber);
        raw.put("_rawRecord", rawRecord);
        raw.put("_signedAmount", signedAmount.toPlainString());
        raw.put("_sourceHash", sourceHash);
        raw.put("_merchantRaw", merchant.raw() == null ? "" : merchant.raw());
        raw.put("_merchantNormalized", merchant.normalized() == null ? "" : merchant.normalized());
        raw.put("_counterparty", counterparty.raw() == null ? "" : counterparty.raw());
        raw.put("_counterpartyDocumentHash", counterparty.documentHash() == null ? "" : counterparty.documentHash());

        var rawJson = objectMapper.writeValueAsString(raw);

        var normalizedMovement = new NormalizedImportMovement(
                TransactionImportSource.MERCADO_PAGO,
                sourceOperationId.isBlank() ? null : sourceOperationId,
                sourceHash,
                parsedDate.realDate(),
                parsedDate.operationDateTime(),
                signedAmount,
                currency,
                rawDescription,
                normalizedDescription,
                extendedDescription,
                merchant.raw(),
                merchant.normalized(),
                counterparty.raw(),
                counterparty.documentHash(),
                paymentChannel,
                signedAmount.signum() < 0 ? ImportOperationKind.DEBIT : ImportOperationKind.CREDIT,
                operationType,
                paymentMethodType,
                paymentMethod,
                liquidated,
                operationTags,
                identificationNumber,
                payer,
                rawJson
        );

        var classification = classifier.classify(normalizedMovement);
        var budgetDate = resolveBudgetDate(parsedDate.realDate(), year, month);

        return ImportedMovementCandidate.builder()
                .source(TransactionImportSource.MERCADO_PAGO)
                .detectedFormat(DETECTED_FORMAT)
                .sourceOperationId(sourceOperationId.isBlank() ? null : sourceOperationId)
                .sourceHash(sourceHash)
                .externalSequence(null)
                .realDate(parsedDate.realDate())
                .budgetDate(budgetDate)
                .operationDateTime(parsedDate.operationDateTime())
                .operationDateTimePrecision(MoneyTransaction.OperationDateTimePrecision.DATE_TIME)
                .signedAmount(signedAmount)
                .amountAbs(signedAmount.abs())
                .currency(currency)
                .rawDescription(firstNonBlank(rawDescription, "Movimiento MercadoPago"))
                .normalizedDescription(normalizedDescription)
                .extendedDescription(extendedDescription)
                .merchantName(merchant.raw())
                .counterparty(counterparty.raw())
                .counterpartyDocumentHash(counterparty.documentHash())
                .paymentChannel(classification.paymentChannel())
                .movementType(classification.movementType())
                .balanceImpact(classification.balanceImpact())
                .categorySuggestionKey(classification.categorySuggestionKey())
                .categorySuggestionName(classification.categorySuggestionName())
                .classificationStatus(classification.classificationStatus())
                .classificationReason(classification.classificationReason())
                .classificationLayer(classification.classificationLayer())
                .classificationMatchedField(classification.matchedField())
                .classificationMatchedValue(classification.matchedValue())
                .classificationExplanationJson(classification.explanationJson())
                .confidence(classification.confidence())
                .rawJson(rawJson)
                .rowNumber(rowNumber)
                .sheetName(SHEET_NAME)
                .targetEntity(targetEntity(classification.movementType()))
                .rowStatus(classification.rowStatus())
                .warning(classification.warning())
                .build();
    }

    private ImportedMovementCandidate errorRow(
            int rowNumber,
            List<String> headers,
            List<String> values,
            String message
    ) {
        try {
            return ImportedMovementCandidate.builder()
                    .source(TransactionImportSource.MERCADO_PAGO)
                    .detectedFormat(DETECTED_FORMAT)
                    .rawDescription("")
                    .normalizedDescription("")
                    .currency("ARS")
                    .confidence(Confidence.NONE)
                    .rawJson(objectMapper.writeValueAsString(rawMap(headers, values, rowNumber)))
                    .rowNumber(rowNumber)
                    .sheetName(SHEET_NAME)
                    .targetEntity(ImportTargetEntity.UNKNOWN)
                    .rowStatus(RowStatus.ERROR)
                    .warning("Fila inválida: " + message)
                    .classificationStatus(MoneyTransaction.ClassificationStatus.REVIEW)
                    .classificationReason("PARSE_ERROR")
                    .build();
        } catch (Exception ex) {
            throw new BadRequestException("No se pudo construir fila de error: " + ex.getMessage());
        }
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
            if (headers.containsKey(normalizer.canonicalHeader(name))) {
                return true;
            }
        }

        return false;
    }

    private String firstHeaderValue(
            List<String> values,
            Map<String, Integer> indexes,
            String... keys
    ) {
        for (var key : keys) {
            var value = value(values, indexes, key);

            if (!value.isBlank()) {
                return value;
            }
        }

        return "";
    }

    private String value(List<String> values, Map<String, Integer> indexes, String key) {
        var index = indexes.get(normalizer.canonicalHeader(key));

        if (index == null || index < 0 || index >= values.size()) {
            return "";
        }

        return normalizer.cleanRaw(values.get(index));
    }

    private Map<String, Integer> buildHeaderIndex(List<String> headers) {
        var indexes = new LinkedHashMap<String, Integer>();

        for (int i = 0; i < headers.size(); i++) {
            indexes.put(normalizer.canonicalHeader(headers.get(i)), i);
        }

        return indexes;
    }

    private Map<String, String> rawMap(List<String> headers, List<String> values, int rowNumber) {
        var raw = new LinkedHashMap<String, String>();

        for (int i = 0; i < headers.size(); i++) {
            raw.put(headers.get(i), i < values.size() ? normalizer.cleanRaw(values.get(i)) : "");
        }

        raw.put("_detectedFormat", DETECTED_FORMAT);
        raw.put("_sheetName", SHEET_NAME);
        raw.put("_rowNumber", String.valueOf(rowNumber));

        return raw;
    }

    private ParsedDateTime parseMercadoPagoDateTime(String value) {
        var clean = normalizer.cleanRaw(value);

        if (clean.isBlank()) {
            throw new IllegalArgumentException("Fecha MercadoPago vacía.");
        }

        try {
            var local = OffsetDateTime.parse(clean)
                    .atZoneSameInstant(IMPORT_ZONE)
                    .toLocalDateTime();

            return new ParsedDateTime(local.toLocalDate(), local);
        } catch (DateTimeParseException ignored) {
        }

        if (clean.length() >= 10 && clean.charAt(4) == '-' && clean.charAt(7) == '-') {
            var localDate = LocalDate.parse(clean.substring(0, 10));

            try {
                var localDateTime = LocalDateTime.parse(
                        clean.substring(0, Math.min(clean.length(), 19)),
                        DateTimeFormatter.ISO_LOCAL_DATE_TIME
                );

                return new ParsedDateTime(localDateTime.toLocalDate(), localDateTime);
            } catch (DateTimeParseException ignored) {
            }

            return new ParsedDateTime(localDate, localDate.atStartOfDay());
        }

        var formatters = List.of(
                DateTimeFormatter.ofPattern("d/M/uuuu"),
                DateTimeFormatter.ofPattern("dd/MM/uuuu"),
                DateTimeFormatter.ofPattern("d-M-uuuu"),
                DateTimeFormatter.ofPattern("dd-MM-uuuu"),
                DateTimeFormatter.ISO_LOCAL_DATE
        );

        for (var formatter : formatters) {
            try {
                var localDate = LocalDate.parse(clean, formatter);
                return new ParsedDateTime(localDate, localDate.atStartOfDay());
            } catch (DateTimeException ignored) {
            }
        }

        throw new IllegalArgumentException("Fecha MercadoPago inválida: " + value);
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
                if (!current.isEmpty()) {
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

        if (!current.isEmpty()) {
            records.add(current.toString());
        }

        return records;
    }

    private List<String> splitMercadoPagoLine(String line, char delimiter) {
        if (line == null || line.isBlank()) {
            return List.of();
        }

        if (delimiter == '\t') {
            var parts = line.split("\t", -1);
            var values = new ArrayList<String>();

            for (var part : parts) {
                values.add(normalizer.cleanRaw(part));
            }

            return values;
        }

        return splitDelimitedLine(line, delimiter);
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
                result.add(normalizer.cleanRaw(current.toString()));
                current.setLength(0);
                continue;
            }

            current.append(currentChar);
        }

        result.add(normalizer.cleanRaw(current.toString()));
        return result;
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

    private String decodeTextFile(byte[] bytes) {
        if (bytes == null || bytes.length == 0) {
            throw new BadRequestException("El archivo está vacío.");
        }

        if (bytes.length >= 3
                && (bytes[0] & 0xFF) == 0xEF
                && (bytes[1] & 0xFF) == 0xBB
                && (bytes[2] & 0xFF) == 0xBF) {
            return new String(bytes, 3, bytes.length - 3, StandardCharsets.UTF_8);
        }

        if (bytes.length >= 2
                && (bytes[0] & 0xFF) == 0xFF
                && (bytes[1] & 0xFF) == 0xFE) {
            return new String(bytes, 2, bytes.length - 2, StandardCharsets.UTF_16LE);
        }

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

        if (nullsInOddPositions > inspected / 8) {
            return new String(bytes, StandardCharsets.UTF_16LE);
        }

        try {
            return decodeStrict(bytes, StandardCharsets.UTF_8);
        } catch (CharacterCodingException ignored) {
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

    private ImportTargetEntity targetEntity(MoneyTransaction.MovementType movementType) {
        if (movementType == null) {
            return ImportTargetEntity.UNKNOWN;
        }

        return switch (movementType) {
            case INCOME -> ImportTargetEntity.INCOME;
            case EXPENSE -> ImportTargetEntity.EXPENSE;
            case SAVING -> ImportTargetEntity.SAVING;
            default -> ImportTargetEntity.UNKNOWN;
        };
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

    private record ParsedDateTime(LocalDate realDate, LocalDateTime operationDateTime) {
    }
}
