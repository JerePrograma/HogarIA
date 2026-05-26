package com.hogaria.dto;

import com.hogaria.entity.MoneyTransaction;
import java.util.List;
import java.util.UUID;

public record BulkRecategorizeApplyRequest(
        String targetMode,
        UUID toCategoryId,
        MoneyTransaction.MovementType targetMovementType,
        MoneyTransaction.Status targetStatus,
        MoneyTransaction.ClassificationStatus targetClassificationStatus,
        String targetClassificationReason,
        List<UUID> transactionIds,
        List<BulkRecategorizeApplyItem> updates,
        Boolean forceAmbiguous
) {
}