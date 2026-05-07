package com.hogaria.repository;

import com.hogaria.entity.MoneyTransaction;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MoneyTransactionRepository extends JpaRepository<MoneyTransaction, UUID> {
  List<MoneyTransaction> findByProfileIdAndBudgetDateBetween(UUID profileId, LocalDate from, LocalDate to);
}
