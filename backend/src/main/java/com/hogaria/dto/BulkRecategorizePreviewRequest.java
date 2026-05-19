package com.hogaria.dto;

import com.hogaria.entity.MoneyTransaction;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record BulkRecategorizePreviewRequest(
        UUID accountId,
        LocalDate from,
        LocalDate to,
        UUID fromCategoryId,
        Boolean onlyWithoutCategory,
        String targetMode,
        UUID toCategoryId,
        MoneyTransaction.MovementType movementType,
        String descriptionContains,
        BigDecimal exactAmount,
        BigDecimal minAmount,
        BigDecimal maxAmount,
        Boolean onlyImported
) {
}
