package com.hogaria.dto;

import java.util.List;
import java.util.UUID;

public record BulkRecategorizeApplyRequest(
        String targetMode,
        UUID toCategoryId,
        List<UUID> transactionIds,
        List<BulkRecategorizeApplyItem> updates
) {
}
