package com.hogaria.expense.repository;

import com.hogaria.expense.model.RecurringExpense;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RecurringExpenseRepository extends JpaRepository<RecurringExpense, Long> {
    // Busca todas las recurrentes cuyo gasto plantila pertenece a familyId
    List<RecurringExpense> findAllByExpenseTemplate_FamilyId(Long familyId);
}
