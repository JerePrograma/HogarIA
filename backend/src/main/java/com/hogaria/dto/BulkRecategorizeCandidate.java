package com.hogaria.dto;

import com.hogaria.entity.MoneyTransaction;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record BulkRecategorizeCandidate(
        UUID transactionId,
        UUID accountId,
        UUID currentCategoryId,
        UUID targetCategoryId,
        MoneyTransaction.MovementType movementType,
        LocalDate realDate,
        LocalDate budgetDate,
        BigDecimal amount,
        String description,
        MoneyTransaction.Origin origin,
        MoneyTransaction.Status status,
        String previewStatus,
        String warning
) {
}