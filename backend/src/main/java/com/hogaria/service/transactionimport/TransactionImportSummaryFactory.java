package com.hogaria.service.transactionimport;

import com.hogaria.dto.TransactionImportDtos.RowStatus;
import com.hogaria.dto.TransactionImportDtos.TransactionImportPreviewResponse;
import com.hogaria.dto.TransactionImportDtos.TransactionImportPreviewRow;
import com.hogaria.dto.TransactionImportDtos.TransactionImportSource;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class TransactionImportSummaryFactory {

    public TransactionImportPreviewResponse summarize(
            UUID batchId,
            TransactionImportSource source,
            UUID accountId,
            List<TransactionImportPreviewRow> rows
    ) {
        int duplicates = 0;
        int skipped = 0;
        int unresolved = 0;
        int importable = 0;
        int suggested = 0;
        int review = 0;
        int errors = 0;

        for (var row : rows) {
            if (row.suggestedCategoryId() != null) {
                suggested++;
            }

            if (isDuplicateLike(row.status())) {
                duplicates++;
            } else if (row.status() == RowStatus.SKIPPED) {
                skipped++;
            } else if (row.status() == RowStatus.ERROR) {
                errors++;
            } else if (row.status() == RowStatus.NEEDS_CATEGORY) {
                unresolved++;
            } else if (row.status() == RowStatus.REVIEW) {
                review++;

                if (row.suggestedCategoryId() == null) {
                    unresolved++;
                } else {
                    importable++;
                }
            } else if (row.status() == RowStatus.READY) {
                importable++;
            }
        }

        var detectedFormat = rows
                .stream()
                .map(TransactionImportPreviewRow::detectedFormat)
                .filter(Objects::nonNull)
                .findFirst()
                .orElse(null);

        return new TransactionImportPreviewResponse(
                batchId,
                source,
                accountId,
                rows.size(),
                importable,
                duplicates,
                skipped,
                unresolved,
                rows,
                List.of(),
                List.of(),
                detectedFormat,
                suggested,
                unresolved,
                review,
                errors
        );
    }

    private boolean isDuplicateLike(RowStatus status) {
        return status == RowStatus.DUPLICATE
                || status == RowStatus.DUPLICATE_EXACT
                || status == RowStatus.POSSIBLE_INTERNAL_TRANSFER
                || status == RowStatus.INTERNAL_TRANSFER_MATCHED
                || status == RowStatus.POSSIBLE_CROSS_SOURCE_DUPLICATE;
    }
}