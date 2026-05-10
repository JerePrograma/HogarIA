package com.hogaria.dto;

import com.hogaria.dto.MonthlyPlanDtos.MonthlyPlanItemCreateRequest;
import com.hogaria.dto.MonthlyPlanDtos.MonthlyPlanItemResponse;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import java.util.List;

public class QuickCaptureDtos {
  public enum QuickCaptureConfidence {HIGH,MEDIUM,LOW}

  public record QuickCapturePreviewRequest(@NotBlank(message = "rawText es obligatorio") String rawText,
                                           @Min(value = 2000, message = "defaultYear debe estar entre 2000 y 2100") @Max(value = 2100, message = "defaultYear debe estar entre 2000 y 2100") Integer defaultYear,
                                           @Min(value = 1, message = "defaultMonth debe estar entre 1 y 12") @Max(value = 12, message = "defaultMonth debe estar entre 1 y 12") Integer defaultMonth,
                                           @Pattern(regexp = "^[A-Za-z]{3}$", message = "defaultCurrency debe tener 3 letras") String defaultCurrency) {}
  public record QuickCapturePreviewResponse(String rawText, QuickCaptureConfidence confidence, List<String> warnings, MonthlyPlanItemCreateRequest parsed,
                                            String detectedDateText, String detectedAmountText, String detectedRangeText, String detectedRecoveryText,
                                            String detectedInstallmentText, String detectedCounterpartyText, String detectedTypeText) {}
  public record QuickCaptureCommitRequest(String rawText, MonthlyPlanItemCreateRequest payload) {}
  public record QuickCaptureCommitResponse(MonthlyPlanItemResponse item, List<String> warnings) {}
}
