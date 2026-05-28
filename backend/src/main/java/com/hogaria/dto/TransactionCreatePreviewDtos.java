package com.hogaria.dto;

import com.hogaria.entity.Category;
import com.hogaria.entity.MoneyTransaction;
import com.hogaria.service.TransactionCategorySuggestionService;
import java.util.List;
import java.util.UUID;

public class TransactionCreatePreviewDtos {

    public enum RiskLevel {
        OK,
        WARNING,
        BLOCKING
    }

    public enum RecommendedAction {
        CREATE,
        REVIEW_DUPLICATE,
        LINK_TRANSFER,
        CHOOSE_CATEGORY
    }

    public record CategorySuggestionPreview(
            UUID categoryId,
            String categoryName,
            Category.Type categoryType,
            MoneyTransaction.MovementType movementType,
            TransactionCategorySuggestionService.Confidence confidence,
            TransactionCategorySuggestionService.Status status,
            String reason,
            String humanReason
    ) {
    }

    public record TransactionCreatePreviewResponse(
            RiskLevel riskLevel,
            boolean canCreateDirectly,
            String normalizedDescription,
            String financialImpact,
            String humanSummary,
            List<TransactionReviewDtos.TransactionReviewItem> duplicateCandidates,
            List<TransactionReviewDtos.InternalTransferCandidate> internalTransferCandidates,
            CategorySuggestionPreview categorySuggestion,
            RecommendedAction recommendedAction
    ) {
    }
}
