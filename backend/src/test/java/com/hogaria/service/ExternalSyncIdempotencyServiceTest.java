package com.hogaria.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.hogaria.entity.ExternalSyncMapping;
import com.hogaria.exception.ForbiddenException;
import com.hogaria.repository.ExternalSyncMappingRepository;
import com.hogaria.repository.FinancialProfileRepository;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ExternalSyncIdempotencyServiceTest {
  @Mock ExternalSyncMappingRepository repository;
  @Mock FinancialProfileRepository profileRepository;

  ExternalSyncIdempotencyService service;

  @BeforeEach
  void setUp() {
    service = new ExternalSyncIdempotencyService(repository, profileRepository);
  }

  @Test
  void detectsAlreadyProcessedEvent() {
    UUID userId = UUID.randomUUID();
    UUID profileId = UUID.randomUUID();
    when(profileRepository.existsByIdAndUserId(profileId, userId)).thenReturn(true);
    when(repository.findByProfileIdAndExternalSystemAndExternalEntityTypeAndExternalEntityIdAndExternalEventType(
            profileId, "CJPRESTAMOS", "LOAN", "123", "RECOVERY"))
        .thenReturn(Optional.of(ExternalSyncMapping.builder().status("PROCESSED").build()));

    assertTrue(service.isAlreadyProcessed(userId, profileId, "CJPRESTAMOS", "LOAN", "123", "RECOVERY"));
  }

  @Test
  void returnsFalseWhenFoundButFailedStatus() {
    UUID userId = UUID.randomUUID();
    UUID profileId = UUID.randomUUID();
    when(profileRepository.existsByIdAndUserId(profileId, userId)).thenReturn(true);
    when(repository.findByProfileIdAndExternalSystemAndExternalEntityTypeAndExternalEntityIdAndExternalEventType(
            profileId, "CJPRESTAMOS", "LOAN", "123", "RECOVERY"))
        .thenReturn(Optional.of(ExternalSyncMapping.builder().status("FAILED").build()));

    assertFalse(service.isAlreadyProcessed(userId, profileId, "CJPRESTAMOS", "LOAN", "123", "RECOVERY"));
  }

  @Test
  void throwsWhenProfileDoesNotBelongToUser() {
    UUID userId = UUID.randomUUID();
    UUID profileId = UUID.randomUUID();
    when(profileRepository.existsByIdAndUserId(profileId, userId)).thenReturn(false);

    assertThrows(
        ForbiddenException.class,
        () -> service.isAlreadyProcessed(userId, profileId, "CJPRESTAMOS", "LOAN", "123", "RECOVERY"));
  }

  @Test
  void upsertsProcessedMappingWithoutDuplicatingRows() {
    UUID profileId = UUID.randomUUID();
    ExternalSyncMapping existing =
        ExternalSyncMapping.builder()
            .id(UUID.randomUUID())
            .profileId(profileId)
            .externalSystem("CJPRESTAMOS")
            .externalEntityType("INSTALLMENT")
            .externalEntityId("999")
            .externalEventType("PAID")
            .status("FAILED")
            .build();

    when(repository.findByProfileIdAndExternalSystemAndExternalEntityTypeAndExternalEntityIdAndExternalEventType(
            profileId, "CJPRESTAMOS", "INSTALLMENT", "999", "PAID"))
        .thenReturn(Optional.of(existing));
    when(repository.save(any(ExternalSyncMapping.class))).thenAnswer(i -> i.getArgument(0));

    ExternalSyncMapping result =
        service.markProcessed(
            profileId,
            "CJPRESTAMOS",
            "INSTALLMENT",
            "999",
            "PAID",
            "hash-1",
            UUID.randomUUID(),
            null);

    assertEquals(existing.getId(), result.getId());
    assertEquals("PROCESSED", result.getStatus());
    assertNotNull(result.getSyncedAt());
    verify(repository, times(1)).save(existing);
  }

  @Test
  void sameExternalIdInDifferentProfilesIsNotDuplicate() {
    UUID userId = UUID.randomUUID();
    UUID profileA = UUID.randomUUID();
    UUID profileB = UUID.randomUUID();
    when(profileRepository.existsByIdAndUserId(profileA, userId)).thenReturn(true);
    when(profileRepository.existsByIdAndUserId(profileB, userId)).thenReturn(true);
    when(repository.findByProfileIdAndExternalSystemAndExternalEntityTypeAndExternalEntityIdAndExternalEventType(
            profileA, "CJPRESTAMOS", "LOAN", "123", "RECOVERY"))
        .thenReturn(Optional.of(ExternalSyncMapping.builder().status("PROCESSED").build()));
    when(repository.findByProfileIdAndExternalSystemAndExternalEntityTypeAndExternalEntityIdAndExternalEventType(
            profileB, "CJPRESTAMOS", "LOAN", "123", "RECOVERY"))
        .thenReturn(Optional.empty());

    assertTrue(service.isAlreadyProcessed(userId, profileA, "CJPRESTAMOS", "LOAN", "123", "RECOVERY"));
    assertFalse(service.isAlreadyProcessed(userId, profileB, "CJPRESTAMOS", "LOAN", "123", "RECOVERY"));
  }

  @Test
  void sameExternalIdInSameProfileIsDuplicate() {
    UUID userId = UUID.randomUUID();
    UUID profileId = UUID.randomUUID();
    when(profileRepository.existsByIdAndUserId(profileId, userId)).thenReturn(true);
    when(repository.findByProfileIdAndExternalSystemAndExternalEntityTypeAndExternalEntityIdAndExternalEventType(
            profileId, "CJPRESTAMOS", "LOAN", "123", "RECOVERY"))
        .thenReturn(Optional.of(ExternalSyncMapping.builder().status("PROCESSED").build()));

    assertTrue(service.isAlreadyProcessed(userId, profileId, "CJPRESTAMOS", "LOAN", "123", "RECOVERY"));
  }
}
