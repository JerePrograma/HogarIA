package com.hogaria.service.transactionimport;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import com.hogaria.dto.TransactionCreateRequest;
import com.hogaria.dto.TransactionImportDtos.RowStatus;
import com.hogaria.dto.TransactionImportDtos.TransactionImportCommitRequest;
import com.hogaria.dto.TransactionImportDtos.TransactionImportCommitRow;
import com.hogaria.dto.TransactionResponse;
import com.hogaria.entity.Category;
import com.hogaria.entity.ExcelImportBatch;
import com.hogaria.entity.ExcelImportRow;
import com.hogaria.entity.FinancialProfile;
import com.hogaria.entity.ImportTargetEntity;
import com.hogaria.entity.MoneyTransaction;
import com.hogaria.repository.AccountRepository;
import com.hogaria.repository.FinancialProfileRepository;
import com.hogaria.service.TransactionService;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class TransactionImportCommitServiceTest {

    @Test
    void forwardsCompleteImportAuditMetadataToTransactionCreation() {
        var profiles = org.mockito.Mockito.mock(FinancialProfileRepository.class);
        var accounts = org.mockito.Mockito.mock(AccountRepository.class);
        var txService = org.mockito.Mockito.mock(TransactionService.class);
        var batchStore = org.mockito.Mockito.mock(TransactionImportBatchStore.class);
        var duplicateDetector = org.mockito.Mockito.mock(TransactionImportDuplicateDetector.class);
        var categoryResolver = org.mockito.Mockito.mock(TransactionImportCategoryResolver.class);
        var internalTransfers = org.mockito.Mockito.mock(ImportInternalTransferMatchingService.class);
        var userId = UUID.randomUUID();
        var profileId = UUID.randomUUID();
        var accountId = UUID.randomUUID();
        var batchId = UUID.randomUUID();
        var categoryId = UUID.randomUUID();
        var transactionId = UUID.randomUUID();
        var row = auditRow(categoryId);
        var entity = ExcelImportRow.builder().id(UUID.randomUUID()).batchId(batchId).rowNumber(1).build();
        when(profiles.findByIdAndUserId(profileId, userId))
                .thenReturn(Optional.of(FinancialProfile.builder().id(profileId).userId(userId).build()));
        when(accounts.existsByIdAndProfileId(accountId, profileId)).thenReturn(true);
        when(batchStore.getBatchOrThrow(batchId)).thenReturn(
                ExcelImportBatch.builder().id(batchId).profileId(profileId).accountId(accountId).build()
        );
        when(batchStore.loadRowSnapshots(batchId))
                .thenReturn(List.of(new TransactionImportBatchStore.LoadedPreviewRow(row, entity)));
        when(categoryResolver.resolveCategoryId(any(), any(), any(), any(), any())).thenReturn(categoryId);
        when(categoryResolver.findCategory(categoryId)).thenReturn(
                Category.builder().id(categoryId).type(Category.Type.VARIABLE_EXPENSE).active(true).build()
        );
        when(categoryResolver.isMovementCategoryCompatible(row.movementType(), Category.Type.VARIABLE_EXPENSE))
                .thenReturn(true);
        when(categoryResolver.importStatusFor(row, categoryId)).thenReturn(MoneyTransaction.Status.CONFIRMED);
        when(categoryResolver.inferClassificationStatus(row, categoryId))
                .thenReturn(MoneyTransaction.ClassificationStatus.CLASSIFIED);
        when(categoryResolver.inferClassificationReason(row)).thenReturn("RULE_AUDIT");
        when(duplicateDetector.findImportMatch(profileId, accountId, row)).thenReturn(TransactionImportMatch.none());
        var response = org.mockito.Mockito.mock(TransactionResponse.class);
        when(response.id()).thenReturn(transactionId);
        when(txService.create(any(TransactionCreateRequest.class), eq(userId), any(TransactionService.TransactionMetadata.class)))
                .thenReturn(response);
        var service = new TransactionImportCommitService(
                profiles, accounts, txService, batchStore, duplicateDetector, categoryResolver, internalTransfers
        );
        var request = new TransactionImportCommitRequest(List.of(
                new TransactionImportCommitRow(
                        1, categoryId, accountId, row.movementType(), row.amount(), RowStatus.READY, row.rawDescription()
                )
        ), false, true);

        var result = service.commit(userId, profileId, batchId, request);

        assertEquals(1, result.createdCount());
        var requestCaptor = ArgumentCaptor.forClass(TransactionCreateRequest.class);
        var metadataCaptor = ArgumentCaptor.forClass(TransactionService.TransactionMetadata.class);
        org.mockito.Mockito.verify(txService).create(requestCaptor.capture(), eq(userId), metadataCaptor.capture());
        assertEquals("MERCADO_PAGO", requestCaptor.getValue().source());
        assertEquals("operation-audit", requestCaptor.getValue().sourceOperationId());
        assertEquals("hash-audit", requestCaptor.getValue().sourceHash());
        assertEquals(MoneyTransaction.PaymentChannel.MERCADO_PAGO, requestCaptor.getValue().paymentChannel());
        assertEquals("Proveedor auditado", requestCaptor.getValue().counterparty());
        assertEquals(MoneyTransaction.ClassificationStatus.CLASSIFIED, requestCaptor.getValue().classificationStatus());
        assertEquals("RULE_AUDIT", requestCaptor.getValue().classificationReason());
        assertEquals("{\"reason\":\"audit\"}", requestCaptor.getValue().classificationExplanationJson());
        assertEquals(batchId, requestCaptor.getValue().importBatchId());
        assertEquals(MoneyTransaction.BalanceImpact.CONSUMPTION_EXPENSE, metadataCaptor.getValue().balanceImpact());
    }

    private com.hogaria.dto.TransactionImportDtos.TransactionImportPreviewRow auditRow(UUID categoryId) {
        var base = TransactionImportTestData.row(
                1,
                RowStatus.READY,
                MoneyTransaction.MovementType.EXPENSE,
                MoneyTransaction.BalanceImpact.CONSUMPTION_EXPENSE,
                MoneyTransaction.ClassificationStatus.CLASSIFIED,
                categoryId
        );
        return new com.hogaria.dto.TransactionImportDtos.TransactionImportPreviewRow(
                base.rowNumber(), base.source(), "operation-audit", "hash-audit", base.realDate(), base.budgetDate(),
                base.rawDescription(), base.normalizedDescription(), base.rawSignedAmount(), base.amount(), base.currency(),
                base.movementType(), base.suggestedCategoryId(), base.suggestedCategoryName(), base.confidence(), base.status(),
                base.skipReason(), base.rawPayload(), null, null, null, null, null, null, base.detectedFormat(),
                base.operationDateTime(), base.operationDateTimePrecision(), base.extendedDescription(), base.merchantName(),
                "Proveedor auditado", base.counterpartyDocumentHash(), MoneyTransaction.PaymentChannel.MERCADO_PAGO,
                base.balanceImpact(), base.classificationStatus(), "RULE_AUDIT", base.classificationLayer(),
                base.classificationMatchedField(), base.classificationMatchedValue(), "{\"reason\":\"audit\"}",
                base.categorySuggestionKey(), base.externalSequence(), base.sheetName(), ImportTargetEntity.EXPENSE, base.rawJson()
        );
    }
}
