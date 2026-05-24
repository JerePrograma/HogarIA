package com.hogaria.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.hogaria.dto.BancoProvinciaLoanImportDtos.*;
import com.hogaria.dto.MonthlyPlanDtos;
import com.hogaria.entity.*;
import com.hogaria.repository.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import org.junit.jupiter.api.Test;

class BancoProvinciaLoanImportServiceTest {
  @Test void previewYCommitConDuplicadosYCategoriaNula() {
    var profiles = mock(FinancialProfileRepository.class); var parser = mock(BancoProvinciaLoanExcelParserService.class); var planRepo = mock(MonthlyPlanItemRepository.class); var planSvc = mock(MonthlyPlanService.class); var catRepo = mock(CategoryRepository.class);
    UUID userId = UUID.randomUUID(), profileId = UUID.randomUUID();
    when(profiles.findByIdAndUserId(profileId, userId)).thenReturn(Optional.of(new FinancialProfile()));
    when(parser.parse(any())).thenReturn(List.of(new BancoProvinciaLoanExcelParserService.ParsedLoanRow(3, "t", "i", "6176-1", new BigDecimal("1000"), new BigDecimal("1200"), LocalDate.of(2026, 7, 1))));
    when(planRepo.findByProfileIdAndPeriodYearAndPeriodMonth(profileId, 2026, 5)).thenReturn(List.of());
    when(catRepo.findByProfileIdAndActiveTrue(profileId)).thenReturn(List.of());
    when(planSvc.create(eq(userId), eq(profileId), any())).thenReturn(new MonthlyPlanDtos.MonthlyPlanItemResponse(UUID.randomUUID(), profileId, null, null, MonthlyPlanItem.Type.DEBT, "x", null, null, 2026, 5, new BigDecimal("500"), null, null, "ARS", null, null, null, null, null, MonthlyPlanItem.Priority.ESSENTIAL, MonthlyPlanItem.Status.ESTIMATED, MonthlyPlanItem.Source.IMPORT, null, null, null, null, null, null, null, null, null));
    var svc = new BancoProvinciaLoanImportService(profiles, parser, planRepo, planSvc, catRepo);
    var preview = svc.preview(userId, profileId, 2026, 5, "ARS", null);
    assertTrue(preview.candidates().get(0).warnings().stream().anyMatch(w -> w.contains("Estimación lineal")));
    assertEquals("500.00", preview.candidates().get(0).estimatedMonthlyAmount().toPlainString());
    var commitReq = new BancoProvinciaLoanCommitRequest(2026, 5, List.of(new BancoProvinciaLoanCommitCandidate(3, "t", "i", "6176-1", new BigDecimal("1000"), new BigDecimal("1200"), LocalDate.of(2026, 7, 1), 2, new BigDecimal("500.00"), "Provincia préstamo 6176-1", "ARS", List.of())), true, true);
    var commit = svc.commit(userId, profileId, commitReq);
    assertEquals(1, commit.created().size());
    verify(planSvc, times(1)).create(eq(userId), eq(profileId), any());
  }
}
