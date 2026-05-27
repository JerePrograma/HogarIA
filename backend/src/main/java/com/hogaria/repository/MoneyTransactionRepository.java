package com.hogaria.repository;

import com.hogaria.entity.MoneyTransaction;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface MoneyTransactionRepository extends JpaRepository<MoneyTransaction, UUID> {

    List<MoneyTransaction> findByProfileId(UUID profileId);

    Optional<MoneyTransaction> findByIdAndProfileId(UUID id, UUID profileId);

    List<MoneyTransaction> findByProfileIdAndBudgetDateBetween(
            UUID profileId,
            LocalDate from,
            LocalDate to
    );

    List<MoneyTransaction> findByProfileIdAndRealDateBetween(
            UUID profileId,
            LocalDate from,
            LocalDate to
    );

    List<MoneyTransaction> findByProfileIdAndAccountIdAndRealDateBetween(
            UUID profileId,
            UUID accountId,
            LocalDate from,
            LocalDate to
    );

    List<MoneyTransaction> findByProfileIdAndCategoryIdAndRealDateBetween(
            UUID profileId,
            UUID categoryId,
            LocalDate from,
            LocalDate to
    );

    List<MoneyTransaction> findByProfileIdAndAccountIdAndCategoryIdAndRealDateBetween(
            UUID profileId,
            UUID accountId,
            UUID categoryId,
            LocalDate from,
            LocalDate to
    );

    boolean existsByProfileIdAndSourceHash(UUID profileId, String sourceHash);

    Optional<MoneyTransaction> findByProfileIdAndSourceHash(UUID profileId, String sourceHash);

    List<MoneyTransaction> findByProfileIdAndSourceAndSourceOperationId(
            UUID profileId,
            String source,
            String sourceOperationId
    );

    List<MoneyTransaction> findByProfileIdAndRealDateBetweenAndAmount(
            UUID profileId,
            LocalDate from,
            LocalDate to,
            java.math.BigDecimal amount
    );

    List<MoneyTransaction> findByProfileIdAndRealDateBetweenAndAmountAndAccountIdNot(
            UUID profileId,
            LocalDate from,
            LocalDate to,
            java.math.BigDecimal amount,
            UUID accountId
    );

    List<MoneyTransaction> findByProfileIdAndIdIn(UUID profileId, List<UUID> ids);

    List<MoneyTransaction> findByProfileIdAndInternalTransferGroupId(UUID profileId, UUID internalTransferGroupId);

    List<MoneyTransaction> findByProfileIdAndOperationDateTimeBetween(
            UUID profileId,
            LocalDateTime from,
            LocalDateTime to
    );

    @Query("""
            select t from MoneyTransaction t
            where t.profileId = :profileId
              and t.accountId = :accountId
              and t.duplicateFingerprint = :fingerprint
              and t.status <> :ignoredStatus
              and t.classificationStatus <> :ignoredClassification
              and (:excludeId is null or t.id <> :excludeId)
            """)
    List<MoneyTransaction> findActiveDuplicatesByFingerprint(
            @Param("profileId") UUID profileId,
            @Param("accountId") UUID accountId,
            @Param("fingerprint") String fingerprint,
            @Param("ignoredStatus") MoneyTransaction.Status ignoredStatus,
            @Param("ignoredClassification") MoneyTransaction.ClassificationStatus ignoredClassification,
            @Param("excludeId") UUID excludeId
    );

    @Query("""
            select t from MoneyTransaction t
            where t.profileId = :profileId
              and t.source = :source
              and t.sourceOperationId = :sourceOperationId
              and (:excludeId is null or t.id <> :excludeId)
            """)
    List<MoneyTransaction> findByStrongSourceOperation(
            @Param("profileId") UUID profileId,
            @Param("source") String source,
            @Param("sourceOperationId") String sourceOperationId,
            @Param("excludeId") UUID excludeId
    );
}
