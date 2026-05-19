package com.hogaria.dto;

import java.util.UUID;

public record BulkRecategorizeApplyItem(
        UUID transactionId,
        UUID targetCategoryId
) {
}
