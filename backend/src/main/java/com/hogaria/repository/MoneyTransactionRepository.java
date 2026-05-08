package com.hogaria.repository;
import com.hogaria.entity.MoneyTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDate;
import java.util.*;
public interface MoneyTransactionRepository extends JpaRepository<MoneyTransaction, UUID> { List<MoneyTransaction> findByProfileIdAndBudgetDateBetween(UUID profileId, LocalDate from, LocalDate to); List<MoneyTransaction> findByProfileId(UUID profileId); Optional<MoneyTransaction> findByIdAndProfileId(UUID id, UUID profileId); }
