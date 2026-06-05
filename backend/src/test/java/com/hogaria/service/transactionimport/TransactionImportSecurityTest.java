package com.hogaria.service.transactionimport;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.hogaria.dto.TransactionImportDtos.RowStatus;
import com.hogaria.dto.TransactionImportDtos.TransactionImportCommitRequest;
import com.hogaria.dto.TransactionImportDtos.TransactionImportCommitRow;
import com.hogaria.dto.TransactionImportDtos.TransactionImportSource;
import com.hogaria.entity.ExcelImportBatch;
import com.hogaria.entity.ExcelImportRow;
import com.hogaria.entity.FinancialProfile;
import com.hogaria.entity.MoneyTransaction;
import com.hogaria.exception.BadRequestException;
import com.hogaria.exception.ForbiddenException;
import com.hogaria.repository.AccountRepository;
import com.hogaria.repository.ExcelImportBatchRepository;
import com.hogaria.repository.FinancialProfileRepository;
import com.hogaria.service.TransactionService;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

class TransactionImportSecurityTest {

    @Test
    void previewRejectsAccountOutsideProfileBeforeParsing() {
        var profiles = org.mockito.Mockito.mock(FinancialProfileRepository.class);
        var accounts = org.mockito.Mockito.mock(AccountRepository.class);
        var parser = org.mockito.Mockito.mock(TransactionImportParserRouter.class);
        var userId = UUID.randomUUID();
        var profileId = UUID.randomUUID();
        var accountId = UUID.randomUUID();
        when(profiles.findByIdAndUserId(profileId, userId))
                .thenReturn(Optional.of(FinancialProfile.builder().id(profileId).userId(userId).build()));
        when(accounts.existsByIdAndProfileId(accountId, profileId)).thenReturn(false);
        var service = new TransactionImportPreviewService(
                profiles,
                accounts,
                org.mockito.Mockito.mock(ExcelImportBatchRepository.class),
                parser,
                org.mockito.Mockito.mock(TransactionImportDuplicateDetector.class),
                org.mockito.Mockito.mock(TransactionImportBatchStore.class),
                org.mockito.Mockito.mock(TransactionImportSummaryFactory.class),
                org.mockito.Mockito.mock(ImportInternalTransferMatchingService.class)
        );

        assertThrows(BadRequestException.class, () -> service.preview(
                userId,
                profileId,
                accountId,
                TransactionImportSource.AUTO,
                new MockMultipartFile("file", "test.xlsx", "application/octet-stream", new byte[]{1}),
                null,
                null
        ));
        verifyNoInteractions(parser);
    }

    @Test
    void commitRejectsBatchOutsideProfileAndInvalidBatchAccount() {
        var profiles = org.mockito.Mockito.mock(FinancialProfileRepository.class);
        var accounts = org.mockito.Mockito.mock(AccountRepository.class);
        var batchStore = org.mockito.Mockito.mock(TransactionImportBatchStore.class);
        var userId = UUID.randomUUID();
        var profileId = UUID.randomUUID();
        var accountId = UUID.randomUUID();
        var batchId = UUID.randomUUID();
        when(profiles.findByIdAndUserId(profileId, userId))
                .thenReturn(Optional.of(FinancialProfile.builder().id(profileId).userId(userId).build()));
        var service = commitService(profiles, accounts, batchStore);
        var request = new TransactionImportCommitRequest(List.of(
                new TransactionImportCommitRow(
                        1, null, accountId, MoneyTransaction.MovementType.EXPENSE,
                        BigDecimal.ONE, RowStatus.READY, "Movimiento"
                )
        ), false, true);

        when(batchStore.getBatchOrThrow(batchId)).thenReturn(
                ExcelImportBatch.builder().id(batchId).profileId(UUID.randomUUID()).accountId(accountId).build()
        );
        assertThrows(ForbiddenException.class, () -> service.commit(userId, profileId, batchId, request));

        when(batchStore.getBatchOrThrow(batchId)).thenReturn(
                ExcelImportBatch.builder().id(batchId).profileId(profileId).accountId(accountId).build()
        );
        when(accounts.existsByIdAndProfileId(accountId, profileId)).thenReturn(false);
        assertThrows(BadRequestException.class, () -> service.commit(userId, profileId, batchId, request));
    }

    @Test
    void duplicateCannotBeImportedUnlessSkipDuplicatesIsConfirmed() {
        var profiles = org.mockito.Mockito.mock(FinancialProfileRepository.class);
        var accounts = org.mockito.Mockito.mock(AccountRepository.class);
        var batchStore = org.mockito.Mockito.mock(TransactionImportBatchStore.class);
        var txService = org.mockito.Mockito.mock(TransactionService.class);
        var userId = UUID.randomUUID();
        var profileId = UUID.randomUUID();
        var accountId = UUID.randomUUID();
        var batchId = UUID.randomUUID();
        var previewRow = TransactionImportTestData.row(
                1,
                RowStatus.DUPLICATE_EXACT,
                MoneyTransaction.MovementType.EXPENSE,
                MoneyTransaction.BalanceImpact.CONSUMPTION_EXPENSE,
                MoneyTransaction.ClassificationStatus.CLASSIFIED,
                UUID.randomUUID()
        );
        when(profiles.findByIdAndUserId(profileId, userId))
                .thenReturn(Optional.of(FinancialProfile.builder().id(profileId).userId(userId).build()));
        when(accounts.existsByIdAndProfileId(accountId, profileId)).thenReturn(true);
        when(batchStore.getBatchOrThrow(batchId)).thenReturn(
                ExcelImportBatch.builder().id(batchId).profileId(profileId).accountId(accountId).build()
        );
        when(batchStore.loadRowSnapshots(batchId)).thenReturn(List.of(
                new TransactionImportBatchStore.LoadedPreviewRow(
                        previewRow,
                        ExcelImportRow.builder().id(UUID.randomUUID()).batchId(batchId).rowNumber(1).build()
                )
        ));
        var service = new TransactionImportCommitService(
                profiles,
                accounts,
                txService,
                batchStore,
                org.mockito.Mockito.mock(TransactionImportDuplicateDetector.class),
                org.mockito.Mockito.mock(TransactionImportCategoryResolver.class),
                org.mockito.Mockito.mock(ImportInternalTransferMatchingService.class)
        );
        var request = new TransactionImportCommitRequest(List.of(
                new TransactionImportCommitRow(
                        1, previewRow.suggestedCategoryId(), accountId, previewRow.movementType(),
                        previewRow.amount(), RowStatus.DUPLICATE_EXACT, previewRow.rawDescription()
                )
        ), false, false);

        var response = service.commit(userId, profileId, batchId, request);

        assertEquals(1, response.failedCount());
        verify(txService, never()).create(any(), any(), any());
    }

    private TransactionImportCommitService commitService(
            FinancialProfileRepository profiles,
            AccountRepository accounts,
            TransactionImportBatchStore batchStore
    ) {
        return new TransactionImportCommitService(
                profiles,
                accounts,
                org.mockito.Mockito.mock(TransactionService.class),
                batchStore,
                org.mockito.Mockito.mock(TransactionImportDuplicateDetector.class),
                org.mockito.Mockito.mock(TransactionImportCategoryResolver.class),
                org.mockito.Mockito.mock(ImportInternalTransferMatchingService.class)
        );
    }
}
