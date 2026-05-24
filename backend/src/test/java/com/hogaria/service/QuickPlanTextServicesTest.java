package com.hogaria.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.hogaria.dto.MonthlyPlanDtos.MonthlyPlanItemCreateRequest;
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
  @Test void parser_exact_thousands_and_approx() {
    var p = new QuickPlanTextParserService();
    var exact = p.parseLine("Hostal 430", AmountScale.THOUSANDS, new BigDecimal("0.2"));
    assertEquals(new BigDecimal("430000.00"), exact.amount());
    var approx = p.parseLine("Viaje como 60", AmountScale.THOUSANDS, new BigDecimal("0.2"));
    assertNull(approx.amount());
    assertEquals(new BigDecimal("48000.00"), approx.minAmount());
    assertEquals(new BigDecimal("72000.00"), approx.maxAmount());
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
    var reqItem = new MonthlyPlanItemCreateRequest(MonthlyPlanItem.Type.EXPENSE, "Hostal", null, null, 2026, 5, new BigDecimal("100"), null, null, "ARS", null, null, null, null, null, MonthlyPlanItem.Priority.IMPORTANT, MonthlyPlanItem.Status.ESTIMATED, MonthlyPlanItem.Source.QUICK_CAPTURE, null, null);
    when(monthlyPlanService.create(eq(userId), eq(profileId), any())).thenReturn(new MonthlyPlanItemResponse(UUID.randomUUID(), profileId, null, null, MonthlyPlanItem.Type.EXPENSE, "Hostal", null, null, 2026, 5, new BigDecimal("100"), null, null, "ARS", null, null, null, null, null, MonthlyPlanItem.Priority.IMPORTANT, MonthlyPlanItem.Status.ESTIMATED, MonthlyPlanItem.Source.QUICK_CAPTURE, null, new BigDecimal("100"), new BigDecimal("100"), BigDecimal.ZERO, BigDecimal.ZERO, new BigDecimal("100"), new BigDecimal("100"), LocalDateTime.now(), LocalDateTime.now()));
    var res = service.commit(userId, profileId, new QuickPlanTextCommitRequest(List.of(reqItem)));
    assertEquals(1, res.created().size());

    var existing = MonthlyPlanItem.builder().title("Hostal").periodYear(2026).periodMonth(5).amount(new BigDecimal("100")).build();
    when(repo.findByProfileIdAndPeriodYearAndPeriodMonth(profileId, 2026, 5)).thenReturn(List.of(existing));
    var deduped = service.commit(userId, profileId, new QuickPlanTextCommitRequest(List.of(reqItem)));
    assertEquals(0, deduped.created().size());
    assertFalse(deduped.warnings().isEmpty());
  }
}
