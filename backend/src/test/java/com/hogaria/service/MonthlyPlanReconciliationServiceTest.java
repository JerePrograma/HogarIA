package com.hogaria.service;

import com.hogaria.entity.*;
import com.hogaria.repository.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MonthlyPlanReconciliationServiceTest {
  @Mock FinancialProfileRepository profiles;
  @Mock MonthlyPlanItemRepository itemRepo;
  @Mock MoneyTransactionRepository txRepo;
  @Mock MonthlyPlanTransactionMatchRepository matchRepo;
  @InjectMocks MonthlyPlanReconciliationService service;

  UUID userId = UUID.randomUUID(); UUID profileId = UUID.randomUUID();

  void ok(){ when(profiles.findByIdAndUserId(profileId, userId)).thenReturn(Optional.of(new FinancialProfile())); }

  @Test void unplannedExcluyeMatchPersistidoYLegacyYDeduplica() {
    ok();
    var item1 = MonthlyPlanItem.builder().id(UUID.randomUUID()).profileId(profileId).periodYear(2026).periodMonth(5).type(MonthlyPlanItem.Type.EXPENSE).title("i1").amount(new BigDecimal("100")).build();
    var legacyTxId = UUID.randomUUID();
    var item2 = MonthlyPlanItem.builder().id(UUID.randomUUID()).profileId(profileId).periodYear(2026).periodMonth(5).type(MonthlyPlanItem.Type.EXPENSE).title("i2").amount(new BigDecimal("20")).transactionId(legacyTxId).build();
    when(itemRepo.findByProfileIdAndPeriodYearAndPeriodMonth(profileId, 2026, 5)).thenReturn(List.of(item1, item2));

    var persistedTxId = UUID.randomUUID();
    var tx1 = tx(persistedTxId, "50", MoneyTransaction.Status.CONFIRMED);
    var tx2 = tx(legacyTxId, "20", MoneyTransaction.Status.CONFIRMED);
    var tx3 = tx(UUID.randomUUID(), "10", MoneyTransaction.Status.CONFIRMED);
    when(txRepo.findByProfileIdAndBudgetDateBetween(eq(profileId), any(), any())).thenReturn(List.of(tx1, tx2, tx3, tx3));

    when(matchRepo.findByProfileIdAndMonthlyPlanItemIdIn(eq(profileId), any())).thenReturn(List.of(
      MonthlyPlanTransactionMatch.builder().id(UUID.randomUUID()).profileId(profileId).monthlyPlanItemId(item1.getId()).moneyTransactionId(persistedTxId).matchedAmount(new BigDecimal("50")).matchType(MonthlyPlanTransactionMatch.MatchType.MANUAL).confidence(MonthlyPlanTransactionMatch.Confidence.HIGH).build()
    ));
    when(matchRepo.findByProfileIdAndMoneyTransactionIdIn(eq(profileId), any())).thenReturn(List.of());

    var summary = service.getSummary(userId, profileId, 2026, 5);
    assertEquals(1, summary.unplannedCount());
    assertEquals(new BigDecimal("10"), summary.unplannedTransactionsTotal());
  }

  @Test void ignoredYPendingNoCuentanComoUnplannedYSugerenciasExcluyenVinculadas() {
    ok();
    var item = MonthlyPlanItem.builder().id(UUID.randomUUID()).profileId(profileId).periodYear(2026).periodMonth(5).type(MonthlyPlanItem.Type.EXPENSE).title("i").amount(new BigDecimal("30")).build();
    when(itemRepo.findByProfileIdAndPeriodYearAndPeriodMonth(profileId, 2026, 5)).thenReturn(List.of(item));

    var matchedTxId = UUID.randomUUID();
    var confirmedMatched = tx(matchedTxId, "30", MoneyTransaction.Status.CONFIRMED);
    var ignored = tx(UUID.randomUUID(), "100", MoneyTransaction.Status.IGNORED);
    var pending = tx(UUID.randomUUID(), "200", MoneyTransaction.Status.PENDING);
    when(txRepo.findByProfileIdAndBudgetDateBetween(eq(profileId), any(), any())).thenReturn(List.of(confirmedMatched, ignored, pending));
    when(matchRepo.findByProfileIdAndMonthlyPlanItemIdIn(eq(profileId), any())).thenReturn(List.of(
      MonthlyPlanTransactionMatch.builder().id(UUID.randomUUID()).profileId(profileId).monthlyPlanItemId(item.getId()).moneyTransactionId(matchedTxId).matchedAmount(new BigDecimal("30")).matchType(MonthlyPlanTransactionMatch.MatchType.MANUAL).confidence(MonthlyPlanTransactionMatch.Confidence.HIGH).build()
    ));
    when(matchRepo.findByProfileIdAndMoneyTransactionIdIn(eq(profileId), any())).thenReturn(List.of());

    var summary = service.getSummary(userId, profileId, 2026, 5);
    assertEquals(0, summary.unplannedCount());
    assertTrue(summary.suggestedMatches().isEmpty());
  }

  private MoneyTransaction tx(UUID id, String amount, MoneyTransaction.Status status){
    return MoneyTransaction.builder().id(id).profileId(profileId).budgetDate(LocalDate.of(2026,5,1)).movementType(MoneyTransaction.MovementType.EXPENSE).amount(new BigDecimal(amount)).status(status).build();
  }
}
