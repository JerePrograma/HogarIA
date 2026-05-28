package com.hogaria.dto;

import com.hogaria.entity.MoneyTransaction;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import java.util.UUID;

public class TransactionBulkDtos {

    public record BulkCategorizeRequest(
            @NotEmpty List<UUID> transactionIds,
            @NotNull UUID categoryId
    ) {
    }

    public record BulkStatusRequest(
            @NotEmpty List<UUID> transactionIds,
            @NotNull MoneyTransaction.Status status,
            MoneyTransaction.ClassificationStatus classificationStatus,
            String reason
    ) {
    }

    public record BulkIgnoreRequest(
            @NotEmpty List<UUID> transactionIds,
            String reason
    ) {
    }

    public record BulkActionResponse(
            int updatedCount,
            List<UUID> updatedTransactionIds,
            List<String> warnings
    ) {
    }
}
