package com.hogaria.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.hogaria.entity.Category;
import com.hogaria.entity.ExternalLoanSyncConfig;
import com.hogaria.exception.BadRequestException;
import com.hogaria.exception.ForbiddenException;
import com.hogaria.integration.cjprestamos.CjPrestamosClient;
import com.hogaria.integration.cjprestamos.CjPrestamosIntegrationConfigValidator;
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
  @Mock ExternalLoanSyncEventProcessor eventProcessor;
  @Mock CjPrestamosIntegrationConfigValidator configValidator;

  ExternalLoansService service;

  @BeforeEach void setUp() {
    service = new ExternalLoansService(client, properties, profileRepository, new CjPrestamosBridgeMapper(), syncConfigRepository, accountRepository, categoryRepository, eventProcessor, configValidator);
    lenient().when(properties.syncEnabled()).thenReturn(true);
  }

  @Test void returnsDisabledWhenIntegrationOff() { UUID userId = UUID.randomUUID(); UUID profileId = UUID.randomUUID(); when(profileRepository.existsByIdAndUserId(profileId, userId)).thenReturn(true); when(properties.enabled()).thenReturn(false); var response = service.getSummary(userId, profileId); assertEquals("DISABLED", response.status()); verifyNoInteractions(client); }
  @Test void throwsWhenProfileDoesNotBelongToUser() { UUID userId = UUID.randomUUID(); UUID profileId = UUID.randomUUID(); when(profileRepository.existsByIdAndUserId(profileId, userId)).thenReturn(false); assertThrows(ForbiddenException.class, () -> service.getSummary(userId, profileId)); verifyNoInteractions(client); }
  @Test void returnsDashboardWhenClientResponds() { UUID userId = UUID.randomUUID(); UUID profileId = UUID.randomUUID(); when(profileRepository.existsByIdAndUserId(profileId, userId)).thenReturn(true); when(properties.enabled()).thenReturn(true); var dashboard = new CjPrestamosDashboardRemoteResponse(new BigDecimal("1000"), new BigDecimal("80"), new BigDecimal("120"), new BigDecimal("50"), 3L); var cash = new CjPrestamosCashControlRemoteResponse(BigDecimal.TEN, BigDecimal.ONE, BigDecimal.ONE, BigDecimal.ONE, BigDecimal.ONE, BigDecimal.ONE, BigDecimal.ONE, BigDecimal.ONE, BigDecimal.ONE, BigDecimal.ONE, BigDecimal.ONE, BigDecimal.ONE, BigDecimal.ZERO, 2L, 1L, new BigDecimal("0.9"), new BigDecimal("0.2")); var loans = List.of(new CjPrestamosLoanActiveRemoteResponse(10L, 22L, "Ana", new BigDecimal("200"), 12, "MENSUAL", "ACTIVE", new BigDecimal("50"), new BigDecimal("150"), new BigDecimal("10"), new BigDecimal("20"), LocalDateTime.now(), LocalDateTime.now())); when(client.getDashboardSummary(profileId, userId)).thenReturn(dashboard); when(client.getCashControl(profileId, userId)).thenReturn(cash); when(client.getActiveLoans(profileId, userId)).thenReturn(loans);
    var response = service.getSummary(userId, profileId); assertEquals("ENABLED", response.status()); assertEquals(3L, response.dashboard().activeLoans()); assertEquals(10L, response.activeLoans().getFirst().externalLoanId()); }

  @Test void summaryMarksReadOnlyWhenSyncDisabled() {
    UUID userId = UUID.randomUUID(); UUID profileId = UUID.randomUUID();
    when(profileRepository.existsByIdAndUserId(profileId, userId)).thenReturn(true);
    when(properties.enabled()).thenReturn(true);
    when(properties.syncEnabled()).thenReturn(false);
    var dashboard = new CjPrestamosDashboardRemoteResponse(new BigDecimal("1000"), new BigDecimal("80"), new BigDecimal("120"), new BigDecimal("50"), 3L);
    var cash = new CjPrestamosCashControlRemoteResponse(BigDecimal.TEN, BigDecimal.ONE, BigDecimal.ONE, BigDecimal.ONE, BigDecimal.ONE, BigDecimal.ONE, BigDecimal.ONE, BigDecimal.ONE, BigDecimal.ONE, BigDecimal.ONE, BigDecimal.ONE, BigDecimal.ONE, BigDecimal.ZERO, 2L, 1L, new BigDecimal("0.9"), new BigDecimal("0.2"));
    when(client.getDashboardSummary(profileId, userId)).thenReturn(dashboard);
    when(client.getCashControl(profileId, userId)).thenReturn(cash);
    when(client.getActiveLoans(profileId, userId)).thenReturn(List.of());

    var response = service.getSummary(userId, profileId);

    assertTrue(response.readOnly());
  }

  @Test void propagatesRemoteError() { UUID userId = UUID.randomUUID(); UUID profileId = UUID.randomUUID(); when(profileRepository.existsByIdAndUserId(profileId, userId)).thenReturn(true); when(properties.enabled()).thenReturn(true); when(client.getDashboardSummary(profileId, userId)).thenThrow(new CjPrestamosIntegrationException("timeout")); assertThrows(CjPrestamosIntegrationException.class, () -> service.getSummary(userId, profileId)); }

  @Test void syncFailsWhenMissingPrincipalInterestSplit() {
    UUID userId = UUID.randomUUID(); UUID profileId = UUID.randomUUID(); UUID accountId = UUID.randomUUID(); UUID c1 = UUID.randomUUID(); UUID c2 = UUID.randomUUID(); UUID c3 = UUID.randomUUID();
    when(profileRepository.existsByIdAndUserId(profileId, userId)).thenReturn(true);
    when(syncConfigRepository.findByProfileId(profileId)).thenReturn(Optional.of(ExternalLoanSyncConfig.builder().profileId(profileId).enabled(true).accountId(accountId).loanDisbursementCategoryId(c1).principalRecoveryCategoryId(c2).interestIncomeCategoryId(c3).build()));
    when(accountRepository.existsByIdAndProfileId(accountId, profileId)).thenReturn(true);
    when(categoryRepository.findById(any())).thenAnswer(inv -> Optional.of(Category.builder().id(inv.getArgument(0)).profileId(profileId).build()));
    when(client.getActiveLoans(profileId, userId)).thenReturn(List.of(new CjPrestamosLoanActiveRemoteResponse(1L, 1L, "Ana", new BigDecimal("100"), 1, "MENSUAL", "ACTIVE", BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, LocalDateTime.now(), LocalDateTime.now())));
    when(client.getLoanPayments(profileId, userId, 1L)).thenReturn(List.of(new CjPrestamosPaymentRemoteResponse(9L, 1L, LocalDate.now(), new BigDecimal("50"), null, null, "r", "sin split", "OK")));

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
    when(eventProcessor.processDisbursement(any(), any(), any(), any(), any(), any(), any())).thenReturn(false);
    when(eventProcessor.processPaymentPrincipal(any(), any(), any(), any(), any(), any())).thenReturn(true);
    when(eventProcessor.processPaymentInterest(any(), any(), any(), any(), any(), any())).thenReturn(true);

    var response = service.sync(userId, profileId);
    assertEquals(1, response.paymentsSynced());
    assertEquals(2, response.movementsCreated());
    assertEquals(1, response.skippedDuplicates());
  }

  @Test void syncStoresMappingsPerPaymentComponentWithTransactionIds() {
    UUID userId = UUID.randomUUID(); UUID profileId = UUID.randomUUID(); UUID accountId = UUID.randomUUID(); UUID c1 = UUID.randomUUID(); UUID c2 = UUID.randomUUID(); UUID c3 = UUID.randomUUID();
    when(profileRepository.existsByIdAndUserId(profileId, userId)).thenReturn(true);
    when(syncConfigRepository.findByProfileId(profileId)).thenReturn(Optional.of(ExternalLoanSyncConfig.builder().profileId(profileId).enabled(true).accountId(accountId).loanDisbursementCategoryId(c1).principalRecoveryCategoryId(c2).interestIncomeCategoryId(c3).build()));
    when(accountRepository.existsByIdAndProfileId(accountId, profileId)).thenReturn(true);
    when(categoryRepository.findById(any())).thenAnswer(inv -> Optional.of(Category.builder().id(inv.getArgument(0)).profileId(profileId).build()));
    when(client.getActiveLoans(profileId, userId)).thenReturn(List.of(new CjPrestamosLoanActiveRemoteResponse(1L, 1L, "Ana", new BigDecimal("100"), 1, "MENSUAL", "ACTIVE", BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, LocalDateTime.now(), LocalDateTime.now())));
    when(client.getLoanPayments(profileId, userId, 1L)).thenReturn(List.of(new CjPrestamosPaymentRemoteResponse(9L, 1L, LocalDate.now(), new BigDecimal("50"), new BigDecimal("30"), new BigDecimal("20"), "r", null, "OK")));
    when(eventProcessor.processDisbursement(any(), any(), any(), any(), any(), any(), any())).thenReturn(false);
    when(eventProcessor.processPaymentPrincipal(any(), any(), any(), any(), any(), any())).thenReturn(true);
    when(eventProcessor.processPaymentInterest(any(), any(), any(), any(), any(), any())).thenReturn(true);

    service.sync(userId, profileId);

    verify(eventProcessor).processPaymentPrincipal(any(), any(), any(), eq("9"), any(), any());
    verify(eventProcessor).processPaymentInterest(any(), any(), any(), eq("9"), any(), any());
  }

  @Test void syncOmitsZeroComponentMappings() {
    UUID userId = UUID.randomUUID(); UUID profileId = UUID.randomUUID(); UUID accountId = UUID.randomUUID(); UUID c1 = UUID.randomUUID(); UUID c2 = UUID.randomUUID(); UUID c3 = UUID.randomUUID();
    when(profileRepository.existsByIdAndUserId(profileId, userId)).thenReturn(true);
    when(syncConfigRepository.findByProfileId(profileId)).thenReturn(Optional.of(ExternalLoanSyncConfig.builder().profileId(profileId).enabled(true).accountId(accountId).loanDisbursementCategoryId(c1).principalRecoveryCategoryId(c2).interestIncomeCategoryId(c3).build()));
    when(accountRepository.existsByIdAndProfileId(accountId, profileId)).thenReturn(true);
    when(categoryRepository.findById(any())).thenAnswer(inv -> Optional.of(Category.builder().id(inv.getArgument(0)).profileId(profileId).build()));
    when(client.getActiveLoans(profileId, userId)).thenReturn(List.of(new CjPrestamosLoanActiveRemoteResponse(1L, 1L, "Ana", new BigDecimal("100"), 1, "MENSUAL", "ACTIVE", BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, LocalDateTime.now(), LocalDateTime.now())));
    when(client.getLoanPayments(profileId, userId, 1L)).thenReturn(List.of(new CjPrestamosPaymentRemoteResponse(9L, 1L, LocalDate.now(), new BigDecimal("50"), new BigDecimal("50"), BigDecimal.ZERO, "r", null, "OK")));
    when(eventProcessor.processDisbursement(any(), any(), any(), any(), any(), any(), any())).thenReturn(false);
    when(eventProcessor.processPaymentPrincipal(any(), any(), any(), any(), any(), any())).thenReturn(true);

    var response = service.sync(userId, profileId);

    assertEquals(1, response.paymentsSynced());
    verify(eventProcessor, times(1)).processPaymentPrincipal(any(), any(), any(), eq("9"), any(), any());
    verify(eventProcessor, times(1)).processPaymentInterest(any(), any(), any(), eq("9"), any(), eq(BigDecimal.ZERO));
  }

  @Test void syncReturnsControlledErrorWhenSyncFeatureDisabled() {
    UUID userId = UUID.randomUUID(); UUID profileId = UUID.randomUUID();
    when(profileRepository.existsByIdAndUserId(profileId, userId)).thenReturn(true);
    when(properties.syncEnabled()).thenReturn(false);

    var ex = assertThrows(BadRequestException.class, () -> service.sync(userId, profileId));
    assertEquals("La sincronización contable está deshabilitada. La integración está en modo solo lectura.", ex.getMessage());
  }

  @Test void syncDoesNotCreateTransactionsWhenSyncFeatureDisabled() {
    UUID userId = UUID.randomUUID(); UUID profileId = UUID.randomUUID();
    when(profileRepository.existsByIdAndUserId(profileId, userId)).thenReturn(true);
    when(properties.syncEnabled()).thenReturn(false);

    assertThrows(BadRequestException.class, () -> service.sync(userId, profileId));
    verifyNoInteractions(eventProcessor, client);
  }

  @Test void syncFailsFastWhenConfigIncomplete() {
    UUID userId = UUID.randomUUID(); UUID profileId = UUID.randomUUID();
    when(profileRepository.existsByIdAndUserId(profileId, userId)).thenReturn(true);
    when(syncConfigRepository.findByProfileId(profileId)).thenReturn(Optional.of(ExternalLoanSyncConfig.builder().profileId(profileId).enabled(true).build()));

    assertThrows(BadRequestException.class, () -> service.sync(userId, profileId));
    verifyNoInteractions(eventProcessor, client);
  }

  @Test void dryRunDoesNotPersistAndDetectsDuplicates() {
    UUID userId = UUID.randomUUID(); UUID profileId = UUID.randomUUID(); UUID accountId = UUID.randomUUID(); UUID c1 = UUID.randomUUID(); UUID c2 = UUID.randomUUID(); UUID c3 = UUID.randomUUID();
    when(profileRepository.existsByIdAndUserId(profileId, userId)).thenReturn(true);
    when(syncConfigRepository.findByProfileId(profileId)).thenReturn(Optional.of(ExternalLoanSyncConfig.builder().profileId(profileId).enabled(true).accountId(accountId).loanDisbursementCategoryId(c1).principalRecoveryCategoryId(c2).interestIncomeCategoryId(c3).build()));
    when(accountRepository.existsByIdAndProfileId(accountId, profileId)).thenReturn(true);
    when(categoryRepository.findById(any())).thenAnswer(inv -> Optional.of(Category.builder().id(inv.getArgument(0)).profileId(profileId).build()));
    when(client.getActiveLoans(profileId, userId)).thenReturn(List.of(new CjPrestamosLoanActiveRemoteResponse(1L, 1L, "Ana", new BigDecimal("100"), 1, "MENSUAL", "ACTIVE", BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, LocalDateTime.now(), LocalDateTime.now())));
    when(client.getLoanPayments(profileId, userId, 1L)).thenReturn(List.of(new CjPrestamosPaymentRemoteResponse(9L, 1L, LocalDate.now(), new BigDecimal("50"), new BigDecimal("30"), new BigDecimal("20"), "r", null, "OK")));
    when(eventProcessor.isAlreadyProcessed(any(), any(), eq("LOAN"), eq("1"), eq("DISBURSEMENT"))).thenReturn(true);
    when(eventProcessor.isAlreadyProcessed(any(), any(), eq("PAYMENT"), eq("9"), eq("PAYMENT_PRINCIPAL_RECOVERY"))).thenReturn(false);
    when(eventProcessor.isAlreadyProcessed(any(), any(), eq("PAYMENT"), eq("9"), eq("PAYMENT_INTEREST_INCOME"))).thenReturn(false);

    var response = service.dryRunSync(userId, profileId);
    assertTrue(response.dryRun());
    assertEquals(2, response.movementsCreated());
    assertEquals(1, response.skippedDuplicates());
    verify(eventProcessor, never()).processDisbursement(any(), any(), any(), any(), any(), any(), any());
    verify(eventProcessor, never()).processPaymentPrincipal(any(), any(), any(), any(), any(), any());
    verify(eventProcessor, never()).processPaymentInterest(any(), any(), any(), any(), any(), any());
  }


  @Test void syncDisabledDoesNotWriteAnything() {
    UUID userId = UUID.randomUUID(); UUID profileId = UUID.randomUUID();
    when(profileRepository.existsByIdAndUserId(profileId, userId)).thenReturn(true);
    when(properties.syncEnabled()).thenReturn(false);
    assertThrows(BadRequestException.class, () -> service.sync(userId, profileId));
    verifyNoInteractions(eventProcessor, client);
  }

  @Test void incompleteConfigDoesNotCallRemoteClient() {
    UUID userId = UUID.randomUUID(); UUID profileId = UUID.randomUUID();
    when(profileRepository.existsByIdAndUserId(profileId, userId)).thenReturn(true);
    when(syncConfigRepository.findByProfileId(profileId)).thenReturn(Optional.of(ExternalLoanSyncConfig.builder().profileId(profileId).enabled(true).build()));
    assertThrows(BadRequestException.class, () -> service.sync(userId, profileId));
    verifyNoInteractions(client);
  }

  @Test void incompleteConfigDoesNotWriteAnything() {
    UUID userId = UUID.randomUUID(); UUID profileId = UUID.randomUUID();
    when(profileRepository.existsByIdAndUserId(profileId, userId)).thenReturn(true);
    when(syncConfigRepository.findByProfileId(profileId)).thenReturn(Optional.of(ExternalLoanSyncConfig.builder().profileId(profileId).enabled(true).build()));
    assertThrows(BadRequestException.class, () -> service.sync(userId, profileId));
    verifyNoInteractions(eventProcessor);
  }

  @Test void healthWhenIntegrationDisabledDoesNotCallRemote() {
    UUID userId = UUID.randomUUID(); UUID profileId = UUID.randomUUID();
    when(profileRepository.existsByIdAndUserId(profileId, userId)).thenReturn(true);
    when(properties.enabled()).thenReturn(false);
    when(properties.syncEnabled()).thenReturn(false);
    when(properties.resolvedApiPrefix()).thenReturn("/api/integration/hogaria");

    var response = service.checkIntegration(userId, profileId);
    assertEquals("MISCONFIGURED", response.status());
    assertFalse(response.remoteCheckExecuted());
    verifyNoInteractions(client);
  }

  @Test void healthWithIncompleteConfigReturnsControlledStatus() {
    UUID userId = UUID.randomUUID(); UUID profileId = UUID.randomUUID();
    when(profileRepository.existsByIdAndUserId(profileId, userId)).thenReturn(true);
    when(properties.enabled()).thenReturn(true);
    when(properties.syncEnabled()).thenReturn(true);
    when(properties.resolvedApiPrefix()).thenReturn("/api/integration/hogaria");
    when(properties.missingRequiredFields()).thenReturn("CJP_BASE_URL, CJP_USERNAME");

    var response = service.checkIntegration(userId, profileId);
    assertEquals("MISCONFIGURED", response.status());
    assertEquals(List.of("CJP_BASE_URL", "CJP_USERNAME"), response.missingFields());
    assertFalse(response.remoteCheckExecuted());
    verifyNoInteractions(client);
  }

  @Test void healthDoesNotExposePassword() {
    UUID userId = UUID.randomUUID(); UUID profileId = UUID.randomUUID();
    when(profileRepository.existsByIdAndUserId(profileId, userId)).thenReturn(true);
    when(properties.enabled()).thenReturn(true);
    when(properties.syncEnabled()).thenReturn(true);
    when(properties.resolvedApiPrefix()).thenReturn("/api/integration/hogaria");
    when(properties.baseUrl()).thenReturn("http://localhost:8081");
    when(properties.username()).thenReturn("user");
    when(properties.password()).thenReturn("supersecret");
    when(properties.connectTimeoutMs()).thenReturn(3000);
    when(properties.readTimeoutMs()).thenReturn(5000);
    when(client.getDashboardSummary(profileId, userId)).thenReturn(null);

    var response = service.checkIntegration(userId, profileId);
    assertTrue(response.hasPassword());
    assertFalse(response.message().contains("supersecret"));
  }

}
