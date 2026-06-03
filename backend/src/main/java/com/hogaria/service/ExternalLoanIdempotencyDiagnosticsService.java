package com.hogaria.service;

import com.hogaria.entity.MoneyTransaction;
import com.hogaria.integration.cjprestamos.dto.ExternalLoanBackfillDtos.BackfillCandidate;
import com.hogaria.integration.cjprestamos.dto.ExternalLoanIdempotencyDiagnosticsResponse;
import com.hogaria.integration.cjprestamos.dto.ExternalLoanIdempotencyDiagnosticsResponse.DuplicateSourceHashGroup;
import com.hogaria.integration.cjprestamos.dto.ExternalLoanIdempotencyDiagnosticsResponse.DuplicateSourceOperationGroup;
import com.hogaria.integration.cjprestamos.dto.ExternalLoanIdempotencyDiagnosticsResponse.DuplicateTransactionSample;
import com.hogaria.repository.MoneyTransactionRepository;
import com.hogaria.repository.MoneyTransactionRepository.DuplicateSourceHashGroupProjection;
import com.hogaria.repository.MoneyTransactionRepository.DuplicateSourceOperationGroupProjection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ExternalLoanIdempotencyDiagnosticsService {
  private static final int DUPLICATE_GROUP_LIMIT = 50;
  private static final int DUPLICATE_TRANSACTION_SAMPLE_LIMIT = 10;
  private static final String CONFIDENCE_HIGH = "HIGH";
  private static final String CONFIDENCE_MEDIUM = "MEDIUM";
  private static final String CONFIDENCE_LOW = "LOW";
  private static final String WARNING_MAPPING_ALREADY_EXISTS = "mapping already exists";
  private static final String WARNING_TRANSACTION_ALREADY_MAPPED = "transaction already mapped";

  private final ExternalLoanBackfillService backfillService;
  private final MoneyTransactionRepository transactionRepository;

  public ExternalLoanIdempotencyDiagnosticsService(
      ExternalLoanBackfillService backfillService,
      MoneyTransactionRepository transactionRepository) {
    this.backfillService = backfillService;
    this.transactionRepository = transactionRepository;
  }

  @Transactional(readOnly = true)
  public ExternalLoanIdempotencyDiagnosticsResponse diagnose(UUID userId, UUID profileId) {
    List<BackfillCandidate> candidates = backfillService.dryRun(userId, profileId).candidates();
    Map<String, Integer> candidateCountsByConfidence = initialConfidenceCounts();
    int wouldCreateMappings = 0;
    int alreadyMappedEvents = 0;
    int alreadyMappedTransactions = 0;

    for (BackfillCandidate candidate : candidates) {
      candidateCountsByConfidence.compute(
          candidate.confidence(), (ignored, count) -> count == null ? 1 : count + 1);

      if (candidate.wouldCreateMapping()) {
        wouldCreateMappings++;
      } else if (WARNING_MAPPING_ALREADY_EXISTS.equals(candidate.warning())) {
        alreadyMappedEvents++;
      } else if (WARNING_TRANSACTION_ALREADY_MAPPED.equals(candidate.warning())) {
        alreadyMappedTransactions++;
      }
    }

    List<DuplicateSourceOperationGroup> duplicateSourceOperationGroups =
        transactionRepository
            .findDuplicateSourceOperationGroups(
                profileId, PageRequest.of(0, DUPLICATE_GROUP_LIMIT))
            .stream()
            .map(this::toSourceOperationGroup)
            .toList();
    List<DuplicateSourceHashGroup> duplicateSourceHashGroups =
        transactionRepository
            .findDuplicateSourceHashGroups(profileId, PageRequest.of(0, DUPLICATE_GROUP_LIMIT))
            .stream()
            .map(this::toSourceHashGroup)
            .toList();

    boolean hasIndexBlockingDuplicates =
        !duplicateSourceOperationGroups.isEmpty() || !duplicateSourceHashGroups.isEmpty();
    boolean backfillRecommended = wouldCreateMappings > 0;
    boolean requiresManualReview =
        hasIndexBlockingDuplicates
            || candidateCountsByConfidence.getOrDefault(CONFIDENCE_LOW, 0) > 0;
    int mappedTransactions = alreadyMappedEvents + alreadyMappedTransactions;

    return new ExternalLoanIdempotencyDiagnosticsResponse(
        candidates.size(),
        mappedTransactions,
        wouldCreateMappings,
        backfillRecommended,
        !hasIndexBlockingDuplicates,
        hasIndexBlockingDuplicates,
        requiresManualReview,
        candidateCountsByConfidence,
        wouldCreateMappings,
        alreadyMappedEvents,
        alreadyMappedTransactions,
        duplicateSourceOperationGroups,
        duplicateSourceHashGroups);
  }

  private Map<String, Integer> initialConfidenceCounts() {
    Map<String, Integer> counts = new LinkedHashMap<>();
    counts.put(CONFIDENCE_HIGH, 0);
    counts.put(CONFIDENCE_MEDIUM, 0);
    counts.put(CONFIDENCE_LOW, 0);
    return counts;
  }

  private DuplicateSourceOperationGroup toSourceOperationGroup(
      DuplicateSourceOperationGroupProjection group) {
    List<DuplicateTransactionSample> transactions =
        transactionRepository
            .findDuplicateSourceOperationSamples(
                group.getProfileId(),
                group.getSource(),
                group.getSourceOperationId(),
                PageRequest.of(0, DUPLICATE_TRANSACTION_SAMPLE_LIMIT))
            .stream()
            .map(this::toSample)
            .toList();

    return new DuplicateSourceOperationGroup(
        group.getProfileId(),
        group.getSource(),
        group.getSourceOperationId(),
        group.getTransactionCount(),
        transactions);
  }

  private DuplicateSourceHashGroup toSourceHashGroup(DuplicateSourceHashGroupProjection group) {
    List<DuplicateTransactionSample> transactions =
        transactionRepository
            .findDuplicateSourceHashSamples(
                group.getProfileId(),
                group.getSourceHash(),
                PageRequest.of(0, DUPLICATE_TRANSACTION_SAMPLE_LIMIT))
            .stream()
            .map(this::toSample)
            .toList();

    return new DuplicateSourceHashGroup(
        group.getProfileId(), group.getSourceHash(), group.getTransactionCount(), transactions);
  }

  private DuplicateTransactionSample toSample(MoneyTransaction transaction) {
    return new DuplicateTransactionSample(
        transaction.getId(),
        transaction.getRealDate(),
        transaction.getDescription(),
        transaction.getAmount());
  }
}
