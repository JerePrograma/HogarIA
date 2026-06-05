package com.hogaria.service.transactionimport;

import static org.junit.jupiter.api.Assertions.*;

import com.hogaria.dto.TransactionImportDtos.RowStatus;
import com.hogaria.dto.TransactionImportDtos.TransactionImportPreviewRow;
import com.hogaria.dto.TransactionImportDtos.TransactionImportSource;
import com.hogaria.entity.MoneyTransaction;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ImportInternalTransferMatcherTest {

    private final ImportInternalTransferMatcher matcher = new ImportInternalTransferMatcher();

    @Test
    void confirmsOnlyOwnDebinWithExactOperationDateAndAmount() {
        var debit = row(
                TransactionImportSource.BANCO_PROVINCIA,
                "-400000.00",
                LocalDate.of(2026, 5, 7),
                LocalDate.of(2026, 5, 6),
                "DEBITO DEBIN",
                "DB.DEBIN 06/05-S.123 C:20384307400",
                MoneyTransaction.PaymentChannel.DEBIN,
                RowStatus.REVIEW
        );
        var credit = row(
                TransactionImportSource.MERCADO_PAGO,
                "400000.00",
                LocalDate.of(2026, 5, 6),
                LocalDate.of(2026, 5, 6),
                "Pago Debin",
                "",
                MoneyTransaction.PaymentChannel.BANK_TRANSFER,
                RowStatus.REVIEW
        );

        var result = matcher.match(snapshots(debit, credit), Set.of("20384307400"));

        assertEquals(1, result.matches().size());
        assertTrue(result.matches().getFirst().strong());
        assertTrue(result.rows().stream().allMatch(row -> row.row().status() == RowStatus.INTERNAL_TRANSFER_MATCHED));
        assertTrue(result.rows().stream().allMatch(row -> row.row().balanceImpact() == MoneyTransaction.BalanceImpact.INTERNAL_TRANSFER));
        assertEquals(MoneyTransaction.PaymentChannel.DEBIN, result.rows().get(0).row().paymentChannel());
        assertEquals(MoneyTransaction.PaymentChannel.BANK_TRANSFER, result.rows().get(1).row().paymentChannel());
    }

    @Test
    void sameAmountAndNearbyDateWithoutOwnAliasStaysPossible() {
        var debit = row(
                TransactionImportSource.BANCO_PROVINCIA,
                "-10000.00",
                LocalDate.of(2026, 5, 7),
                LocalDate.of(2026, 5, 7),
                "DEBITO DEBIN",
                "DB.DEBIN 07/05-S.123 C:27999999999",
                MoneyTransaction.PaymentChannel.DEBIN,
                RowStatus.REVIEW
        );
        var credit = row(
                TransactionImportSource.MERCADO_PAGO,
                "10000.00",
                LocalDate.of(2026, 5, 8),
                LocalDate.of(2026, 5, 8),
                "Bank Transfer",
                "",
                MoneyTransaction.PaymentChannel.BANK_TRANSFER,
                RowStatus.REVIEW
        );

        var result = matcher.match(snapshots(debit, credit), Set.of("20384307400"));

        assertEquals(1, result.matches().size());
        assertFalse(result.matches().getFirst().strong());
        assertTrue(result.rows().stream().allMatch(row -> row.row().status() == RowStatus.POSSIBLE_INTERNAL_TRANSFER));
        assertTrue(result.rows().stream().noneMatch(row -> row.row().balanceImpact() == MoneyTransaction.BalanceImpact.INTERNAL_TRANSFER));
    }

    @Test
    void ownIdOutsideDebinCounterpartyFieldDoesNotCreateStrongMatch() {
        var debit = row(
                TransactionImportSource.BANCO_PROVINCIA,
                "-10000.00",
                LocalDate.of(2026, 5, 7),
                LocalDate.of(2026, 5, 7),
                "DEBITO DEBIN",
                "DB.DEBIN 07/05-S.123 REF:20384307400 C:27999999999",
                MoneyTransaction.PaymentChannel.DEBIN,
                RowStatus.REVIEW
        );
        var credit = row(
                TransactionImportSource.MERCADO_PAGO,
                "10000.00",
                LocalDate.of(2026, 5, 7),
                LocalDate.of(2026, 5, 7),
                "Bank Transfer",
                "",
                MoneyTransaction.PaymentChannel.BANK_TRANSFER,
                RowStatus.REVIEW
        );

        var result = matcher.match(snapshots(debit, credit), Set.of("20384307400"));

        assertEquals(1, result.matches().size());
        assertFalse(result.matches().getFirst().strong());
        assertTrue(result.rows().stream().allMatch(row -> row.row().status() == RowStatus.POSSIBLE_INTERNAL_TRANSFER));
    }

    @Test
    void duplicateRowsAreNotUsedAsTransferLegs() {
        var duplicateDebit = row(
                TransactionImportSource.BANCO_PROVINCIA,
                "-10000.00",
                LocalDate.of(2026, 5, 7),
                LocalDate.of(2026, 5, 7),
                "DEBITO DEBIN",
                "DB.DEBIN 07/05-S.123 C:20384307400",
                MoneyTransaction.PaymentChannel.DEBIN,
                RowStatus.DUPLICATE_EXACT
        );
        var credit = row(
                TransactionImportSource.MERCADO_PAGO,
                "10000.00",
                LocalDate.of(2026, 5, 7),
                LocalDate.of(2026, 5, 7),
                "Pago Debin",
                "",
                MoneyTransaction.PaymentChannel.DEBIN,
                RowStatus.REVIEW
        );

        var result = matcher.match(snapshots(duplicateDebit, credit), Set.of("20384307400"));

        assertTrue(result.matches().isEmpty());
        assertEquals(RowStatus.DUPLICATE_EXACT, result.rows().getFirst().row().status());
    }

    private List<ImportInternalTransferMatcher.ImportRowSnapshot> snapshots(TransactionImportPreviewRow... rows) {
        return java.util.Arrays.stream(rows)
                .map(row -> new ImportInternalTransferMatcher.ImportRowSnapshot(UUID.randomUUID(), UUID.randomUUID(), row))
                .toList();
    }

    private TransactionImportPreviewRow row(
            TransactionImportSource source,
            String signedAmount,
            LocalDate realDate,
            LocalDate operationDate,
            String description,
            String extendedDescription,
            MoneyTransaction.PaymentChannel channel,
            RowStatus status
    ) {
        var signed = new BigDecimal(signedAmount);
        var movementType = signed.signum() < 0
                ? MoneyTransaction.MovementType.EXPENSE
                : MoneyTransaction.MovementType.INCOME;
        var base = TransactionImportTestData.row(
                1,
                status,
                movementType,
                MoneyTransaction.BalanceImpact.UNKNOWN,
                MoneyTransaction.ClassificationStatus.REVIEW,
                null
        );
        return new TransactionImportPreviewRow(
                base.rowNumber(),
                source,
                UUID.randomUUID().toString(),
                UUID.randomUUID().toString(),
                realDate,
                realDate,
                description,
                description,
                signed,
                signed.abs(),
                "ARS",
                movementType,
                null,
                null,
                base.confidence(),
                status,
                null,
                "{}",
                null,
                null,
                null,
                null,
                null,
                null,
                base.detectedFormat(),
                operationDate.atStartOfDay(),
                MoneyTransaction.OperationDateTimePrecision.DATE_ONLY,
                extendedDescription,
                null,
                null,
                null,
                channel,
                MoneyTransaction.BalanceImpact.UNKNOWN,
                MoneyTransaction.ClassificationStatus.REVIEW,
                "TEST",
                null,
                null,
                null,
                null,
                null,
                null,
                base.sheetName(),
                base.targetEntity(),
                "{}"
        );
    }
}
