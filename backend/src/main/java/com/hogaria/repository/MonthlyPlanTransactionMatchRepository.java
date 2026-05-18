package com.hogaria.repository;

import com.hogaria.entity.MonthlyPlanTransactionMatch;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.*;

public interface MonthlyPlanTransactionMatchRepository extends JpaRepository<MonthlyPlanTransactionMatch, UUID> {
  List<MonthlyPlanTransactionMatch> findByProfileId(UUID profileId);
  List<MonthlyPlanTransactionMatch> findByProfileIdAndMonthlyPlanItemIdIn(UUID profileId, Collection<UUID> itemIds);
  List<MonthlyPlanTransactionMatch> findByProfileIdAndMoneyTransactionIdIn(UUID profileId, Collection<UUID> txIds);
  Optional<MonthlyPlanTransactionMatch> findByProfileIdAndMonthlyPlanItemIdAndMoneyTransactionId(UUID profileId, UUID itemId, UUID txId);
  Optional<MonthlyPlanTransactionMatch> findByIdAndProfileId(UUID id, UUID profileId);
}
