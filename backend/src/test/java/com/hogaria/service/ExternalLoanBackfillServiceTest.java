package com.hogaria.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.hogaria.entity.ExternalSyncMapping;
import com.hogaria.entity.MoneyTransaction;
import com.hogaria.integration.cjprestamos.dto.ExternalLoanBackfillDtos;
import com.hogaria.repository.ExternalSyncMappingRepository;
import com.hogaria.repository.FinancialProfileRepository;
import com.hogaria.repository.MoneyTransactionRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ExternalLoanBackfillServiceTest {
  private static final String SYSTEM = "CJPRESTAMOS";

  @Mock MoneyTransactionRepository transactionRepository;
  @Mock ExternalSyncMappingRepository mappingRepository;
  @Mock FinancialProfileRepository profileRepository;
  @Mock ExternalSyncIdempotencyService idempotencyService;

  ExternalLoanBackfillService service;
  UUID userId;
  UUID profileId;

  @BeforeEach
  void setUp() {
    service =
        new ExternalLoanBackfillService(
            transactionRepository, mappingRepository, profileRepository, idempotencyService);
    userId = UUID.randomUUID();
    profileId = UUID.randomUUID();
    lenient().when(profileRepository.existsByIdAndUserId(profileId, userId)).thenReturn(true);
  }

  @Test
  void alreadyMappedByEventIsSkipped() {
    MoneyTransaction transaction =
        transaction("LOAN:1:DISBURSEMENT", "Préstamo CJ #1", "CJPRESTAMOS_DISBURSEMENT");
    when(transactionRepository.findByProfileId(profileId)).thenReturn(List.of(transaction));
    when(mappingRepository
            .findByProfileIdAndExternalSystemAndExternalEntityTypeAndExternalEntityIdAndExternalEventType(
                profileId, SYSTEM, "LOAN", "1", "DISBURSEMENT"))
        .thenReturn(Optional.of(ExternalSyncMapping.builder().status("PROCESSED").build()));

    var dryRun = service.dryRun(userId, profileId);
    var response =
        service.apply(userId, profileId, new ExternalLoanBackfillDtos.BackfillApplyRequest(true));

    assertEquals(1, dryRun.candidates().size());
    assertFalse(dryRun.candidates().getFirst().wouldCreateMapping());
    assertEquals(0, response.createdMappings());
    assertEquals(1, response.skipped().size());
    assertTrue(response.skipped().getFirst().contains("mapping already exists"));
    assertTrue(response.errors().isEmpty());
    verify(idempotencyService, never()).markProcessed(any(), any(), any(), any(), any(), any(), any(), any());
  }

  @Test
  void transactionAlreadyMappedToAnotherEventIsSkippedBeforeCreate() {
    MoneyTransaction transaction =
        transaction("LOAN:2:DISBURSEMENT", "Préstamo CJ #2", "CJPRESTAMOS_DISBURSEMENT");
    when(transactionRepository.findByProfileId(profileId)).thenReturn(List.of(transaction));
    when(mappingRepository
            .findByProfileIdAndExternalSystemAndExternalEntityTypeAndExternalEntityIdAndExternalEventType(
                profileId, SYSTEM, "LOAN", "2", "DISBURSEMENT"))
        .thenReturn(Optional.empty());
    when(mappingRepository.existsByProfileIdAndMoneyTransactionId(profileId, transaction.getId()))
        .thenReturn(true);

    var response =
        service.apply(userId, profileId, new ExternalLoanBackfillDtos.BackfillApplyRequest(true));

    assertEquals(0, response.createdMappings());
    assertEquals(1, response.skipped().size());
    assertTrue(response.skipped().getFirst().contains("transaction already mapped"));
    assertTrue(response.errors().isEmpty());
    verify(idempotencyService, never()).markProcessed(any(), any(), any(), any(), any(), any(), any(), any());
  }

  @Test
  void lowConfidenceIsSkippedUnlessExplicitlyIncluded() {
    MoneyTransaction transaction =
        transaction(null, "CJ prestamo historico 77", "CJPRESTAMOS_DISBURSEMENT");
    when(transactionRepository.findByProfileId(profileId)).thenReturn(List.of(transaction));
    when(mappingRepository
            .findByProfileIdAndExternalSystemAndExternalEntityTypeAndExternalEntityIdAndExternalEventType(
                profileId, SYSTEM, "LOAN", "77", "DISBURSEMENT"))
        .thenReturn(Optional.empty());
    when(mappingRepository.existsByProfileIdAndMoneyTransactionId(profileId, transaction.getId()))
        .thenReturn(false);
    when(transactionRepository.findByIdAndProfileId(transaction.getId(), profileId))
        .thenReturn(Optional.of(transaction));

    var skippedResponse =
        service.apply(userId, profileId, new ExternalLoanBackfillDtos.BackfillApplyRequest(false));

    assertEquals(0, skippedResponse.createdMappings());
    assertEquals(1, skippedResponse.skipped().size());
    assertTrue(skippedResponse.skipped().getFirst().contains("low confidence"));
    verify(idempotencyService, never()).markProcessed(any(), any(), any(), any(), any(), any(), any(), any());

    var includedResponse =
        service.apply(userId, profileId, new ExternalLoanBackfillDtos.BackfillApplyRequest(true));

    assertEquals(1, includedResponse.createdMappings());
    assertTrue(includedResponse.skipped().isEmpty());
    assertTrue(includedResponse.errors().isEmpty());
    verify(idempotencyService)
        .markProcessed(
            eq(profileId),
            eq(SYSTEM),
            eq("LOAN"),
            eq("77"),
            eq("DISBURSEMENT"),
            any(),
            eq(transaction.getId()),
            isNull());
  }

  @Test
  void writeFailuresAreReportedAsErrorsNotSkipped() {
    MoneyTransaction transaction =
        transaction("PAYMENT:9:PRINCIPAL", "Recupero capital CJ pago #9", "CJPRESTAMOS_PAYMENT_PRINCIPAL_RECOVERY");
    when(transactionRepository.findByProfileId(profileId)).thenReturn(List.of(transaction));
    when(mappingRepository
            .findByProfileIdAndExternalSystemAndExternalEntityTypeAndExternalEntityIdAndExternalEventType(
                profileId, SYSTEM, "PAYMENT", "9", "PAYMENT_PRINCIPAL_RECOVERY"))
        .thenReturn(Optional.empty());
    when(mappingRepository.existsByProfileIdAndMoneyTransactionId(profileId, transaction.getId()))
        .thenReturn(false);
    when(transactionRepository.findByIdAndProfileId(transaction.getId(), profileId))
        .thenReturn(Optional.of(transaction));
    when(idempotencyService.markProcessed(any(), any(), any(), any(), any(), any(), any(), any()))
        .thenThrow(new IllegalStateException("boom"));

    var response =
        service.apply(userId, profileId, new ExternalLoanBackfillDtos.BackfillApplyRequest(true));

    assertEquals(0, response.createdMappings());
    assertTrue(response.skipped().isEmpty());
    assertEquals(1, response.errors().size());
    assertTrue(response.errors().getFirst().contains("boom"));
  }

  private MoneyTransaction transaction(
      String sourceOperationId, String description, String classificationReason) {
    return MoneyTransaction.builder()
        .id(UUID.randomUUID())
        .profileId(profileId)
        .accountId(UUID.randomUUID())
        .categoryId(UUID.randomUUID())
        .movementType(MoneyTransaction.MovementType.ADJUSTMENT)
        .realDate(LocalDate.of(2026, 5, 10))
        .budgetDate(LocalDate.of(2026, 5, 10))
        .amount(new BigDecimal("100.00"))
        .currency("ARS")
        .origin(MoneyTransaction.Origin.SYSTEM)
        .status(MoneyTransaction.Status.CONFIRMED)
        .source(SYSTEM)
        .sourceOperationId(sourceOperationId)
        .sourceHash("hash-" + UUID.randomUUID())
        .description(description)
        .classificationReason(classificationReason)
        .build();
  }
}
