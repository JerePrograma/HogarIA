package com.hogaria.service;

import com.hogaria.entity.ExternalSyncMapping;
import com.hogaria.entity.MoneyTransaction;
import com.hogaria.exception.ForbiddenException;
import com.hogaria.integration.cjprestamos.dto.ExternalLoanBackfillDtos.BackfillApplyRequest;
import com.hogaria.integration.cjprestamos.dto.ExternalLoanBackfillDtos.BackfillApplyResponse;
import com.hogaria.integration.cjprestamos.dto.ExternalLoanBackfillDtos.BackfillCandidate;
import com.hogaria.integration.cjprestamos.dto.ExternalLoanBackfillDtos.BackfillDryRunResponse;
import com.hogaria.repository.ExternalSyncMappingRepository;
import com.hogaria.repository.FinancialProfileRepository;
import com.hogaria.repository.MoneyTransactionRepository;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class ExternalLoanBackfillService {
  private static final String SYSTEM = "CJPRESTAMOS";
  private static final String STATUS_PROCESSED = "PROCESSED";
  private static final String CONFIDENCE_HIGH = "HIGH";
  private static final String CONFIDENCE_MEDIUM = "MEDIUM";
  private static final String CONFIDENCE_LOW = "LOW";

  private static final Pattern DISBURSEMENT_DESCRIPTION = Pattern.compile("Préstamo CJ #(\\d+)");
  private static final Pattern PRINCIPAL_DESCRIPTION =
      Pattern.compile("Recupero capital CJ pago #(\\d+)");
  private static final Pattern INTEREST_DESCRIPTION = Pattern.compile("Interés CJ pago #(\\d+)");
  private static final Pattern FIRST_NUMBER = Pattern.compile("(\\d+)");

  private final MoneyTransactionRepository transactionRepository;
  private final ExternalSyncMappingRepository mappingRepository;
  private final FinancialProfileRepository profileRepository;
  private final ExternalSyncIdempotencyService idempotencyService;

  public ExternalLoanBackfillService(
      MoneyTransactionRepository transactionRepository,
      ExternalSyncMappingRepository mappingRepository,
      FinancialProfileRepository profileRepository,
      ExternalSyncIdempotencyService idempotencyService) {
    this.transactionRepository = transactionRepository;
    this.mappingRepository = mappingRepository;
    this.profileRepository = profileRepository;
    this.idempotencyService = idempotencyService;
  }

  public BackfillDryRunResponse dryRun(UUID userId, UUID profileId) {
    ensureProfileBelongsToUser(profileId, userId);
    List<BackfillCandidate> candidates = new ArrayList<>();

    for (MoneyTransaction transaction : transactionRepository.findByProfileId(profileId)) {
      if (!isCjTransaction(transaction)) {
        continue;
      }

      Inference inference = infer(transaction);
      if (inference == null) {
        continue;
      }

      Optional<ExternalSyncMapping> eventMapping =
          mappingRepository
              .findByProfileIdAndExternalSystemAndExternalEntityTypeAndExternalEntityIdAndExternalEventType(
                  profileId,
                  SYSTEM,
                  inference.entityType,
                  inference.entityId,
                  inference.eventType);
      boolean transactionAlreadyMapped =
          mappingRepository.existsByProfileIdAndMoneyTransactionId(profileId, transaction.getId());
      String warning = null;
      boolean wouldCreateMapping = true;

      if (eventMapping.filter(mapping -> STATUS_PROCESSED.equals(mapping.getStatus())).isPresent()) {
        warning = "mapping already exists";
        wouldCreateMapping = false;
      } else if (transactionAlreadyMapped) {
        warning = "transaction already mapped";
        wouldCreateMapping = false;
      }

      candidates.add(
          new BackfillCandidate(
              transaction.getId(),
              transaction.getDescription(),
              transaction.getAmount(),
              transaction.getRealDate(),
              inference.entityType,
              inference.entityId,
              inference.eventType,
              inference.confidence,
              warning,
              wouldCreateMapping));
    }

    return new BackfillDryRunResponse(candidates);
  }

  public BackfillApplyResponse apply(UUID userId, UUID profileId, BackfillApplyRequest request) {
    BackfillApplyRequest safeRequest =
        request == null ? new BackfillApplyRequest(false) : request;
    List<String> skipped = new ArrayList<>();
    List<String> errors = new ArrayList<>();
    int created = 0;

    for (BackfillCandidate candidate : dryRun(userId, profileId).candidates()) {
      if (!candidate.wouldCreateMapping()) {
        skipped.add(candidate.transactionId() + ": " + candidate.warning());
        continue;
      }
      if (CONFIDENCE_LOW.equals(candidate.confidence()) && !safeRequest.includeLowConfidence()) {
        skipped.add(candidate.transactionId() + ": low confidence");
        continue;
      }

      try {
        Optional<MoneyTransaction> transaction =
            transactionRepository.findByIdAndProfileId(candidate.transactionId(), profileId);
        if (transaction.isEmpty()) {
          errors.add(candidate.transactionId() + ": transaction not found");
          continue;
        }

        if (mappingRepository.existsByProfileIdAndMoneyTransactionId(
            profileId, candidate.transactionId())) {
          skipped.add(candidate.transactionId() + ": transaction already mapped");
          continue;
        }

        idempotencyService.markProcessed(
            profileId,
            SYSTEM,
            candidate.inferredEntityType(),
            candidate.inferredEntityId(),
            candidate.inferredEventType(),
            eventHash(candidate, transaction.get()),
            candidate.transactionId(),
            null);
        created++;
      } catch (Exception ex) {
        errors.add(candidate.transactionId() + ": " + ex.getMessage());
      }
    }

    return new BackfillApplyResponse(created, skipped, errors);
  }

  private boolean isCjTransaction(MoneyTransaction transaction) {
    return SYSTEM.equalsIgnoreCase(transaction.getSource())
        || containsCj(transaction.getDescription())
        || containsCj(transaction.getClassificationReason());
  }

  private boolean containsCj(String value) {
    return value != null && value.toUpperCase().contains("CJ");
  }

  private Inference infer(MoneyTransaction transaction) {
    if (StringUtils.hasText(transaction.getSourceOperationId())) {
      String[] parts = transaction.getSourceOperationId().split(":");
      if (parts.length == 3) {
        return new Inference(
            parts[0],
            parts[1],
            normalizeOperationEvent(parts[2]),
            CONFIDENCE_HIGH);
      }
    }

    String description = Optional.ofNullable(transaction.getDescription()).orElse("");
    Matcher matcher = DISBURSEMENT_DESCRIPTION.matcher(description);
    if (matcher.find()) {
      return new Inference("LOAN", matcher.group(1), "DISBURSEMENT", CONFIDENCE_MEDIUM);
    }

    matcher = PRINCIPAL_DESCRIPTION.matcher(description);
    if (matcher.find()) {
      return new Inference(
          "PAYMENT", matcher.group(1), "PAYMENT_PRINCIPAL_RECOVERY", CONFIDENCE_MEDIUM);
    }

    matcher = INTEREST_DESCRIPTION.matcher(description);
    if (matcher.find()) {
      return new Inference("PAYMENT", matcher.group(1), "PAYMENT_INTEREST_INCOME", CONFIDENCE_MEDIUM);
    }

    return inferLowConfidence(transaction, description);
  }

  private Inference inferLowConfidence(MoneyTransaction transaction, String description) {
    String inferredId = firstNumber(description);
    if (!StringUtils.hasText(inferredId)) {
      return null;
    }

    return switch (Optional.ofNullable(transaction.getClassificationReason()).orElse("")) {
      case "CJPRESTAMOS_DISBURSEMENT" ->
          new Inference("LOAN", inferredId, "DISBURSEMENT", CONFIDENCE_LOW);
      case "CJPRESTAMOS_PAYMENT_PRINCIPAL_RECOVERY" ->
          new Inference("PAYMENT", inferredId, "PAYMENT_PRINCIPAL_RECOVERY", CONFIDENCE_LOW);
      case "CJPRESTAMOS_PAYMENT_INTEREST_INCOME" ->
          new Inference("PAYMENT", inferredId, "PAYMENT_INTEREST_INCOME", CONFIDENCE_LOW);
      default -> null;
    };
  }

  private String firstNumber(String value) {
    Matcher matcher = FIRST_NUMBER.matcher(Optional.ofNullable(value).orElse(""));
    return matcher.find() ? matcher.group(1) : null;
  }

  private String normalizeOperationEvent(String eventType) {
    return switch (eventType) {
      case "PRINCIPAL" -> "PAYMENT_PRINCIPAL_RECOVERY";
      case "INTEREST" -> "PAYMENT_INTEREST_INCOME";
      default -> eventType;
    };
  }

  private String eventHash(BackfillCandidate candidate, MoneyTransaction transaction) {
    if (StringUtils.hasText(transaction.getSourceHash())) {
      return transaction.getSourceHash();
    }

    return sha256(
        String.join(
            "|",
            transaction.getProfileId().toString(),
            SYSTEM,
            candidate.inferredEntityType(),
            candidate.inferredEntityId(),
            candidate.inferredEventType(),
            String.valueOf(candidate.transactionId())));
  }

  private String sha256(String raw) {
    try {
      MessageDigest digest = MessageDigest.getInstance("SHA-256");
      byte[] hash = digest.digest(raw.getBytes(StandardCharsets.UTF_8));
      StringBuilder sb = new StringBuilder();
      for (byte b : hash) {
        sb.append(String.format("%02x", b));
      }
      return sb.toString();
    } catch (NoSuchAlgorithmException e) {
      throw new IllegalStateException("SHA-256 unavailable", e);
    }
  }

  private void ensureProfileBelongsToUser(UUID profileId, UUID userId) {
    if (!profileRepository.existsByIdAndUserId(profileId, userId)) {
      throw new ForbiddenException("Profile no pertenece al usuario");
    }
  }

  private record Inference(String entityType, String entityId, String eventType, String confidence) {}
}
