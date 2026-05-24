package com.hogaria.dto;

import com.hogaria.dto.MonthlyPlanDtos.MonthlyPlanItemResponse;
import com.hogaria.entity.MonthlyPlanItem;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public class QuickPlanTextDtos {
  public enum AmountScale {UNITS,THOUSANDS}

  public record QuickPlanTextPreviewRequest(
      @NotBlank String rawText,
      @NotNull @Min(2000) @Max(2100) Integer periodYear,
      @NotNull @Min(1) @Max(12) Integer periodMonth,
      AmountScale defaultAmountScale,
      @DecimalMin("0") @DecimalMax("1") BigDecimal approximateMargin,
      @Pattern(regexp = "^[A-Za-z]{3}$") String currency) {}

  public record NormalizedCandidate(
      Integer lineNumber,
      String title,
      MonthlyPlanItem.Type type,
      MonthlyPlanItem.Priority priority,
      BigDecimal amount,
      BigDecimal minAmount,
      BigDecimal maxAmount,
      UUID categoryId,
      UUID accountId) {}

  public record QuickPlanTextCandidate(
      Integer lineNumber,
      String rawLine,
      NormalizedCandidate candidate,
      UUID suggestedCategoryId,
      String suggestedCategoryName,
      List<String> warnings,
      boolean duplicate) {}

  public record QuickPlanTextPreviewResponse(List<QuickPlanTextCandidate> candidates, List<String> warnings) {}

  public record QuickPlanTextCommitRequest(
      @NotNull @Min(2000) @Max(2100) Integer periodYear,
      @NotNull @Min(1) @Max(12) Integer periodMonth,
      @NotNull List<NormalizedCandidate> candidates,
      boolean skipDuplicates) {}

  public record QuickPlanTextCommitResponse(List<MonthlyPlanItemResponse> created, List<String> warnings, int skippedDuplicates) {}

  public record ClassificationResult(MonthlyPlanItem.Type type, MonthlyPlanItem.Priority priority, String categoryHint) {}
}
