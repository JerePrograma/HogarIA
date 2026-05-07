package com.hogaria.service;

import com.hogaria.dto.BudgetDtos.BudgetStatus;
import com.hogaria.entity.*;
import com.hogaria.repository.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BudgetServiceTest {
  @Mock FinancialProfileRepository pr; @Mock BudgetYearRepository byr; @Mock BudgetMonthRepository bmr; @Mock BudgetCategoryItemRepository bir; @Mock CategoryRepository cr; @Mock MoneyTransactionRepository tr; @InjectMocks BudgetService s;

  @Test void comparisonExcludesIncome(){
    var u=UUID.randomUUID(); var p=UUID.randomUUID(); var byId=UUID.randomUUID(); var bmId=UUID.randomUUID();
    var rentId=UUID.randomUUID(); var marketId=UUID.randomUUID(); var saveId=UUID.randomUUID(); var salaryId=UUID.randomUUID();
    when(pr.findByIdAndUserId(p,u)).thenReturn(Optional.of(new FinancialProfile()));
    when(byr.findByProfileIdAndYear(p,2026)).thenReturn(Optional.of(BudgetYear.builder().id(byId).profileId(p).year(2026).build()));
    when(bmr.findByBudgetYearIdAndMonth(byId,5)).thenReturn(Optional.of(BudgetMonth.builder().id(bmId).budgetYearId(byId).month(5).build()));
    when(bir.findByBudgetMonthId(bmId)).thenReturn(List.of(
      BudgetCategoryItem.builder().budgetMonthId(bmId).categoryId(rentId).budgetAmount(new BigDecimal("300000")).build(),
      BudgetCategoryItem.builder().budgetMonthId(bmId).categoryId(marketId).budgetAmount(new BigDecimal("150000")).build(),
      BudgetCategoryItem.builder().budgetMonthId(bmId).categoryId(saveId).budgetAmount(new BigDecimal("200000")).build()
    ));
    when(tr.findByProfileIdAndBudgetDateBetween(eq(p),any(),any())).thenReturn(List.of(
      MoneyTransaction.builder().profileId(p).categoryId(salaryId).amount(new BigDecimal("1000000")).status(MoneyTransaction.Status.CONFIRMED).budgetDate(LocalDate.of(2026,5,2)).build(),
      MoneyTransaction.builder().profileId(p).categoryId(rentId).amount(new BigDecimal("300000")).status(MoneyTransaction.Status.CONFIRMED).budgetDate(LocalDate.of(2026,5,5)).build(),
      MoneyTransaction.builder().profileId(p).categoryId(marketId).amount(new BigDecimal("180000")).status(MoneyTransaction.Status.CONFIRMED).budgetDate(LocalDate.of(2026,5,6)).build(),
      MoneyTransaction.builder().profileId(p).categoryId(saveId).amount(new BigDecimal("200000")).status(MoneyTransaction.Status.CONFIRMED).budgetDate(LocalDate.of(2026,5,10)).build()
    ));
    when(cr.findAllById(any())).thenReturn(List.of(
      Category.builder().id(rentId).name("Alquiler").type(Category.Type.FIXED_EXPENSE).build(),
      Category.builder().id(marketId).name("Supermercado").type(Category.Type.VARIABLE_EXPENSE).build(),
      Category.builder().id(saveId).name("Ahorro").type(Category.Type.SAVING).build(),
      Category.builder().id(salaryId).name("Sueldo").type(Category.Type.INCOME).build()
    ));

    var out=s.getComparison(u,p,2026,5);
    assertEquals(new BigDecimal("650000"),out.totalBudget());
    assertEquals(new BigDecimal("680000"),out.totalReal());
    assertEquals(new BigDecimal("-30000"),out.totalDifference());
    assertTrue(out.items().stream().noneMatch(i->i.categoryType()==Category.Type.INCOME));
    assertTrue(out.items().stream().anyMatch(i->i.categoryName().equals("Supermercado")&&i.status()==BudgetStatus.EXCEEDED));
  }
}
