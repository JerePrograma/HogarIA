package com.hogaria.service.transactionimport;

import java.util.UUID;

public record TransactionImportMatch(
        TransactionImportMatchType type,
        UUID matchedTransactionId,
        String reason
) {
    public static TransactionImportMatch none() {
        return new TransactionImportMatch(TransactionImportMatchType.NONE, null, null);
    }

    public boolean found() {
        return type != null && type != TransactionImportMatchType.NONE;
    }
}