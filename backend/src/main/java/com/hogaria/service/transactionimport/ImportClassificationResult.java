package com.hogaria.service.transactionimport;

import com.hogaria.dto.TransactionImportDtos.Confidence;
import com.hogaria.dto.TransactionImportDtos.RowStatus;
import com.hogaria.entity.MoneyTransaction;

public record ImportClassificationResult(
        MoneyTransaction.MovementType movementType,
        MoneyTransaction.BalanceImpact balanceImpact,
        MoneyTransaction.PaymentChannel paymentChannel,
        String categorySuggestionKey,
        String categorySuggestionName,
        MoneyTransaction.ClassificationStatus classificationStatus,
        String classificationReason,
        Confidence confidence,
        RowStatus rowStatus,
        String warning
) {
}
