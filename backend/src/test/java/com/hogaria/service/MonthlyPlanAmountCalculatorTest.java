package com.hogaria.service;

import com.hogaria.entity.MonthlyPlanItem;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class MonthlyPlanAmountCalculatorTest {
  MonthlyPlanAmountCalculator s = new MonthlyPlanAmountCalculator();

  @Test void usaMontoExacto() {
    var item = MonthlyPlanItem.builder().type(MonthlyPlanItem.Type.EXPENSE).amount(new BigDecimal("100")).build();
    assertEquals(new BigDecimal("100"), s.plannedAmountForReconciliation(item));
  }

  @Test void rangoIngresoConservadorYSalidaMaxima() {
    var in = MonthlyPlanItem.builder().type(MonthlyPlanItem.Type.INCOME).minAmount(new BigDecimal("100")).maxAmount(new BigDecimal("150")).build();
    var out = MonthlyPlanItem.builder().type(MonthlyPlanItem.Type.EXPENSE).minAmount(new BigDecimal("100")).maxAmount(new BigDecimal("150")).build();
    assertEquals(new BigDecimal("100"), s.plannedAmountForReconciliation(in));
    assertEquals(new BigDecimal("150"), s.plannedAmountForReconciliation(out));
  }

  @Test void recoveryFijoYPorcentaje() {
    var fixed = MonthlyPlanItem.builder().type(MonthlyPlanItem.Type.RECOVERY).minAmount(new BigDecimal("200")).maxAmount(new BigDecimal("300")).expectedRecoveryAmount(new BigDecimal("50")).build();
    var pct = MonthlyPlanItem.builder().type(MonthlyPlanItem.Type.RECOVERY).minAmount(new BigDecimal("100")).maxAmount(new BigDecimal("200")).expectedRecoveryPercent(new BigDecimal("10")).build();
    assertEquals(new BigDecimal("150"), s.plannedAmountForReconciliation(fixed));
    assertEquals(new BigDecimal("80.000000"), s.plannedAmountForReconciliation(pct));
  }

  @Test void cancelledYTodoNoInflan() {
    var cancelled = MonthlyPlanItem.builder().type(MonthlyPlanItem.Type.EXPENSE).amount(new BigDecimal("10")).status(MonthlyPlanItem.Status.CANCELLED).build();
    var todo = MonthlyPlanItem.builder().type(MonthlyPlanItem.Type.TODO).amount(new BigDecimal("10")).status(MonthlyPlanItem.Status.DRAFT).build();
    assertEquals(BigDecimal.ZERO, s.plannedAmountForReconciliation(cancelled));
    assertEquals(BigDecimal.ZERO, s.plannedAmountForReconciliation(todo));
  }
}
