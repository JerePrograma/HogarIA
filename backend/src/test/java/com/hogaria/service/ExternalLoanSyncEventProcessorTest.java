package com.hogaria.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.hogaria.entity.ExternalLoanSyncConfig;
import com.hogaria.entity.MoneyTransaction;
import com.hogaria.repository.MoneyTransactionRepository;
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
  ExternalLoanSyncEventProcessor processor;

  @BeforeEach void setUp() { processor = new ExternalLoanSyncEventProcessor(transactionRepository, idempotencyService); }

  @Test void disbursementCreatesRecoverableAdjustmentConfirmedSystem() {
    UUID userId = UUID.randomUUID(); UUID profileId = UUID.randomUUID();
    var cfg = ExternalLoanSyncConfig.builder().accountId(UUID.randomUUID()).loanDisbursementCategoryId(UUID.randomUUID()).build();
    when(idempotencyService.isAlreadyProcessed(any(), any(), any(), any(), any(), any())).thenReturn(false);
    when(transactionRepository.save(any())).thenAnswer(i -> { var tx=i.getArgument(0, MoneyTransaction.class); tx.setId(UUID.randomUUID()); return tx;});

    assertTrue(processor.processDisbursement(userId, profileId, cfg, "1", "Ana", LocalDate.now(), new BigDecimal("100")));
    ArgumentCaptor<MoneyTransaction> captor = ArgumentCaptor.forClass(MoneyTransaction.class);
    verify(transactionRepository).save(captor.capture());
    assertEquals(MoneyTransaction.MovementType.ADJUSTMENT, captor.getValue().getMovementType());
    assertEquals("CJPRESTAMOS_DISBURSEMENT", captor.getValue().getClassificationReason());
    assertEquals(MoneyTransaction.Origin.SYSTEM, captor.getValue().getOrigin());
    assertEquals(MoneyTransaction.Status.CONFIRMED, captor.getValue().getStatus());
  }

  @Test void backfillMigrationMovesHistoricalDisbursementExpensesToAdjustment() throws Exception {
    String migration = Files.readString(Path.of("src/main/resources/db/migration/V13__recoverable_outflows_and_cross_source_review.sql"));
    assertTrue(migration.contains("classification_reason = 'CJPRESTAMOS_DISBURSEMENT'"));
    assertTrue(migration.contains("movement_type = 'EXPENSE'"));
    assertTrue(migration.contains("SET movement_type = 'ADJUSTMENT'"));
  }

  @Test void paymentPrincipalCreatesAdjustment() {
    when(idempotencyService.isAlreadyProcessed(any(), any(), any(), any(), any(), any())).thenReturn(false);
    when(transactionRepository.save(any())).thenAnswer(i -> { var tx=i.getArgument(0, MoneyTransaction.class); tx.setId(UUID.randomUUID()); return tx;});
    var cfg = ExternalLoanSyncConfig.builder().accountId(UUID.randomUUID()).principalRecoveryCategoryId(UUID.randomUUID()).build();

    processor.processPaymentPrincipal(UUID.randomUUID(), UUID.randomUUID(), cfg, "9", LocalDate.now(), new BigDecimal("20"));
    ArgumentCaptor<MoneyTransaction> captor = ArgumentCaptor.forClass(MoneyTransaction.class);
    verify(transactionRepository).save(captor.capture());
    assertEquals(MoneyTransaction.MovementType.ADJUSTMENT, captor.getValue().getMovementType());
  }

  @Test void paymentInterestCreatesIncomeAndSkipsZero() {
    when(idempotencyService.isAlreadyProcessed(any(), any(), any(), any(), any(), any())).thenReturn(false);
    when(transactionRepository.save(any())).thenAnswer(i -> { var tx=i.getArgument(0, MoneyTransaction.class); tx.setId(UUID.randomUUID()); return tx;});
    var cfg = ExternalLoanSyncConfig.builder().accountId(UUID.randomUUID()).interestIncomeCategoryId(UUID.randomUUID()).build();

    assertTrue(processor.processPaymentInterest(UUID.randomUUID(), UUID.randomUUID(), cfg, "9", LocalDate.now(), new BigDecimal("2")));
    assertFalse(processor.processPaymentInterest(UUID.randomUUID(), UUID.randomUUID(), cfg, "10", LocalDate.now(), BigDecimal.ZERO));
    verify(transactionRepository, times(1)).save(any());
  }

  @Test void markProcessedFailurePropagates() {
    when(idempotencyService.isAlreadyProcessed(any(), any(), any(), any(), any(), any())).thenReturn(false);
    when(transactionRepository.save(any())).thenAnswer(i -> { var tx=i.getArgument(0, MoneyTransaction.class); tx.setId(UUID.randomUUID()); return tx;});
    doThrow(new DataIntegrityViolationException("dup")).when(idempotencyService).markProcessed(any(), any(), any(), any(), any(), any(), any(), any());
    var cfg = ExternalLoanSyncConfig.builder().accountId(UUID.randomUUID()).loanDisbursementCategoryId(UUID.randomUUID()).build();

    assertThrows(DataIntegrityViolationException.class, () -> processor.processDisbursement(UUID.randomUUID(), UUID.randomUUID(), cfg, "1", "Ana", LocalDate.now(), BigDecimal.ONE));
  }

  @Test void processedEventDoesNotCreateTransaction() {
    when(idempotencyService.isAlreadyProcessed(any(), any(), any(), any(), any(), any())).thenReturn(true);
    var cfg = ExternalLoanSyncConfig.builder().accountId(UUID.randomUUID()).loanDisbursementCategoryId(UUID.randomUUID()).build();

    assertFalse(processor.processDisbursement(UUID.randomUUID(), UUID.randomUUID(), cfg, "1", "Ana", LocalDate.now(), BigDecimal.ONE));
    verifyNoInteractions(transactionRepository);
  }
}
