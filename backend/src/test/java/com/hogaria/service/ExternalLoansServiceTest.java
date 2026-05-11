package com.hogaria.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.hogaria.exception.ForbiddenException;
import com.hogaria.integration.cjprestamos.CjPrestamosClient;
import com.hogaria.integration.cjprestamos.CjPrestamosIntegrationException;
import com.hogaria.integration.cjprestamos.CjPrestamosProperties;
import com.hogaria.integration.cjprestamos.dto.ExternalLoanCashControlResponse;
import com.hogaria.integration.cjprestamos.dto.ExternalLoanResponse;
import com.hogaria.integration.cjprestamos.dto.ExternalLoanSummaryResponse;
import com.hogaria.repository.FinancialProfileRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ExternalLoansServiceTest {
  @Mock CjPrestamosClient client;
  @Mock FinancialProfileRepository profileRepository;
  @Mock CjPrestamosProperties properties;
  @InjectMocks ExternalLoansService service;

  @Test void returnsDisabledWhenIntegrationOff() {
    UUID userId = UUID.randomUUID(); UUID profileId = UUID.randomUUID();
    when(profileRepository.existsByIdAndUserId(profileId, userId)).thenReturn(true);
    when(properties.enabled()).thenReturn(false);

    var response = service.getSummary(userId, profileId);

    assertEquals("DISABLED", response.status());
    verifyNoInteractions(client);
  }

  @Test void throwsWhenProfileDoesNotBelongToUser() {
    UUID userId = UUID.randomUUID(); UUID profileId = UUID.randomUUID();
    when(profileRepository.existsByIdAndUserId(profileId, userId)).thenReturn(false);

    assertThrows(ForbiddenException.class, () -> service.getSummary(userId, profileId));
    verifyNoInteractions(client);
  }

  @Test void returnsDashboardWhenClientResponds() {
    UUID userId = UUID.randomUUID(); UUID profileId = UUID.randomUUID();
    when(profileRepository.existsByIdAndUserId(profileId, userId)).thenReturn(true);
    when(properties.enabled()).thenReturn(true);
    var dashboard = new ExternalLoanSummaryResponse(3, new BigDecimal("1000"), new BigDecimal("50"), new BigDecimal("80"), new BigDecimal("120"), new BigDecimal("0.88"), new BigDecimal("0.05"));
    var cash = new ExternalLoanCashControlResponse(LocalDate.now(), BigDecimal.TEN, BigDecimal.ONE, BigDecimal.ONE, BigDecimal.ONE, BigDecimal.ZERO, BigDecimal.TEN);
    var loans = List.of(new ExternalLoanResponse(UUID.randomUUID(), "Ana", new BigDecimal("200"), new BigDecimal("100"), new BigDecimal("25"), LocalDate.now(), LocalDate.now(), "ACTIVE", 12, 5, BigDecimal.ZERO));
    when(client.getDashboardSummary(profileId, userId)).thenReturn(dashboard);
    when(client.getCashControl(profileId, userId)).thenReturn(cash);
    when(client.getActiveLoans(profileId, userId)).thenReturn(loans);

    var response = service.getSummary(userId, profileId);

    assertEquals("ENABLED", response.status());
    assertEquals(3, response.dashboard().activeLoans());
    assertEquals(1, response.activeLoans().size());
  }

  @Test void propagatesRemoteError() {
    UUID userId = UUID.randomUUID(); UUID profileId = UUID.randomUUID();
    when(profileRepository.existsByIdAndUserId(profileId, userId)).thenReturn(true);
    when(properties.enabled()).thenReturn(true);
    when(client.getDashboardSummary(profileId, userId)).thenThrow(new CjPrestamosIntegrationException("timeout"));

    assertThrows(CjPrestamosIntegrationException.class, () -> service.getSummary(userId, profileId));
  }
}
