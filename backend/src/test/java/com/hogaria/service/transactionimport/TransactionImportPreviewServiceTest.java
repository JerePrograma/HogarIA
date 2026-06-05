package com.hogaria.service.transactionimport;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.hogaria.dto.TransactionImportDtos.RowStatus;
import com.hogaria.dto.TransactionImportDtos.TransactionImportPreviewResponse;
import com.hogaria.dto.TransactionImportDtos.TransactionImportSource;
import com.hogaria.entity.ExcelImportBatch;
import com.hogaria.entity.FinancialProfile;
import com.hogaria.entity.MoneyTransaction;
import com.hogaria.repository.AccountRepository;
import com.hogaria.repository.ExcelImportBatchRepository;
import com.hogaria.repository.FinancialProfileRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

class TransactionImportPreviewServiceTest {

    @Test
    void storesPreviewBatchWithValidatedProfileAccountAndDetectedSource() {
        var profiles = org.mockito.Mockito.mock(FinancialProfileRepository.class);
        var accounts = org.mockito.Mockito.mock(AccountRepository.class);
        var parser = org.mockito.Mockito.mock(TransactionImportParserRouter.class);
        var duplicates = org.mockito.Mockito.mock(TransactionImportDuplicateDetector.class);
        var batchStore = org.mockito.Mockito.mock(TransactionImportBatchStore.class);
        var summaryFactory = org.mockito.Mockito.mock(TransactionImportSummaryFactory.class);
        var userId = UUID.randomUUID();
        var profileId = UUID.randomUUID();
        var accountId = UUID.randomUUID();
        var batchId = UUID.randomUUID();
        var row = TransactionImportTestData.row(
                1, RowStatus.READY, MoneyTransaction.MovementType.EXPENSE,
                MoneyTransaction.BalanceImpact.CONSUMPTION_EXPENSE,
                MoneyTransaction.ClassificationStatus.CLASSIFIED, UUID.randomUUID()
        );
        var response = org.mockito.Mockito.mock(TransactionImportPreviewResponse.class);
        when(profiles.findByIdAndUserId(profileId, userId))
                .thenReturn(Optional.of(FinancialProfile.builder().id(profileId).userId(userId).build()));
        when(accounts.existsByIdAndProfileId(accountId, profileId)).thenReturn(true);
        when(parser.parse(eq(TransactionImportSource.AUTO), any(), eq(profileId), eq(accountId), eq(2026), eq(5)))
                .thenReturn(List.of(row));
        when(duplicates.applyDuplicateStatus(profileId, accountId, List.of(row))).thenReturn(List.of(row));
        when(batchStore.createPreviewBatch(
                eq(profileId), eq(accountId), eq(TransactionImportSource.MERCADO_PAGO),
                eq("movimientos.xlsx"), eq("ARS"), eq(2026), eq(5), any()
        )).thenReturn(ExcelImportBatch.builder().id(batchId).profileId(profileId).accountId(accountId).build());
        when(summaryFactory.summarize(batchId, TransactionImportSource.MERCADO_PAGO, accountId, List.of(row)))
                .thenReturn(response);
        var service = new TransactionImportPreviewService(
                profiles,
                accounts,
                org.mockito.Mockito.mock(ExcelImportBatchRepository.class),
                parser,
                duplicates,
                batchStore,
                summaryFactory
        );

        var actual = service.preview(
                userId,
                profileId,
                accountId,
                TransactionImportSource.AUTO,
                new MockMultipartFile("file", "movimientos.xlsx", "application/octet-stream", new byte[]{1}),
                2026,
                5
        );

        assertSame(response, actual);
        verify(batchStore).savePreviewRows(batchId, TransactionImportSource.MERCADO_PAGO, List.of(row));
    }
}
