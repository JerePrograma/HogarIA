package com.hogaria.dto;

import com.hogaria.dto.MonthlyPlanDtos.MonthlyPlanItemCreateRequest;
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
      @Min(2000) @Max(2100) Integer periodYear,
      @Min(1) @Max(12) Integer periodMonth,
      AmountScale defaultAmountScale,
      @DecimalMin("0") @DecimalMax("1") BigDecimal approximateMargin,
      @Pattern(regexp = "^[A-Za-z]{3}$") String currency) {}

  public record QuickPlanTextCandidate(
      Integer lineNumber,
      String rawLine,
      MonthlyPlanItemCreateRequest item,
      UUID suggestedCategoryId,
      String suggestedCategoryName,
      List<String> warnings,
      boolean duplicate) {}

  public record QuickPlanTextPreviewResponse(List<QuickPlanTextCandidate> candidates, List<String> warnings) {}

  public record QuickPlanTextCommitRequest(List<MonthlyPlanItemCreateRequest> items) {}

  public record QuickPlanTextCommitResponse(List<MonthlyPlanItemResponse> created, List<String> warnings) {}

  public record ClassificationResult(MonthlyPlanItem.Type type, MonthlyPlanItem.Priority priority, String categoryHint) {}
}
