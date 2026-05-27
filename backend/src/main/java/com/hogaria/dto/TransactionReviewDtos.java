package com.hogaria.dto;

import com.hogaria.entity.MoneyTransaction;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public class TransactionReviewDtos {

    public record TransactionReviewItem(
            UUID id,
            UUID accountId,
            UUID categoryId,
            MoneyTransaction.MovementType movementType,
            LocalDate realDate,
            LocalDateTime operationDateTime,
            BigDecimal amount,
            String currency,
            String description,
            String normalizedDescription,
            String source,
            String sourceOperationId,
            String sourceHash,
            String duplicateFingerprint,
            MoneyTransaction.Status status,
            MoneyTransaction.ClassificationStatus classificationStatus,
            String classificationReason,
            UUID internalTransferGroupId,
            MoneyTransaction.BalanceImpact balanceImpact
    ) {
    }

    public record DuplicatePreviewRequest(Integer year, Integer month) {
    }

    public record DuplicateGroup(
            String groupType,
            String key,
            BigDecimal amount,
            String currency,
            List<TransactionReviewItem> transactions
    ) {
    }

    public record DuplicatePreviewResponse(
            int exactDuplicateGroups,
            int possibleCrossSourceGroups,
            List<DuplicateGroup> groups
    ) {
    }

    public record DuplicateResolveRequest(
            UUID keepTransactionId,
            List<UUID> duplicateTransactionIds,
            String note
    ) {
    }

    public record DuplicateResolveResponse(
            UUID keepTransactionId,
            int ignoredCount,
            List<UUID> ignoredTransactionIds
    ) {
    }

    public record InternalTransferPreviewRequest(Integer year, Integer month, Integer toleranceDays) {
    }

    public record InternalTransferCandidate(
            TransactionReviewItem debitLeg,
            TransactionReviewItem creditLeg,
            BigDecimal amountDifference,
            long dayDistance,
            String reason
    ) {
    }

    public record InternalTransferPreviewResponse(
            int candidateCount,
            List<InternalTransferCandidate> candidates
    ) {
    }

    public record InternalTransferLinkRequest(
            UUID debitTransactionId,
            UUID creditTransactionId,
            BigDecimal toleranceAmount,
            Integer toleranceDays
    ) {
    }

    public record InternalTransferLinkResponse(
            UUID internalTransferGroupId,
            TransactionReviewItem debitLeg,
            TransactionReviewItem creditLeg
    ) {
    }

    public record InternalTransferUnlinkRequest(UUID internalTransferGroupId, List<UUID> transactionIds) {
    }

    public record InternalTransferUnlinkResponse(int unlinkedCount, List<UUID> transactionIds) {
    }
}
