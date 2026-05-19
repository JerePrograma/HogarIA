package com.hogaria.dto;

import java.util.List;
import java.util.UUID;

public record BulkRecategorizeApplyResponse(
        int updatedCount,
        int skippedCount,
        int failedCount,
        List<UUID> updatedTransactionIds,
        List<String> warnings,
        List<String> errors
) {
}