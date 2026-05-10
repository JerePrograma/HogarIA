package com.hogaria.dto;

import com.hogaria.dto.MonthlyPlanDtos.MonthlyPlanItemCreateRequest;
import com.hogaria.dto.MonthlyPlanDtos.MonthlyPlanItemResponse;
import java.util.List;

public class QuickCaptureDtos {
  public enum QuickCaptureConfidence {HIGH,MEDIUM,LOW}

  public record QuickCapturePreviewRequest(String rawText, Integer defaultYear, Integer defaultMonth, String defaultCurrency) {}
  public record QuickCapturePreviewResponse(String rawText, QuickCaptureConfidence confidence, List<String> warnings, MonthlyPlanItemCreateRequest parsed,
                                            String detectedDateText, String detectedAmountText, String detectedRangeText, String detectedRecoveryText,
                                            String detectedInstallmentText, String detectedCounterpartyText, String detectedTypeText) {}
  public record QuickCaptureCommitRequest(String rawText, MonthlyPlanItemCreateRequest payload) {}
  public record QuickCaptureCommitResponse(MonthlyPlanItemResponse item, List<String> warnings) {}
}
