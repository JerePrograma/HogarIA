package com.hogaria.repository;

import com.hogaria.entity.MonthlyPlanTransactionMatch;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MonthlyPlanTransactionMatchRepository
    extends JpaRepository<MonthlyPlanTransactionMatch, UUID> {

  List<MonthlyPlanTransactionMatch> findByProfileIdAndMonthlyPlanItemIdIn(
      UUID profileId,
      Collection<UUID> monthlyPlanItemIds
  );

  List<MonthlyPlanTransactionMatch> findByProfileIdAndMoneyTransactionIdIn(
      UUID profileId,
      Collection<UUID> moneyTransactionIds
  );

  Optional<MonthlyPlanTransactionMatch> findByIdAndProfileId(UUID id, UUID profileId);

  Optional<MonthlyPlanTransactionMatch> findByProfileIdAndMonthlyPlanItemIdAndMoneyTransactionId(
      UUID profileId,
      UUID monthlyPlanItemId,
      UUID moneyTransactionId
  );

  void deleteByMonthlyPlanItemId(UUID monthlyPlanItemId);

  void deleteByMoneyTransactionId(UUID moneyTransactionId);
}
