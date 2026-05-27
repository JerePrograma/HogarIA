package com.hogaria.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import com.hogaria.domains.transactions.lifecycle.TransactionLifecycleService;
import com.hogaria.dto.TransactionCreateRequest;
import com.hogaria.dto.TransactionUpdateRequest;
import com.hogaria.entity.Category;
import com.hogaria.entity.FinancialProfile;
import com.hogaria.entity.MoneyTransaction;
import com.hogaria.exception.DomainConflictException;
import com.hogaria.repository.AccountRepository;
import com.hogaria.repository.CategoryRepository;
import com.hogaria.repository.FinancialProfileRepository;
import com.hogaria.repository.MoneyTransactionRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TransactionDuplicateDetectionTest {

    @Mock MoneyTransactionRepository txRepo;
    @Mock FinancialProfileRepository profileRepo;
    @Mock AccountRepository accountRepo;
    @Mock CategoryRepository categoryRepo;
    @Mock TransactionCategorySuggestionService suggestionService;
    @Mock TransactionLifecycleService lifecycleService;

    UUID userId = UUID.randomUUID();
    UUID profileId = UUID.randomUUID();
    UUID accountId = UUID.randomUUID();
    UUID otherAccountId = UUID.randomUUID();
    UUID categoryId = UUID.randomUUID();

    DescriptionNormalizer descriptionNormalizer = new DescriptionNormalizer();
    TransactionFingerprintService fingerprintService = new TransactionFingerprintService();
    TransactionFinancialImpactService impactService = new TransactionFinancialImpactService();
    DuplicateDetectionService duplicateDetectionService;
    TransactionService service;

    @BeforeEach
    void setup() {
        duplicateDetectionService = new DuplicateDetectionService(txRepo);
        service = new TransactionService(
                txRepo,
                profileRepo,
                accountRepo,
                categoryRepo,
                suggestionService,
                lifecycleService,
                descriptionNormalizer,
                fingerprintService,
                duplicateDetectionService,
                impactService
        );

        org.mockito.Mockito.lenient().when(profileRepo.findByIdAndUserId(profileId, userId)).thenReturn(Optional.of(new FinancialProfile()));
        org.mockito.Mockito.lenient().when(accountRepo.existsByIdAndProfileId(accountId, profileId)).thenReturn(true);
        org.mockito.Mockito.lenient().when(categoryRepo.findById(categoryId)).thenReturn(Optional.of(expenseCategory()));
        org.mockito.Mockito.lenient().when(txRepo.save(any(MoneyTransaction.class))).thenAnswer(invocation -> {
            MoneyTransaction tx = invocation.getArgument(0);
            if (tx.getId() == null) {
                tx.setId(UUID.randomUUID());
            }
            return tx;
        });
    }

    @Test
    void sameProfileAccountDateDescriptionAmountAndCurrencyIsDuplicateDespiteCaseSpacesAndAccents() {
        var existing = transaction(accountId, " Café   pago ", new BigDecimal("1200.00"));
        existing.setNormalizedDescription(descriptionNormalizer.normalizeNullable(existing.getDescription()));
        existing.setOperationDateTime(existing.getRealDate().atStartOfDay());
        existing.setDuplicateFingerprint(fingerprintService.buildFingerprint(existing));

        when(txRepo.findActiveDuplicatesByFingerprint(eq(profileId), eq(accountId), any(), any(), any(), eq(null)))
                .thenReturn(List.of(existing));

        var ex = assertThrows(
                DomainConflictException.class,
                () -> service.create(request(accountId, "CAFE pago", new BigDecimal("1200.00"), null, null, null), userId)
        );

        assertEquals("TRANSACTION_EXACT_DUPLICATE", ex.getCode());
    }

    @Test
    void differentAccountSameAmountCanBeCreatedAsPotentialInternalTransferLeg() {
        when(accountRepo.existsByIdAndProfileId(otherAccountId, profileId)).thenReturn(true);
        when(txRepo.findActiveDuplicatesByFingerprint(eq(profileId), eq(otherAccountId), any(), any(), any(), eq(null)))
                .thenReturn(List.of());

        var response = service.create(
                request(otherAccountId, "Fondeo Mercado Pago DEBIN", new BigDecimal("400000.00"), null, null, null),
                userId
        );

        assertNotNull(response.id());
        assertEquals("FONDEO MERCADO PAGO DEBIN", response.normalizedDescription());
    }

    @Test
    void repeatedSourceHashIsRejectedAsIdempotentDuplicate() {
        var existing = transaction(accountId, "Importado", new BigDecimal("10.00"));
        existing.setSourceHash("abc123");
        when(txRepo.findByProfileIdAndSourceHash(profileId, "abc123")).thenReturn(Optional.of(existing));

        var ex = assertThrows(
                DomainConflictException.class,
                () -> service.create(request(accountId, "Importado", new BigDecimal("10.00"), "BANCO_PROVINCIA", null, "abc123"), userId)
        );

        assertEquals("TRANSACTION_SOURCE_DUPLICATE", ex.getCode());
    }

    @Test
    void repeatedSourceOperationIdIsRejectedAsIdempotentDuplicate() {
        var existing = transaction(accountId, "Importado", new BigDecimal("10.00"));
        existing.setSource("BANCO_PROVINCIA");
        existing.setSourceOperationId("op-1");
        when(txRepo.findByStrongSourceOperation(profileId, "BANCO_PROVINCIA", "op-1", null))
                .thenReturn(List.of(existing));

        var ex = assertThrows(
                DomainConflictException.class,
                () -> service.create(request(accountId, "Importado", new BigDecimal("10.00"), "BANCO_PROVINCIA", "op-1", null), userId)
        );

        assertEquals("TRANSACTION_SOURCE_DUPLICATE", ex.getCode());
    }

    @Test
    void updateCannotBecomeExactDuplicateOfAnotherTransaction() {
        var transactionId = UUID.randomUUID();
        var existing = transaction(accountId, "Super", new BigDecimal("100.00"));
        var edited = transaction(accountId, "Otro", new BigDecimal("50.00"));
        edited.setId(transactionId);

        when(txRepo.findById(transactionId)).thenReturn(Optional.of(edited));
        when(txRepo.findActiveDuplicatesByFingerprint(eq(profileId), eq(accountId), any(), any(), any(), eq(transactionId)))
                .thenReturn(List.of(existing));

        var ex = assertThrows(
                DomainConflictException.class,
                () -> service.update(userId, transactionId, new TransactionUpdateRequest(
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        LocalDateTime.of(2026, 5, 6, 0, 0),
                        new BigDecimal("100.00"),
                        null,
                        "Super",
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
                ))
        );

        assertEquals("TRANSACTION_EXACT_DUPLICATE", ex.getCode());
    }

    private TransactionCreateRequest request(
            UUID accountId,
            String description,
            BigDecimal amount,
            String source,
            String sourceOperationId,
            String sourceHash
    ) {
        return new TransactionCreateRequest(
                profileId,
                accountId,
                categoryId,
                MoneyTransaction.MovementType.EXPENSE,
                LocalDate.of(2026, 5, 6),
                LocalDate.of(2026, 5, 1),
                null,
                amount,
                "ARS",
                description,
                MoneyTransaction.Origin.MANUAL,
                MoneyTransaction.Status.CONFIRMED,
                source,
                sourceOperationId,
                sourceHash,
                null,
                null,
                null,
                null,
                null,
                null
        );
    }

    private MoneyTransaction transaction(UUID accountId, String description, BigDecimal amount) {
        return MoneyTransaction.builder()
                .id(UUID.randomUUID())
                .profileId(profileId)
                .accountId(accountId)
                .categoryId(categoryId)
                .movementType(MoneyTransaction.MovementType.EXPENSE)
                .realDate(LocalDate.of(2026, 5, 6))
                .budgetDate(LocalDate.of(2026, 5, 1))
                .amount(amount)
                .currency("ARS")
                .description(description)
                .status(MoneyTransaction.Status.CONFIRMED)
                .classificationStatus(MoneyTransaction.ClassificationStatus.CLASSIFIED)
                .build();
    }

    private Category expenseCategory() {
        return Category.builder()
                .id(categoryId)
                .profileId(profileId)
                .active(true)
                .type(Category.Type.VARIABLE_EXPENSE)
                .build();
    }
}
