package com.hogaria.service;

import com.hogaria.entity.ExternalSyncMapping;
import com.hogaria.entity.MoneyTransaction;
import com.hogaria.exception.ForbiddenException;
import com.hogaria.repository.ExternalSyncMappingRepository;
import com.hogaria.repository.FinancialProfileRepository;
import com.hogaria.repository.MoneyTransactionRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDate;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class ExternalLoanEventDuplicateDetector {
  public static final String SYSTEM = "CJPRESTAMOS";
  public static final String REASON_NONE = "NONE";
  public static final String REASON_MAPPING_PROCESSED = "EXTERNAL_SYNC_MAPPING_PROCESSED";
  public static final String REASON_SOURCE_OPERATION_ID = "MONEY_TRANSACTION_SOURCE_OPERATION_ID";
  public static final String REASON_SOURCE_HASH = "MONEY_TRANSACTION_SOURCE_HASH";

  private static final String STATUS_PROCESSED = "PROCESSED";
  private static final String LOAN_ENTITY = "LOAN";
  private static final String PAYMENT_ENTITY = "PAYMENT";
  private static final String PAYMENT_PRINCIPAL_RECOVERY_EVENT = "PAYMENT_PRINCIPAL_RECOVERY";

  private final ExternalSyncMappingRepository mappingRepository;
  private final MoneyTransactionRepository transactionRepository;
  private final FinancialProfileRepository profileRepository;

  public ExternalLoanEventDuplicateDetector(
      ExternalSyncMappingRepository mappingRepository,
      MoneyTransactionRepository transactionRepository,
      FinancialProfileRepository profileRepository) {
    this.mappingRepository = mappingRepository;
    this.transactionRepository = transactionRepository;
    this.profileRepository = profileRepository;
  }

  public DuplicateDetectionResult detect(
      UUID userId,
      UUID profileId,
      UUID accountId,
      UUID categoryId,
      String entityType,
      String entityId,
      String eventType,
      LocalDate date,
      BigDecimal amount) {
    ensureProfileBelongsToUser(userId, profileId);
    String sourceOperationId = sourceOperationId(entityType, entityId, eventType);
    String sourceHash =
        sourceHash(profileId, accountId, categoryId, entityType, entityId, eventType, date, amount);

    return detect(profileId, entityType, entityId, eventType, sourceOperationId, sourceHash);
  }

  public String sourceOperationId(String entityType, String entityId, String eventType) {
    if (LOAN_ENTITY.equals(entityType)) {
      return "LOAN:" + entityId + ":" + eventType;
    }
    if (PAYMENT_ENTITY.equals(entityType)) {
      return "PAYMENT:"
          + entityId
          + ":"
          + (PAYMENT_PRINCIPAL_RECOVERY_EVENT.equals(eventType) ? "PRINCIPAL" : "INTEREST");
    }
    return entityType + ":" + entityId + ":" + eventType;
  }

  public String sourceHash(
      UUID profileId,
      UUID accountId,
      UUID categoryId,
      String entityType,
      String entityId,
      String eventType,
      LocalDate date,
      BigDecimal amount) {
    String normalizedAmount = amount.setScale(2, RoundingMode.HALF_UP).toPlainString();
    String raw =
        String.join(
            "|",
            profileId.toString(),
            SYSTEM,
            entityType,
            entityId,
            eventType,
            normalizedAmount,
            date.toString(),
            String.valueOf(accountId),
            String.valueOf(categoryId));
    return sha256(raw);
  }

  private DuplicateDetectionResult detect(
      UUID profileId,
      String entityType,
      String entityId,
      String eventType,
      String sourceOperationId,
      String sourceHash) {
    return mappingRepository
        .findByProfileIdAndExternalSystemAndExternalEntityTypeAndExternalEntityIdAndExternalEventType(
            profileId, SYSTEM, entityType, entityId, eventType)
        .filter(mapping -> STATUS_PROCESSED.equals(mapping.getStatus()))
        .map(mapping -> mappingProcessed(mapping, sourceOperationId, sourceHash))
        .orElseGet(() -> detectTransactionDuplicate(profileId, sourceOperationId, sourceHash));
  }

  private DuplicateDetectionResult mappingProcessed(
      ExternalSyncMapping mapping, String sourceOperationId, String sourceHash) {
    return DuplicateDetectionResult.duplicate(
        REASON_MAPPING_PROCESSED,
        mapping.getMoneyTransactionId(),
        sourceOperationId,
        sourceHash);
  }

  private DuplicateDetectionResult detectTransactionDuplicate(
      UUID profileId, String sourceOperationId, String sourceHash) {
    var bySourceOperation =
        transactionRepository.findByProfileIdAndSourceAndSourceOperationId(
            profileId, SYSTEM, sourceOperationId);
    if (!bySourceOperation.isEmpty()) {
      return DuplicateDetectionResult.duplicate(
          REASON_SOURCE_OPERATION_ID,
          bySourceOperation.getFirst().getId(),
          sourceOperationId,
          sourceHash);
    }

    return transactionRepository
        .findByProfileIdAndSourceHash(profileId, sourceHash)
        .map(MoneyTransaction::getId)
        .map(
            transactionId ->
                DuplicateDetectionResult.duplicate(
                    REASON_SOURCE_HASH, transactionId, sourceOperationId, sourceHash))
        .orElseGet(() -> DuplicateDetectionResult.notDuplicate(sourceOperationId, sourceHash));
  }

  private void ensureProfileBelongsToUser(UUID userId, UUID profileId) {
    if (!profileRepository.existsByIdAndUserId(profileId, userId)) {
      throw new ForbiddenException("Profile no pertenece al usuario");
    }
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

  public record DuplicateDetectionResult(
      boolean duplicate,
      String reason,
      UUID existingTransactionId,
      String sourceOperationId,
      String sourceHash) {

    public static DuplicateDetectionResult notDuplicate(String sourceOperationId, String sourceHash) {
      return new DuplicateDetectionResult(false, REASON_NONE, null, sourceOperationId, sourceHash);
    }

    public static DuplicateDetectionResult duplicate(
        String reason, UUID existingTransactionId, String sourceOperationId, String sourceHash) {
      return new DuplicateDetectionResult(true, reason, existingTransactionId, sourceOperationId, sourceHash);
    }

    public boolean existingTransactionWithoutMapping() {
      return duplicate
          && (REASON_SOURCE_OPERATION_ID.equals(reason) || REASON_SOURCE_HASH.equals(reason));
    }
  }
}
