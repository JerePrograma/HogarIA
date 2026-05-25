package com.hogaria.integration.cjprestamos.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public class ExternalLoanBackfillDtos {
  public record BackfillDryRunResponse(List<BackfillCandidate> candidates) {}
  public record BackfillApplyRequest(boolean includeLowConfidence) {}
  public record BackfillApplyResponse(int createdMappings, List<String> skipped, List<String> errors) {}
  public record BackfillCandidate(UUID transactionId, String description, BigDecimal amount, LocalDate realDate,
                                  String inferredEntityType, String inferredEntityId, String inferredEventType,
                                  String confidence, String warning, boolean wouldCreateMapping) {}
}
