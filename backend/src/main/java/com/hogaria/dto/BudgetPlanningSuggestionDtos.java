package com.hogaria.dto;

import com.hogaria.entity.Category;
import com.hogaria.entity.MonthlyPlanItem;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public class BudgetPlanningSuggestionDtos {

    public enum SuggestionMode {
        CURRENT_MONTH_ONLY,
        LAST_3_MONTHS_AVERAGE,
        LAST_6_MONTHS_AVERAGE
    }

    public enum SuggestionTarget {
        BUDGET,
        MONTHLY_PLAN,
        BOTH
    }

    public enum SuggestionConfidence {
        HIGH,
        MEDIUM,
        LOW,
        NONE
    }

    public record BudgetPlanningSuggestionPreviewRequest(
            @NotNull @Min(2000) @Max(2100) Integer year,
            @NotNull @Min(1) @Max(12) Integer month,
            @NotNull SuggestionMode mode,
            Boolean includeImportedOnly,
            Boolean includeManual,
            Boolean includeReview,
            @NotNull SuggestionTarget target,
            Boolean nextMonth,
            List<UUID> selectedTransactionIds,
            @DecimalMin("1.00") BigDecimal roundingMultiple
    ) {
    }

    public record BudgetSuggestion(
            UUID categoryId,
            String categoryName,
            Category.Type categoryType,
            BigDecimal realAmount,
            BigDecimal suggestedBudgetAmount,
            Integer transactionCount,
            SuggestionConfidence confidence,
            String reason,
            Boolean outlierDetected,
            Boolean outlierAffectsSuggestedAmount,
            Boolean applyByDefault,
            List<UUID> sourceTransactionIds
    ) {
    }

    public record MonthlyPlanSuggestion(
            String title,
            String description,
            LocalDate expectedDate,
            Integer periodYear,
            Integer periodMonth,
            BigDecimal amount,
            BigDecimal minAmount,
            BigDecimal maxAmount,
            UUID categoryId,
            String categoryName,
            UUID accountId,
            String accountName,
            MonthlyPlanItem.Type type,
            MonthlyPlanItem.Priority priority,
            MonthlyPlanItem.Source source,
            SuggestionConfidence confidence,
            String reason,
            Boolean duplicate,
            Boolean applyByDefault,
            List<UUID> sourceTransactionIds
    ) {
    }

    public record BudgetPlanningSuggestionTotals(
            BigDecimal totalBudgetRealAmount,
            BigDecimal totalSuggestedBudgetAmount,
            Integer budgetSuggestionCount,
            BigDecimal totalMonthlyPlanAmount,
            Integer monthlyPlanSuggestionCount,
            Integer outlierCount
    ) {
    }

    public record BudgetPlanningSuggestionPreviewResponse(
            List<BudgetSuggestion> budgetSuggestions,
            List<MonthlyPlanSuggestion> monthlyPlanSuggestions,
            List<String> warnings,
            BudgetPlanningSuggestionTotals totals
    ) {
    }

    public record BudgetPlanningSuggestionCommitRequest(
            @NotNull @Min(2000) @Max(2100) Integer year,
            @NotNull @Min(1) @Max(12) Integer month,
            @Valid List<ApplyBudgetSuggestion> applyBudgetSuggestions,
            @Valid List<ApplyMonthlyPlanSuggestion> applyMonthlyPlanSuggestions,
            Boolean skipDuplicates,
            Boolean overwriteExistingBudgetItems
    ) {
    }

    public record ApplyBudgetSuggestion(
            UUID categoryId,
            BigDecimal suggestedBudgetAmount,
            Boolean apply,
            Boolean outlierDetected,
            Boolean outlierAffectsSuggestedAmount,
            String reason
    ) {
    }

    public record ApplyMonthlyPlanSuggestion(
            String title,
            String description,
            LocalDate expectedDate,
            Integer periodYear,
            Integer periodMonth,
            BigDecimal amount,
            BigDecimal minAmount,
            BigDecimal maxAmount,
            UUID categoryId,
            UUID accountId,
            MonthlyPlanItem.Type type,
            MonthlyPlanItem.Priority priority,
            MonthlyPlanItem.Source source,
            Boolean apply,
            Boolean duplicate,
            List<UUID> sourceTransactionIds
    ) {
    }

    public record BudgetPlanningSuggestionCommitResponse(
            Integer createdBudgetItems,
            Integer updatedBudgetItems,
            Integer createdMonthlyPlanItems,
            Integer skippedDuplicates,
            List<String> warnings,
            List<String> errors
    ) {
    }
}
