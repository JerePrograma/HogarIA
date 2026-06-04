package com.hogaria.service.transactionimport;

import java.util.UUID;

public record TransactionImportCommitResult(
        int createdCount,
        int skippedCount,
        int duplicateCount,
        int failedCount,
        UUID createdTransactionId
) {
    public static TransactionImportCommitResult created(UUID id) {
        return new TransactionImportCommitResult(1, 0, 0, 0, id);
    }

    public static TransactionImportCommitResult skipped() {
        return new TransactionImportCommitResult(0, 1, 0, 0, null);
    }

    public static TransactionImportCommitResult duplicate() {
        return new TransactionImportCommitResult(0, 0, 1, 0, null);
    }

    public static TransactionImportCommitResult failed() {
        return new TransactionImportCommitResult(0, 0, 0, 1, null);
    }
}