package com.hogaria.repository;

import com.hogaria.entity.MonthlyPlanItem;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MonthlyPlanItemRepository extends JpaRepository<MonthlyPlanItem, UUID> {

    List<MonthlyPlanItem> findByProfileIdAndPeriodYearAndPeriodMonth(
            UUID profileId,
            Integer year,
            Integer month
    );

    List<MonthlyPlanItem> findByProfileId(UUID profileId);

    Optional<MonthlyPlanItem> findByIdAndProfileId(UUID id, UUID profileId);

    List<MonthlyPlanItem> findByProfileIdAndTransactionId(UUID profileId, UUID transactionId);

    boolean existsByProfileIdAndTransactionId(UUID profileId, UUID transactionId);
}
