package com.hogaria.service.transactionimport;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hogaria.dto.TransactionImportDtos.Confidence;
import com.hogaria.dto.TransactionImportDtos.RowStatus;
import com.hogaria.dto.TransactionImportDtos.TransactionImportSource;
import com.hogaria.entity.ImportTargetEntity;
import com.hogaria.entity.MoneyTransaction;
import com.hogaria.exception.BadRequestException;
import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.springframework.stereotype.Component;

@Component
public class BancoProvinciaMovimientosExcelParser extends ExcelImportParserSupport implements TransactionExcelMovementParser {

  private final TransactionImportRuleClassifier classifier;
  private final ImportSourceHashService hashService;
  private final ObjectMapper objectMapper;
  private final MerchantExtractor merchantExtractor;
  private final CounterpartyExtractor counterpartyExtractor;

  public BancoProvinciaMovimientosExcelParser(
          ImportTextNormalizer normalizer,
          TransactionImportRuleClassifier classifier,
          ImportSourceHashService hashService,
          ObjectMapper objectMapper
  ) {
    super(normalizer);
    this.classifier = classifier;
    this.hashService = hashService;
    this.objectMapper = objectMapper;
    this.merchantExtractor = new MerchantExtractor(normalizer);
    this.counterpartyExtractor = new CounterpartyExtractor(normalizer);
  }

  @Override
  public ExcelImportTemplate template() {
    return ExcelImportTemplate.BANCO_PROVINCIA_MOVIMIENTOS;
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
      throw new BadRequestException("No se pudo parsear Banco Provincia movimientos: " + ex.getMessage());
    }

    if (rows.isEmpty()) {
      throw new BadRequestException("No se detectaron movimientos en Banco Provincia.");
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
    var sequence = value(values, detection.headerIndexes(), "Número Secuencia");
    var dateText = value(values, detection.headerIndexes(), "Fecha");
    var amountText = value(values, detection.headerIndexes(), "Importe");
    var balanceText = value(values, detection.headerIndexes(), "Saldo");
    var description = value(values, detection.headerIndexes(), "Descripción");
    var extendedDescription = value(values, detection.headerIndexes(), "Descripción Extendida");
    var merchantName = value(values, detection.headerIndexes(), "Nombre Comercio");

    if (dateText.isBlank() && amountText.isBlank() && description.isBlank()) {
      return null;
    }

    var realDate = parseBancoProvinciaDate(dateText);
    var signedAmount = parseAmount(amountText);

    if (signedAmount.signum() == 0) {
      return null;
    }

    var merchant = merchantExtractor.fromBancoProvincia(description, extendedDescription, merchantName);
    var counterparty = counterpartyExtractor.fromBancoProvincia(extendedDescription, merchant);
    var normalizedDescription = normalizer.normalize(joinUseful(description, extendedDescription, merchant.raw()));
    var paymentChannel = classifier.inferBancoProvinciaPaymentChannel(description, extendedDescription);
    var sourceHash = hashService.fromFallback(
            profileId,
            accountId,
            TransactionImportSource.BANCO_PROVINCIA,
            sequence + "|" + realDate + "|" + signedAmount
    );
    var raw = rawMap(detection.headers(), values, detection.displayName(), detection.sheetName(), rowNumber);
    raw.put("_signedAmount", signedAmount.toPlainString());
    raw.put("_sourceHash", sourceHash);
    raw.put("_merchantRaw", merchant.raw() == null ? "" : merchant.raw());
    raw.put("_merchantNormalized", merchant.normalized() == null ? "" : merchant.normalized());
    raw.put("_counterparty", counterparty.raw() == null ? "" : counterparty.raw());
    raw.put("_counterpartyDocumentHash", counterparty.documentHash() == null ? "" : counterparty.documentHash());

    var normalizedMovement = new NormalizedImportMovement(
            TransactionImportSource.BANCO_PROVINCIA,
            sequence.isBlank() ? null : sequence,
            sourceHash,
            realDate,
            realDate.atStartOfDay(),
            signedAmount,
            DEFAULT_CURRENCY,
            description,
            normalizedDescription,
            extendedDescription,
            merchant.raw(),
            merchant.normalized(),
            counterparty.raw(),
            counterparty.documentHash(),
            paymentChannel,
            signedAmount.signum() < 0 ? ImportOperationKind.DEBIT : ImportOperationKind.CREDIT,
            description,
            null,
            null,
            null,
            null,
            null,
            counterparty.raw(),
            objectMapper.writeValueAsString(raw)
    );
    var classification = classifier.classify(normalizedMovement);

    return ImportedMovementCandidate.builder()
            .source(TransactionImportSource.BANCO_PROVINCIA)
            .detectedFormat(detection.displayName())
            .sourceOperationId(sequence.isBlank() ? null : sequence)
            .sourceHash(sourceHash)
            .externalSequence(sequence.isBlank() ? null : sequence)
            .realDate(realDate)
            .budgetDate(realDate)
            .operationDateTime(realDate.atStartOfDay())
            .operationDateTimePrecision(MoneyTransaction.OperationDateTimePrecision.DATE_ONLY)
            .signedAmount(signedAmount)
            .amountAbs(signedAmount.abs())
            .currency(DEFAULT_CURRENCY)
            .rawDescription(description)
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
            .rawJson(normalizedMovement.rawPayload())
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
              .source(TransactionImportSource.BANCO_PROVINCIA)
              .detectedFormat(detection.displayName())
              .rawDescription("")
              .normalizedDescription("")
              .currency(DEFAULT_CURRENCY)
              .confidence(Confidence.NONE)
              .rawJson(objectMapper.writeValueAsString(rawMap(detection.headers(), values, detection.displayName(), detection.sheetName(), rowNumber)))
              .rowNumber(rowNumber)
              .sheetName(detection.sheetName())
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
}
