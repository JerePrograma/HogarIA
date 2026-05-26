package com.hogaria.repository;

import com.hogaria.entity.ExternalSyncMapping;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ExternalSyncMappingRepository extends JpaRepository<ExternalSyncMapping, UUID> {

    List<ExternalSyncMapping> findByProfileId(UUID profileId);

    List<ExternalSyncMapping> findByProfileIdAndMoneyTransactionId(UUID profileId, UUID moneyTransactionId);

    boolean existsByProfileIdAndMoneyTransactionId(UUID profileId, UUID moneyTransactionId);

    Optional<ExternalSyncMapping>
            findByProfileIdAndExternalSystemAndExternalEntityTypeAndExternalEntityIdAndExternalEventType(
                    UUID profileId,
                    String externalSystem,
                    String externalEntityType,
                    String externalEntityId,
                    String externalEventType
            );
}
