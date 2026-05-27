package com.hogaria.domains.transactions.lifecycle;

public record TransactionDeletionDecision(
        DeletionMode mode,
        LinkHandling linkHandling,
        String reason,
        String code
) {

    public enum DeletionMode {
        PHYSICAL_DELETE,
        SOFT_IGNORE
    }

    public enum LinkHandling {
        UNLINK_MONTHLY_PLAN,
        KEEP_LINKS,
        BLOCK_IF_LINKED
    }
}
