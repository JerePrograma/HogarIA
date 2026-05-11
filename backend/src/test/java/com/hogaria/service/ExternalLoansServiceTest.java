package com.hogaria.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.hogaria.exception.ForbiddenException;
import com.hogaria.integration.cjprestamos.CjPrestamosClient;
import com.hogaria.integration.cjprestamos.CjPrestamosIntegrationException;
import com.hogaria.integration.cjprestamos.CjPrestamosProperties;
import com.hogaria.integration.cjprestamos.mapper.CjPrestamosBridgeMapper;
import com.hogaria.integration.cjprestamos.remote.CjPrestamosCashControlRemoteResponse;
import com.hogaria.integration.cjprestamos.remote.CjPrestamosDashboardRemoteResponse;
import com.hogaria.integration.cjprestamos.remote.CjPrestamosLoanActiveRemoteResponse;
import com.hogaria.repository.FinancialProfileRepository;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ExternalLoansServiceTest {
  @Mock CjPrestamosClient client;
  @Mock FinancialProfileRepository profileRepository;
  @Mock CjPrestamosProperties properties;

  ExternalLoansService service;

  @BeforeEach void setUp() {
    service = new ExternalLoansService(client, properties, profileRepository, new CjPrestamosBridgeMapper());
  }

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
    var dashboard = new CjPrestamosDashboardRemoteResponse(new BigDecimal("1000"), new BigDecimal("80"), new BigDecimal("120"), new BigDecimal("50"), 3L);
    var cash = new CjPrestamosCashControlRemoteResponse(BigDecimal.TEN, BigDecimal.ONE, BigDecimal.ONE, BigDecimal.ONE, BigDecimal.ONE, BigDecimal.ONE, BigDecimal.ONE, BigDecimal.ONE, BigDecimal.ONE, BigDecimal.ONE, BigDecimal.ONE, BigDecimal.ONE, BigDecimal.ZERO, 2L, 1L, new BigDecimal("0.9"), new BigDecimal("0.2"));
    var loans = List.of(new CjPrestamosLoanActiveRemoteResponse(10L, 22L, "Ana", new BigDecimal("200"), 12, "MENSUAL", "ACTIVE", new BigDecimal("50"), new BigDecimal("150"), new BigDecimal("10"), new BigDecimal("20"), LocalDateTime.now(), LocalDateTime.now()));
    when(client.getDashboardSummary(profileId, userId)).thenReturn(dashboard);
    when(client.getCashControl(profileId, userId)).thenReturn(cash);
    when(client.getActiveLoans(profileId, userId)).thenReturn(loans);

    var response = service.getSummary(userId, profileId);

    assertEquals("ENABLED", response.status());
    assertEquals(3L, response.dashboard().activeLoans());
    assertEquals(10L, response.activeLoans().getFirst().externalLoanId());
  }

  @Test void propagatesRemoteError() {
    UUID userId = UUID.randomUUID(); UUID profileId = UUID.randomUUID();
    when(profileRepository.existsByIdAndUserId(profileId, userId)).thenReturn(true);
    when(properties.enabled()).thenReturn(true);
    when(client.getDashboardSummary(profileId, userId)).thenThrow(new CjPrestamosIntegrationException("timeout"));
    assertThrows(CjPrestamosIntegrationException.class, () -> service.getSummary(userId, profileId));
  }
}
