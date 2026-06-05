package com.hogaria.dto;

import com.hogaria.entity.MoneyTransaction;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;

public class TransactionImportDtos {
  public enum TransactionImportSource {
    AUTO,
    BANCO_PROVINCIA,
    MERCADO_PAGO,
    TARJETA_CREDITO_GENERICA,
    DEUDAS_TARJETA_GENERICA
  }
  public enum Confidence { HIGH, MEDIUM, LOW, NONE }
  public enum RowStatus {
    READY,
    NEEDS_CATEGORY,
    DUPLICATE,
    DUPLICATE_EXACT,
    POSSIBLE_INTERNAL_TRANSFER,
    INTERNAL_TRANSFER_MATCHED,
    POSSIBLE_CROSS_SOURCE_DUPLICATE,
    REVIEW,
    SKIPPED,
    ERROR
  }

  public record TransactionImportPreviewRow(Integer rowNumber, TransactionImportSource source, String sourceOperationId, String sourceHash,
                                            LocalDate realDate, LocalDate budgetDate, String rawDescription, String normalizedDescription,
                                            BigDecimal rawSignedAmount, BigDecimal amount, String currency, MoneyTransaction.MovementType movementType,
                                            UUID suggestedCategoryId, String suggestedCategoryName, Confidence confidence, RowStatus status,
                                            String skipReason, String rawPayload,
                                            UUID matchedTransactionId, UUID matchedAccountId, UUID matchedCurrentCategoryId,
                                            String matchedCurrentCategoryName, String matchType, String matchReason,
                                            String detectedFormat, java.time.LocalDateTime operationDateTime,
                                            MoneyTransaction.OperationDateTimePrecision operationDateTimePrecision,
                                            String extendedDescription, String merchantName, String counterparty,
                                            String counterpartyDocumentHash,
                                            MoneyTransaction.PaymentChannel paymentChannel, MoneyTransaction.BalanceImpact balanceImpact,
                                            MoneyTransaction.ClassificationStatus classificationStatus, String classificationReason,
                                            String classificationLayer, String classificationMatchedField,
                                            String classificationMatchedValue, String classificationExplanationJson,
                                            String categorySuggestionKey, String externalSequence, String sheetName,
                                            com.hogaria.entity.ImportTargetEntity targetEntity, String rawJson) {
    public TransactionImportPreviewRow(Integer rowNumber, TransactionImportSource source, String sourceOperationId, String sourceHash,
                                       LocalDate realDate, LocalDate budgetDate, String rawDescription, String normalizedDescription,
                                       BigDecimal rawSignedAmount, BigDecimal amount, String currency, MoneyTransaction.MovementType movementType,
                                       UUID suggestedCategoryId, String suggestedCategoryName, Confidence confidence, RowStatus status,
                                       String skipReason, String rawPayload,
                                       UUID matchedTransactionId, UUID matchedAccountId, UUID matchedCurrentCategoryId,
                                       String matchedCurrentCategoryName, String matchType, String matchReason) {
      this(
              rowNumber,
              source,
              sourceOperationId,
              sourceHash,
              realDate,
              budgetDate,
              rawDescription,
              normalizedDescription,
              rawSignedAmount,
              amount,
              currency,
              movementType,
              suggestedCategoryId,
              suggestedCategoryName,
              confidence,
              status,
              skipReason,
              rawPayload,
              matchedTransactionId,
              matchedAccountId,
              matchedCurrentCategoryId,
              matchedCurrentCategoryName,
              matchType,
              matchReason,
              null,
              null,
              null,
              null,
              null,
              null,
              null,
              null,
              null,
              null,
              null,
              null,
              null,
              null,
              null,
              null,
              null,
              null,
              null,
              null
      );
    }
  }

  public record TransactionImportPreviewResponse(UUID batchId, TransactionImportSource source, UUID accountId, int totalRows, int importableRows,
                                                 int duplicateRows, int skippedRows, int unresolvedRows, List<TransactionImportPreviewRow> rows,
                                                 List<String> warnings, List<String> errors,
                                                 String detectedFormat, int suggestedCategoryRows, int needsCategoryRows,
                                                 int reviewRows, int errorRows, int internalTransferRows,
                                                 int technicalNeutralRows, int blockedRows,
                                                 int blockingCategoryRows, int crossSourceRiskRows) {
    public TransactionImportPreviewResponse(UUID batchId, TransactionImportSource source, UUID accountId, int totalRows, int importableRows,
                                            int duplicateRows, int skippedRows, int unresolvedRows, List<TransactionImportPreviewRow> rows,
                                            List<String> warnings, List<String> errors) {
      this(
              batchId,
              source,
              accountId,
              totalRows,
              importableRows,
              duplicateRows,
              skippedRows,
              unresolvedRows,
              rows,
              warnings,
              errors,
              null,
              rows == null ? 0 : (int) rows.stream().filter(row -> row.suggestedCategoryId() != null).count(),
              unresolvedRows,
              rows == null ? 0 : (int) rows.stream().filter(row -> row.status() == RowStatus.REVIEW).count(),
              rows == null ? 0 : (int) rows.stream().filter(row -> row.status() == RowStatus.ERROR).count(),
              0,
              0,
              Math.max(0, totalRows - importableRows),
              unresolvedRows,
              0
      );
    }
  }

  public record TransactionImportCommitRow(@NotNull Integer rowNumber, UUID categoryId, UUID accountId, MoneyTransaction.MovementType movementType,
                                           BigDecimal amount, RowStatus status, String description) {}

  public record TransactionImportCommitRequest(@NotNull List<TransactionImportCommitRow> rows, boolean createMissingFallbackCategory,
                                               boolean skipDuplicates) {}

  public record TransactionImportCommitResponse(int createdCount, int skippedCount, int duplicateCount, int failedCount,
                                                List<UUID> createdTransactionIds, List<String> warnings, List<String> errors) {}
}
