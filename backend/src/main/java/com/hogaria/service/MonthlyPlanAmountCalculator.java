package com.hogaria.service;

import com.hogaria.entity.MonthlyPlanItem;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Optional;

public class MonthlyPlanAmountCalculator {

  private static final BigDecimal ONE_HUNDRED = new BigDecimal("100");
  private static final int RECOVERY_PERCENT_SCALE = 6;

  public MonthlyPlanAmounts calculate(MonthlyPlanItem item) {
    var grossMin = grossMin(item);
    var grossMax = grossMax(item);
    var recoveryMin = recovery(item, grossMin);
    var recoveryMax = recovery(item, grossMax);

    return new MonthlyPlanAmounts(
        grossMin,
        grossMax,
        recoveryMin,
        recoveryMax,
        grossMin.subtract(recoveryMax),
        grossMax.subtract(recoveryMin)
    );
  }

  public Optional<BigDecimal> exactConvertibleAmount(MonthlyPlanItem item) {
    if (item.getAmount() != null) {
      return Optional.of(item.getAmount());
    }

    if (item.getMinAmount() != null
        && item.getMaxAmount() != null
        && item.getMinAmount().compareTo(item.getMaxAmount()) == 0) {
      return Optional.of(item.getMinAmount());
    }

    return Optional.empty();
  }

  public BigDecimal targetAmountForReconciliation(MonthlyPlanItem item) {
    if (item.getAmount() != null) {
      return item.getAmount();
    }

    if (item.getMaxAmount() != null) {
      return item.getMaxAmount();
    }

    if (item.getMinAmount() != null) {
      return item.getMinAmount();
    }

    return BigDecimal.ZERO;
  }

  private BigDecimal grossMin(MonthlyPlanItem item) {
    if (item.getAmount() != null) {
      return item.getAmount();
    }

    if (item.getMinAmount() != null) {
      return item.getMinAmount();
    }

    return BigDecimal.ZERO;
  }

  private BigDecimal grossMax(MonthlyPlanItem item) {
    if (item.getAmount() != null) {
      return item.getAmount();
    }

    if (item.getMaxAmount() != null) {
      return item.getMaxAmount();
    }

    if (item.getMinAmount() != null) {
      return item.getMinAmount();
    }

    return BigDecimal.ZERO;
  }

  private BigDecimal recovery(MonthlyPlanItem item, BigDecimal baseAmount) {
    if (item.getExpectedRecoveryAmount() != null) {
      return item.getExpectedRecoveryAmount();
    }

    if (item.getExpectedRecoveryPercent() == null) {
      return BigDecimal.ZERO;
    }

    var factor = item.getExpectedRecoveryPercent().divide(
        ONE_HUNDRED,
        RECOVERY_PERCENT_SCALE,
        RoundingMode.HALF_UP
    );

    return baseAmount.multiply(factor);
  }

  public record MonthlyPlanAmounts(
      BigDecimal grossMin,
      BigDecimal grossMax,
      BigDecimal recoveryMin,
      BigDecimal recoveryMax,
      BigDecimal netMin,
      BigDecimal netMax
  ) {}
}
