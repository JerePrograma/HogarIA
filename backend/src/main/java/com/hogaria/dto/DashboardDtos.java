package com.hogaria.dto;

import com.hogaria.entity.Category;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public class DashboardDtos {
  public record CategorySummaryResponse(UUID categoryId, String categoryName, Category.Type categoryType, BigDecimal totalAmount, BigDecimal percentOfIncome, long movementCount) {}
  public record FiftyThirtyTwentyResponse(BigDecimal fixedPercent, BigDecimal variablePercent, BigDecimal savingPercent) {}
  public record MonthlyBalanceResponse(BigDecimal totalIncome, BigDecimal totalExpenses, BigDecimal savings, BigDecimal balance) {}
  public record DashboardSummaryResponse(MonthlyBalanceResponse monthlyBalance, FiftyThirtyTwentyResponse fiftyThirtyTwenty, BigDecimal fixedExpenses, BigDecimal variableExpenses, String financialHealth, List<CategorySummaryResponse> categoryBreakdown) {}
}
