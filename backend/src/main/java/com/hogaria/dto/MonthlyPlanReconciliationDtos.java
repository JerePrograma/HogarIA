package com.hogaria.dto;

import com.hogaria.entity.MonthlyPlanItem;
import com.hogaria.entity.MonthlyPlanTransactionMatch;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public class MonthlyPlanReconciliationDtos {
  public record TransactionMatch(UUID id, UUID monthlyPlanItemId, UUID moneyTransactionId, BigDecimal matchedAmount,
                                 MonthlyPlanTransactionMatch.MatchType matchType, MonthlyPlanTransactionMatch.Confidence confidence) {}
  public record PlanItemReconciliation(UUID itemId, String title, MonthlyPlanItem.Type type,
                                       LocalDate expectedDate, Integer periodYear, Integer periodMonth,
                                       UUID accountId, UUID categoryId, MonthlyPlanItem.Status status,
                                       BigDecimal plannedAmount,
                                       BigDecimal matchedAmount, BigDecimal remainingAmount, String executionStatus,
                                       List<TransactionMatch> matches) {}
  public record UnplannedTransaction(UUID transactionId, LocalDate realDate, LocalDate budgetDate, String description, UUID accountId, UUID categoryId,
                                     String movementType, BigDecimal amount, String status, String classificationStatus, String operationalKind) {}
  public record SuggestedPlanTransactionMatch(UUID itemId, String itemTitle, MonthlyPlanItem.Type itemType,
                                              LocalDate expectedDate, Integer periodYear, Integer periodMonth,
                                              BigDecimal plannedAmount, UUID itemAccountId, UUID itemCategoryId,
                                              UUID transactionId, String transactionDescription, LocalDate transactionRealDate,
                                              LocalDate transactionBudgetDate, String transactionMovementType,
                                              BigDecimal transactionAmount, UUID transactionAccountId, UUID transactionCategoryId,
                                              BigDecimal suggestedAmount, BigDecimal difference, String confidence, List<String> reasons) {}
  public record MonthlyPlanReconciliationSummary(BigDecimal plannedTotal, BigDecimal matchedTotal, BigDecimal pendingTotal,
                                                 BigDecimal unplannedTransactionsTotal, int unplannedCount, int suggestedCount,
                                                 List<UnplannedTransaction> unplannedTransactions,
                                                 List<SuggestedPlanTransactionMatch> suggestedMatches,
                                                 List<PlanItemReconciliation> planItems) {}
  public record ConfirmPlanTransactionMatchPayload(UUID monthlyPlanItemId, UUID moneyTransactionId, BigDecimal matchedAmount,
                                                   MonthlyPlanTransactionMatch.MatchType matchType,
                                                   MonthlyPlanTransactionMatch.Confidence confidence) {}
}
