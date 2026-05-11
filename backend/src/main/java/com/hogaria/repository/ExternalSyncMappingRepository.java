package com.hogaria.repository;

import com.hogaria.entity.ExternalSyncMapping;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ExternalSyncMappingRepository extends JpaRepository<ExternalSyncMapping, UUID> {
  Optional<ExternalSyncMapping>
      findByProfileIdAndExternalSystemAndExternalEntityTypeAndExternalEntityIdAndExternalEventType(
          UUID profileId,
          String externalSystem,
          String externalEntityType,
          String externalEntityId,
          String externalEventType);
}
