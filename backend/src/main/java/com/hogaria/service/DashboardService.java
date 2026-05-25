package com.hogaria.service;

import com.hogaria.dto.DashboardDtos.BudgetSummaryResponse;
import com.hogaria.dto.DashboardDtos.CategorySummaryResponse;
import com.hogaria.dto.DashboardDtos.DashboardOperationalSummaryResponse;
import com.hogaria.dto.DashboardDtos.DashboardSummaryResponse;
import com.hogaria.dto.DashboardDtos.FiftyThirtyTwentyResponse;
import com.hogaria.dto.DashboardDtos.MonthlyBalanceResponse;
import com.hogaria.dto.DashboardDtos.MonthlyCashFlowSummaryResponse;
import com.hogaria.dto.DashboardDtos.PlanningDashboardSummaryResponse;
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
import java.util.LinkedHashMap;
import java.util.List;
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
  private final FinancialCashFlowClassifier classifier;
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
      FinancialCashFlowClassifier classifier,
      MonthlyPlanAmountCalculator monthlyPlanAmountCalculator) {
    this.profileRepository = profileRepository;
    this.transactionRepository = transactionRepository;
    this.categoryRepository = categoryRepository;
    this.budgetYearRepository = budgetYearRepository;
    this.budgetMonthRepository = budgetMonthRepository;
    this.budgetCategoryItemRepository = budgetCategoryItemRepository;
    this.monthlyPlanItemRepository = monthlyPlanItemRepository;
    this.externalSyncMappingRepository = externalSyncMappingRepository;
    this.classifier = classifier;
    this.monthlyPlanAmountCalculator = monthlyPlanAmountCalculator;
  }

  public DashboardSummaryResponse getMonthlySummary(UUID userId, UUID profileId, int year, int month) {
    profileRepository
        .findByIdAndUserId(profileId, userId)
        .orElseThrow(() -> new ForbiddenException("Profile does not belong to user"));

    var from = LocalDate.of(year, month, 1);
    var to = from.withDayOfMonth(from.lengthOfMonth());
    var txs =
        transactionRepository.findByProfileIdAndBudgetDateBetween(profileId, from, to).stream()
            .filter(t -> t.getStatus() == MoneyTransaction.Status.CONFIRMED)
            .toList();
    var categoryIds =
        txs.stream().map(MoneyTransaction::getCategoryId).filter(id -> id != null).collect(Collectors.toSet());
    var categories =
        categoryRepository.findAllById(categoryIds).stream()
            .collect(Collectors.toMap(Category::getId, Function.identity()));

    var planning = buildPlanning(profileId, year, month);
    var cashFlowSummary = buildCashFlowSummary(profileId, txs, categories);

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

    var breakdown = buildBreakdown(txs, categories, income);
    var budgetSummary = buildBudgetSummary(profileId, year, month, txs);
    var operational = buildOperational(cashFlowSummary, planning, txs.isEmpty());

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
        cashFlowSummary);
  }

  private List<CategorySummaryResponse> buildBreakdown(
      List<MoneyTransaction> txs, Map<UUID, Category> categories, BigDecimal income) {
    Map<UUID, List<MoneyTransaction>> byCategory = new LinkedHashMap<>();
    for (var tx : txs) {
      byCategory.computeIfAbsent(tx.getCategoryId(), ignored -> new ArrayList<>()).add(tx);
    }

    return byCategory.entrySet().stream()
        .map(
            entry -> {
              var category = categories.get(entry.getKey());
              var total = sum(entry.getValue().stream().map(MoneyTransaction::getAmount).toList());
              return new CategorySummaryResponse(
                  entry.getKey(),
                  category == null ? "Unknown" : category.getName(),
                  category == null ? null : category.getType(),
                  total,
                  percent(total, income),
                  entry.getValue().size());
            })
        .toList();
  }

  private BudgetSummaryResponse buildBudgetSummary(
      UUID profileId, int year, int month, List<MoneyTransaction> txs) {
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
    BigDecimal totalBudget = itemMap.values().stream().reduce(BigDecimal.ZERO, BigDecimal::add);
    BigDecimal totalReal =
        txs.stream()
            .filter(t -> itemMap.containsKey(t.getCategoryId()))
            .map(MoneyTransaction::getAmount)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    long exceeded =
        itemMap.entrySet().stream()
            .filter(
                entry -> {
                  var real =
                      txs.stream()
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
                      txs.stream()
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

  private PlanningDashboardSummaryResponse buildPlanning(UUID profileId, int year, int month) {
    var items = monthlyPlanItemRepository.findByProfileIdAndPeriodYearAndPeriodMonth(profileId, year, month);
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

  private MonthlyCashFlowSummaryResponse buildCashFlowSummary(
      UUID profileId, List<MoneyTransaction> txs, Map<UUID, Category> categories) {
    var mappingByTx =
        externalSyncMappingRepository.findByProfileId(profileId).stream()
            .filter(m -> m.getMoneyTransactionId() != null)
            .collect(Collectors.toMap(ExternalSyncMapping::getMoneyTransactionId, ExternalSyncMapping::getExternalEventType, (a, b) -> a));
    BigDecimal gross = BigDecimal.ZERO, consumption = BigDecimal.ZERO, fixed = BigDecimal.ZERO, variable = BigDecimal.ZERO;
    BigDecimal debt = BigDecimal.ZERO, saving = BigDecimal.ZERO, investment = BigDecimal.ZERO, recoverableOut = BigDecimal.ZERO;
    BigDecimal principal = BigDecimal.ZERO, refund = BigDecimal.ZERO, earned = BigDecimal.ZERO, interest = BigDecimal.ZERO;
    BigDecimal internalTransfer = BigDecimal.ZERO, externalTransfer = BigDecimal.ZERO, neutral = BigDecimal.ZERO, unknown = BigDecimal.ZERO;
    List<String> alerts = new ArrayList<>();

    for (var tx : txs) {
      var treatment = classifier.classify(tx, categories.get(tx.getCategoryId()), mappingByTx.get(tx.getId()));
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
        case NEUTRAL_ADJUSTMENT -> neutral = neutral.add(amount);
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

  private BigDecimal sum(List<BigDecimal> values) {
    return values.stream().reduce(BigDecimal.ZERO, BigDecimal::add);
  }

  private BigDecimal percent(BigDecimal amount, BigDecimal income) {
    if (income.signum() == 0) {
      return BigDecimal.ZERO;
    }
    return amount.multiply(new BigDecimal("100")).divide(income, 2, RoundingMode.HALF_UP);
  }
}
