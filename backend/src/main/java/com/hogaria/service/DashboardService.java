package com.hogaria.service;

import com.hogaria.dto.DashboardDtos.BudgetSummaryResponse;
import com.hogaria.dto.DashboardDtos.CategoryDeviationResponse;
import com.hogaria.dto.DashboardDtos.CategorySummaryResponse;
import com.hogaria.dto.DashboardDtos.ClosingProjectionResponse;
import com.hogaria.dto.DashboardDtos.DashboardAlertResponse;
import com.hogaria.dto.DashboardDtos.DashboardOperationalSummaryResponse;
import com.hogaria.dto.DashboardDtos.DashboardSummaryResponse;
import com.hogaria.dto.DashboardDtos.FiftyThirtyTwentyResponse;
import com.hogaria.dto.DashboardDtos.MonthlyBalanceResponse;
import com.hogaria.dto.DashboardDtos.MonthlyCashFlowSummaryResponse;
import com.hogaria.dto.DashboardDtos.PlanningDashboardSummaryResponse;
import com.hogaria.dto.DashboardDtos.RealConfirmedSummaryResponse;
import com.hogaria.dto.DashboardDtos.RealVsPlannedResponse;
import com.hogaria.entity.BudgetCategoryItem;
import com.hogaria.entity.Category;
import com.hogaria.entity.ExternalSyncMapping;
import com.hogaria.entity.MoneyTransaction;
import com.hogaria.entity.MonthlyPlanItem;
import com.hogaria.exception.ForbiddenException;
import com.hogaria.repository.BudgetCategoryItemRepository;
import com.hogaria.repository.BudgetMonthRepository;
import com.hogaria.repository.BudgetYearRepository;
import com.hogaria.repository.CategoryRepository;
import com.hogaria.repository.ExternalSyncMappingRepository;
import com.hogaria.repository.FinancialProfileRepository;
import com.hogaria.repository.MoneyTransactionRepository;
import com.hogaria.repository.MonthlyPlanItemRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

@Service
public class DashboardService {
  private final FinancialProfileRepository profileRepository;
  private final MoneyTransactionRepository transactionRepository;
  private final CategoryRepository categoryRepository;
  private final BudgetYearRepository budgetYearRepository;
  private final BudgetMonthRepository budgetMonthRepository;
  private final BudgetCategoryItemRepository budgetCategoryItemRepository;
  private final MonthlyPlanItemRepository monthlyPlanItemRepository;
  private final ExternalSyncMappingRepository externalSyncMappingRepository;
  private final TransactionFinancialImpactService impactService;
  private final MonthlyPlanAmountCalculator monthlyPlanAmountCalculator;

  public DashboardService(
      FinancialProfileRepository profileRepository,
      MoneyTransactionRepository transactionRepository,
      CategoryRepository categoryRepository,
      BudgetYearRepository budgetYearRepository,
      BudgetMonthRepository budgetMonthRepository,
      BudgetCategoryItemRepository budgetCategoryItemRepository,
      MonthlyPlanItemRepository monthlyPlanItemRepository,
      ExternalSyncMappingRepository externalSyncMappingRepository,
      TransactionFinancialImpactService impactService,
      MonthlyPlanAmountCalculator monthlyPlanAmountCalculator) {
    this.profileRepository = profileRepository;
    this.transactionRepository = transactionRepository;
    this.categoryRepository = categoryRepository;
    this.budgetYearRepository = budgetYearRepository;
    this.budgetMonthRepository = budgetMonthRepository;
    this.budgetCategoryItemRepository = budgetCategoryItemRepository;
    this.monthlyPlanItemRepository = monthlyPlanItemRepository;
    this.externalSyncMappingRepository = externalSyncMappingRepository;
    this.impactService = impactService;
    this.monthlyPlanAmountCalculator = monthlyPlanAmountCalculator;
  }

  public DashboardSummaryResponse getMonthlySummary(UUID userId, UUID profileId, int year, int month) {
    profileRepository
        .findByIdAndUserId(profileId, userId)
        .orElseThrow(() -> new ForbiddenException("Profile does not belong to user"));

    var from = LocalDate.of(year, month, 1);
    var to = from.withDayOfMonth(from.lengthOfMonth());
    var allTransactions =
        transactionRepository.findByProfileIdAndBudgetDateBetween(profileId, from, to);
    var txs =
        allTransactions.stream()
            .filter(t -> t.getStatus() == MoneyTransaction.Status.CONFIRMED)
            .toList();
    var categoryIds =
        allTransactions.stream().map(MoneyTransaction::getCategoryId).filter(id -> id != null).collect(Collectors.toSet());
    var planningItems = monthlyPlanItemRepository.findByProfileIdAndPeriodYearAndPeriodMonth(profileId, year, month);
    categoryIds.addAll(
        planningItems.stream()
            .map(MonthlyPlanItem::getCategoryId)
            .filter(id -> id != null)
            .collect(Collectors.toSet()));
    var categories =
        categoryRepository.findAllById(categoryIds).stream()
            .collect(Collectors.toMap(Category::getId, Function.identity()));

    var mappingByTx = loadExternalEventTypeByTransaction(profileId);
    var planning = buildPlanning(planningItems);
    var cashFlowSummary = buildCashFlowSummary(txs, categories, mappingByTx);
    var realConfirmedSummary = buildRealConfirmedSummary(allTransactions, categories, mappingByTx);
    var realVsPlanned = buildRealVsPlanned(allTransactions, planningItems, categories, mappingByTx);
    var closingProjection = buildClosingProjection(realConfirmedSummary, planningItems);

    BigDecimal income = cashFlowSummary.totalIncome();
    BigDecimal fixed = cashFlowSummary.fixedExpense();
    BigDecimal variable = cashFlowSummary.variableExpense();
    BigDecimal savings = cashFlowSummary.savingOutflow().add(cashFlowSummary.investmentOutflow());
    BigDecimal totalExpenses = cashFlowSummary.consumptionExpense();
    BigDecimal balance = cashFlowSummary.operationalBalanceExcludingRecoverables();

    BigDecimal fp = percent(fixed, income);
    BigDecimal vp = percent(variable, income);
    BigDecimal sp = percent(savings, income);
    String health =
        balance.signum() < 0
            ? "CRITICAL"
            : (sp.compareTo(new BigDecimal("20")) >= 0
                    && fp.compareTo(new BigDecimal("50")) <= 0
                    && vp.compareTo(new BigDecimal("30")) <= 0
                ? "EXCELLENT"
                : (savings.signum() > 0 ? "HEALTHY" : "WARNING"));

    var breakdown = buildBreakdown(txs, categories, mappingByTx, income);
    var budgetSummary = buildBudgetSummary(profileId, year, month, txs, categories, mappingByTx);
    var operational = buildOperational(cashFlowSummary, planning, txs.isEmpty());
    var alerts = buildDashboardAlerts(realConfirmedSummary, realVsPlanned, closingProjection, operational);

    return new DashboardSummaryResponse(
        new MonthlyBalanceResponse(income, totalExpenses, savings, balance),
        new FiftyThirtyTwentyResponse(fp, vp, sp),
        fixed,
        variable,
        health,
        breakdown,
        budgetSummary,
        planning,
        operational,
        cashFlowSummary,
        realConfirmedSummary,
        realVsPlanned,
        closingProjection,
        realVsPlanned.categories(),
        alerts);
  }

  private List<CategorySummaryResponse> buildBreakdown(
      List<MoneyTransaction> txs,
      Map<UUID, Category> categories,
      Map<UUID, String> externalEventTypeByTx,
      BigDecimal income) {
    Map<UUID, List<MoneyTransaction>> byCategory = new LinkedHashMap<>();
    for (var tx : txs) {
      var impact = impactService.analyze(tx, categories.get(tx.getCategoryId()), externalEventTypeByTx.get(tx.getId()));
      if (impact.balanceImpact() != MoneyTransaction.BalanceImpact.CONSUMPTION_EXPENSE) {
        continue;
      }
      byCategory.computeIfAbsent(tx.getCategoryId(), ignored -> new ArrayList<>()).add(tx);
    }

    return byCategory.entrySet().stream()
        .map(
            entry -> {
              var category = categories.get(entry.getKey());
              var total = sum(entry.getValue().stream().map(MoneyTransaction::getAmount).toList());
              return new CategorySummaryResponse(
                  entry.getKey(),
                  category == null ? "Sin categoría" : category.getName(),
                  category == null ? null : category.getType(),
                  total,
                  percent(total, income),
                  entry.getValue().size());
            })
        .toList();
  }

  private BudgetSummaryResponse buildBudgetSummary(
      UUID profileId,
      int year,
      int month,
      List<MoneyTransaction> txs,
      Map<UUID, Category> categories,
      Map<UUID, String> externalEventTypeByTx) {
    var budgetYear = budgetYearRepository.findByProfileIdAndYear(profileId, year);
    if (budgetYear.isEmpty()) {
      return null;
    }

    var budgetMonth = budgetMonthRepository.findByBudgetYearIdAndMonth(budgetYear.get().getId(), month);
    if (budgetMonth.isEmpty()) {
      return null;
    }

    var itemMap =
        budgetCategoryItemRepository.findByBudgetMonthId(budgetMonth.get().getId()).stream()
            .collect(Collectors.toMap(BudgetCategoryItem::getCategoryId, BudgetCategoryItem::getBudgetAmount));
    var comparableTxs =
        txs.stream()
            .filter(t -> itemMap.containsKey(t.getCategoryId()))
            .filter(t -> {
              var impact = impactService.analyze(t, categories.get(t.getCategoryId()), externalEventTypeByTx.get(t.getId()));
              return impact.impactsOperationalBalance() && isOutflowImpact(impact.balanceImpact());
            })
            .toList();
    BigDecimal totalBudget = itemMap.values().stream().reduce(BigDecimal.ZERO, BigDecimal::add);
    BigDecimal totalReal =
        comparableTxs.stream()
            .map(MoneyTransaction::getAmount)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    long exceeded =
        itemMap.entrySet().stream()
            .filter(
                entry -> {
                  var real =
                      comparableTxs.stream()
                          .filter(t -> t.getCategoryId() != null && t.getCategoryId().equals(entry.getKey()))
                          .map(MoneyTransaction::getAmount)
                          .reduce(BigDecimal.ZERO, BigDecimal::add);
                  if (entry.getValue().signum() == 0) {
                    return real.signum() > 0;
                  }
                  return real
                          .multiply(new BigDecimal("100"))
                          .divide(entry.getValue(), 2, RoundingMode.HALF_UP)
                          .compareTo(new BigDecimal("100"))
                      > 0;
                })
            .count();
    long warnings =
        itemMap.entrySet().stream()
            .filter(
                entry -> {
                  if (entry.getValue().signum() == 0) {
                    return false;
                  }
                  var real =
                      comparableTxs.stream()
                          .filter(t -> t.getCategoryId() != null && t.getCategoryId().equals(entry.getKey()))
                          .map(MoneyTransaction::getAmount)
                          .reduce(BigDecimal.ZERO, BigDecimal::add);
                  var percentage =
                      real.multiply(new BigDecimal("100"))
                          .divide(entry.getValue(), 2, RoundingMode.HALF_UP);
                  return percentage.compareTo(new BigDecimal("80")) > 0
                      && percentage.compareTo(new BigDecimal("100")) <= 0;
                })
            .count();

    return new BudgetSummaryResponse(totalBudget, totalReal, totalBudget.subtract(totalReal), exceeded, warnings);
  }

  private PlanningDashboardSummaryResponse buildPlanning(List<MonthlyPlanItem> items) {
    BigDecimal inMin = BigDecimal.ZERO, inMax = BigDecimal.ZERO, exMin = BigDecimal.ZERO, exMax = BigDecimal.ZERO;
    BigDecimal recMin = BigDecimal.ZERO, recMax = BigDecimal.ZERO, nMin = BigDecimal.ZERO, nMax = BigDecimal.ZERO;
    BigDecimal pendingIncome = BigDecimal.ZERO, pendingExpense = BigDecimal.ZERO;
    int unpriced = 0, due7 = 0, cancelled = 0, converted = 0;
    var pendingStatuses = Set.of(MonthlyPlanItem.Status.DRAFT, MonthlyPlanItem.Status.ESTIMATED, MonthlyPlanItem.Status.SCHEDULED, MonthlyPlanItem.Status.DUE);
    var today = LocalDate.now();

    for (var item : items) {
      var calculated = monthlyPlanAmountCalculator.calculate(item);
      boolean isCancelled = item.getStatus() == MonthlyPlanItem.Status.CANCELLED;
      if (isCancelled) cancelled++;
      if (item.getTransactionId() != null) converted++;
      if (!isCancelled && item.getAmount() == null && item.getMinAmount() == null && item.getMaxAmount() == null) unpriced++;
      if (!isCancelled && item.getExpectedDate() != null && !item.getExpectedDate().isBefore(today) && !item.getExpectedDate().isAfter(today.plusDays(7))) due7++;
      if (isCancelled || item.getType() == MonthlyPlanItem.Type.TODO) continue;

      boolean incoming = item.getType() == MonthlyPlanItem.Type.INCOME || item.getType() == MonthlyPlanItem.Type.RECOVERY;
      if (incoming) {
        inMin = inMin.add(calculated.grossMin());
        inMax = inMax.add(calculated.grossMax());
        nMin = nMin.add(calculated.netMin());
        nMax = nMax.add(calculated.netMax());
        if (pendingStatuses.contains(item.getStatus())) pendingIncome = pendingIncome.add(calculated.netMax());
      } else {
        exMin = exMin.add(calculated.grossMin());
        exMax = exMax.add(calculated.grossMax());
        recMin = recMin.add(calculated.recoveryMin());
        recMax = recMax.add(calculated.recoveryMax());
        nMin = nMin.subtract(calculated.netMax());
        nMax = nMax.subtract(calculated.netMin());
        if (pendingStatuses.contains(item.getStatus())) pendingExpense = pendingExpense.add(calculated.netMax());
      }
    }

    return new PlanningDashboardSummaryResponse(
        inMin, inMax, exMin, exMax, recMin, recMax, nMin, nMax, pendingIncome, pendingExpense,
        unpriced, due7, items.size(), cancelled, converted);
  }

  private DashboardOperationalSummaryResponse buildOperational(
      MonthlyCashFlowSummaryResponse cashFlow, PlanningDashboardSummaryResponse planning, boolean noConfirmed) {
    BigDecimal confirmedBalance = cashFlow.operationalBalanceExcludingRecoverables();
    BigDecimal confirmedSavings = cashFlow.savingOutflow().add(cashFlow.investmentOutflow());
    BigDecimal deltaMin = planning.projectedNetMin().subtract(confirmedBalance);
    BigDecimal deltaMax = planning.projectedNetMax().subtract(confirmedBalance);
    var risk = "OK";
    var alerts = new ArrayList<String>();

    if (planning.projectedNetMin().signum() < 0) {
      risk = "CRITICAL";
      alerts.add("El neto proyectado mínimo queda negativo.");
    } else if (planning.projectedNetMax().compareTo(confirmedBalance) < 0
        || planning.pendingExpense().compareTo(planning.pendingIncome().add(confirmedBalance)) > 0) {
      risk = "RISK";
    } else if (planning.unpricedCount() > 0 || planning.dueNext7DaysCount() > 0) {
      risk = "WATCH";
    }

    if (planning.unpricedCount() > 0) alerts.add("Hay " + planning.unpricedCount() + " ítems sin cotizar.");
    if (planning.dueNext7DaysCount() > 0) alerts.add("Hay " + planning.dueNext7DaysCount() + " vencimientos/cobros en los próximos 7 días.");
    if (planning.pendingExpense().compareTo(planning.pendingIncome().add(confirmedBalance)) > 0) alerts.add("Los egresos pendientes superan los ingresos pendientes más el balance operativo confirmado.");
    if (planning.plannedItemsCount() > 0 && noConfirmed) alerts.add("Hay planificación cargada pero todavía no hay movimientos confirmados.");

    return new DashboardOperationalSummaryResponse(
        cashFlow.totalIncome(),
        cashFlow.consumptionExpense(),
        confirmedSavings,
        confirmedBalance,
        planning.projectedNetMin(),
        planning.projectedNetMax(),
        deltaMin,
        deltaMax,
        planning.pendingIncome(),
        planning.pendingExpense(),
        planning.totalRecoveryMin(),
        planning.totalRecoveryMax(),
        planning.unpricedCount(),
        planning.dueNext7DaysCount(),
        risk,
        alerts,
        cashFlow.consumptionExpense(),
        cashFlow.recoverableOutflow(),
        cashFlow.principalRecovered(),
        cashFlow.operationalBalanceExcludingRecoverables(),
        cashFlow.netCashFlowIncludingRecoverables());
  }

  private RealConfirmedSummaryResponse buildRealConfirmedSummary(
      List<MoneyTransaction> transactions,
      Map<UUID, Category> categories,
      Map<UUID, String> externalEventTypeByTx) {
    BigDecimal confirmedIncome = BigDecimal.ZERO;
    BigDecimal confirmedExpenses = BigDecimal.ZERO;
    BigDecimal confirmedSavings = BigDecimal.ZERO;
    BigDecimal operationalOutflows = BigDecimal.ZERO;
    BigDecimal operationalBalance = BigDecimal.ZERO;
    BigDecimal ignoredAmount = BigDecimal.ZERO;
    BigDecimal transfersAmount = BigDecimal.ZERO;
    BigDecimal adjustmentsAmount = BigDecimal.ZERO;
    BigDecimal technicalAmount = BigDecimal.ZERO;
    BigDecimal nonOperationalAmount = BigDecimal.ZERO;
    BigDecimal excludedInternalTransferAmount = BigDecimal.ZERO;
    BigDecimal excludedDuplicateAmount = BigDecimal.ZERO;
    BigDecimal reviewAmount = BigDecimal.ZERO;
    long confirmedCount = 0;
    long pendingCount = 0;
    long ignoredCount = 0;
    long withoutCategoryCount = 0;
    long reviewCount = 0;
    long technicalCount = 0;
    long transferCount = 0;
    long adjustmentCount = 0;
    long nonOperationalCount = 0;
    long excludedInternalTransferCount = 0;
    long excludedDuplicateCount = 0;

    for (var tx : transactions) {
      var amount = tx.getAmount() == null ? BigDecimal.ZERO : tx.getAmount();
      var classification = classificationStatus(tx);
      var impact = impactService.analyze(tx, categories.get(tx.getCategoryId()), externalEventTypeByTx.get(tx.getId()));
      boolean internalTransferExcluded = isInternalTransferExcluded(tx, impact);
      boolean duplicateExcluded = isDuplicateExcluded(tx);

      if (tx.getStatus() == MoneyTransaction.Status.CONFIRMED) confirmedCount++;
      if (tx.getStatus() == MoneyTransaction.Status.PENDING) pendingCount++;
      if (tx.getStatus() == MoneyTransaction.Status.IGNORED) {
        ignoredCount++;
        ignoredAmount = ignoredAmount.add(amount);
      }
      if (tx.getCategoryId() == null || classification == MoneyTransaction.ClassificationStatus.NEEDS_CATEGORY) {
        withoutCategoryCount++;
      }
      if (classification == MoneyTransaction.ClassificationStatus.REVIEW) reviewCount++;
      if (classification == MoneyTransaction.ClassificationStatus.TECHNICAL) {
        technicalCount++;
        if (tx.getStatus() == MoneyTransaction.Status.CONFIRMED) technicalAmount = technicalAmount.add(amount);
      }
      if (tx.getMovementType() == MoneyTransaction.MovementType.TRANSFER) {
        transferCount++;
        if (tx.getStatus() == MoneyTransaction.Status.CONFIRMED) transfersAmount = transfersAmount.add(amount);
      }
      if (tx.getMovementType() == MoneyTransaction.MovementType.ADJUSTMENT) {
        adjustmentCount++;
        if (tx.getStatus() == MoneyTransaction.Status.CONFIRMED) adjustmentsAmount = adjustmentsAmount.add(amount);
      }

      if (tx.getStatus() != MoneyTransaction.Status.PENDING && internalTransferExcluded) {
        excludedInternalTransferCount++;
        excludedInternalTransferAmount = excludedInternalTransferAmount.add(amount);
      }

      if (tx.getStatus() != MoneyTransaction.Status.PENDING && duplicateExcluded) {
        excludedDuplicateCount++;
        excludedDuplicateAmount = excludedDuplicateAmount.add(amount);
      }

      if (tx.getStatus() != MoneyTransaction.Status.CONFIRMED) continue;

      if (!impact.impactsOperationalBalance()) {
        nonOperationalCount++;
        nonOperationalAmount = nonOperationalAmount.add(amount);
        if (classification == MoneyTransaction.ClassificationStatus.REVIEW
            && !internalTransferExcluded
            && !duplicateExcluded) {
          reviewAmount = reviewAmount.add(amount);
        }
        continue;
      }

      if (isIncomeImpact(impact.balanceImpact())) {
        confirmedIncome = confirmedIncome.add(amount);
        operationalBalance = operationalBalance.add(amount);
      } else if (isExpenseImpact(impact.balanceImpact())) {
        confirmedExpenses = confirmedExpenses.add(amount);
        operationalBalance = operationalBalance.subtract(amount);
      } else if (isSavingImpact(impact.balanceImpact())) {
        confirmedSavings = confirmedSavings.add(amount);
        operationalBalance = operationalBalance.subtract(amount);
      }

      operationalOutflows = confirmedExpenses.add(confirmedSavings);
    }

    return new RealConfirmedSummaryResponse(
        confirmedIncome,
        confirmedExpenses,
        confirmedSavings,
        operationalOutflows,
        operationalBalance,
        confirmedCount,
        pendingCount,
        ignoredCount,
        withoutCategoryCount,
        reviewCount,
        technicalCount,
        transferCount,
        adjustmentCount,
        nonOperationalCount,
        ignoredAmount,
        transfersAmount,
        adjustmentsAmount,
        technicalAmount,
        nonOperationalAmount,
        excludedInternalTransferCount,
        excludedInternalTransferAmount,
        excludedDuplicateCount,
        excludedDuplicateAmount,
        reviewAmount);
  }

  private RealVsPlannedResponse buildRealVsPlanned(
      List<MoneyTransaction> allTransactions,
      List<MonthlyPlanItem> planItems,
      Map<UUID, Category> categories,
      Map<UUID, String> externalEventTypeByTx) {
    Map<UUID, CategoryDeviationAccumulator> byCategory = new LinkedHashMap<>();
    var convertedTransactionIds =
        planItems.stream()
            .map(MonthlyPlanItem::getTransactionId)
            .filter(id -> id != null)
            .collect(Collectors.toSet());
    var pendingStatuses =
        Set.of(
            MonthlyPlanItem.Status.DRAFT,
            MonthlyPlanItem.Status.ESTIMATED,
            MonthlyPlanItem.Status.SCHEDULED,
            MonthlyPlanItem.Status.DUE);

    for (var item : planItems) {
      if (!usesPlanItemForExecutionComparison(item)) continue;

      var row = accumulatorFor(byCategory, item.getCategoryId(), categories);
      var amount = plannedComparableAmount(item);
      row.plannedAmount = row.plannedAmount.add(amount);
      row.plannedCount++;

      if (item.getTransactionId() == null && pendingStatuses.contains(item.getStatus())) {
        row.pendingPlannedAmount = row.pendingPlannedAmount.add(amount);
      }
    }

    for (var tx : allTransactions) {
      if (tx.getStatus() != MoneyTransaction.Status.CONFIRMED) continue;

      var impact = impactService.analyze(tx, categories.get(tx.getCategoryId()), externalEventTypeByTx.get(tx.getId()));
      if (!impact.impactsOperationalBalance()) continue;
      if (!isOutflowImpact(impact.balanceImpact())) continue;

      var row = accumulatorFor(byCategory, tx.getCategoryId(), categories);
      var amount = tx.getAmount() == null ? BigDecimal.ZERO : tx.getAmount();
      row.realConfirmedAmount = row.realConfirmedAmount.add(amount);
      row.realCount++;

      if (!convertedTransactionIds.contains(tx.getId())) {
        row.realUnplannedAmount = row.realUnplannedAmount.add(amount);
      }
    }

    var rows =
        byCategory.values().stream()
            .map(this::toCategoryDeviation)
            .sorted(
                Comparator.comparingInt((CategoryDeviationResponse row) -> executionStatusPriority(row.status()))
                    .thenComparing(
                        (CategoryDeviationResponse row) -> row.difference().abs(),
                        Comparator.reverseOrder())
                    .thenComparing(CategoryDeviationResponse::categoryName, String.CASE_INSENSITIVE_ORDER))
            .toList();

    var totalPlanned = rows.stream().map(CategoryDeviationResponse::plannedAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
    var totalReal = rows.stream().map(CategoryDeviationResponse::realConfirmedAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
    var pendingPlanned = rows.stream().map(CategoryDeviationResponse::pendingPlannedAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
    var realUnplanned = rows.stream().map(CategoryDeviationResponse::realUnplannedAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
    var totalDifference = totalReal.subtract(totalPlanned);

    return new RealVsPlannedResponse(
        totalPlanned,
        totalReal,
        totalDifference,
        percent(totalReal, totalPlanned),
        pendingPlanned,
        realUnplanned,
        executionStatus(totalPlanned, totalReal),
        rows);
  }

  private ClosingProjectionResponse buildClosingProjection(
      RealConfirmedSummaryResponse realSummary, List<MonthlyPlanItem> planItems) {
    BigDecimal pendingPlannedNet = BigDecimal.ZERO;
    BigDecimal plannedNet = BigDecimal.ZERO;
    var pendingStatuses =
        Set.of(
            MonthlyPlanItem.Status.DRAFT,
            MonthlyPlanItem.Status.ESTIMATED,
            MonthlyPlanItem.Status.SCHEDULED,
            MonthlyPlanItem.Status.DUE);

    for (var item : planItems) {
      if (!usesPlanItemForFinancialProjection(item)) continue;

      var signedNet = signedPlannedNet(item);
      plannedNet = plannedNet.add(signedNet);

      if (item.getTransactionId() == null && pendingStatuses.contains(item.getStatus())) {
        pendingPlannedNet = pendingPlannedNet.add(signedNet);
      }
    }

    var estimatedClosing = realSummary.operationalBalance().add(pendingPlannedNet);

    return new ClosingProjectionResponse(
        realSummary.operationalBalance(),
        pendingPlannedNet,
        estimatedClosing,
        plannedNet,
        estimatedClosing.subtract(plannedNet));
  }

  private List<DashboardAlertResponse> buildDashboardAlerts(
      RealConfirmedSummaryResponse realSummary,
      RealVsPlannedResponse realVsPlanned,
      ClosingProjectionResponse closingProjection,
      DashboardOperationalSummaryResponse operational) {
    var alerts = new ArrayList<DashboardAlertResponse>();

    for (var alert : operational.alerts()) {
      alerts.add(new DashboardAlertResponse("Alerta operativa", alert, inferRisk(alert)));
    }

    if (realSummary.withoutCategoryCount() > 0) {
      alerts.add(
          new DashboardAlertResponse(
              "Movimientos sin categoría",
              realSummary.withoutCategoryCount()
                  + " "
                  + movementLabel(realSummary.withoutCategoryCount())
                  + " necesita"
                  + (realSummary.withoutCategoryCount() == 1 ? "" : "n")
                  + " categoría para mejorar desvíos y reportes.",
              "WATCH"));
    }

    if (realSummary.pendingCount() > 0) {
      alerts.add(
          new DashboardAlertResponse(
              "Movimientos pendientes",
              realSummary.pendingCount()
                  + " "
                  + movementLabel(realSummary.pendingCount())
                  + " no impacta"
                  + (realSummary.pendingCount() == 1 ? "" : "n")
                  + " en el real confirmado.",
              "WATCH"));
    }

    if (realVsPlanned.realUnplannedAmount().signum() > 0) {
      alerts.add(
          new DashboardAlertResponse(
              "Real no planificado",
              "Hay movimientos confirmados por fuera del plan por " + realVsPlanned.realUnplannedAmount() + ".",
              "RISK"));
    }

    if (Set.of("EXCEEDED", "CRITICAL").contains(realVsPlanned.status())) {
      alerts.add(
          new DashboardAlertResponse(
              "Desvío contra plan",
              "La ejecución real difiere del plan por " + realVsPlanned.totalDifference().abs() + ".",
              realVsPlanned.status().equals("CRITICAL") ? "CRITICAL" : "RISK"));
    }

    if (closingProjection.estimatedClosing().signum() < 0) {
      alerts.add(
          new DashboardAlertResponse(
              "Proyección de cierre negativa",
              "El cierre estimado queda por debajo de cero si se cumplen los pendientes.",
              "CRITICAL"));
    }

    return alerts.stream()
        .sorted(
            Comparator.comparingInt((DashboardAlertResponse alert) -> riskPriority(alert.riskLevel()))
                .thenComparing(DashboardAlertResponse::title, String.CASE_INSENSITIVE_ORDER))
        .toList();
  }

  private MonthlyCashFlowSummaryResponse buildCashFlowSummary(
      List<MoneyTransaction> txs, Map<UUID, Category> categories, Map<UUID, String> externalEventTypeByTx) {
    BigDecimal gross = BigDecimal.ZERO, consumption = BigDecimal.ZERO, fixed = BigDecimal.ZERO, variable = BigDecimal.ZERO;
    BigDecimal debt = BigDecimal.ZERO, saving = BigDecimal.ZERO, investment = BigDecimal.ZERO, recoverableOut = BigDecimal.ZERO;
    BigDecimal principal = BigDecimal.ZERO, refund = BigDecimal.ZERO, earned = BigDecimal.ZERO, interest = BigDecimal.ZERO;
    BigDecimal internalTransfer = BigDecimal.ZERO, externalTransfer = BigDecimal.ZERO, neutral = BigDecimal.ZERO, unknown = BigDecimal.ZERO;
    List<String> alerts = new ArrayList<>();

    for (var tx : txs) {
      var treatment = impactService.analyze(tx, categories.get(tx.getCategoryId()), externalEventTypeByTx.get(tx.getId())).treatment();
      var amount = tx.getAmount();
      switch (treatment) {
        case FIXED_CONSUMPTION_EXPENSE -> {
          fixed = fixed.add(amount);
          consumption = consumption.add(amount);
          gross = gross.add(amount);
        }
        case VARIABLE_CONSUMPTION_EXPENSE, CONSUMPTION_EXPENSE -> {
          variable = variable.add(amount);
          consumption = consumption.add(amount);
          gross = gross.add(amount);
        }
        case DEBT_OUTFLOW, DEBT_INTEREST -> {
          debt = debt.add(amount);
          gross = gross.add(amount);
        }
        case SAVING_OUTFLOW -> {
          saving = saving.add(amount);
          gross = gross.add(amount);
        }
        case INVESTMENT_OUTFLOW -> {
          investment = investment.add(amount);
          gross = gross.add(amount);
        }
        case RECOVERABLE_OUTFLOW -> {
          recoverableOut = recoverableOut.add(amount);
          gross = gross.add(amount);
        }
        case PRINCIPAL_RECOVERY -> principal = principal.add(amount);
        case REFUND_OR_REIMBURSEMENT -> refund = refund.add(amount);
        case EARNED_INCOME -> earned = earned.add(amount);
        case INTEREST_INCOME -> interest = interest.add(amount);
        case INTERNAL_TRANSFER -> internalTransfer = internalTransfer.add(amount);
        case EXTERNAL_TRANSFER -> {
          externalTransfer = externalTransfer.add(amount);
          gross = gross.add(amount);
        }
        case CASH_WITHDRAWAL, LOAN_ORIGINATION, NEUTRAL_ADJUSTMENT -> neutral = neutral.add(amount);
        case UNKNOWN -> {
          if (tx.getMovementType() == MoneyTransaction.MovementType.EXPENSE) {
            unknown = unknown.add(amount);
          }
          alerts.add("Movimiento sin clasificación: " + (tx.getDescription() == null ? tx.getId() : tx.getDescription()));
        }
      }
    }

    var totalIncome = earned.add(interest);
    var netCashIncludingRecoverables = totalIncome.add(principal).add(refund).subtract(gross);
    var operationalExcludingRecoverables = totalIncome.add(refund).subtract(consumption).subtract(saving).subtract(investment);
    var economic = consumption.add(debt).subtract(refund).subtract(interest);
    return new MonthlyCashFlowSummaryResponse(
        gross,
        consumption,
        fixed,
        variable,
        debt,
        saving,
        investment,
        recoverableOut,
        principal,
        refund,
        earned,
        interest,
        totalIncome,
        netCashIncludingRecoverables,
        economic,
        internalTransfer,
        externalTransfer,
        neutral,
        unknown,
        alerts,
        principal,
        operationalExcludingRecoverables,
        netCashIncludingRecoverables);
  }

  private Map<UUID, String> loadExternalEventTypeByTransaction(UUID profileId) {
    return externalSyncMappingRepository.findByProfileId(profileId).stream()
        .filter(m -> m.getMoneyTransactionId() != null)
        .collect(Collectors.toMap(
            ExternalSyncMapping::getMoneyTransactionId,
            ExternalSyncMapping::getExternalEventType,
            (a, b) -> a));
  }

  private CategoryDeviationAccumulator accumulatorFor(
      Map<UUID, CategoryDeviationAccumulator> rows, UUID categoryId, Map<UUID, Category> categories) {
    return rows.computeIfAbsent(
        categoryId,
        ignored -> {
          var category = categoryId == null ? null : categories.get(categoryId);
          var row = new CategoryDeviationAccumulator();
          row.categoryId = categoryId;
          row.categoryName = category == null ? "Sin categoría" : category.getName();
          row.categoryType = category == null ? null : category.getType();
          return row;
        });
  }

  private CategoryDeviationResponse toCategoryDeviation(CategoryDeviationAccumulator row) {
    var difference = row.realConfirmedAmount.subtract(row.plannedAmount);

    return new CategoryDeviationResponse(
        row.categoryId,
        row.categoryName,
        row.categoryType,
        row.plannedAmount,
        row.realConfirmedAmount,
        row.pendingPlannedAmount,
        row.realUnplannedAmount,
        difference,
        percent(row.realConfirmedAmount, row.plannedAmount),
        executionStatus(row.plannedAmount, row.realConfirmedAmount),
        row.plannedCount,
        row.realCount);
  }

  private MoneyTransaction.ClassificationStatus classificationStatus(MoneyTransaction tx) {
    if (tx.getClassificationStatus() != null) return tx.getClassificationStatus();
    return tx.getCategoryId() == null
        ? MoneyTransaction.ClassificationStatus.NEEDS_CATEGORY
        : MoneyTransaction.ClassificationStatus.CLASSIFIED;
  }

  private boolean isInternalTransferExcluded(MoneyTransaction tx, TransactionFinancialImpact impact) {
    return tx.getInternalTransferGroupId() != null
        || tx.getPaymentChannel() == MoneyTransaction.PaymentChannel.INTERNAL_TRANSFER
        || impact.balanceImpact() == MoneyTransaction.BalanceImpact.INTERNAL_TRANSFER
        || hasAnyReason(
            tx,
            "INTERNAL_TRANSFER_MATCHED",
            "USER_MARKED_INTERNAL_TRANSFER");
  }

  private boolean isDuplicateExcluded(MoneyTransaction tx) {
    return hasAnyReason(
        tx,
        "USER_IGNORED_CROSS_SOURCE",
        "DUPLICATE_RESOLVED_KEEP",
        "EXACT_DUPLICATE",
        "SOURCE_DUPLICATE");
  }

  private boolean hasAnyReason(MoneyTransaction tx, String... needles) {
    var reason = tx.getClassificationReason();
    if (reason == null || reason.isBlank()) {
      return false;
    }

    var normalized = reason.toUpperCase(Locale.ROOT);
    for (var needle : needles) {
      if (normalized.contains(needle)) {
        return true;
      }
    }
    return false;
  }

  private boolean isIncomeImpact(MoneyTransaction.BalanceImpact balanceImpact) {
    return balanceImpact == MoneyTransaction.BalanceImpact.OPERATING_INCOME
        || balanceImpact == MoneyTransaction.BalanceImpact.INTEREST_INCOME;
  }

  private boolean isExpenseImpact(MoneyTransaction.BalanceImpact balanceImpact) {
    return balanceImpact == MoneyTransaction.BalanceImpact.CONSUMPTION_EXPENSE
        || balanceImpact == MoneyTransaction.BalanceImpact.DEBT_OUTFLOW;
  }

  private boolean isSavingImpact(MoneyTransaction.BalanceImpact balanceImpact) {
    return balanceImpact == MoneyTransaction.BalanceImpact.SAVING_OUTFLOW
        || balanceImpact == MoneyTransaction.BalanceImpact.INVESTMENT_OUTFLOW;
  }

  private boolean isOutflowImpact(MoneyTransaction.BalanceImpact balanceImpact) {
    return isExpenseImpact(balanceImpact) || isSavingImpact(balanceImpact);
  }

  private boolean usesPlanItemForFinancialProjection(MonthlyPlanItem item) {
    return item.getStatus() != MonthlyPlanItem.Status.CANCELLED && item.getType() != MonthlyPlanItem.Type.TODO;
  }

  private boolean usesPlanItemForExecutionComparison(MonthlyPlanItem item) {
    if (!usesPlanItemForFinancialProjection(item)) return false;
    return item.getType() == MonthlyPlanItem.Type.EXPENSE
        || item.getType() == MonthlyPlanItem.Type.SAVING
        || item.getType() == MonthlyPlanItem.Type.DEBT;
  }

  private BigDecimal signedPlannedNet(MonthlyPlanItem item) {
    var calculated = monthlyPlanAmountCalculator.calculate(item);
    var value = calculated.netMax();
    return isIncomingPlanItem(item) ? value : value.negate();
  }

  private BigDecimal plannedComparableAmount(MonthlyPlanItem item) {
    return signedPlannedNet(item).abs();
  }

  private boolean isIncomingPlanItem(MonthlyPlanItem item) {
    return item.getType() == MonthlyPlanItem.Type.INCOME || item.getType() == MonthlyPlanItem.Type.RECOVERY;
  }

  private String executionStatus(BigDecimal planned, BigDecimal real) {
    if (planned.signum() <= 0 && real.signum() > 0) return "EXCEEDED";
    if (planned.signum() <= 0) return "OK";

    var percentage = real.multiply(new BigDecimal("100")).divide(planned, 2, RoundingMode.HALF_UP);

    if (percentage.compareTo(new BigDecimal("120")) > 0) return "CRITICAL";
    if (percentage.compareTo(new BigDecimal("100")) > 0) return "EXCEEDED";
    if (percentage.compareTo(new BigDecimal("85")) >= 0) return "WARNING";
    return "OK";
  }

  private int executionStatusPriority(String status) {
    return switch (status) {
      case "CRITICAL" -> 1;
      case "EXCEEDED" -> 2;
      case "WARNING" -> 3;
      default -> 4;
    };
  }

  private String inferRisk(String text) {
    var normalized = text == null ? "" : text.toLowerCase();

    if (normalized.contains("negativo") || normalized.contains("superan")) return "CRITICAL";
    if (normalized.contains("riesgo") || normalized.contains("desvío") || normalized.contains("exced")) return "RISK";
    if (normalized.contains("pendiente") || normalized.contains("revis") || normalized.contains("cotizar")) return "WATCH";
    return "OK";
  }

  private int riskPriority(String riskLevel) {
    return switch (riskLevel) {
      case "CRITICAL" -> 1;
      case "RISK" -> 2;
      case "WATCH" -> 3;
      default -> 4;
    };
  }

  private String movementLabel(long count) {
    return count == 1 ? "movimiento" : "movimientos";
  }

  private BigDecimal sum(List<BigDecimal> values) {
    return values.stream().reduce(BigDecimal.ZERO, BigDecimal::add);
  }

  private BigDecimal percent(BigDecimal amount, BigDecimal income) {
    if (income.signum() == 0) {
      return BigDecimal.ZERO;
    }
    return amount.multiply(new BigDecimal("100")).divide(income, 2, RoundingMode.HALF_UP);
  }

  private static class CategoryDeviationAccumulator {
    UUID categoryId;
    String categoryName;
    Category.Type categoryType;
    BigDecimal plannedAmount = BigDecimal.ZERO;
    BigDecimal realConfirmedAmount = BigDecimal.ZERO;
    BigDecimal pendingPlannedAmount = BigDecimal.ZERO;
    BigDecimal realUnplannedAmount = BigDecimal.ZERO;
    long plannedCount = 0;
    long realCount = 0;
  }
}
