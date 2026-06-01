package com.hogaria.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.hogaria.entity.ExternalLoanSyncConfig;
import com.hogaria.entity.MoneyTransaction;
import com.hogaria.repository.MoneyTransactionRepository;
import com.hogaria.service.ExternalLoanEventDuplicateDetector.DuplicateDetectionResult;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

@ExtendWith(MockitoExtension.class)
class ExternalLoanSyncEventProcessorTest {
  @Mock MoneyTransactionRepository transactionRepository;
  @Mock ExternalSyncIdempotencyService idempotencyService;
  @Mock ExternalLoanEventDuplicateDetector duplicateDetector;
  ExternalLoanSyncEventProcessor processor;

  @BeforeEach
  void setUp() {
    processor =
        new ExternalLoanSyncEventProcessor(
            transactionRepository, idempotencyService, duplicateDetector);
    lenient()
        .when(duplicateDetector.detect(any(), any(), any(), any(), any(), any(), any(), any(), any()))
        .thenReturn(DuplicateDetectionResult.notDuplicate("source-op", "source-hash"));
  }

  @Test void disbursementCreatesRecoverableAdjustmentConfirmedSystem() {
    UUID userId = UUID.randomUUID(); UUID profileId = UUID.randomUUID();
    var cfg = ExternalLoanSyncConfig.builder().accountId(UUID.randomUUID()).loanDisbursementCategoryId(UUID.randomUUID()).build();
    when(transactionRepository.save(any())).thenAnswer(i -> { var tx=i.getArgument(0, MoneyTransaction.class); tx.setId(UUID.randomUUID()); return tx;});

    assertTrue(processor.processDisbursement(userId, profileId, cfg, "1", "Ana", LocalDate.now(), new BigDecimal("100")));
    ArgumentCaptor<MoneyTransaction> captor = ArgumentCaptor.forClass(MoneyTransaction.class);
    verify(transactionRepository).save(captor.capture());
    assertEquals(MoneyTransaction.MovementType.ADJUSTMENT, captor.getValue().getMovementType());
    assertEquals("CJPRESTAMOS_DISBURSEMENT", captor.getValue().getClassificationReason());
    assertEquals(MoneyTransaction.Origin.SYSTEM, captor.getValue().getOrigin());
    assertEquals(MoneyTransaction.Status.CONFIRMED, captor.getValue().getStatus());
  }

  @Test void baselineSeedsCapitalPrestadoAsRecoverableAdjustmentNotBudgetable() throws Exception {
    String migration = Files.readString(Path.of("src/main/resources/db/migration/V1__baseline_schema_and_seed.sql"));
    assertTrue(migration.contains("'CJ - Capital prestado', 'INVESTMENT', 'GLOBAL', 'ADJUSTMENT', FALSE, FALSE"));
  }

  @Test void paymentPrincipalCreatesAdjustment() {
    when(transactionRepository.save(any())).thenAnswer(i -> { var tx=i.getArgument(0, MoneyTransaction.class); tx.setId(UUID.randomUUID()); return tx;});
    var cfg = ExternalLoanSyncConfig.builder().accountId(UUID.randomUUID()).principalRecoveryCategoryId(UUID.randomUUID()).build();

    processor.processPaymentPrincipal(UUID.randomUUID(), UUID.randomUUID(), cfg, "9", LocalDate.now(), new BigDecimal("20"));
    ArgumentCaptor<MoneyTransaction> captor = ArgumentCaptor.forClass(MoneyTransaction.class);
    verify(transactionRepository).save(captor.capture());
    assertEquals(MoneyTransaction.MovementType.ADJUSTMENT, captor.getValue().getMovementType());
  }

  @Test void paymentInterestCreatesIncomeAndSkipsZero() {
    when(transactionRepository.save(any())).thenAnswer(i -> { var tx=i.getArgument(0, MoneyTransaction.class); tx.setId(UUID.randomUUID()); return tx;});
    var cfg = ExternalLoanSyncConfig.builder().accountId(UUID.randomUUID()).interestIncomeCategoryId(UUID.randomUUID()).build();

    assertTrue(processor.processPaymentInterest(UUID.randomUUID(), UUID.randomUUID(), cfg, "9", LocalDate.now(), new BigDecimal("2")));
    assertFalse(processor.processPaymentInterest(UUID.randomUUID(), UUID.randomUUID(), cfg, "10", LocalDate.now(), BigDecimal.ZERO));
    verify(transactionRepository, times(1)).save(any());
  }

  @Test void markProcessedFailurePropagates() {
    when(transactionRepository.save(any())).thenAnswer(i -> { var tx=i.getArgument(0, MoneyTransaction.class); tx.setId(UUID.randomUUID()); return tx;});
    doThrow(new DataIntegrityViolationException("dup")).when(idempotencyService).markProcessed(any(), any(), any(), any(), any(), any(), any(), any());
    var cfg = ExternalLoanSyncConfig.builder().accountId(UUID.randomUUID()).loanDisbursementCategoryId(UUID.randomUUID()).build();

    assertThrows(DataIntegrityViolationException.class, () -> processor.processDisbursement(UUID.randomUUID(), UUID.randomUUID(), cfg, "1", "Ana", LocalDate.now(), BigDecimal.ONE));
  }

  @Test void processedEventDoesNotCreateTransaction() {
    when(duplicateDetector.detect(any(), any(), any(), any(), any(), any(), any(), any(), any()))
        .thenReturn(
            DuplicateDetectionResult.duplicate(
                ExternalLoanEventDuplicateDetector.REASON_MAPPING_PROCESSED,
                UUID.randomUUID(),
                "source-op",
                "source-hash"));
    var cfg = ExternalLoanSyncConfig.builder().accountId(UUID.randomUUID()).loanDisbursementCategoryId(UUID.randomUUID()).build();

    assertFalse(processor.processDisbursement(UUID.randomUUID(), UUID.randomUUID(), cfg, "1", "Ana", LocalDate.now(), BigDecimal.ONE));
    verifyNoInteractions(transactionRepository);
  }
}
