package com.hogaria.dto; import com.hogaria.entity.Category; import jakarta.validation.constraints.*; import java.math.BigDecimal; import java.time.LocalDateTime; import java.util.*;
public class BudgetDtos {
public record BudgetYearCreateRequest(@NotNull @Min(2000) @Max(2100) Integer year,@DecimalMin("0.00") BigDecimal targetIncome,@DecimalMin("0.00") BigDecimal targetSaving,String notes){}
public record BudgetYearUpdateRequest(@DecimalMin("0.00") BigDecimal targetIncome,@DecimalMin("0.00") BigDecimal targetSaving,String notes){}
public record BudgetYearResponse(UUID id,UUID profileId,Integer year,BigDecimal targetIncome,BigDecimal targetSaving,String notes,LocalDateTime createdAt,LocalDateTime updatedAt){}
public record BudgetMonthCreateRequest(@NotNull @Min(1) @Max(12) Integer month,String notes){}
public record BudgetMonthUpdateRequest(String notes){}
public record BudgetCategoryItemUpsertRequest(@NotNull UUID categoryId,@NotNull @DecimalMin("0.00") BigDecimal budgetAmount){}
public record BudgetCategoryItemResponse(UUID id,UUID budgetMonthId,UUID categoryId,String categoryName,Category.Type categoryType,BigDecimal budgetAmount,LocalDateTime createdAt,LocalDateTime updatedAt){}
public record BudgetMonthResponse(UUID id,UUID budgetYearId,Integer month,String notes,List<BudgetCategoryItemResponse> items,LocalDateTime createdAt,LocalDateTime updatedAt){}
public enum BudgetStatus {OK,WARNING,EXCEEDED}
public record BudgetComparisonItemResponse(UUID categoryId,String categoryName,Category.Type categoryType,BigDecimal budgetAmount,BigDecimal realAmount,BigDecimal difference,BigDecimal percentUsed,BudgetStatus status){}
public record BudgetComparisonResponse(UUID profileId,Integer year,Integer month,BigDecimal totalBudget,BigDecimal totalReal,BigDecimal totalDifference,List<BudgetComparisonItemResponse> items){}
}
