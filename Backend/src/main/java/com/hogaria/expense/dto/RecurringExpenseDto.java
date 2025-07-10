package com.hogaria.expense.dto;


import java.time.LocalDate;

public record RecurringExpenseDto(
        Long id,
        Long expenseTemplateId,
        String frequency,
        LocalDate nextDueDate
) {
}
