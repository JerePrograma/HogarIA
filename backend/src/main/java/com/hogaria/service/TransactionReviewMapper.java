package com.hogaria.service;

import com.hogaria.dto.TransactionReviewDtos.TransactionReviewItem;
import com.hogaria.entity.MoneyTransaction;
import org.springframework.stereotype.Component;

@Component
public class TransactionReviewMapper {

    public TransactionReviewItem toItem(MoneyTransaction transaction) {
        return new TransactionReviewItem(
                transaction.getId(),
                transaction.getAccountId(),
                transaction.getCategoryId(),
                transaction.getMovementType(),
                transaction.getRealDate(),
                transaction.getOperationDateTime(),
                transaction.getAmount(),
                transaction.getCurrency(),
                transaction.getDescription(),
                transaction.getNormalizedDescription(),
                transaction.getSource(),
                transaction.getSourceOperationId(),
                transaction.getSourceHash(),
                transaction.getDuplicateFingerprint(),
                transaction.getStatus(),
                transaction.getClassificationStatus(),
                transaction.getClassificationReason(),
                transaction.getInternalTransferGroupId(),
                transaction.getBalanceImpact()
        );
    }
}
