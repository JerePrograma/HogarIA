package com.hogaria.repository;

import com.hogaria.entity.MoneyTransaction;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

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
}