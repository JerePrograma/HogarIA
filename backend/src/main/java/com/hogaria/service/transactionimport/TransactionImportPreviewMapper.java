package com.hogaria.service.transactionimport;

import com.hogaria.dto.TransactionImportDtos.Confidence;
import com.hogaria.dto.TransactionImportDtos.RowStatus;
import com.hogaria.dto.TransactionImportDtos.TransactionImportPreviewRow;
import com.hogaria.entity.Category;
import com.hogaria.entity.MoneyTransaction;
import com.hogaria.repository.CategoryRepository;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class TransactionImportPreviewMapper {

    private final CategoryRepository categoryRepository;
    private final TransactionImportCategoryResolver categoryResolver;

    public TransactionImportPreviewMapper(
            CategoryRepository categoryRepository,
            TransactionImportCategoryResolver categoryResolver
    ) {
        this.categoryRepository = categoryRepository;
        this.categoryResolver = categoryResolver;
    }

    public TransactionImportPreviewRow toPreviewRow(
            UUID profileId,
            ImportedMovementCandidate candidate
    ) {
        var categories = loadVisibleCategories(profileId);

        var category = findCategoryByNameOrKeyAndCompatibleType(
                categories,
                candidate.categorySuggestionName(),
                candidate.categorySuggestionKey(),
                candidate.movementType()
        );

        var status = candidate.rowStatus() == null
                ? RowStatus.NEEDS_CATEGORY
                : candidate.rowStatus();

        if (status == RowStatus.READY
                && candidate.categorySuggestionName() != null
                && category == null) {
            status = RowStatus.NEEDS_CATEGORY;
        }

        if (status == RowStatus.NEEDS_CATEGORY
                && category != null
                && candidate.classificationStatus() != MoneyTransaction.ClassificationStatus.REVIEW) {
            status = RowStatus.READY;
        }

        var suggestedCategoryName = category == null
                ? candidate.categorySuggestionName()
                : category.getName();

        return new TransactionImportPreviewRow(
                candidate.rowNumber(),
                candidate.source(),
                candidate.sourceOperationId(),
                candidate.sourceHash(),
                candidate.realDate(),
                candidate.budgetDate(),
                candidate.rawDescription(),
                candidate.normalizedDescription(),
                candidate.signedAmount(),
                candidate.amountAbs(),
                ImportTextSupport.firstNonBlank(candidate.currency(), ImportTextSupport.DEFAULT_CURRENCY)
                        .toUpperCase(Locale.ROOT),
                candidate.movementType(),
                category == null ? null : category.getId(),
                suggestedCategoryName,
                candidate.confidence() == null ? Confidence.LOW : candidate.confidence(),
                status,
                candidate.warning(),
                candidate.rawJson(),
                null,
                null,
                null,
                null,
                null,
                null,
                candidate.detectedFormat(),
                candidate.operationDateTime(),
                candidate.operationDateTimePrecision(),
                candidate.extendedDescription(),
                candidate.merchantName(),
                candidate.counterparty(),
                candidate.counterpartyDocumentHash(),
                candidate.paymentChannel(),
                candidate.balanceImpact(),
                candidate.classificationStatus(),
                candidate.classificationReason(),
                candidate.classificationLayer() == null ? null : candidate.classificationLayer().name(),
                candidate.classificationMatchedField(),
                candidate.classificationMatchedValue(),
                candidate.classificationExplanationJson(),
                candidate.categorySuggestionKey(),
                candidate.externalSequence(),
                candidate.sheetName(),
                candidate.targetEntity(),
                candidate.rawJson()
        );
    }

    private List<Category> loadVisibleCategories(UUID profileId) {
        var categories = new ArrayList<Category>();
        categories.addAll(categoryRepository.findByProfileIdAndActiveTrue(profileId));
        categories.addAll(categoryRepository.findByProfileIdIsNullAndActiveTrue());
        return categories;
    }

    private Category findCategoryByNameOrKeyAndCompatibleType(
            List<Category> categories,
            String name,
            String key,
            MoneyTransaction.MovementType movementType
    ) {
        if (ImportTextSupport.isBlank(name) && ImportTextSupport.isBlank(key)) {
            return null;
        }

        return categories
                .stream()
                .filter(category -> Boolean.TRUE.equals(category.getActive()))
                .filter(category ->
                        ImportTextSupport.sameCategoryName(category.getName(), name)
                                || ImportTextSupport.sameCategoryKey(category.getCategoryKey(), key)
                )
                .filter(category -> categoryResolver.isMovementCategoryCompatible(movementType, category.getType()))
                .findFirst()
                .orElse(null);
    }
}