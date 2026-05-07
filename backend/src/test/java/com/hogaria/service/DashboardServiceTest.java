package com.hogaria.service;

import com.hogaria.entity.*;
import com.hogaria.repository.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DashboardServiceTest {
  @Mock FinancialProfileRepository profileRepository; @Mock MoneyTransactionRepository transactionRepository; @Mock CategoryRepository categoryRepository; @Mock BudgetYearRepository budgetYearRepository; @Mock BudgetMonthRepository budgetMonthRepository; @Mock BudgetCategoryItemRepository budgetCategoryItemRepository; @InjectMocks DashboardService service;

  @Test void dashboardBudgetSummaryMatchesExpenseAndSavingsOnly(){
    var u=UUID.randomUUID(); var p=UUID.randomUUID(); var byId=UUID.randomUUID(); var bmId=UUID.randomUUID();
    var rentId=UUID.randomUUID(); var marketId=UUID.randomUUID(); var saveId=UUID.randomUUID(); var salaryId=UUID.randomUUID();
    when(profileRepository.findByIdAndUserId(p,u)).thenReturn(Optional.of(new FinancialProfile()));
    when(transactionRepository.findByProfileIdAndBudgetDateBetween(eq(p),any(),any())).thenReturn(List.of(
      MoneyTransaction.builder().profileId(p).categoryId(salaryId).movementType(MoneyTransaction.MovementType.INCOME).amount(new BigDecimal("1000000")).status(MoneyTransaction.Status.CONFIRMED).budgetDate(LocalDate.now()).build(),
      MoneyTransaction.builder().profileId(p).categoryId(rentId).movementType(MoneyTransaction.MovementType.EXPENSE).amount(new BigDecimal("300000")).status(MoneyTransaction.Status.CONFIRMED).budgetDate(LocalDate.now()).build(),
      MoneyTransaction.builder().profileId(p).categoryId(marketId).movementType(MoneyTransaction.MovementType.EXPENSE).amount(new BigDecimal("180000")).status(MoneyTransaction.Status.CONFIRMED).budgetDate(LocalDate.now()).build(),
      MoneyTransaction.builder().profileId(p).categoryId(saveId).movementType(MoneyTransaction.MovementType.SAVING).amount(new BigDecimal("200000")).status(MoneyTransaction.Status.CONFIRMED).budgetDate(LocalDate.now()).build()
    ));
    when(categoryRepository.findAllById(any())).thenReturn(List.of(
      Category.builder().id(salaryId).name("Sueldo").type(Category.Type.INCOME).build(),
      Category.builder().id(rentId).name("Alquiler").type(Category.Type.FIXED_EXPENSE).build(),
      Category.builder().id(marketId).name("Supermercado").type(Category.Type.VARIABLE_EXPENSE).build(),
      Category.builder().id(saveId).name("Ahorro").type(Category.Type.SAVING).build()
    ));
    when(budgetYearRepository.findByProfileIdAndYear(p,2026)).thenReturn(Optional.of(BudgetYear.builder().id(byId).profileId(p).year(2026).build()));
    when(budgetMonthRepository.findByBudgetYearIdAndMonth(byId,5)).thenReturn(Optional.of(BudgetMonth.builder().id(bmId).budgetYearId(byId).month(5).build()));
    when(budgetCategoryItemRepository.findByBudgetMonthId(bmId)).thenReturn(List.of(
      BudgetCategoryItem.builder().budgetMonthId(bmId).categoryId(rentId).budgetAmount(new BigDecimal("300000")).build(),
      BudgetCategoryItem.builder().budgetMonthId(bmId).categoryId(marketId).budgetAmount(new BigDecimal("150000")).build(),
      BudgetCategoryItem.builder().budgetMonthId(bmId).categoryId(saveId).budgetAmount(new BigDecimal("200000")).build()
    ));

    var res=service.getMonthlySummary(u,p,2026,5);
    assertNotNull(res.budgetSummary());
    assertEquals(new BigDecimal("650000"),res.budgetSummary().totalBudget());
    assertEquals(new BigDecimal("680000"),res.budgetSummary().totalReal());
    assertEquals(new BigDecimal("-30000"),res.budgetSummary().totalDifference());
    assertEquals(1,res.budgetSummary().exceededCount());
  }
}
