package com.hogaria.service.transactionimport;

public enum TransactionImportMatchType {
    NONE,
    EXACT_DUPLICATE,
    SOURCE_DUPLICATE,
    STRONG_SAME_ACCOUNT_DUPLICATE,
    POSSIBLE_INTERNAL_TRANSFER,
    INTERNAL_TRANSFER_MATCHED,
    POSSIBLE_CROSS_SOURCE_DUPLICATE;

    public boolean isDuplicate() {
        return this == EXACT_DUPLICATE
                || this == SOURCE_DUPLICATE
                || this == STRONG_SAME_ACCOUNT_DUPLICATE;
    }

    public boolean isReviewOnlyRisk() {
        return this == POSSIBLE_INTERNAL_TRANSFER
                || this == INTERNAL_TRANSFER_MATCHED
                || this == POSSIBLE_CROSS_SOURCE_DUPLICATE;
    }
}