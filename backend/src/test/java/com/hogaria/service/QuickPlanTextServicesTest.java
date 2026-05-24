package com.hogaria.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.hogaria.dto.MonthlyPlanDtos.MonthlyPlanItemResponse;
import com.hogaria.dto.QuickPlanTextDtos.*;
import com.hogaria.entity.FinancialProfile;
import com.hogaria.entity.MonthlyPlanItem;
import com.hogaria.repository.CategoryRepository;
import com.hogaria.repository.FinancialProfileRepository;
import com.hogaria.repository.MonthlyPlanItemRepository;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import org.junit.jupiter.api.Test;

class QuickPlanTextServicesTest {
  @Test void parser_argentinian_amounts() {
    var p = new QuickPlanTextParserService();
    assertEquals(new BigDecimal("430000.00"), p.parseLine("Alquiler $ 430.000", AmountScale.UNITS, null).amount());
    assertEquals(new BigDecimal("68653.06"), p.parseLine("Prepaga 68.653,06", AmountScale.UNITS, null).amount());
    assertEquals(new BigDecimal("430000.00"), p.parseLine("Seguro 430 mil", AmountScale.UNITS, null).amount());
    assertEquals(new BigDecimal("1500000.00"), p.parseLine("Viaje 1,5M", AmountScale.UNITS, null).amount());
    assertEquals(new BigDecimal("430000.00"), p.parseLine("Cochera 430k", AmountScale.UNITS, null).amount());
  }

  @Test void classification_debt_health_travel() {
    var c = new QuickPlanClassificationService();
    assertEquals(MonthlyPlanItem.Type.DEBT, c.classify("tarjeta vencimiento 120").type());
    assertEquals("salud", c.classify("psiquiatra 70").categoryHint());
    assertEquals("transporte", c.classify("viaje mdp 60").categoryHint());
  }

  @Test void commit_creates_and_dedupe() {
    FinancialProfileRepository profiles = mock(FinancialProfileRepository.class);
    MonthlyPlanService monthlyPlanService = mock(MonthlyPlanService.class);
    MonthlyPlanItemRepository repo = mock(MonthlyPlanItemRepository.class);
    CategoryRepository categories = mock(CategoryRepository.class);
    var service = new QuickPlanTextImportService(profiles, monthlyPlanService, repo, categories, new QuickPlanTextParserService(), new QuickPlanClassificationService());
    UUID userId = UUID.randomUUID(), profileId = UUID.randomUUID();
    when(profiles.findByIdAndUserId(profileId, userId)).thenReturn(Optional.of(new FinancialProfile()));
    when(repo.findByProfileIdAndPeriodYearAndPeriodMonth(profileId, 2026, 5)).thenReturn(List.of());
    var candidate = new NormalizedCandidate(1, "Hostal", MonthlyPlanItem.Type.EXPENSE, MonthlyPlanItem.Priority.IMPORTANT, new BigDecimal("100"), null, null, null, null);
    when(monthlyPlanService.create(eq(userId), eq(profileId), any())).thenReturn(new MonthlyPlanItemResponse(UUID.randomUUID(), profileId, null, null, MonthlyPlanItem.Type.EXPENSE, "Hostal", null, null, 2026, 5, new BigDecimal("100"), null, null, "ARS", null, null, null, null, null, MonthlyPlanItem.Priority.IMPORTANT, MonthlyPlanItem.Status.ESTIMATED, MonthlyPlanItem.Source.QUICK_CAPTURE, null, new BigDecimal("100"), new BigDecimal("100"), BigDecimal.ZERO, BigDecimal.ZERO, new BigDecimal("100"), new BigDecimal("100"), LocalDateTime.now(), LocalDateTime.now()));
    var res = service.commit(userId, profileId, new QuickPlanTextCommitRequest(2026, 5, List.of(candidate), true));
    assertEquals(1, res.created().size());

    var existing = MonthlyPlanItem.builder().title("Hostal").periodYear(2026).periodMonth(5).amount(new BigDecimal("100")).build();
    when(repo.findByProfileIdAndPeriodYearAndPeriodMonth(profileId, 2026, 5)).thenReturn(List.of(existing));
    var deduped = service.commit(userId, profileId, new QuickPlanTextCommitRequest(2026, 5, List.of(candidate), true));
    assertEquals(0, deduped.created().size());
    assertEquals(1, deduped.skippedDuplicates());
  }
}
