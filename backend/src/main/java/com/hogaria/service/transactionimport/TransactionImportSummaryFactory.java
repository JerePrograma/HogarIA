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

    private final TransactionImportCategoryResolver categoryResolver;

    public TransactionImportSummaryFactory(TransactionImportCategoryResolver categoryResolver) {
        this.categoryResolver = categoryResolver;
    }

    public TransactionImportPreviewResponse summarize(
            UUID batchId,
            TransactionImportSource source,
            UUID accountId,
            List<TransactionImportPreviewRow> rows
    ) {
        return summarize(batchId, source, accountId, rows, false);
    }

    public TransactionImportPreviewResponse summarize(
            UUID batchId,
            TransactionImportSource source,
            UUID accountId,
            List<TransactionImportPreviewRow> rows,
            boolean createMissingFallbackCategory
    ) {
        int duplicates = 0;
        int skipped = 0;
        int unresolved = 0;
        int importable = 0;
        int suggested = 0;
        int needsCategory = 0;
        int review = 0;
        int errors = 0;
        int internalTransfers = 0;
        int technicalNeutral = 0;
        int crossSourceRisks = 0;

        for (var row : rows) {
            if (row.suggestedCategoryId() != null) {
                suggested++;
            }

            if (row.status() == RowStatus.NEEDS_CATEGORY) {
                needsCategory++;
            }

            if (isDuplicate(row.status())) {
                duplicates++;
            } else if (row.status() == RowStatus.SKIPPED) {
                skipped++;
            } else if (row.status() == RowStatus.ERROR) {
                errors++;
            } else if (isImportable(row, createMissingFallbackCategory)) {
                importable++;
            }

            if (isBlockingCategory(row, createMissingFallbackCategory)) {
                unresolved++;
            }

            if (isReview(row)) {
                review++;
            }

            if (isInternalTransfer(row)) {
                internalTransfers++;
            }

            if (isTechnicalNeutral(row)) {
                technicalNeutral++;
            }

            if (row.status() == RowStatus.POSSIBLE_CROSS_SOURCE_DUPLICATE) {
                crossSourceRisks++;
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
                needsCategory,
                review,
                errors,
                internalTransfers,
                technicalNeutral,
                rows.size() - importable,
                unresolved,
                crossSourceRisks
        );
    }

    private boolean isDuplicate(RowStatus status) {
        return status == RowStatus.DUPLICATE
                || status == RowStatus.DUPLICATE_EXACT;
    }

    private boolean isImportable(TransactionImportPreviewRow row, boolean createMissingFallbackCategory) {
        if (row.status() == RowStatus.ERROR
                || row.status() == RowStatus.SKIPPED
                || isDuplicate(row.status())) {
            return false;
        }

        if (row.suggestedCategoryId() != null || categoryResolver.canImportWithoutCategory(row)) {
            return true;
        }

        return createMissingFallbackCategory && categoryResolver.needsFallbackCategory(row);
    }

    private boolean isBlockingCategory(TransactionImportPreviewRow row, boolean createMissingFallbackCategory) {
        if (row.suggestedCategoryId() != null
                || categoryResolver.canImportWithoutCategory(row)
                || row.status() == RowStatus.ERROR
                || row.status() == RowStatus.SKIPPED
                || isDuplicate(row.status())) {
            return false;
        }

        return !createMissingFallbackCategory || !categoryResolver.needsFallbackCategory(row);
    }

    private boolean isReview(TransactionImportPreviewRow row) {
        return row.status() == RowStatus.REVIEW
                || row.status() == RowStatus.POSSIBLE_INTERNAL_TRANSFER
                || row.status() == RowStatus.INTERNAL_TRANSFER_MATCHED
                || row.status() == RowStatus.POSSIBLE_CROSS_SOURCE_DUPLICATE
                || row.classificationStatus() == com.hogaria.entity.MoneyTransaction.ClassificationStatus.REVIEW;
    }

    private boolean isInternalTransfer(TransactionImportPreviewRow row) {
        return row.status() == RowStatus.POSSIBLE_INTERNAL_TRANSFER
                || row.status() == RowStatus.INTERNAL_TRANSFER_MATCHED
                || row.balanceImpact() == com.hogaria.entity.MoneyTransaction.BalanceImpact.INTERNAL_TRANSFER
                || row.paymentChannel() == com.hogaria.entity.MoneyTransaction.PaymentChannel.INTERNAL_TRANSFER
                || (row.classificationStatus() == com.hogaria.entity.MoneyTransaction.ClassificationStatus.TECHNICAL
                && row.movementType() == com.hogaria.entity.MoneyTransaction.MovementType.TRANSFER);
    }

    private boolean isTechnicalNeutral(TransactionImportPreviewRow row) {
        return row.classificationStatus() == com.hogaria.entity.MoneyTransaction.ClassificationStatus.TECHNICAL
                || row.balanceImpact() == com.hogaria.entity.MoneyTransaction.BalanceImpact.INTERNAL_TRANSFER
                || row.balanceImpact() == com.hogaria.entity.MoneyTransaction.BalanceImpact.NEUTRAL_ADJUSTMENT
                || row.balanceImpact() == com.hogaria.entity.MoneyTransaction.BalanceImpact.TECHNICAL;
    }
}
