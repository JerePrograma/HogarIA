package com.hogaria.expense.repository;

import com.hogaria.expense.model.Expense;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface ExpenseRepository extends JpaRepository<Expense, Long> {

    @Query("""
            SELECT e FROM Expense e
             WHERE e.familyId = :familyId
               AND (:startDate   IS NULL OR e.date >= :startDate)
               AND (:endDate     IS NULL OR e.date <= :endDate)
               AND (:categoryId  IS NULL OR e.categoryId = :categoryId)
               AND (:subcategoryId IS NULL OR e.subcategoryId = :subcategoryId)
            """)
    List<Expense> findAllByFilters(
            @Param("familyId") Long familyId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate,
            @Param("categoryId") Long categoryId,
            @Param("subcategoryId") Long subcategoryId
    );
}
