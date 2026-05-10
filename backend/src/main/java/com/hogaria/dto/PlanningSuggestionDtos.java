package com.hogaria.dto;

import com.hogaria.entity.MonthlyPlanItem;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public class PlanningSuggestionDtos {
  public enum SuggestionConfidence { HIGH, MEDIUM, LOW, NONE }

  public record PlanningSuggestionRequest(
      MonthlyPlanItem.Type type,
      String title,
      String counterparty,
      BigDecimal amount,
      BigDecimal minAmount,
      BigDecimal maxAmount,
      BigDecimal expectedRecoveryAmount,
      BigDecimal expectedRecoveryPercent) {}

  public record SuggestedAccount(UUID id, String name, SuggestionConfidence confidence, String reason) {}

  public record SuggestedCategory(UUID id, String name, SuggestionConfidence confidence, String reason) {}

  public record PlanningSuggestionResponse(
      SuggestedAccount accountSuggestion,
      SuggestedCategory categorySuggestion,
      SuggestionConfidence confidence,
      List<String> reasons) {}
}
