package com.hogaria.dto;

import com.hogaria.entity.MoneyTransaction;
import java.util.UUID;

public record TransactionDeletionResponse(
        UUID transactionId,
        Mode mode,
        String code,
        String message,
        Integer linkedItemsUpdated,
        Integer matchesDeleted,
        Integer systemConversionMatchesDeleted,
        MoneyTransaction.Status resultingStatus,
        MoneyTransaction.ClassificationStatus resultingClassificationStatus
) {

    public enum Mode {
        PHYSICAL_DELETE,
        SOFT_IGNORE
    }
}
