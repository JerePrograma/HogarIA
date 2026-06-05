package com.hogaria.service;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.hogaria.entity.Category;
import com.hogaria.entity.MoneyTransaction;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class FinancialCashFlowClassifierTest {
  private final FinancialCashFlowClassifier classifier = new FinancialCashFlowClassifier();

  @Test
  void disbursementExternalEventIsRecoverableOutflow() {
    var tx = tx(MoneyTransaction.MovementType.ADJUSTMENT, "CJPRESTAMOS_DISBURSEMENT");

    assertEquals(CashFlowTreatment.RECOVERABLE_OUTFLOW, classifier.classify(tx, investmentCategory(), "DISBURSEMENT"));
  }

  @Test
  void historicalCjDisbursementReasonIsRecoverableEvenWithoutMapping() {
    var tx = tx(MoneyTransaction.MovementType.EXPENSE, "CJPRESTAMOS_DISBURSEMENT");

    assertEquals(CashFlowTreatment.RECOVERABLE_OUTFLOW, classifier.classify(tx, investmentCategory(), null));
  }

  @Test
  void possibleCrossSourceDuplicateIsNotConsumptionByDefault() {
    var tx = tx(MoneyTransaction.MovementType.EXPENSE, "POSSIBLE_CROSS_SOURCE_DUPLICATE");
    tx.setClassificationStatus(MoneyTransaction.ClassificationStatus.REVIEW);

    assertEquals(CashFlowTreatment.UNKNOWN, classifier.classify(tx, variableCategory(), null));
  }

  private MoneyTransaction tx(MoneyTransaction.MovementType movementType, String reason) {
    return MoneyTransaction.builder()
        .movementType(movementType)
        .amount(new BigDecimal("100"))
        .classificationReason(reason)
        .build();
  }

  private Category investmentCategory() {
    return Category.builder().type(Category.Type.INVESTMENT).build();
  }

  private Category variableCategory() {
    return Category.builder().type(Category.Type.VARIABLE_EXPENSE).build();
  }
}
