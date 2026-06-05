package com.hogaria.service.transactionimport;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.hogaria.dto.TransactionImportDtos.RowStatus;
import com.hogaria.dto.TransactionImportDtos.TransactionImportCommitRequest;
import com.hogaria.dto.TransactionImportDtos.TransactionImportCommitRow;
import com.hogaria.entity.Category;
import com.hogaria.entity.MoneyTransaction;
import com.hogaria.repository.CategoryRepository;
import java.math.BigDecimal;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class TransactionImportCategoryResolverTest {

    private CategoryRepository repository;
    private TransactionImportCategoryResolver resolver;
    private UUID profileId;

    @BeforeEach
    void setup() {
        repository = org.mockito.Mockito.mock(CategoryRepository.class);
        resolver = new TransactionImportCategoryResolver(repository);
        profileId = UUID.randomUUID();
        when(repository.findByProfileIdAndActiveTrue(profileId)).thenReturn(List.of());
        when(repository.findByProfileIdIsNullAndActiveTrue()).thenReturn(List.of());
        when(repository.save(any(Category.class))).thenAnswer(invocation -> {
            var category = invocation.<Category>getArgument(0);
            category.setId(UUID.randomUUID());
            return category;
        });
    }

    @Test
    void canImportWithoutCategoryOnlyForTechnicalOrNeutralSemantics() {
        assertTrue(resolver.canImportWithoutCategory(row(
                RowStatus.REVIEW,
                MoneyTransaction.MovementType.TRANSFER,
                MoneyTransaction.BalanceImpact.INTERNAL_TRANSFER,
                MoneyTransaction.ClassificationStatus.TECHNICAL
        )));
        assertTrue(resolver.canImportWithoutCategory(row(
                RowStatus.REVIEW,
                MoneyTransaction.MovementType.ADJUSTMENT,
                MoneyTransaction.BalanceImpact.NEUTRAL_ADJUSTMENT,
                MoneyTransaction.ClassificationStatus.REVIEW
        )));
        assertFalse(resolver.canImportWithoutCategory(row(
                RowStatus.REVIEW,
                MoneyTransaction.MovementType.EXPENSE,
                MoneyTransaction.BalanceImpact.CONSUMPTION_EXPENSE,
                MoneyTransaction.ClassificationStatus.REVIEW
        )));
    }

    @Test
    void createsCompatibleFallbackForEveryMovementType() {
        for (var movementType : MoneyTransaction.MovementType.values()) {
            var categoryId = resolver.resolveCategoryId(
                    profileId,
                    commitRow(movementType),
                    row(
                            RowStatus.NEEDS_CATEGORY,
                            movementType,
                            MoneyTransaction.BalanceImpact.UNKNOWN,
                            MoneyTransaction.ClassificationStatus.NEEDS_CATEGORY
                    ),
                    new TransactionImportCommitRequest(List.of(), true, true),
                    movementType
            );

            assertNotNull(categoryId);
        }

        var captor = ArgumentCaptor.forClass(Category.class);
        verify(repository, org.mockito.Mockito.times(5)).save(captor.capture());
        var names = captor.getAllValues().stream().map(Category::getName).collect(Collectors.toSet());

        assertTrue(names.containsAll(Set.of(
                "Ingresos a revisar",
                "Varios a revisar",
                "Transferencias internas a revisar",
                "Ajustes a revisar",
                "Ahorro / inversión a revisar"
        )));
    }

    @Test
    void fallbackAlsoAppliesToReviewMarkedNeedsCategoryButNotTechnicalRows() {
        var reviewNeedsCategory = row(
                RowStatus.REVIEW,
                MoneyTransaction.MovementType.EXPENSE,
                MoneyTransaction.BalanceImpact.UNKNOWN,
                MoneyTransaction.ClassificationStatus.NEEDS_CATEGORY
        );
        var technical = row(
                RowStatus.NEEDS_CATEGORY,
                MoneyTransaction.MovementType.TRANSFER,
                MoneyTransaction.BalanceImpact.INTERNAL_TRANSFER,
                MoneyTransaction.ClassificationStatus.TECHNICAL
        );
        var request = new TransactionImportCommitRequest(List.of(), true, true);

        assertNotNull(resolver.resolveCategoryId(
                profileId,
                commitRow(MoneyTransaction.MovementType.EXPENSE),
                reviewNeedsCategory,
                request,
                MoneyTransaction.MovementType.EXPENSE
        ));
        resolver.resolveCategoryId(
                profileId,
                commitRow(MoneyTransaction.MovementType.TRANSFER),
                technical,
                request,
                MoneyTransaction.MovementType.TRANSFER
        );

        verify(repository, org.mockito.Mockito.times(1)).save(any(Category.class));
    }

    @Test
    void validatesMovementCategoryCompatibility() {
        assertTrue(resolver.isMovementCategoryCompatible(MoneyTransaction.MovementType.INCOME, Category.Type.INCOME));
        assertTrue(resolver.isMovementCategoryCompatible(MoneyTransaction.MovementType.EXPENSE, Category.Type.DEBT));
        assertTrue(resolver.isMovementCategoryCompatible(MoneyTransaction.MovementType.SAVING, Category.Type.INVESTMENT));
        assertTrue(resolver.isMovementCategoryCompatible(MoneyTransaction.MovementType.TRANSFER, Category.Type.SAVING));
        assertTrue(resolver.isMovementCategoryCompatible(MoneyTransaction.MovementType.ADJUSTMENT, Category.Type.DEBT));
        assertFalse(resolver.isMovementCategoryCompatible(MoneyTransaction.MovementType.INCOME, Category.Type.VARIABLE_EXPENSE));
        assertFalse(resolver.isMovementCategoryCompatible(MoneyTransaction.MovementType.TRANSFER, Category.Type.INCOME));
        assertFalse(resolver.isMovementCategoryCompatible(MoneyTransaction.MovementType.ADJUSTMENT, Category.Type.INCOME));
    }

    private com.hogaria.dto.TransactionImportDtos.TransactionImportPreviewRow row(
            RowStatus status,
            MoneyTransaction.MovementType movementType,
            MoneyTransaction.BalanceImpact balanceImpact,
            MoneyTransaction.ClassificationStatus classificationStatus
    ) {
        return TransactionImportTestData.row(1, status, movementType, balanceImpact, classificationStatus, null);
    }

    private TransactionImportCommitRow commitRow(MoneyTransaction.MovementType movementType) {
        return new TransactionImportCommitRow(
                1,
                null,
                UUID.randomUUID(),
                movementType,
                BigDecimal.ONE,
                RowStatus.NEEDS_CATEGORY,
                "Movimiento"
        );
    }
}
