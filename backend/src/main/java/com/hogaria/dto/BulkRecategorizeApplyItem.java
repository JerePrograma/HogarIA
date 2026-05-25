package com.hogaria.dto;

import com.hogaria.entity.MoneyTransaction;
import java.util.UUID;

public record BulkRecategorizeApplyItem(
        UUID transactionId,
        UUID targetCategoryId,
        MoneyTransaction.MovementType targetMovementType,
        MoneyTransaction.Status targetStatus,
        MoneyTransaction.ClassificationStatus targetClassificationStatus,
        String targetClassificationReason
) {
}
