package com.hogaria.service;

import com.hogaria.entity.ExternalSyncMapping;
import com.hogaria.exception.ForbiddenException;
import com.hogaria.repository.ExternalSyncMappingRepository;
import com.hogaria.repository.FinancialProfileRepository;
import java.time.LocalDateTime;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class ExternalSyncIdempotencyService {
  private static final String STATUS_PROCESSED = "PROCESSED";
  private static final String STATUS_FAILED = "FAILED";

  private final ExternalSyncMappingRepository repository;
  private final FinancialProfileRepository profileRepository;

  public ExternalSyncIdempotencyService(
      ExternalSyncMappingRepository repository, FinancialProfileRepository profileRepository) {
    this.repository = repository;
    this.profileRepository = profileRepository;
  }

  public boolean isAlreadyProcessed(
      UUID userId,
      UUID profileId,
      String externalSystem,
      String externalEntityType,
      String externalEntityId,
      String externalEventType) {
    ensureProfileBelongsToUser(userId, profileId);
    return repository
        .findByExternalSystemAndExternalEntityTypeAndExternalEntityIdAndExternalEventType(
            externalSystem, externalEntityType, externalEntityId, externalEventType)
        .filter(mapping -> STATUS_PROCESSED.equals(mapping.getStatus()))
        .isPresent();
  }

  public ExternalSyncMapping markProcessed(
      UUID profileId,
      String externalSystem,
      String externalEntityType,
      String externalEntityId,
      String externalEventType,
      String eventHash,
      UUID moneyTransactionId,
      UUID monthlyPlanItemId) {
    ExternalSyncMapping mapping =
        findOrCreate(profileId, externalSystem, externalEntityType, externalEntityId, externalEventType);
    mapping.setEventHash(eventHash);
    mapping.setMoneyTransactionId(moneyTransactionId);
    mapping.setMonthlyPlanItemId(monthlyPlanItemId);
    mapping.setStatus(STATUS_PROCESSED);
    mapping.setErrorMessage(null);
    mapping.setSyncedAt(LocalDateTime.now());
    return repository.save(mapping);
  }

  public ExternalSyncMapping markFailed(
      UUID profileId,
      String externalSystem,
      String externalEntityType,
      String externalEntityId,
      String externalEventType,
      String eventHash,
      String errorMessage) {
    ExternalSyncMapping mapping =
        findOrCreate(profileId, externalSystem, externalEntityType, externalEntityId, externalEventType);
    mapping.setEventHash(eventHash);
    mapping.setStatus(STATUS_FAILED);
    mapping.setErrorMessage(errorMessage);
    return repository.save(mapping);
  }

  private ExternalSyncMapping findOrCreate(
      UUID profileId,
      String externalSystem,
      String externalEntityType,
      String externalEntityId,
      String externalEventType) {
    return repository
        .findByExternalSystemAndExternalEntityTypeAndExternalEntityIdAndExternalEventType(
            externalSystem, externalEntityType, externalEntityId, externalEventType)
        .orElseGet(
            () ->
                ExternalSyncMapping.builder()
                    .profileId(profileId)
                    .externalSystem(externalSystem)
                    .externalEntityType(externalEntityType)
                    .externalEntityId(externalEntityId)
                    .externalEventType(externalEventType)
                    .build());
  }

  private void ensureProfileBelongsToUser(UUID userId, UUID profileId) {
    if (!profileRepository.existsByIdAndUserId(profileId, userId)) {
      throw new ForbiddenException("Profile no pertenece al usuario");
    }
  }
}
