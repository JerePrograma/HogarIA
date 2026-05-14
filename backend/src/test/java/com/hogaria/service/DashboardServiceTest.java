package com.hogaria.service;

import com.hogaria.entity.*;import com.hogaria.repository.*;import org.junit.jupiter.api.Test;import org.junit.jupiter.api.extension.ExtendWith;import org.mockito.*;import org.mockito.junit.jupiter.MockitoExtension;import java.math.BigDecimal;import java.time.LocalDate;import java.util.*;import static org.junit.jupiter.api.Assertions.*;import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DashboardServiceTest {
  @Mock FinancialProfileRepository profileRepository; @Mock MoneyTransactionRepository transactionRepository; @Mock CategoryRepository categoryRepository; @Mock BudgetYearRepository budgetYearRepository; @Mock BudgetMonthRepository budgetMonthRepository; @Mock BudgetCategoryItemRepository budgetCategoryItemRepository; @Mock MonthlyPlanItemRepository monthlyPlanItemRepository; @Mock ExternalSyncMappingRepository externalSyncMappingRepository; @Mock FinancialCashFlowClassifier classifier; @InjectMocks DashboardService service;

  @Test void includesPlanningAndOperationalData(){
    var u=UUID.randomUUID(); var p=UUID.randomUUID(); var incomeCat=UUID.randomUUID(); var expenseCat=UUID.randomUUID();
    when(profileRepository.findByIdAndUserId(p,u)).thenReturn(Optional.of(new FinancialProfile()));
    when(transactionRepository.findByProfileIdAndBudgetDateBetween(eq(p),any(),any())).thenReturn(List.of(MoneyTransaction.builder().profileId(p).categoryId(incomeCat).movementType(MoneyTransaction.MovementType.INCOME).amount(new BigDecimal("100")).status(MoneyTransaction.Status.CONFIRMED).budgetDate(LocalDate.now()).build()));
    when(categoryRepository.findAllById(any())).thenReturn(List.of(Category.builder().id(incomeCat).type(Category.Type.INCOME).name("Ingreso").build(),Category.builder().id(expenseCat).type(Category.Type.VARIABLE_EXPENSE).name("Gasto").build()));
    when(externalSyncMappingRepository.findByProfileId(p)).thenReturn(List.of());
    when(classifier.classify(any(), any(), any())).thenReturn(CashFlowTreatment.EARNED_INCOME);
    when(monthlyPlanItemRepository.findByProfileIdAndPeriodYearAndPeriodMonth(p,2026,5)).thenReturn(List.of(
      MonthlyPlanItem.builder().type(MonthlyPlanItem.Type.INCOME).title("Cobro").periodYear(2026).periodMonth(5).minAmount(new BigDecimal("50")).maxAmount(new BigDecimal("100")).status(MonthlyPlanItem.Status.ESTIMATED).build(),
      MonthlyPlanItem.builder().type(MonthlyPlanItem.Type.EXPENSE).title("Pago").periodYear(2026).periodMonth(5).amount(new BigDecimal("80")).status(MonthlyPlanItem.Status.DUE).transactionId(UUID.randomUUID()).build(),
      MonthlyPlanItem.builder().type(MonthlyPlanItem.Type.TODO).title("Cotizar").periodYear(2026).periodMonth(5).status(MonthlyPlanItem.Status.DRAFT).build(),
      MonthlyPlanItem.builder().type(MonthlyPlanItem.Type.EXPENSE).title("Cancelado").periodYear(2026).periodMonth(5).amount(new BigDecimal("999")).status(MonthlyPlanItem.Status.CANCELLED).build()
    ));
    var res=service.getMonthlySummary(u,p,2026,5);
    assertNotNull(res.planningSummary());
    assertEquals(new BigDecimal("-30"),res.planningSummary().projectedNetMin());
    assertEquals(new BigDecimal("20"),res.planningSummary().projectedNetMax());
    assertEquals(1,res.planningSummary().unpricedCount());
    assertEquals(1,res.planningSummary().convertedItemsCount());
    assertEquals("CRITICAL",res.operationalSummary().financialRiskLevel());
    assertTrue(res.operationalSummary().alerts().stream().anyMatch(a->a.contains("ítems sin cotizar")));
  }

  @Test void includesInvestmentAsMonthlyExpense(){
    var u=UUID.randomUUID(); var p=UUID.randomUUID(); var incomeCat=UUID.randomUUID(); var investmentCat=UUID.randomUUID();
    when(profileRepository.findByIdAndUserId(p,u)).thenReturn(Optional.of(new FinancialProfile()));
    when(transactionRepository.findByProfileIdAndBudgetDateBetween(eq(p),any(),any())).thenReturn(List.of(
        MoneyTransaction.builder().profileId(p).categoryId(incomeCat).movementType(MoneyTransaction.MovementType.INCOME).amount(new BigDecimal("1000")).status(MoneyTransaction.Status.CONFIRMED).budgetDate(LocalDate.now()).build(),
        MoneyTransaction.builder().profileId(p).categoryId(investmentCat).movementType(MoneyTransaction.MovementType.EXPENSE).amount(new BigDecimal("300")).status(MoneyTransaction.Status.CONFIRMED).budgetDate(LocalDate.now()).build()
    ));
    when(categoryRepository.findAllById(any())).thenReturn(List.of(
        Category.builder().id(incomeCat).type(Category.Type.INCOME).name("Ingreso").build(),
        Category.builder().id(investmentCat).type(Category.Type.INVESTMENT).name("Inversión").build()
    ));
    when(externalSyncMappingRepository.findByProfileId(p)).thenReturn(List.of());
    when(classifier.classify(any(), any(), any())).thenReturn(CashFlowTreatment.EARNED_INCOME);
    when(externalSyncMappingRepository.findByProfileId(p)).thenReturn(List.of());
    when(classifier.classify(any(), any(), any())).thenReturn(CashFlowTreatment.EARNED_INCOME);
    when(monthlyPlanItemRepository.findByProfileIdAndPeriodYearAndPeriodMonth(p,2026,5)).thenReturn(List.of());

    var res=service.getMonthlySummary(u,p,2026,5);

    assertEquals(new BigDecimal("300"),res.monthlyBalance().totalExpenses());
    assertEquals(new BigDecimal("700"),res.monthlyBalance().balance());
  }
}
