package com.hogaria.expense.service;

import com.hogaria.expense.dto.RecurringExpenseDto;

import java.util.List;

public interface RecurringExpenseService {
    List<RecurringExpenseDto> getByFamilyId(Long familyId);

    RecurringExpenseDto create(RecurringExpenseDto dto);

    RecurringExpenseDto update(Long id, RecurringExpenseDto dto);

    void delete(Long id);
}
