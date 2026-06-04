package com.hogaria.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.hogaria.domains.transactions.lifecycle.TransactionLifecycleService;
import com.hogaria.dto.TransactionBulkDtos.BulkCategorizeRequest;
import com.hogaria.dto.TransactionBulkDtos.BulkIgnoreRequest;
import com.hogaria.dto.TransactionBulkDtos.BulkStatusRequest;
import com.hogaria.dto.TransactionCreateRequest;
import com.hogaria.dto.TransactionUpdateRequest;
import com.hogaria.entity.Category;
import com.hogaria.entity.FinancialProfile;
import com.hogaria.entity.MoneyTransaction;
import com.hogaria.exception.BadRequestException;
import com.hogaria.repository.AccountRepository;
import com.hogaria.repository.CategoryRepository;
import com.hogaria.repository.FinancialProfileRepository;
import com.hogaria.repository.MoneyTransactionRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TransactionServiceTest {

    @Mock MoneyTransactionRepository txRepo;
    @Mock FinancialProfileRepository profileRepo;
    @Mock AccountRepository accountRepo;
    @Mock CategoryRepository categoryRepo;
    @Mock TransactionCategorySuggestionService suggestionService;
    @Mock TransactionLifecycleService transactionLifecycleService;
    @Mock DescriptionNormalizer descriptionNormalizer;
    @Mock TransactionFingerprintService fingerprintService;
    @Mock DuplicateDetectionService duplicateDetectionService;
    @Mock TransactionFinancialImpactService financialImpactService;
    @Mock TransactionReviewMapper transactionReviewMapper;

    @InjectMocks TransactionService service;

    @BeforeEach
    void setupImpact() {
        lenientImpact();
    }

    @Test
    void rejectsAmountZero() {
        UUID id = UUID.randomUUID();
        var req = new TransactionCreateRequest(
                id,
                id,
                id,
                MoneyTransaction.MovementType.EXPENSE,
                LocalDate.now(),
                LocalDate.now(),
                null,
                BigDecimal.ZERO,
                "ARS",
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null
        );

        assertThrows(BadRequestException.class, () -> service.create(req, id));
    }

    @Test
    void updateClearCategorySetsNeedsCategory() {
        var userId = UUID.randomUUID();
        var profileId = UUID.randomUUID();
        var transactionId = UUID.randomUUID();
        var accountId = UUID.randomUUID();
        var categoryId = UUID.randomUUID();
        var transaction = transaction(profileId, accountId, categoryId);

        when(txRepo.findById(transactionId)).thenReturn(Optional.of(transaction));
        when(profileRepo.findByIdAndUserId(profileId, userId)).thenReturn(Optional.of(new FinancialProfile()));
        when(accountRepo.existsByIdAndProfileId(accountId, profileId)).thenReturn(true);
        when(txRepo.save(any(MoneyTransaction.class))).thenAnswer(invocation -> invocation.getArgument(0));

        service.update(userId, transactionId, new TransactionUpdateRequest(
                null,
                null,
                true,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null
        ));

        assertNull(transaction.getCategoryId());
        assertEquals(MoneyTransaction.ClassificationStatus.NEEDS_CATEGORY, transaction.getClassificationStatus());
        assertNull(transaction.getClassificationReason());
    }

    @Test
    void updateRejectsIncompatibleCategory() {
        var userId = UUID.randomUUID();
        var profileId = UUID.randomUUID();
        var transactionId = UUID.randomUUID();
        var accountId = UUID.randomUUID();
        var categoryId = UUID.randomUUID();
        var transaction = transaction(profileId, accountId, UUID.randomUUID());

        when(txRepo.findById(transactionId)).thenReturn(Optional.of(transaction));
        when(profileRepo.findByIdAndUserId(profileId, userId)).thenReturn(Optional.of(new FinancialProfile()));
        when(accountRepo.existsByIdAndProfileId(accountId, profileId)).thenReturn(true);
        when(categoryRepo.findById(categoryId)).thenReturn(Optional.of(Category.builder()
                .id(categoryId)
                .profileId(profileId)
                .active(true)
                .type(Category.Type.INCOME)
                .build()));

        var request = new TransactionUpdateRequest(
                null,
                categoryId,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null
        );

        assertThrows(BadRequestException.class, () -> service.update(userId, transactionId, request));
    }

    @Test
    void bulkCategorizePreservesInternalTransferIntegrity() {
        var userId = UUID.randomUUID();
        var profileId = UUID.randomUUID();
        var transactionId = UUID.randomUUID();
        var accountId = UUID.randomUUID();
        var categoryId = UUID.randomUUID();
        var transaction = transaction(profileId, accountId, null);
        transaction.setId(transactionId);
        transaction.setMovementType(MoneyTransaction.MovementType.TRANSFER);
        transaction.setInternalTransferGroupId(UUID.randomUUID());
        transaction.setBalanceImpact(MoneyTransaction.BalanceImpact.INTERNAL_TRANSFER);

        when(profileRepo.findByIdAndUserId(profileId, userId)).thenReturn(Optional.of(new FinancialProfile()));
        when(categoryRepo.findById(categoryId)).thenReturn(Optional.of(Category.builder()
                .id(categoryId)
                .profileId(profileId)
                .active(true)
                .type(Category.Type.VARIABLE_EXPENSE)
                .build()));
        when(txRepo.findByProfileIdAndIdIn(profileId, List.of(transactionId))).thenReturn(List.of(transaction));

        assertThrows(BadRequestException.class, () -> service.bulkCategorize(
                userId,
                profileId,
                new BulkCategorizeRequest(List.of(transactionId), categoryId)
        ));
    }

    @Test
    void bulkIgnoreDoesNotDeleteMovements() {
        var userId = UUID.randomUUID();
        var profileId = UUID.randomUUID();
        var transactionId = UUID.randomUUID();
        var transaction = transaction(profileId, UUID.randomUUID(), UUID.randomUUID());
        transaction.setId(transactionId);

        when(profileRepo.findByIdAndUserId(profileId, userId)).thenReturn(Optional.of(new FinancialProfile()));
        when(txRepo.findByProfileIdAndIdIn(profileId, List.of(transactionId))).thenReturn(List.of(transaction));
        when(txRepo.save(any(MoneyTransaction.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var response = service.bulkIgnore(
                userId,
                profileId,
                new BulkIgnoreRequest(List.of(transactionId), "DUPLICATE_RESOLVED")
        );

        assertEquals(1, response.updatedCount());
        assertEquals(MoneyTransaction.Status.IGNORED, transaction.getStatus());
        assertEquals(MoneyTransaction.ClassificationStatus.IGNORED_BY_RULE, transaction.getClassificationStatus());
        verify(txRepo, never()).delete(any(MoneyTransaction.class));
    }

    @Test
    void bulkStatusRecalculatesImpact() {
        var userId = UUID.randomUUID();
        var profileId = UUID.randomUUID();
        var transactionId = UUID.randomUUID();
        var transaction = transaction(profileId, UUID.randomUUID(), UUID.randomUUID());
        transaction.setId(transactionId);

        when(profileRepo.findByIdAndUserId(profileId, userId)).thenReturn(Optional.of(new FinancialProfile()));
        when(txRepo.findByProfileIdAndIdIn(profileId, List.of(transactionId))).thenReturn(List.of(transaction));
        when(txRepo.save(any(MoneyTransaction.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(financialImpactService.analyze(any(), any(), any()))
                .thenReturn(new TransactionFinancialImpact(
                        CashFlowTreatment.UNKNOWN,
                        MoneyTransaction.BalanceImpact.IGNORED,
                        false,
                        false,
                        false,
                        false,
                        false,
                        false,
                        false,
                        false,
                        false,
                        false,
                        false,
                        true,
                        false,
                        false,
                        false
                ));

        service.bulkStatus(
                userId,
                profileId,
                new BulkStatusRequest(
                        List.of(transactionId),
                        MoneyTransaction.Status.IGNORED,
                        null,
                        "Revisión masiva"
                )
        );

        assertEquals(MoneyTransaction.BalanceImpact.IGNORED, transaction.getBalanceImpact());
    }

    private MoneyTransaction transaction(UUID profileId, UUID accountId, UUID categoryId) {
        return MoneyTransaction.builder()
                .profileId(profileId)
                .accountId(accountId)
                .categoryId(categoryId)
                .movementType(MoneyTransaction.MovementType.EXPENSE)
                .realDate(LocalDate.now())
                .budgetDate(LocalDate.now())
                .amount(new BigDecimal("10.00"))
                .currency("ARS")
                .status(MoneyTransaction.Status.CONFIRMED)
                .classificationStatus(MoneyTransaction.ClassificationStatus.CLASSIFIED)
                .classificationReason("MANUAL")
                .build();
    }

    private void lenientImpact() {
        org.mockito.Mockito.lenient()
                .when(financialImpactService.analyze(any(), any(), any()))
                .thenReturn(new TransactionFinancialImpact(
                        CashFlowTreatment.VARIABLE_CONSUMPTION_EXPENSE,
                        MoneyTransaction.BalanceImpact.CONSUMPTION_EXPENSE,
                        true,
                        false,
                        true,
                        false,
                        false,
                        false,
                        false,
                        false,
                        false,
                        false,
                        false,
                        false,
                        false,
                        false,
                        false
                ));
    }
}
