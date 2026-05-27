package com.hogaria.domains.transactions.lifecycle;

import com.hogaria.entity.MoneyTransaction;
import com.hogaria.repository.ExternalSyncMappingRepository;
import java.util.Locale;
import org.springframework.stereotype.Service;

@Service
public class TransactionDeletionPolicy {

    private static final String CJPRESTAMOS = "CJPRESTAMOS";

    private final ExternalSyncMappingRepository externalSyncMappingRepository;

    public TransactionDeletionPolicy(ExternalSyncMappingRepository externalSyncMappingRepository) {
        this.externalSyncMappingRepository = externalSyncMappingRepository;
    }

    public TransactionDeletionDecision decide(MoneyTransaction transaction) {
        if (CJPRESTAMOS.equals(normalize(transaction.getSource()))) {
            return softIgnore(
                    TransactionDeletionDecision.LinkHandling.UNLINK_MONTHLY_PLAN,
                    "Los movimientos de cjprestamos se ignoran para preservar idempotencia externa.",
                    "EXTERNAL_LOAN_TRANSACTION_SOFT_IGNORED"
            );
        }

        if (externalSyncMappingRepository.existsByProfileIdAndMoneyTransactionId(
                transaction.getProfileId(),
                transaction.getId()
        )) {
            return softIgnore(
                    TransactionDeletionDecision.LinkHandling.UNLINK_MONTHLY_PLAN,
                    "El movimiento sostiene idempotencia de una sincronización externa.",
                    "TRANSACTION_EXTERNAL_SYNC_SOFT_IGNORED"
            );
        }

        if (transaction.getOrigin() == MoneyTransaction.Origin.IMPORT) {
            return softIgnore(
                    TransactionDeletionDecision.LinkHandling.UNLINK_MONTHLY_PLAN,
                    "Los movimientos importados se ignoran para preservar trazabilidad.",
                    "IMPORTED_TRANSACTION_SOFT_IGNORED"
            );
        }

        return new TransactionDeletionDecision(
                TransactionDeletionDecision.DeletionMode.PHYSICAL_DELETE,
                TransactionDeletionDecision.LinkHandling.UNLINK_MONTHLY_PLAN,
                "Movimiento sin restricciones de trazabilidad externa.",
                "TRANSACTION_PHYSICALLY_DELETED"
        );
    }

    private TransactionDeletionDecision softIgnore(
            TransactionDeletionDecision.LinkHandling linkHandling,
            String reason,
            String code
    ) {
        return new TransactionDeletionDecision(
                TransactionDeletionDecision.DeletionMode.SOFT_IGNORE,
                linkHandling,
                reason,
                code
        );
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().toUpperCase(Locale.ROOT);
    }
}
