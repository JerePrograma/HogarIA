package com.hogaria.expense.service;

import com.hogaria.expense.dto.ExpenseDto;

import java.time.LocalDate;
import java.util.List;

public interface ExpenseService {
    ExpenseDto createExpense(ExpenseDto dto);

    ExpenseDto updateExpense(Long id, ExpenseDto dto);

    List<ExpenseDto> getExpenses(
            Long familyId,
            LocalDate startDate,
            LocalDate endDate,
            Long categoryId,
            Long subcategoryId
    );
}
