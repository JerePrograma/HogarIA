package com.hogaria.service.transactionimport;

import com.hogaria.dto.TransactionImportDtos.TransactionImportCommitResponse;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class TransactionImportCommitAccumulator {

    private int created;
    private int skipped;
    private int duplicates;
    private int failed;

    private final List<UUID> createdIds = new ArrayList<>();
    private final List<String> warnings = new ArrayList<>();
    private final List<String> errors = new ArrayList<>();

    public void add(TransactionImportCommitResult result) {
        created += result.createdCount();
        skipped += result.skippedCount();
        duplicates += result.duplicateCount();
        failed += result.failedCount();

        if (result.createdTransactionId() != null) {
            createdIds.add(result.createdTransactionId());
        }
    }

    public void warning(String value) {
        warnings.add(value);
    }

    public void error(String value) {
        errors.add(value);
    }

    public int created() {
        return created;
    }

    public int skipped() {
        return skipped;
    }

    public int duplicates() {
        return duplicates;
    }

    public int failed() {
        return failed;
    }

    public List<String> warnings() {
        return warnings;
    }

    public List<String> errors() {
        return errors;
    }

    public TransactionImportCommitResponse toResponse() {
        return new TransactionImportCommitResponse(
                created,
                skipped,
                duplicates,
                failed,
                createdIds,
                warnings,
                errors
        );
    }
}