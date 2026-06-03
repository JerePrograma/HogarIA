package com.hogaria.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.hogaria.entity.MoneyTransaction;
import com.hogaria.exception.ForbiddenException;
import com.hogaria.integration.cjprestamos.dto.ExternalLoanBackfillDtos.BackfillCandidate;
import com.hogaria.integration.cjprestamos.dto.ExternalLoanBackfillDtos.BackfillDryRunResponse;
import com.hogaria.repository.MoneyTransactionRepository;
import com.hogaria.repository.MoneyTransactionRepository.DuplicateSourceHashGroupProjection;
import com.hogaria.repository.MoneyTransactionRepository.DuplicateSourceOperationGroupProjection;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;

@ExtendWith(MockitoExtension.class)
class ExternalLoanIdempotencyDiagnosticsServiceTest {
  @Mock ExternalLoanBackfillService backfillService;
  @Mock MoneyTransactionRepository transactionRepository;

  ExternalLoanIdempotencyDiagnosticsService service;
  UUID userId;
  UUID profileId;

  @BeforeEach
  void setUp() {
    service = new ExternalLoanIdempotencyDiagnosticsService(backfillService, transactionRepository);
    userId = UUID.randomUUID();
    profileId = UUID.randomUUID();
  }

  @Test
  void diagnosticsReturnsNoBlockingDuplicatesWhenDbIsClean() {
    when(backfillService.dryRun(userId, profileId)).thenReturn(new BackfillDryRunResponse(List.of()));
    when(transactionRepository.findDuplicateSourceOperationGroups(eq(profileId), any(Pageable.class)))
        .thenReturn(List.of());
    when(transactionRepository.findDuplicateSourceHashGroups(eq(profileId), any(Pageable.class)))
        .thenReturn(List.of());

    var diagnostics = service.diagnose(userId, profileId);

    assertEquals(0, diagnostics.cjTransactions());
    assertFalse(diagnostics.hasIndexBlockingDuplicates());
    assertTrue(diagnostics.canRunSync());
    assertFalse(diagnostics.backfillRecommended());
    assertFalse(diagnostics.requiresManualReview());
  }

  @Test
  void diagnosticsDetectsDuplicateSourceOperationIdGroups() {
    var group = new SourceOperationGroup(profileId, "CJPRESTAMOS", "LOAN:1:DISBURSEMENT", 2);
    MoneyTransaction first = transaction("Prestamo CJ #1", "LOAN:1:DISBURSEMENT", "hash-a");
    MoneyTransaction second = transaction("Prestamo CJ #1 duplicado", "LOAN:1:DISBURSEMENT", "hash-b");
    when(backfillService.dryRun(userId, profileId)).thenReturn(new BackfillDryRunResponse(List.of()));
    when(transactionRepository.findDuplicateSourceOperationGroups(eq(profileId), any(Pageable.class)))
        .thenReturn(List.of(group));
    when(transactionRepository.findDuplicateSourceOperationSamples(
            eq(profileId), eq("CJPRESTAMOS"), eq("LOAN:1:DISBURSEMENT"), any(Pageable.class)))
        .thenReturn(List.of(first, second));
    when(transactionRepository.findDuplicateSourceHashGroups(eq(profileId), any(Pageable.class)))
        .thenReturn(List.of());

    var diagnostics = service.diagnose(userId, profileId);

    assertTrue(diagnostics.hasIndexBlockingDuplicates());
    assertFalse(diagnostics.canRunSync());
    assertEquals(1, diagnostics.duplicateSourceOperationGroups().size());
    assertEquals(2, diagnostics.duplicateSourceOperationGroups().getFirst().count());
    assertEquals(2, diagnostics.duplicateSourceOperationGroups().getFirst().transactions().size());
  }

  @Test
  void diagnosticsDetectsDuplicateSourceHashGroups() {
    var group = new SourceHashGroup(profileId, "hash-duplicado", 2);
    MoneyTransaction first = transaction("Recupero capital CJ pago #9", "PAYMENT:9:PRINCIPAL", "hash-duplicado");
    MoneyTransaction second = transaction("Recupero capital CJ pago #9 bis", "PAYMENT:9:PRINCIPAL", "hash-duplicado");
    when(backfillService.dryRun(userId, profileId)).thenReturn(new BackfillDryRunResponse(List.of()));
    when(transactionRepository.findDuplicateSourceOperationGroups(eq(profileId), any(Pageable.class)))
        .thenReturn(List.of());
    when(transactionRepository.findDuplicateSourceHashGroups(eq(profileId), any(Pageable.class)))
        .thenReturn(List.of(group));
    when(transactionRepository.findDuplicateSourceHashSamples(
            eq(profileId), eq("hash-duplicado"), any(Pageable.class)))
        .thenReturn(List.of(first, second));

    var diagnostics = service.diagnose(userId, profileId);

    assertTrue(diagnostics.hasIndexBlockingDuplicates());
    assertFalse(diagnostics.canRunSync());
    assertEquals(1, diagnostics.duplicateSourceHashGroups().size());
    assertEquals("hash-duplicado", diagnostics.duplicateSourceHashGroups().getFirst().sourceHash());
  }

  @Test
  void diagnosticsReportsBackfillRecommendedWhenUnmappedCjCandidatesExist() {
    BackfillCandidate high = candidate("HIGH", null, true);
    BackfillCandidate medium = candidate("MEDIUM", null, true);
    when(backfillService.dryRun(userId, profileId))
        .thenReturn(new BackfillDryRunResponse(List.of(high, medium)));
    when(transactionRepository.findDuplicateSourceOperationGroups(eq(profileId), any(Pageable.class)))
        .thenReturn(List.of());
    when(transactionRepository.findDuplicateSourceHashGroups(eq(profileId), any(Pageable.class)))
        .thenReturn(List.of());

    var diagnostics = service.diagnose(userId, profileId);

    assertEquals(2, diagnostics.cjTransactions());
    assertEquals(2, diagnostics.unmappedCandidates());
    assertEquals(2, diagnostics.wouldCreateMappings());
    assertEquals(1, diagnostics.candidateCountsByConfidence().get("HIGH"));
    assertEquals(1, diagnostics.candidateCountsByConfidence().get("MEDIUM"));
    assertTrue(diagnostics.backfillRecommended());
    assertFalse(diagnostics.hasIndexBlockingDuplicates());
  }

  @Test
  void diagnosticsCountsSkippedMappedCandidates() {
    BackfillCandidate mappedEvent = candidate("HIGH", "mapping already exists", false);
    BackfillCandidate mappedTransaction = candidate("MEDIUM", "transaction already mapped", false);
    when(backfillService.dryRun(userId, profileId))
        .thenReturn(new BackfillDryRunResponse(List.of(mappedEvent, mappedTransaction)));
    when(transactionRepository.findDuplicateSourceOperationGroups(eq(profileId), any(Pageable.class)))
        .thenReturn(List.of());
    when(transactionRepository.findDuplicateSourceHashGroups(eq(profileId), any(Pageable.class)))
        .thenReturn(List.of());

    var diagnostics = service.diagnose(userId, profileId);

    assertEquals(2, diagnostics.mappedTransactions());
    assertEquals(1, diagnostics.alreadyMappedEvents());
    assertEquals(1, diagnostics.alreadyMappedTransactions());
    assertEquals(0, diagnostics.unmappedCandidates());
    assertFalse(diagnostics.backfillRecommended());
  }

  @Test
  void diagnosticsRejectsProfileNotOwnedByUser() {
    when(backfillService.dryRun(userId, profileId))
        .thenThrow(new ForbiddenException("Profile no pertenece al usuario"));

    assertThrows(ForbiddenException.class, () -> service.diagnose(userId, profileId));
    verifyNoInteractions(transactionRepository);
  }

  private BackfillCandidate candidate(String confidence, String warning, boolean wouldCreateMapping) {
    return new BackfillCandidate(
        UUID.randomUUID(),
        "Prestamo CJ #1",
        new BigDecimal("100.00"),
        LocalDate.of(2026, 5, 10),
        "LOAN",
        "1",
        "DISBURSEMENT",
        confidence,
        warning,
        wouldCreateMapping);
  }

  private MoneyTransaction transaction(String description, String sourceOperationId, String sourceHash) {
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
        .source("CJPRESTAMOS")
        .sourceOperationId(sourceOperationId)
        .sourceHash(sourceHash)
        .description(description)
        .build();
  }

  private record SourceOperationGroup(
      UUID profileId, String source, String sourceOperationId, long transactionCount)
      implements DuplicateSourceOperationGroupProjection {
    @Override
    public UUID getProfileId() {
      return profileId;
    }

    @Override
    public String getSource() {
      return source;
    }

    @Override
    public String getSourceOperationId() {
      return sourceOperationId;
    }

    @Override
    public long getTransactionCount() {
      return transactionCount;
    }
  }

  private record SourceHashGroup(UUID profileId, String sourceHash, long transactionCount)
      implements DuplicateSourceHashGroupProjection {
    @Override
    public UUID getProfileId() {
      return profileId;
    }

    @Override
    public String getSourceHash() {
      return sourceHash;
    }

    @Override
    public long getTransactionCount() {
      return transactionCount;
    }
  }
}
