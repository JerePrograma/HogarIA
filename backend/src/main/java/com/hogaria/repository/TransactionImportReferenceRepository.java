package com.hogaria.repository;

import com.hogaria.entity.MoneyTransaction;
import com.hogaria.entity.TransactionImportReference;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TransactionImportReferenceRepository extends JpaRepository<TransactionImportReference, UUID> {
  Optional<TransactionImportReference> findByProfileIdAndAccountIdAndImportSourceAndSourceHash(
          UUID profileId,
          UUID accountId,
          String importSource,
          String sourceHash
  );

  @Query("""
          select r from TransactionImportReference r
          join MoneyTransaction t on t.id = r.transactionId
          where r.profileId = :profileId
            and r.accountId = :accountId
            and r.importSource = :importSource
            and r.sourceHash = :sourceHash
            and t.status <> :ignoredStatus
            and t.classificationStatus <> :ignoredClassification
          """)
  Optional<TransactionImportReference> findActiveByProfileIdAndAccountIdAndImportSourceAndSourceHash(
          @Param("profileId") UUID profileId,
          @Param("accountId") UUID accountId,
          @Param("importSource") String importSource,
          @Param("sourceHash") String sourceHash,
          @Param("ignoredStatus") MoneyTransaction.Status ignoredStatus,
          @Param("ignoredClassification") MoneyTransaction.ClassificationStatus ignoredClassification
  );
}
