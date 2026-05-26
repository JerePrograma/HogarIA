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
        if (externalSyncMappingRepository.existsByProfileIdAndMoneyTransactionId(
                transaction.getProfileId(),
                transaction.getId()
        )) {
            return softIgnore(
                    "El movimiento sostiene idempotencia de una sincronización externa.",
                    "TRANSACTION_EXTERNAL_SYNC_SOFT_IGNORED"
            );
        }

        if (transaction.getOrigin() == MoneyTransaction.Origin.IMPORT) {
            return softIgnore(
                    "Los movimientos importados se ignoran para preservar trazabilidad.",
                    "IMPORTED_TRANSACTION_SOFT_IGNORED"
            );
        }

        if (CJPRESTAMOS.equals(normalize(transaction.getSource()))) {
            return softIgnore(
                    "Los movimientos de cjprestamos se ignoran para preservar idempotencia externa.",
                    "EXTERNAL_LOAN_TRANSACTION_SOFT_IGNORED"
            );
        }

        return new TransactionDeletionDecision(
                TransactionDeletionDecision.Mode.PHYSICAL_DELETE,
                "Movimiento sin restricciones de trazabilidad externa.",
                "TRANSACTION_PHYSICALLY_DELETED"
        );
    }

    private TransactionDeletionDecision softIgnore(String reason, String code) {
        return new TransactionDeletionDecision(
                TransactionDeletionDecision.Mode.SOFT_IGNORE,
                reason,
                code
        );
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().toUpperCase(Locale.ROOT);
    }
}
