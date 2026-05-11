package com.hogaria.dto;

import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;
import java.util.UUID;

public class ExternalLoanSyncConfigDtos {
  public record ExternalLoanSyncConfigUpsertRequest(
      @NotNull UUID accountId,
      @NotNull UUID loanDisbursementCategoryId,
      @NotNull UUID principalRecoveryCategoryId,
      @NotNull UUID interestIncomeCategoryId,
      @NotNull Boolean enabled) {}

  public record ExternalLoanSyncConfigResponse(
      UUID id,
      UUID profileId,
      UUID accountId,
      UUID loanDisbursementCategoryId,
      UUID principalRecoveryCategoryId,
      UUID interestIncomeCategoryId,
      Boolean enabled,
      LocalDateTime createdAt,
      LocalDateTime updatedAt) {}
}
