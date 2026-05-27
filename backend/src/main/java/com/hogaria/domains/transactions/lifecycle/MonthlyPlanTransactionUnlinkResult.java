package com.hogaria.domains.transactions.lifecycle;

public record MonthlyPlanTransactionUnlinkResult(
        int linkedItemsUpdated,
        int matchesDeleted,
        int systemConversionMatchesDeleted
) {

    public boolean hasLinks() {
        return linkedItemsUpdated > 0 || matchesDeleted > 0;
    }
}
