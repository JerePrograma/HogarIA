package com.hogaria.service;

import com.hogaria.entity.Category;
import com.hogaria.entity.MoneyTransaction;
import org.springframework.stereotype.Component;

@Component
public class FinancialCashFlowClassifier {

  private final TransactionFinancialImpactService impactService;

  public FinancialCashFlowClassifier() {
    this(new TransactionFinancialImpactService());
  }

  public FinancialCashFlowClassifier(TransactionFinancialImpactService impactService) {
    this.impactService = impactService;
  }

  public CashFlowTreatment classify(MoneyTransaction tx, Category category, String externalEventType) {
    return impactService.classifyTreatment(tx, category, externalEventType);
  }
}
