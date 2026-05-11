package com.hogaria.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.hogaria.entity.Category;
import com.hogaria.entity.ExternalLoanSyncConfig;
import com.hogaria.exception.BadRequestException;
import com.hogaria.exception.ForbiddenException;
import com.hogaria.integration.cjprestamos.CjPrestamosClient;
import com.hogaria.integration.cjprestamos.CjPrestamosIntegrationException;
import com.hogaria.integration.cjprestamos.CjPrestamosProperties;
import com.hogaria.integration.cjprestamos.mapper.CjPrestamosBridgeMapper;
import com.hogaria.integration.cjprestamos.remote.CjPrestamosCashControlRemoteResponse;
import com.hogaria.integration.cjprestamos.remote.CjPrestamosDashboardRemoteResponse;
import com.hogaria.integration.cjprestamos.remote.CjPrestamosLoanActiveRemoteResponse;
import com.hogaria.integration.cjprestamos.remote.CjPrestamosPaymentRemoteResponse;
import com.hogaria.repository.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
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
  @Mock ExternalLoanSyncConfigRepository syncConfigRepository;
  @Mock AccountRepository accountRepository;
  @Mock CategoryRepository categoryRepository;
  @Mock MoneyTransactionRepository transactionRepository;
  @Mock ExternalSyncIdempotencyService idempotencyService;

  ExternalLoansService service;

  @BeforeEach void setUp() {
    service = new ExternalLoansService(client, properties, profileRepository, new CjPrestamosBridgeMapper(), syncConfigRepository, accountRepository, categoryRepository, transactionRepository, idempotencyService);
  }

  @Test void returnsDisabledWhenIntegrationOff() { UUID userId = UUID.randomUUID(); UUID profileId = UUID.randomUUID(); when(profileRepository.existsByIdAndUserId(profileId, userId)).thenReturn(true); when(properties.enabled()).thenReturn(false); var response = service.getSummary(userId, profileId); assertEquals("DISABLED", response.status()); verifyNoInteractions(client); }
  @Test void throwsWhenProfileDoesNotBelongToUser() { UUID userId = UUID.randomUUID(); UUID profileId = UUID.randomUUID(); when(profileRepository.existsByIdAndUserId(profileId, userId)).thenReturn(false); assertThrows(ForbiddenException.class, () -> service.getSummary(userId, profileId)); verifyNoInteractions(client); }
  @Test void returnsDashboardWhenClientResponds() { UUID userId = UUID.randomUUID(); UUID profileId = UUID.randomUUID(); when(profileRepository.existsByIdAndUserId(profileId, userId)).thenReturn(true); when(properties.enabled()).thenReturn(true); var dashboard = new CjPrestamosDashboardRemoteResponse(new BigDecimal("1000"), new BigDecimal("80"), new BigDecimal("120"), new BigDecimal("50"), 3L); var cash = new CjPrestamosCashControlRemoteResponse(BigDecimal.TEN, BigDecimal.ONE, BigDecimal.ONE, BigDecimal.ONE, BigDecimal.ONE, BigDecimal.ONE, BigDecimal.ONE, BigDecimal.ONE, BigDecimal.ONE, BigDecimal.ONE, BigDecimal.ONE, BigDecimal.ONE, BigDecimal.ZERO, 2L, 1L, new BigDecimal("0.9"), new BigDecimal("0.2")); var loans = List.of(new CjPrestamosLoanActiveRemoteResponse(10L, 22L, "Ana", new BigDecimal("200"), 12, "MENSUAL", "ACTIVE", new BigDecimal("50"), new BigDecimal("150"), new BigDecimal("10"), new BigDecimal("20"), LocalDateTime.now(), LocalDateTime.now())); when(client.getDashboardSummary(profileId, userId)).thenReturn(dashboard); when(client.getCashControl(profileId, userId)).thenReturn(cash); when(client.getActiveLoans(profileId, userId)).thenReturn(loans);
    var response = service.getSummary(userId, profileId); assertEquals("ENABLED", response.status()); assertEquals(3L, response.dashboard().activeLoans()); assertEquals(10L, response.activeLoans().getFirst().externalLoanId()); }
  @Test void propagatesRemoteError() { UUID userId = UUID.randomUUID(); UUID profileId = UUID.randomUUID(); when(profileRepository.existsByIdAndUserId(profileId, userId)).thenReturn(true); when(properties.enabled()).thenReturn(true); when(client.getDashboardSummary(profileId, userId)).thenThrow(new CjPrestamosIntegrationException("timeout")); assertThrows(CjPrestamosIntegrationException.class, () -> service.getSummary(userId, profileId)); }

  @Test void syncFailsWhenMissingPrincipalInterestSplit() {
    UUID userId = UUID.randomUUID(); UUID profileId = UUID.randomUUID(); UUID accountId = UUID.randomUUID(); UUID c1 = UUID.randomUUID(); UUID c2 = UUID.randomUUID(); UUID c3 = UUID.randomUUID();
    when(profileRepository.existsByIdAndUserId(profileId, userId)).thenReturn(true);
    when(syncConfigRepository.findByProfileId(profileId)).thenReturn(Optional.of(ExternalLoanSyncConfig.builder().profileId(profileId).enabled(true).accountId(accountId).loanDisbursementCategoryId(c1).principalRecoveryCategoryId(c2).interestIncomeCategoryId(c3).build()));
    when(accountRepository.existsByIdAndProfileId(accountId, profileId)).thenReturn(true);
    when(categoryRepository.findById(any())).thenAnswer(inv -> Optional.of(Category.builder().id(inv.getArgument(0)).profileId(profileId).build()));
    when(client.getActiveLoans(profileId, userId)).thenReturn(List.of(new CjPrestamosLoanActiveRemoteResponse(1L, 1L, "Ana", new BigDecimal("100"), 1, "MENSUAL", "ACTIVE", BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, LocalDateTime.now(), LocalDateTime.now())));
    when(client.getLoanPayments(profileId, userId, 1L)).thenReturn(List.of(new CjPrestamosPaymentRemoteResponse(9L, 1L, LocalDate.now(), new BigDecimal("50"), null, null, "r", "sin split", "OK")));
    when(idempotencyService.isAlreadyProcessed(any(), any(), any(), any(), any(), any())).thenReturn(false);

    var response = service.sync(userId, profileId);
    assertFalse(response.errors().isEmpty());
    assertTrue(response.errors().stream().anyMatch(e -> e.contains("cjprestamos no envió split principal/interés para pago 9")));
  }

  @Test void syncCreatesIncomeForInterestAndNotDuplicate() {
    UUID userId = UUID.randomUUID(); UUID profileId = UUID.randomUUID(); UUID accountId = UUID.randomUUID(); UUID c1 = UUID.randomUUID(); UUID c2 = UUID.randomUUID(); UUID c3 = UUID.randomUUID();
    when(profileRepository.existsByIdAndUserId(profileId, userId)).thenReturn(true);
    when(syncConfigRepository.findByProfileId(profileId)).thenReturn(Optional.of(ExternalLoanSyncConfig.builder().profileId(profileId).enabled(true).accountId(accountId).loanDisbursementCategoryId(c1).principalRecoveryCategoryId(c2).interestIncomeCategoryId(c3).build()));
    when(accountRepository.existsByIdAndProfileId(accountId, profileId)).thenReturn(true);
    when(categoryRepository.findById(any())).thenAnswer(inv -> Optional.of(Category.builder().id(inv.getArgument(0)).profileId(profileId).build()));
    when(client.getActiveLoans(profileId, userId)).thenReturn(List.of(new CjPrestamosLoanActiveRemoteResponse(1L, 1L, "Ana", new BigDecimal("100"), 1, "MENSUAL", "ACTIVE", BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, LocalDateTime.now(), LocalDateTime.now())));
    when(client.getLoanPayments(profileId, userId, 1L)).thenReturn(List.of(new CjPrestamosPaymentRemoteResponse(9L, 1L, LocalDate.now(), new BigDecimal("50"), new BigDecimal("30"), new BigDecimal("20"), "r", null, "OK")));
    when(idempotencyService.isAlreadyProcessed(any(), any(), any(), any(), eq("9"), any())).thenReturn(false);
    when(idempotencyService.isAlreadyProcessed(any(), any(), any(), any(), eq("1"), any())).thenReturn(true);

    var response = service.sync(userId, profileId);
    assertEquals(1, response.paymentsSynced());
    assertEquals(2, response.movementsCreated());
    assertEquals(1, response.skippedDuplicates());
  }
}
