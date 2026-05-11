package com.hogaria.repository;

import com.hogaria.entity.ExternalLoanSyncConfig;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ExternalLoanSyncConfigRepository extends JpaRepository<ExternalLoanSyncConfig, UUID> {
  Optional<ExternalLoanSyncConfig> findByProfileId(UUID profileId);
}
