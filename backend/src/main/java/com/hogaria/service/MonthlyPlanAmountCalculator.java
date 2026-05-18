package com.hogaria.service;

import com.hogaria.entity.MonthlyPlanItem;
import java.math.BigDecimal;
import java.math.RoundingMode;
import org.springframework.stereotype.Component;

@Component
public class MonthlyPlanAmountCalculator {
  public AmountBreakdown calculate(MonthlyPlanItem item) {
    BigDecimal grossMin = item.getAmount() != null ? item.getAmount() : defaulted(item.getMinAmount());
    BigDecimal grossMax = item.getAmount() != null ? item.getAmount() : (item.getMaxAmount() != null ? item.getMaxAmount() : defaulted(item.getMinAmount()));
    BigDecimal percent = item.getExpectedRecoveryPercent() == null
        ? BigDecimal.ZERO
        : item.getExpectedRecoveryPercent().divide(new BigDecimal("100"), 6, RoundingMode.HALF_UP);
    BigDecimal recoveryMin = item.getExpectedRecoveryAmount() != null ? item.getExpectedRecoveryAmount() : grossMin.multiply(percent);
    BigDecimal recoveryMax = item.getExpectedRecoveryAmount() != null ? item.getExpectedRecoveryAmount() : grossMax.multiply(percent);
    return new AmountBreakdown(grossMin, grossMax, recoveryMin, recoveryMax, grossMin.subtract(recoveryMax), grossMax.subtract(recoveryMin));
  }

  public BigDecimal plannedAmountForReconciliation(MonthlyPlanItem item) {
    if (item.getStatus() == MonthlyPlanItem.Status.CANCELLED || item.getType() == MonthlyPlanItem.Type.TODO) {
      return BigDecimal.ZERO;
    }
    var b = calculate(item);
    if (item.getAmount() != null) {
      return b.netMin();
    }
    return switch (item.getType()) {
      case INCOME, RECOVERY -> b.netMin();
      case EXPENSE, DEBT, SAVING, TRANSFER -> b.netMax();
      case TODO -> BigDecimal.ZERO;
    };
  }

  private BigDecimal defaulted(BigDecimal value) { return value == null ? BigDecimal.ZERO : value; }

  public record AmountBreakdown(BigDecimal grossMin, BigDecimal grossMax, BigDecimal recoveryMin, BigDecimal recoveryMax, BigDecimal netMin, BigDecimal netMax) {}
}
