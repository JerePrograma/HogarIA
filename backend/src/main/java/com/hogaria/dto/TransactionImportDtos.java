package com.hogaria.dto;

import com.hogaria.entity.MoneyTransaction;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;

public class TransactionImportDtos {
  public enum TransactionImportSource { BANCO_PROVINCIA, MERCADO_PAGO }
  public enum Confidence { HIGH, MEDIUM, LOW, NONE }
  public enum RowStatus {
    READY,
    NEEDS_CATEGORY,
    DUPLICATE,
    DUPLICATE_EXACT,
    POSSIBLE_INTERNAL_TRANSFER,
    INTERNAL_TRANSFER_MATCHED,
    POSSIBLE_CROSS_SOURCE_DUPLICATE,
    SKIPPED,
    ERROR
  }

  public record TransactionImportPreviewRow(Integer rowNumber, TransactionImportSource source, String sourceOperationId, String sourceHash,
                                            LocalDate realDate, LocalDate budgetDate, String rawDescription, String normalizedDescription,
                                            BigDecimal rawSignedAmount, BigDecimal amount, String currency, MoneyTransaction.MovementType movementType,
                                            UUID suggestedCategoryId, String suggestedCategoryName, Confidence confidence, RowStatus status,
                                            String skipReason, String rawPayload,
                                            UUID matchedTransactionId, UUID matchedAccountId, UUID matchedCurrentCategoryId,
                                            String matchedCurrentCategoryName, String matchType, String matchReason) {}

  public record TransactionImportPreviewResponse(UUID batchId, TransactionImportSource source, UUID accountId, int totalRows, int importableRows,
                                                 int duplicateRows, int skippedRows, int unresolvedRows, List<TransactionImportPreviewRow> rows,
                                                 List<String> warnings, List<String> errors) {}

  public record TransactionImportCommitRow(@NotNull Integer rowNumber, UUID categoryId, UUID accountId, MoneyTransaction.MovementType movementType,
                                           BigDecimal amount, RowStatus status, String description) {}

  public record TransactionImportCommitRequest(@NotNull List<TransactionImportCommitRow> rows, boolean createMissingFallbackCategory,
                                               boolean skipDuplicates) {}

  public record TransactionImportCommitResponse(int createdCount, int skippedCount, int duplicateCount, int failedCount,
                                                List<UUID> createdTransactionIds, List<String> warnings, List<String> errors) {}
}
