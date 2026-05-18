package com.hogaria.dto;

import com.hogaria.entity.MoneyTransaction;
import com.hogaria.entity.MonthlyPlanItem;
import com.hogaria.entity.MonthlyPlanTransactionMatch;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public class MonthlyPlanReconciliationDtos {

  public enum PlanExecutionStatus {
    UNPRICED,
    NOT_EXECUTED,
    PARTIALLY_EXECUTED,
    EXECUTED,
    OVER_EXECUTED,
    CANCELLED,
    NOT_APPLICABLE
  }

  public enum SuggestedMatchConfidence {
    HIGH,
    MEDIUM,
    LOW
  }

  public record MonthlyPlanReconciliationSummaryResponse(
      UUID profileId,
      Integer year,
      Integer month,
      BigDecimal plannedTotal,
      BigDecimal matchedTotal,
      BigDecimal remainingTotal,
      BigDecimal unplannedTransactionsTotal,
      Integer plannedItemsCount,
      Integer matchedItemsCount,
      Integer partialItemsCount,
      Integer overExecutedItemsCount,
      Integer unplannedTransactionsCount,
      List<PlanItemReconciliationResponse> items,
      List<UnplannedTransactionResponse> unplannedTransactions,
      List<SuggestedPlanTransactionMatchResponse> suggestedMatches
  ) {}

  public record PlanItemReconciliationResponse(
      UUID itemId,
      String title,
      MonthlyPlanItem.Type type,
      MonthlyPlanItem.Status status,
      UUID categoryId,
      UUID accountId,
      LocalDate expectedDate,
      BigDecimal plannedAmount,
      BigDecimal matchedAmount,
      BigDecimal remainingAmount,
      PlanExecutionStatus executionStatus,
      List<TransactionMatchResponse> matches
  ) {}

  public record TransactionMatchResponse(
      UUID matchId,
      UUID transactionId,
      BigDecimal matchedAmount,
      MonthlyPlanTransactionMatch.MatchType matchType,
      MonthlyPlanTransactionMatch.MatchConfidence confidence,
      String note
  ) {}

  public record UnplannedTransactionResponse(
      UUID transactionId,
      UUID accountId,
      UUID categoryId,
      MoneyTransaction.MovementType movementType,
      LocalDate realDate,
      LocalDate budgetDate,
      BigDecimal amount,
      String currency,
      String description,
      MoneyTransaction.Origin origin,
      MoneyTransaction.Status status
  ) {}

  public record SuggestedPlanTransactionMatchResponse(
      UUID itemId,
      UUID transactionId,
      BigDecimal suggestedAmount,
      SuggestedMatchConfidence confidence,
      Integer score,
      List<String> reasons
  ) {}

  public record ConfirmPlanTransactionMatchRequest(
      @NotNull UUID itemId,
      @NotNull UUID transactionId,
      @NotNull @DecimalMin("0.01") BigDecimal matchedAmount,
      MonthlyPlanTransactionMatch.MatchType matchType,
      MonthlyPlanTransactionMatch.MatchConfidence confidence,
      String note
  ) {}
}
