package com.hogaria.repository;

import com.hogaria.entity.MonthlyPlanTransactionMatch;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MonthlyPlanTransactionMatchRepository extends JpaRepository<MonthlyPlanTransactionMatch, UUID> {

    List<MonthlyPlanTransactionMatch> findByProfileId(UUID profileId);

    List<MonthlyPlanTransactionMatch> findByProfileIdAndMonthlyPlanItemIdIn(
            UUID profileId,
            Collection<UUID> itemIds
    );

    List<MonthlyPlanTransactionMatch> findByProfileIdAndMoneyTransactionIdIn(
            UUID profileId,
            Collection<UUID> txIds
    );

    List<MonthlyPlanTransactionMatch> findByProfileIdAndMoneyTransactionId(
            UUID profileId,
            UUID moneyTransactionId
    );

    Optional<MonthlyPlanTransactionMatch> findByProfileIdAndMonthlyPlanItemIdAndMoneyTransactionId(
            UUID profileId,
            UUID itemId,
            UUID txId
    );

    Optional<MonthlyPlanTransactionMatch> findByIdAndProfileId(UUID id, UUID profileId);
}
