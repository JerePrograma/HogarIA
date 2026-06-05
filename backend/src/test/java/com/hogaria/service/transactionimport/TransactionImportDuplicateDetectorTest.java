package com.hogaria.service.transactionimport;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import com.hogaria.dto.TransactionImportDtos.RowStatus;
import com.hogaria.dto.TransactionImportDtos.TransactionImportSource;
import com.hogaria.entity.MoneyTransaction;
import com.hogaria.repository.CategoryRepository;
import com.hogaria.repository.MoneyTransactionRepository;
import com.hogaria.repository.TransactionImportReferenceRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class TransactionImportDuplicateDetectorTest {

    private MoneyTransactionRepository transactions;
    private TransactionImportDuplicateDetector detector;
    private UUID profileId;
    private UUID accountId;

    @BeforeEach
    void setup() {
        transactions = org.mockito.Mockito.mock(MoneyTransactionRepository.class);
        detector = new TransactionImportDuplicateDetector(
                transactions,
                org.mockito.Mockito.mock(TransactionImportReferenceRepository.class),
                org.mockito.Mockito.mock(CategoryRepository.class)
        );
        profileId = UUID.randomUUID();
        accountId = UUID.randomUUID();
    }

    @Test
    void sourceHashAndSourceOperationIdAreRealDuplicates() {
        var existing = tx(accountId, TransactionImportSource.MERCADO_PAGO, "Pago", MoneyTransaction.MovementType.EXPENSE);
        when(transactions.findActiveByProfileIdAndSourceHash(eq(profileId), eq("hash-1"), any(), any()))
                .thenReturn(List.of(existing));

        var hashMatch = detector.findImportMatch(profileId, accountId, row(
                TransactionImportSource.MERCADO_PAGO,
                "op-1",
                "hash-1",
                "Pago",
                MoneyTransaction.MovementType.EXPENSE
        ));

        assertTrue(hashMatch.type().isDuplicate());

        when(transactions.findActiveByProfileIdAndSourceHash(eq(profileId), eq("hash-2"), any(), any()))
                .thenReturn(List.of());
        when(transactions.findActiveByStrongSourceOperation(
                eq(profileId), eq("MERCADO_PAGO"), eq("op-2"), any(), any(), any()
        )).thenReturn(List.of(existing));

        var operationMatch = detector.findImportMatch(profileId, accountId, row(
                TransactionImportSource.MERCADO_PAGO,
                "op-2",
                "hash-2",
                "Pago",
                MoneyTransaction.MovementType.EXPENSE
        ));

        assertTrue(operationMatch.type().isDuplicate());
    }

    @Test
    void debinOrBankTransferAloneIsNotDuplicate() {
        when(transactions.findByProfileIdAndRealDateBetweenAndAmount(any(), any(), any(), any()))
                .thenReturn(List.of());

        var match = detector.findImportMatch(profileId, accountId, row(
                TransactionImportSource.BANCO_PROVINCIA,
                null,
                null,
                "DEBITO DEBIN BANK TRANSFER",
                MoneyTransaction.MovementType.EXPENSE
        ));

        assertFalse(match.found());
    }

    @Test
    void reusedSourceOperationWithDifferentAmountIsNotDuplicate() {
        var existing = tx(accountId, TransactionImportSource.MERCADO_PAGO, "Rendimiento", MoneyTransaction.MovementType.INCOME);
        existing.setSourceOperationId("shared-op");
        existing.setAmount(new BigDecimal("0.60"));
        when(transactions.findActiveByStrongSourceOperation(
                eq(profileId), eq("MERCADO_PAGO"), eq("shared-op"), any(), any(), any()
        )).thenReturn(List.of(existing));
        when(transactions.findByProfileIdAndRealDateBetweenAndAmount(any(), any(), any(), any()))
                .thenReturn(List.of());

        var match = detector.findImportMatch(profileId, accountId, row(
                TransactionImportSource.MERCADO_PAGO,
                "shared-op",
                null,
                "Rendimiento",
                MoneyTransaction.MovementType.INCOME
        ));

        assertFalse(match.found());
    }

    @Test
    void crossSourceCandidateIsReviewRiskNotDuplicate() {
        var existing = tx(
                UUID.randomUUID(),
                TransactionImportSource.BANCO_PROVINCIA,
                "PAGO CON TARJETA DEBITO",
                MoneyTransaction.MovementType.EXPENSE
        );
        when(transactions.findByProfileIdAndRealDateBetweenAndAmount(any(), any(), any(), any()))
                .thenReturn(List.of(existing));

        var match = detector.findImportMatch(profileId, accountId, row(
                TransactionImportSource.MERCADO_PAGO,
                null,
                null,
                "Compra final | Tarjeta de débito Visa",
                MoneyTransaction.MovementType.EXPENSE
        ));

        assertEquals(TransactionImportMatchType.POSSIBLE_CROSS_SOURCE_DUPLICATE, match.type());
        assertFalse(match.type().isDuplicate());
    }

    @Test
    void fundingPairWithoutOwnAliasStaysPossibleAndIsNotNeutralized() {
        var existing = tx(
                UUID.randomUUID(),
                TransactionImportSource.MERCADO_PAGO,
                "Pago Debin | Bank Transfer",
                MoneyTransaction.MovementType.TRANSFER
        );
        when(transactions.findByProfileIdAndRealDateBetweenAndAmount(any(), any(), any(), any()))
                .thenReturn(List.of(existing));
        when(transactions.findById(existing.getId())).thenReturn(java.util.Optional.of(existing));

        var resolved = detector.applyDuplicateStatus(profileId, accountId, List.of(row(
                TransactionImportSource.BANCO_PROVINCIA,
                null,
                null,
                "DEBITO DEBIN",
                MoneyTransaction.MovementType.EXPENSE
        ))).get(0);

        assertEquals(RowStatus.POSSIBLE_INTERNAL_TRANSFER, resolved.status());
        assertEquals(MoneyTransaction.MovementType.EXPENSE, resolved.movementType());
        assertNotEquals(MoneyTransaction.BalanceImpact.INTERNAL_TRANSFER, resolved.balanceImpact());
        assertEquals(MoneyTransaction.ClassificationStatus.REVIEW, resolved.classificationStatus());
        assertFalse(TransactionImportMatchType.POSSIBLE_INTERNAL_TRANSFER.isDuplicate());
    }

    private com.hogaria.dto.TransactionImportDtos.TransactionImportPreviewRow row(
            TransactionImportSource source,
            String operationId,
            String sourceHash,
            String description,
            MoneyTransaction.MovementType movementType
    ) {
        var base = TransactionImportTestData.row(
                1,
                RowStatus.READY,
                movementType,
                MoneyTransaction.BalanceImpact.UNKNOWN,
                MoneyTransaction.ClassificationStatus.CLASSIFIED,
                UUID.randomUUID()
        );

        return new com.hogaria.dto.TransactionImportDtos.TransactionImportPreviewRow(
                base.rowNumber(), source, operationId, sourceHash, base.realDate(), base.budgetDate(),
                description, description, base.rawSignedAmount(), base.amount(), base.currency(), movementType,
                base.suggestedCategoryId(), base.suggestedCategoryName(), base.confidence(), base.status(),
                base.skipReason(), base.rawPayload(), null, null, null, null, null, null, base.detectedFormat(),
                base.operationDateTime(), base.operationDateTimePrecision(), base.extendedDescription(),
                base.merchantName(), base.counterparty(), base.counterpartyDocumentHash(), base.paymentChannel(),
                base.balanceImpact(), base.classificationStatus(), base.classificationReason(),
                base.classificationLayer(), base.classificationMatchedField(), base.classificationMatchedValue(),
                base.classificationExplanationJson(), base.categorySuggestionKey(), base.externalSequence(),
                base.sheetName(), base.targetEntity(), base.rawJson()
        );
    }

    private MoneyTransaction tx(
            UUID txAccountId,
            TransactionImportSource source,
            String description,
            MoneyTransaction.MovementType movementType
    ) {
        return MoneyTransaction.builder()
                .id(UUID.randomUUID())
                .profileId(profileId)
                .accountId(txAccountId)
                .source(source.name())
                .description(description)
                .normalizedDescription(description)
                .movementType(movementType)
                .realDate(LocalDate.of(2026, 5, 1))
                .amount(new BigDecimal("100.00"))
                .build();
    }
}
