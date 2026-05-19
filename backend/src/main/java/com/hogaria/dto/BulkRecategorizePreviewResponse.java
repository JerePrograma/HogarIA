package com.hogaria.dto;

import java.util.List;
import java.util.UUID;

public record BulkRecategorizePreviewResponse(
        UUID profileId,
        UUID targetCategoryId,
        int totalMatched,
        int updatableCount,
        int ambiguousCount,
        int skippedCount,
        List<BulkRecategorizeCandidate> candidates,
        List<String> warnings,
        List<String> errors
) {
}