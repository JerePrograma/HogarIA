package com.hogaria.domains.transactions.lifecycle;

public record TransactionDeletionDecision(
        Mode mode,
        String reason,
        String code
) {

    public enum Mode {
        PHYSICAL_DELETE,
        SOFT_IGNORE
    }
}
