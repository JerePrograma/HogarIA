package com.hogaria.service.transactionimport;

import com.hogaria.dto.TransactionImportDtos.Confidence;
import com.hogaria.dto.TransactionImportDtos.RowStatus;
import com.hogaria.dto.TransactionImportDtos.TransactionImportPreviewRow;
import com.hogaria.dto.TransactionImportDtos.TransactionImportSource;
import com.hogaria.entity.ImportTargetEntity;
import com.hogaria.entity.MoneyTransaction;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

final class TransactionImportTestData {

    private TransactionImportTestData() {
    }

    static TransactionImportPreviewRow row(
            int rowNumber,
            RowStatus status,
            MoneyTransaction.MovementType movementType,
            MoneyTransaction.BalanceImpact balanceImpact,
            MoneyTransaction.ClassificationStatus classificationStatus,
            UUID categoryId
    ) {
        var date = LocalDate.of(2026, 5, Math.min(rowNumber, 28));

        return new TransactionImportPreviewRow(
                rowNumber,
                TransactionImportSource.MERCADO_PAGO,
                "op-" + rowNumber,
                "hash-" + rowNumber,
                date,
                date,
                "Movimiento " + rowNumber,
                "movimiento " + rowNumber,
                new BigDecimal("-100.00"),
                new BigDecimal("100.00"),
                "ARS",
                movementType,
                categoryId,
                categoryId == null ? null : "Categoría",
                Confidence.LOW,
                status,
                null,
                "{}",
                null,
                null,
                null,
                null,
                null,
                null,
                "MERCADO_PAGO_SETTLEMENT",
                date.atStartOfDay(),
                MoneyTransaction.OperationDateTimePrecision.DATE_ONLY,
                null,
                null,
                null,
                null,
                MoneyTransaction.PaymentChannel.MERCADO_PAGO,
                balanceImpact,
                classificationStatus,
                "TEST",
                null,
                null,
                null,
                null,
                null,
                null,
                "sheet0",
                ImportTargetEntity.UNKNOWN,
                "{}"
        );
    }
}
