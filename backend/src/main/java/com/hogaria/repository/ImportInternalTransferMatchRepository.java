package com.hogaria.repository;

import com.hogaria.entity.ImportInternalTransferMatch;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ImportInternalTransferMatchRepository extends JpaRepository<ImportInternalTransferMatch, UUID> {

    Optional<ImportInternalTransferMatch> findByDebitImportRowIdAndCreditImportRowId(
            UUID debitImportRowId,
            UUID creditImportRowId
    );

    @Query("""
            select match from ImportInternalTransferMatch match
            where match.profileId = :profileId
              and (match.debitImportRowId = :importRowId or match.creditImportRowId = :importRowId)
            """)
    List<ImportInternalTransferMatch> findByProfileIdAndImportRowId(
            @Param("profileId") UUID profileId,
            @Param("importRowId") UUID importRowId
    );
}
