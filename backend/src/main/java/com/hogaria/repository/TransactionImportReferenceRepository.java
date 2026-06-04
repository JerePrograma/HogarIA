package com.hogaria.repository;

import com.hogaria.entity.TransactionImportReference;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TransactionImportReferenceRepository extends JpaRepository<TransactionImportReference, UUID> {
  Optional<TransactionImportReference> findByProfileIdAndAccountIdAndImportSourceAndSourceHash(
          UUID profileId,
          UUID accountId,
          String importSource,
          String sourceHash
  );
}
