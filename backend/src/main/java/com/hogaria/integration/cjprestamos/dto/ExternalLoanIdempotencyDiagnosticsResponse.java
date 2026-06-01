package com.hogaria.integration.cjprestamos.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public record ExternalLoanIdempotencyDiagnosticsResponse(
    int cjTransactions,
    int mappedTransactions,
    int unmappedCandidates,
    boolean backfillRecommended,
    boolean canRunSync,
    boolean hasIndexBlockingDuplicates,
    boolean requiresManualReview,
    Map<String, Integer> candidateCountsByConfidence,
    int wouldCreateMappings,
    int alreadyMappedEvents,
    int alreadyMappedTransactions,
    List<DuplicateSourceOperationGroup> duplicateSourceOperationGroups,
    List<DuplicateSourceHashGroup> duplicateSourceHashGroups) {

  public record DuplicateSourceOperationGroup(
      UUID profileId,
      String source,
      String sourceOperationId,
      long count,
      List<DuplicateTransactionSample> transactions) {}

  public record DuplicateSourceHashGroup(
      UUID profileId,
      String sourceHash,
      long count,
      List<DuplicateTransactionSample> transactions) {}

  public record DuplicateTransactionSample(
      UUID transactionId,
      LocalDate realDate,
      String description,
      BigDecimal amount) {}
}
