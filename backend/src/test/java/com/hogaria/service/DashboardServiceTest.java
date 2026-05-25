package com.hogaria.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import com.hogaria.entity.Category;
import com.hogaria.entity.ExternalSyncMapping;
import com.hogaria.entity.FinancialProfile;
import com.hogaria.entity.MoneyTransaction;
import com.hogaria.entity.MonthlyPlanItem;
import com.hogaria.repository.BudgetCategoryItemRepository;
import com.hogaria.repository.BudgetMonthRepository;
import com.hogaria.repository.BudgetYearRepository;
import com.hogaria.repository.CategoryRepository;
import com.hogaria.repository.ExternalSyncMappingRepository;
import com.hogaria.repository.FinancialProfileRepository;
import com.hogaria.repository.MoneyTransactionRepository;
import com.hogaria.repository.MonthlyPlanItemRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DashboardServiceTest {
  @Mock FinancialProfileRepository profileRepository;
  @Mock MoneyTransactionRepository transactionRepository;
  @Mock CategoryRepository categoryRepository;
  @Mock BudgetYearRepository budgetYearRepository;
  @Mock BudgetMonthRepository budgetMonthRepository;
  @Mock BudgetCategoryItemRepository budgetCategoryItemRepository;
  @Mock MonthlyPlanItemRepository monthlyPlanItemRepository;
  @Mock ExternalSyncMappingRepository externalSyncMappingRepository;
  @Mock FinancialCashFlowClassifier classifier;
  @Mock MonthlyPlanAmountCalculator monthlyPlanAmountCalculator;
  @InjectMocks DashboardService service;

  @Test
  void includesPlanningAndOperationalData() {
    var userId = UUID.randomUUID();
    var profileId = UUID.randomUUID();
    var incomeCat = UUID.randomUUID();

    when(profileRepository.findByIdAndUserId(profileId, userId)).thenReturn(Optional.of(new FinancialProfile()));
    when(transactionRepository.findByProfileIdAndBudgetDateBetween(eq(profileId), any(), any()))
        .thenReturn(List.of(tx(profileId, incomeCat, MoneyTransaction.MovementType.INCOME, "100", "Ingreso")));
    when(categoryRepository.findAllById(any()))
        .thenReturn(List.of(Category.builder().id(incomeCat).type(Category.Type.INCOME).name("Ingreso").build()));
    when(externalSyncMappingRepository.findByProfileId(profileId)).thenReturn(List.of());
    when(classifier.classify(any(), any(), any())).thenReturn(CashFlowTreatment.EARNED_INCOME);
    when(monthlyPlanAmountCalculator.calculate(any()))
        .thenReturn(new MonthlyPlanAmountCalculator.AmountBreakdown(
            BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO));
    when(monthlyPlanItemRepository.findByProfileIdAndPeriodYearAndPeriodMonth(profileId, 2026, 5))
        .thenReturn(List.of(
            MonthlyPlanItem.builder().type(MonthlyPlanItem.Type.INCOME).title("Cobro").periodYear(2026).periodMonth(5).minAmount(new BigDecimal("50")).maxAmount(new BigDecimal("100")).status(MonthlyPlanItem.Status.ESTIMATED).build(),
            MonthlyPlanItem.builder().type(MonthlyPlanItem.Type.EXPENSE).title("Pago").periodYear(2026).periodMonth(5).amount(new BigDecimal("80")).status(MonthlyPlanItem.Status.DUE).transactionId(UUID.randomUUID()).build(),
            MonthlyPlanItem.builder().type(MonthlyPlanItem.Type.TODO).title("Cotizar").periodYear(2026).periodMonth(5).status(MonthlyPlanItem.Status.DRAFT).build(),
            MonthlyPlanItem.builder().type(MonthlyPlanItem.Type.EXPENSE).title("Cancelado").periodYear(2026).periodMonth(5).amount(new BigDecimal("999")).status(MonthlyPlanItem.Status.CANCELLED).build()));

    var res = service.getMonthlySummary(userId, profileId, 2026, 5);

    assertNotNull(res.planningSummary());
    assertNotNull(res.operationalSummary());
    assertEquals(1, res.planningSummary().unpricedCount());
    assertEquals(1, res.planningSummary().convertedItemsCount());
    assertEquals("RISK", res.operationalSummary().financialRiskLevel());
    assertTrue(res.operationalSummary().alerts().stream().anyMatch(a -> a.contains("ítems sin cotizar")));
  }

  @Test
  void separatesConsumptionRecoverablesPrincipalRecoveryAndNetCashFlow() {
    var userId = UUID.randomUUID();
    var profileId = UUID.randomUUID();
    var incomeCat = UUID.randomUUID();
    var expenseCat = UUID.randomUUID();
    var savingCat = UUID.randomUUID();
    var cjCat = UUID.randomUUID();
    var salary = tx(profileId, incomeCat, MoneyTransaction.MovementType.INCOME, "1000", "Sueldo");
    var market = tx(profileId, expenseCat, MoneyTransaction.MovementType.EXPENSE, "200", "Supermercado");
    var saving = tx(profileId, savingCat, MoneyTransaction.MovementType.SAVING, "100", "Ahorro");
    var disbursement = tx(profileId, cjCat, MoneyTransaction.MovementType.ADJUSTMENT, "300", "Préstamo CJ #5");
    var recovery = tx(profileId, cjCat, MoneyTransaction.MovementType.ADJUSTMENT, "50", "Recupero capital CJ");

    var realClassifier = new FinancialCashFlowClassifier();
    var localService = new DashboardService(
        profileRepository,
        transactionRepository,
        categoryRepository,
        budgetYearRepository,
        budgetMonthRepository,
        budgetCategoryItemRepository,
        monthlyPlanItemRepository,
        externalSyncMappingRepository,
        realClassifier,
        monthlyPlanAmountCalculator);

    when(profileRepository.findByIdAndUserId(profileId, userId)).thenReturn(Optional.of(new FinancialProfile()));
    when(transactionRepository.findByProfileIdAndBudgetDateBetween(eq(profileId), any(), any()))
        .thenReturn(List.of(salary, market, saving, disbursement, recovery));
    when(categoryRepository.findAllById(any()))
        .thenReturn(List.of(
            Category.builder().id(incomeCat).type(Category.Type.INCOME).name("Ingreso").build(),
            Category.builder().id(expenseCat).type(Category.Type.VARIABLE_EXPENSE).name("Gasto").build(),
            Category.builder().id(savingCat).type(Category.Type.SAVING).name("Ahorro").build(),
            Category.builder().id(cjCat).type(Category.Type.INVESTMENT).name("CJ - Capital prestado").build()));
    when(externalSyncMappingRepository.findByProfileId(profileId))
        .thenReturn(List.of(
            mapping(profileId, disbursement.getId(), "DISBURSEMENT"),
            mapping(profileId, recovery.getId(), "PAYMENT_PRINCIPAL_RECOVERY")));
    when(monthlyPlanItemRepository.findByProfileIdAndPeriodYearAndPeriodMonth(profileId, 2026, 5)).thenReturn(List.of());

    var res = localService.getMonthlySummary(userId, profileId, 2026, 5);

    assertEquals(new BigDecimal("200"), res.monthlyCashFlowSummary().consumptionExpense());
    assertEquals(new BigDecimal("300"), res.monthlyCashFlowSummary().recoverableOutflow());
    assertEquals(new BigDecimal("50"), res.monthlyCashFlowSummary().principalRecovery());
    assertEquals(new BigDecimal("700"), res.monthlyCashFlowSummary().operationalBalanceExcludingRecoverables());
    assertEquals(new BigDecimal("450"), res.monthlyCashFlowSummary().netCashFlowIncludingRecoverables());
    assertEquals(new BigDecimal("700"), res.monthlyBalance().balance());
  }

  @Test
  void internalFundingPlusFinalMercadoPagoPurchaseCountsOnlyFinalPurchaseAsConsumption() {
    var userId = UUID.randomUUID();
    var profileId = UUID.randomUUID();
    var expenseCat = UUID.randomUUID();
    var technicalCat = UUID.randomUUID();
    var amount = new BigDecimal("52290.00");
    var bancoFunding = tx(profileId, technicalCat, MoneyTransaction.MovementType.TRANSFER, "52290.00", "DEBITO DEBIN");
    bancoFunding.setClassificationReason("INTERNAL_TRANSFER_MATCHED");
    var mpFunding = tx(profileId, technicalCat, MoneyTransaction.MovementType.TRANSFER, "52290.00", "Pago Debin | Bank Transfer");
    var mpPurchase = tx(profileId, expenseCat, MoneyTransaction.MovementType.EXPENSE, "52290.00", "Pasajes");
    var localService = serviceWithRealClassifier();

    when(profileRepository.findByIdAndUserId(profileId, userId)).thenReturn(Optional.of(new FinancialProfile()));
    when(transactionRepository.findByProfileIdAndBudgetDateBetween(eq(profileId), any(), any()))
        .thenReturn(List.of(bancoFunding, mpFunding, mpPurchase));
    when(categoryRepository.findAllById(any()))
        .thenReturn(List.of(
            Category.builder().id(expenseCat).type(Category.Type.VARIABLE_EXPENSE).name("Viajes").build(),
            Category.builder().id(technicalCat).type(Category.Type.SAVING).name("Fondeo MercadoPago / transferencias internas").build()));
    when(externalSyncMappingRepository.findByProfileId(profileId)).thenReturn(List.of());
    when(monthlyPlanItemRepository.findByProfileIdAndPeriodYearAndPeriodMonth(profileId, 2026, 5)).thenReturn(List.of());

    var res = localService.getMonthlySummary(userId, profileId, 2026, 5);

    assertEquals(amount, res.monthlyCashFlowSummary().consumptionExpense());
    assertEquals(amount, res.monthlyBalance().totalExpenses());
  }

  private DashboardService serviceWithRealClassifier() {
    return new DashboardService(
        profileRepository,
        transactionRepository,
        categoryRepository,
        budgetYearRepository,
        budgetMonthRepository,
        budgetCategoryItemRepository,
        monthlyPlanItemRepository,
        externalSyncMappingRepository,
        new FinancialCashFlowClassifier(),
        monthlyPlanAmountCalculator);
  }

  private MoneyTransaction tx(
      UUID profileId, UUID categoryId, MoneyTransaction.MovementType movementType, String amount, String description) {
    return MoneyTransaction.builder()
        .id(UUID.randomUUID())
        .profileId(profileId)
        .accountId(UUID.randomUUID())
        .categoryId(categoryId)
        .movementType(movementType)
        .amount(new BigDecimal(amount))
        .currency("ARS")
        .description(description)
        .status(MoneyTransaction.Status.CONFIRMED)
        .budgetDate(LocalDate.of(2026, 5, 1))
        .realDate(LocalDate.of(2026, 5, 6))
        .build();
  }

  private ExternalSyncMapping mapping(UUID profileId, UUID transactionId, String eventType) {
    return ExternalSyncMapping.builder()
        .profileId(profileId)
        .externalSystem("CJPRESTAMOS")
        .externalEntityType("LOAN")
        .externalEntityId(UUID.randomUUID().toString())
        .externalEventType(eventType)
        .moneyTransactionId(transactionId)
        .status("PROCESSED")
        .build();
  }
}
