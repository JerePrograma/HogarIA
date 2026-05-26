package com.hogaria.domains.transactions.lifecycle;

import com.hogaria.entity.MoneyTransaction;
import org.springframework.stereotype.Component;

@Component
class NoopTransactionDeletionObserver implements TransactionDeletionObserver {

    @Override
    public void beforeTransactionRemoval(
            MoneyTransaction transaction,
            TransactionDeletionDecision decision,
            MonthlyPlanTransactionUnlinkResult unlinkResult
    ) {
        // Extension point for future audit/metrics without coupling observers to the use case.
    }
}
