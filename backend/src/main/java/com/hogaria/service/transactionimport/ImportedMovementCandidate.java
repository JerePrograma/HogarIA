package com.hogaria.service.transactionimport;

import com.hogaria.dto.TransactionImportDtos.Confidence;
import com.hogaria.dto.TransactionImportDtos.RowStatus;
import com.hogaria.dto.TransactionImportDtos.TransactionImportSource;
import com.hogaria.entity.ImportTargetEntity;
import com.hogaria.entity.MoneyTransaction;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record ImportedMovementCandidate(
        TransactionImportSource source,
        String detectedFormat,
        String sourceOperationId,
        String sourceHash,
        String externalSequence,
        LocalDate realDate,
        LocalDate budgetDate,
        LocalDateTime operationDateTime,
        MoneyTransaction.OperationDateTimePrecision operationDateTimePrecision,
        BigDecimal signedAmount,
        BigDecimal amountAbs,
        String currency,
        String rawDescription,
        String normalizedDescription,
        String extendedDescription,
        String merchantName,
        String counterparty,
        String counterpartyDocumentHash,
        MoneyTransaction.PaymentChannel paymentChannel,
        MoneyTransaction.MovementType movementType,
        MoneyTransaction.BalanceImpact balanceImpact,
        String categorySuggestionKey,
        String categorySuggestionName,
        MoneyTransaction.ClassificationStatus classificationStatus,
        String classificationReason,
        ClassificationLayer classificationLayer,
        String classificationMatchedField,
        String classificationMatchedValue,
        String classificationExplanationJson,
        Confidence confidence,
        String rawJson,
        Integer rowNumber,
        String sheetName,
        ImportTargetEntity targetEntity,
        RowStatus rowStatus,
        String warning
) {
  public static Builder builder() {
    return new Builder();
  }

  public static class Builder {
    private TransactionImportSource source;
    private String detectedFormat;
    private String sourceOperationId;
    private String sourceHash;
    private String externalSequence;
    private LocalDate realDate;
    private LocalDate budgetDate;
    private LocalDateTime operationDateTime;
    private MoneyTransaction.OperationDateTimePrecision operationDateTimePrecision;
    private BigDecimal signedAmount;
    private BigDecimal amountAbs;
    private String currency;
    private String rawDescription;
    private String normalizedDescription;
    private String extendedDescription;
    private String merchantName;
    private String counterparty;
    private String counterpartyDocumentHash;
    private MoneyTransaction.PaymentChannel paymentChannel;
    private MoneyTransaction.MovementType movementType;
    private MoneyTransaction.BalanceImpact balanceImpact;
    private String categorySuggestionKey;
    private String categorySuggestionName;
    private MoneyTransaction.ClassificationStatus classificationStatus;
    private String classificationReason;
    private ClassificationLayer classificationLayer;
    private String classificationMatchedField;
    private String classificationMatchedValue;
    private String classificationExplanationJson;
    private Confidence confidence;
    private String rawJson;
    private Integer rowNumber;
    private String sheetName;
    private ImportTargetEntity targetEntity;
    private RowStatus rowStatus;
    private String warning;

    public Builder source(TransactionImportSource source) { this.source = source; return this; }
    public Builder detectedFormat(String detectedFormat) { this.detectedFormat = detectedFormat; return this; }
    public Builder sourceOperationId(String sourceOperationId) { this.sourceOperationId = sourceOperationId; return this; }
    public Builder sourceHash(String sourceHash) { this.sourceHash = sourceHash; return this; }
    public Builder externalSequence(String externalSequence) { this.externalSequence = externalSequence; return this; }
    public Builder realDate(LocalDate realDate) { this.realDate = realDate; return this; }
    public Builder budgetDate(LocalDate budgetDate) { this.budgetDate = budgetDate; return this; }
    public Builder operationDateTime(LocalDateTime operationDateTime) { this.operationDateTime = operationDateTime; return this; }
    public Builder operationDateTimePrecision(MoneyTransaction.OperationDateTimePrecision operationDateTimePrecision) { this.operationDateTimePrecision = operationDateTimePrecision; return this; }
    public Builder signedAmount(BigDecimal signedAmount) { this.signedAmount = signedAmount; return this; }
    public Builder amountAbs(BigDecimal amountAbs) { this.amountAbs = amountAbs; return this; }
    public Builder currency(String currency) { this.currency = currency; return this; }
    public Builder rawDescription(String rawDescription) { this.rawDescription = rawDescription; return this; }
    public Builder normalizedDescription(String normalizedDescription) { this.normalizedDescription = normalizedDescription; return this; }
    public Builder extendedDescription(String extendedDescription) { this.extendedDescription = extendedDescription; return this; }
    public Builder merchantName(String merchantName) { this.merchantName = merchantName; return this; }
    public Builder counterparty(String counterparty) { this.counterparty = counterparty; return this; }
    public Builder counterpartyDocumentHash(String counterpartyDocumentHash) { this.counterpartyDocumentHash = counterpartyDocumentHash; return this; }
    public Builder paymentChannel(MoneyTransaction.PaymentChannel paymentChannel) { this.paymentChannel = paymentChannel; return this; }
    public Builder movementType(MoneyTransaction.MovementType movementType) { this.movementType = movementType; return this; }
    public Builder balanceImpact(MoneyTransaction.BalanceImpact balanceImpact) { this.balanceImpact = balanceImpact; return this; }
    public Builder categorySuggestionKey(String categorySuggestionKey) { this.categorySuggestionKey = categorySuggestionKey; return this; }
    public Builder categorySuggestionName(String categorySuggestionName) { this.categorySuggestionName = categorySuggestionName; return this; }
    public Builder classificationStatus(MoneyTransaction.ClassificationStatus classificationStatus) { this.classificationStatus = classificationStatus; return this; }
    public Builder classificationReason(String classificationReason) { this.classificationReason = classificationReason; return this; }
    public Builder classificationLayer(ClassificationLayer classificationLayer) { this.classificationLayer = classificationLayer; return this; }
    public Builder classificationMatchedField(String classificationMatchedField) { this.classificationMatchedField = classificationMatchedField; return this; }
    public Builder classificationMatchedValue(String classificationMatchedValue) { this.classificationMatchedValue = classificationMatchedValue; return this; }
    public Builder classificationExplanationJson(String classificationExplanationJson) { this.classificationExplanationJson = classificationExplanationJson; return this; }
    public Builder confidence(Confidence confidence) { this.confidence = confidence; return this; }
    public Builder rawJson(String rawJson) { this.rawJson = rawJson; return this; }
    public Builder rowNumber(Integer rowNumber) { this.rowNumber = rowNumber; return this; }
    public Builder sheetName(String sheetName) { this.sheetName = sheetName; return this; }
    public Builder targetEntity(ImportTargetEntity targetEntity) { this.targetEntity = targetEntity; return this; }
    public Builder rowStatus(RowStatus rowStatus) { this.rowStatus = rowStatus; return this; }
    public Builder warning(String warning) { this.warning = warning; return this; }

    public ImportedMovementCandidate build() {
      return new ImportedMovementCandidate(
              source,
              detectedFormat,
              sourceOperationId,
              sourceHash,
              externalSequence,
              realDate,
              budgetDate,
              operationDateTime,
              operationDateTimePrecision,
              signedAmount,
              amountAbs,
              currency,
              rawDescription,
              normalizedDescription,
              extendedDescription,
              merchantName,
              counterparty,
              counterpartyDocumentHash,
              paymentChannel,
              movementType,
              balanceImpact,
              categorySuggestionKey,
              categorySuggestionName,
              classificationStatus,
              classificationReason,
              classificationLayer,
              classificationMatchedField,
              classificationMatchedValue,
              classificationExplanationJson,
              confidence,
              rawJson,
              rowNumber,
              sheetName,
              targetEntity,
              rowStatus,
              warning
      );
    }
  }
}
