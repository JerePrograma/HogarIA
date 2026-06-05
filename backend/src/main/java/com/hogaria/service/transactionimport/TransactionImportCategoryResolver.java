package com.hogaria.service.transactionimport;

import com.hogaria.dto.TransactionImportDtos.RowStatus;
import com.hogaria.dto.TransactionImportDtos.TransactionImportCommitRequest;
import com.hogaria.dto.TransactionImportDtos.TransactionImportCommitRow;
import com.hogaria.dto.TransactionImportDtos.TransactionImportPreviewRow;
import com.hogaria.dto.TransactionImportDtos.TransactionImportSource;
import com.hogaria.entity.Category;
import com.hogaria.entity.MoneyTransaction;
import com.hogaria.exception.ForbiddenException;
import com.hogaria.exception.NotFoundException;
import com.hogaria.repository.CategoryRepository;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class TransactionImportCategoryResolver {

    private final CategoryRepository categoryRepository;

    public TransactionImportCategoryResolver(CategoryRepository categoryRepository) {
        this.categoryRepository = categoryRepository;
    }

    public UUID resolveCategoryId(
            UUID profileId,
            TransactionImportCommitRow commitRow,
            TransactionImportPreviewRow previewRow,
            TransactionImportCommitRequest request,
            MoneyTransaction.MovementType movementType
    ) {
        if (commitRow.categoryId() != null) {
            ensureCategoryBelongsToProfileOrIsGlobal(profileId, commitRow.categoryId());
            return commitRow.categoryId();
        }

        if (previewRow.suggestedCategoryId() != null) {
            ensureCategoryBelongsToProfileOrIsGlobal(profileId, previewRow.suggestedCategoryId());
            return previewRow.suggestedCategoryId();
        }

        if (!request.createMissingFallbackCategory()
                || canImportWithoutCategory(previewRow)
                || !needsFallbackCategory(previewRow)) {
            return null;
        }

        return getOrCreateFallbackCategory(profileId, movementType).getId();
    }

    public Category findCategory(UUID categoryId) {
        if (categoryId == null) {
            return null;
        }

        return categoryRepository.findById(categoryId).orElse(null);
    }

    public boolean canImportWithoutCategory(TransactionImportPreviewRow row) {
        return canImportWithoutCategory(row.classificationStatus(), row.balanceImpact());
    }

    public boolean canImportWithoutCategory(
            MoneyTransaction.ClassificationStatus classificationStatus,
            MoneyTransaction.BalanceImpact balanceImpact
    ) {
        return classificationStatus == MoneyTransaction.ClassificationStatus.TECHNICAL
                || balanceImpact == MoneyTransaction.BalanceImpact.INTERNAL_TRANSFER
                || balanceImpact == MoneyTransaction.BalanceImpact.NEUTRAL_ADJUSTMENT
                || balanceImpact == MoneyTransaction.BalanceImpact.TECHNICAL;
    }

    public MoneyTransaction.Status importStatusFor(TransactionImportPreviewRow row, UUID categoryId) {
        if (categoryId != null) {
            return MoneyTransaction.Status.CONFIRMED;
        }

        if (row.classificationStatus() == MoneyTransaction.ClassificationStatus.TECHNICAL
                || row.balanceImpact() == MoneyTransaction.BalanceImpact.INTERNAL_TRANSFER
                || row.balanceImpact() == MoneyTransaction.BalanceImpact.NEUTRAL_ADJUSTMENT
                || row.balanceImpact() == MoneyTransaction.BalanceImpact.TECHNICAL) {
            return MoneyTransaction.Status.CONFIRMED;
        }

        return MoneyTransaction.Status.PENDING;
    }

    public boolean needsFallbackCategory(TransactionImportPreviewRow row) {
        return row.status() == RowStatus.NEEDS_CATEGORY
                || row.classificationStatus() == MoneyTransaction.ClassificationStatus.NEEDS_CATEGORY;
    }

    public MoneyTransaction.ClassificationStatus inferClassificationStatus(
            TransactionImportPreviewRow row,
            UUID categoryId
    ) {
        if (row.classificationStatus() != null) {
            return row.classificationStatus();
        }

        if (row.status() == RowStatus.SKIPPED) {
            return MoneyTransaction.ClassificationStatus.IGNORED_BY_RULE;
        }

        if (row.status() == RowStatus.POSSIBLE_CROSS_SOURCE_DUPLICATE
                || row.status() == RowStatus.POSSIBLE_INTERNAL_TRANSFER
                || row.status() == RowStatus.REVIEW) {
            return MoneyTransaction.ClassificationStatus.REVIEW;
        }

        if (row.status() == RowStatus.INTERNAL_TRANSFER_MATCHED
                || row.movementType() == MoneyTransaction.MovementType.TRANSFER
                || row.balanceImpact() == MoneyTransaction.BalanceImpact.INTERNAL_TRANSFER
                || row.balanceImpact() == MoneyTransaction.BalanceImpact.TECHNICAL) {
            return MoneyTransaction.ClassificationStatus.TECHNICAL;
        }

        if (categoryId == null) {
            return MoneyTransaction.ClassificationStatus.NEEDS_CATEGORY;
        }

        return MoneyTransaction.ClassificationStatus.CLASSIFIED;
    }

    public String inferClassificationReason(TransactionImportPreviewRow row) {
        if (!ImportTextSupport.isBlank(row.classificationReason())) {
            return row.classificationReason();
        }

        if (row.status() == RowStatus.POSSIBLE_CROSS_SOURCE_DUPLICATE) {
            return "POSSIBLE_CROSS_SOURCE_DUPLICATE";
        }

        if (row.status() == RowStatus.INTERNAL_TRANSFER_MATCHED) {
            return "INTERNAL_TRANSFER_MATCHED";
        }

        if (row.status() == RowStatus.POSSIBLE_INTERNAL_TRANSFER) {
            return "POSSIBLE_INTERNAL_TRANSFER";
        }

        var reason = ImportTextSupport.firstNonBlank(row.matchReason(), row.skipReason());
        return reason.isBlank() ? null : reason;
    }

    public MoneyTransaction.PaymentChannel inferPaymentChannel(
            TransactionImportSource source,
            String description
    ) {
        var text = ImportTextSupport.normalizeDescription(description);

        if (source == TransactionImportSource.MERCADO_PAGO) {
            return MoneyTransaction.PaymentChannel.MERCADO_PAGO;
        }

        if (text.contains("DEBIN")) {
            return MoneyTransaction.PaymentChannel.DEBIN;
        }

        if (text.contains("CUENTA DNI") || text.contains("CDNI")) {
            return MoneyTransaction.PaymentChannel.CUENTA_DNI;
        }

        if (text.contains("TARJETA DEBITO") || text.contains("T.D.")) {
            return MoneyTransaction.PaymentChannel.DEBIT_CARD;
        }

        if (text.contains("MASTERCARD") || text.contains("VISA")) {
            return MoneyTransaction.PaymentChannel.CREDIT_CARD;
        }

        if (text.contains("TRANSFERENCIA") || text.contains("BANK TRANSFER")) {
            return MoneyTransaction.PaymentChannel.BANK_TRANSFER;
        }

        return MoneyTransaction.PaymentChannel.UNKNOWN;
    }

    public boolean isMovementCategoryCompatible(
            MoneyTransaction.MovementType movementType,
            Category.Type categoryType
    ) {
        if (movementType == null || categoryType == null) {
            return false;
        }

        if (movementType == MoneyTransaction.MovementType.INCOME) {
            return categoryType == Category.Type.INCOME;
        }

        if (movementType == MoneyTransaction.MovementType.SAVING) {
            return categoryType == Category.Type.SAVING
                    || categoryType == Category.Type.INVESTMENT;
        }

        if (movementType == MoneyTransaction.MovementType.EXPENSE) {
            return categoryType == Category.Type.FIXED_EXPENSE
                    || categoryType == Category.Type.VARIABLE_EXPENSE
                    || categoryType == Category.Type.DEBT;
        }

        if (movementType == MoneyTransaction.MovementType.TRANSFER) {
            return categoryType == Category.Type.SAVING
                    || categoryType == Category.Type.INVESTMENT
                    || categoryType == Category.Type.VARIABLE_EXPENSE;
        }

        if (movementType == MoneyTransaction.MovementType.ADJUSTMENT) {
            return categoryType != Category.Type.INCOME;
        }

        return false;
    }

    public void ensureCategoryBelongsToProfileOrIsGlobal(UUID profileId, UUID categoryId) {
        var category = categoryRepository
                .findById(categoryId)
                .orElseThrow(() -> new NotFoundException("Category not found"));

        if (category.getProfileId() != null && !category.getProfileId().equals(profileId)) {
            throw new ForbiddenException("Category does not belong to profile");
        }
    }

    private Category getOrCreateFallbackCategory(
            UUID profileId,
            MoneyTransaction.MovementType movementType
    ) {
        var spec = fallbackSpec(movementType);

        var existing = loadVisibleCategories(profileId)
                .stream()
                .filter(category -> Boolean.TRUE.equals(category.getActive()))
                .filter(category ->
                        ImportTextSupport.sameCategoryKey(category.getCategoryKey(), spec.key())
                                || ImportTextSupport.sameCategoryName(category.getName(), spec.name())
                )
                .filter(category -> isMovementCategoryCompatible(movementType, category.getType()))
                .findFirst();

        return existing.orElseGet(() -> categoryRepository.save(
                Category.builder()
                        .profileId(profileId)
                        .parentId(null)
                        .name(spec.name())
                        .categoryKey(spec.key())
                        .type(spec.type())
                        .scope(Category.Scope.PERSONAL)
                        .defaultMovementType(spec.defaultMovementType())
                        .budgetable(spec.budgetable())
                        .technical(spec.technical())
                        .active(true)
                        .build()
        ));
    }

    private FallbackCategorySpec fallbackSpec(MoneyTransaction.MovementType movementType) {
        if (movementType == MoneyTransaction.MovementType.INCOME) {
            return new FallbackCategorySpec(
                    "Ingresos a revisar",
                    "ingresosarevisar",
                    Category.Type.INCOME,
                    MoneyTransaction.MovementType.INCOME,
                    true,
                    false
            );
        }

        if (movementType == MoneyTransaction.MovementType.SAVING) {
            return new FallbackCategorySpec(
                    "Ahorro / inversión a revisar",
                    "ahorroinversionarevisar",
                    Category.Type.INVESTMENT,
                    MoneyTransaction.MovementType.SAVING,
                    true,
                    false
            );
        }

        if (movementType == MoneyTransaction.MovementType.TRANSFER) {
            return new FallbackCategorySpec(
                    "Transferencias internas a revisar",
                    "transferenciasinternasarevisar",
                    Category.Type.SAVING,
                    MoneyTransaction.MovementType.TRANSFER,
                    false,
                    true
            );
        }

        if (movementType == MoneyTransaction.MovementType.ADJUSTMENT) {
            return new FallbackCategorySpec(
                    "Ajustes a revisar",
                    "ajustesarevisar",
                    Category.Type.VARIABLE_EXPENSE,
                    MoneyTransaction.MovementType.ADJUSTMENT,
                    false,
                    true
            );
        }

        return new FallbackCategorySpec(
                "Varios a revisar",
                "variosarevisar",
                Category.Type.VARIABLE_EXPENSE,
                MoneyTransaction.MovementType.EXPENSE,
                true,
                false
        );
    }

    private List<Category> loadVisibleCategories(UUID profileId) {
        var categories = new ArrayList<Category>();
        categories.addAll(categoryRepository.findByProfileIdAndActiveTrue(profileId));
        categories.addAll(categoryRepository.findByProfileIdIsNullAndActiveTrue());
        return categories;
    }

    private record FallbackCategorySpec(
            String name,
            String key,
            Category.Type type,
            MoneyTransaction.MovementType defaultMovementType,
            boolean budgetable,
            boolean technical
    ) {
    }
}
