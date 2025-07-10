package com.hogaria.expense.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record ExpenseDto(
        Long id,
        Long familyId,
        Long userId,
        BigDecimal amount,
        String currency,
        Long categoryId,
        Long subcategoryId,
        String descripcion,
        LocalDate date
) {}
