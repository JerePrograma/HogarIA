package com.hogaria.domains.transactions.lifecycle;

public record MonthlyPlanTransactionUnlinkResult(
        int linkedItemsUpdated,
        int matchesDeleted,
        int systemConversionMatchesDeleted
) {
}
