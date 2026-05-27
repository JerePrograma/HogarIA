package com.hogaria.service;

import com.hogaria.entity.MoneyTransaction;
import com.hogaria.exception.DomainConflictException;
import com.hogaria.exception.ErrorResponse;
import com.hogaria.repository.MoneyTransactionRepository;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class DuplicateDetectionService {

    private final MoneyTransactionRepository repository;

    public DuplicateDetectionService(MoneyTransactionRepository repository) {
        this.repository = repository;
    }

    public List<MoneyTransaction> findExactDuplicates(MoneyTransaction transaction, UUID excludeId) {
        if (transaction.getDuplicateFingerprint() == null || transaction.getDuplicateFingerprint().isBlank()) {
            return List.of();
        }

        return repository.findActiveDuplicatesByFingerprint(
                transaction.getProfileId(),
                transaction.getAccountId(),
                transaction.getDuplicateFingerprint(),
                MoneyTransaction.Status.IGNORED,
                MoneyTransaction.ClassificationStatus.IGNORED_BY_RULE,
                excludeId
        );
    }

    public List<MoneyTransaction> findStrongSourceDuplicates(MoneyTransaction transaction, UUID excludeId) {
        if (transaction.getSourceHash() != null && !transaction.getSourceHash().isBlank()) {
            return repository
                    .findByProfileIdAndSourceHash(transaction.getProfileId(), transaction.getSourceHash())
                    .filter(existing -> excludeId == null || !existing.getId().equals(excludeId))
                    .map(List::of)
                    .orElseGet(List::of);
        }

        if (transaction.getSource() != null
                && !transaction.getSource().isBlank()
                && transaction.getSourceOperationId() != null
                && !transaction.getSourceOperationId().isBlank()) {
            return repository.findByStrongSourceOperation(
                    transaction.getProfileId(),
                    transaction.getSource().trim().toUpperCase(Locale.ROOT),
                    transaction.getSourceOperationId().trim(),
                    excludeId
            );
        }

        return List.of();
    }

    public void rejectIfDuplicate(MoneyTransaction transaction, UUID excludeId) {
        var sourceDuplicates = findStrongSourceDuplicates(transaction, excludeId);
        if (!sourceDuplicates.isEmpty()) {
            throw conflict(
                    "El movimiento ya fue importado por la misma clave de origen.",
                    "TRANSACTION_SOURCE_DUPLICATE",
                    sourceDuplicates.get(0)
            );
        }

        var exactDuplicates = findExactDuplicates(transaction, excludeId);
        if (!exactDuplicates.isEmpty()) {
            throw conflict(
                    "Ya existe un movimiento activo con la misma cuenta, fecha, descripción normalizada, monto y moneda.",
                    "TRANSACTION_EXACT_DUPLICATE",
                    exactDuplicates.get(0)
            );
        }
    }

    private DomainConflictException conflict(String message, String code, MoneyTransaction existing) {
        return new DomainConflictException(
                message,
                code,
                List.of(
                        new ErrorResponse.Detail("existingTransactionId", existing.getId().toString()),
                        new ErrorResponse.Detail("duplicateFingerprint", existing.getDuplicateFingerprint())
                )
        );
    }
}
