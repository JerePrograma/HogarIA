package com.hogaria.service.transactionimport;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;

import com.hogaria.dto.TransactionImportDtos.RowStatus;
import com.hogaria.dto.TransactionImportDtos.TransactionImportSource;
import com.hogaria.entity.MoneyTransaction;
import com.hogaria.repository.CategoryRepository;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class TransactionImportSummaryFactoryTest {

    private TransactionImportSummaryFactory factory;

    @BeforeEach
    void setup() {
        factory = new TransactionImportSummaryFactory(
                new TransactionImportCategoryResolver(mock(CategoryRepository.class))
        );
    }

    @Test
    void countsOnlyDuplicateAndDuplicateExactAsDuplicates() {
        var summary = summarize(List.of(
                row(1, RowStatus.DUPLICATE),
                row(2, RowStatus.DUPLICATE_EXACT),
                row(3, RowStatus.POSSIBLE_INTERNAL_TRANSFER),
                row(4, RowStatus.INTERNAL_TRANSFER_MATCHED),
                row(5, RowStatus.POSSIBLE_CROSS_SOURCE_DUPLICATE)
        ), false);

        assertEquals(2, summary.duplicateRows());
        assertEquals(3, summary.reviewRows());
        assertEquals(1, summary.crossSourceRiskRows());
    }

    @Test
    void technicalInternalTransferWithoutCategoryIsImportableAndNotBlocking() {
        var technical = TransactionImportTestData.row(
                1,
                RowStatus.REVIEW,
                MoneyTransaction.MovementType.TRANSFER,
                MoneyTransaction.BalanceImpact.INTERNAL_TRANSFER,
                MoneyTransaction.ClassificationStatus.TECHNICAL,
                null
        );

        var summary = summarize(List.of(technical), false);

        assertEquals(1, summary.importableRows());
        assertEquals(1, summary.internalTransferRows());
        assertEquals(1, summary.technicalNeutralRows());
        assertEquals(0, summary.blockingCategoryRows());
    }

    @Test
    void neutralReviewWithoutCategoryDoesNotBlock() {
        var neutral = TransactionImportTestData.row(
                1,
                RowStatus.REVIEW,
                MoneyTransaction.MovementType.ADJUSTMENT,
                MoneyTransaction.BalanceImpact.NEUTRAL_ADJUSTMENT,
                MoneyTransaction.ClassificationStatus.REVIEW,
                null
        );

        var summary = summarize(List.of(neutral), false);

        assertEquals(1, summary.importableRows());
        assertEquals(0, summary.blockingCategoryRows());
    }

    @Test
    void needsCategoryBlocksWithoutFallbackAndUnblocksWithFallback() {
        var needsCategory = row(1, RowStatus.NEEDS_CATEGORY);

        var blocked = summarize(List.of(needsCategory), false);
        var released = summarize(List.of(needsCategory), true);

        assertEquals(0, blocked.importableRows());
        assertEquals(1, blocked.blockingCategoryRows());
        assertEquals(1, released.importableRows());
        assertEquals(0, released.blockingCategoryRows());
    }

    private com.hogaria.dto.TransactionImportDtos.TransactionImportPreviewResponse summarize(
            List<com.hogaria.dto.TransactionImportDtos.TransactionImportPreviewRow> rows,
            boolean fallback
    ) {
        return factory.summarize(
                UUID.randomUUID(),
                TransactionImportSource.MERCADO_PAGO,
                UUID.randomUUID(),
                rows,
                fallback
        );
    }

    private com.hogaria.dto.TransactionImportDtos.TransactionImportPreviewRow row(int number, RowStatus status) {
        return TransactionImportTestData.row(
                number,
                status,
                MoneyTransaction.MovementType.EXPENSE,
                MoneyTransaction.BalanceImpact.UNKNOWN,
                MoneyTransaction.ClassificationStatus.NEEDS_CATEGORY,
                null
        );
    }
}
