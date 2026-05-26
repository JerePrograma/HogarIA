package com.hogaria.domains.transactions.lifecycle;

import com.hogaria.entity.MoneyTransaction;

public interface TransactionDeletionObserver {

    void beforeTransactionRemoval(
            MoneyTransaction transaction,
            TransactionDeletionDecision decision,
            MonthlyPlanTransactionUnlinkResult unlinkResult
    );
}
