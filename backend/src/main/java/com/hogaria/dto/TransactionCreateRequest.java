package com.hogaria.dto;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record TransactionCreateRequest(
  @NotNull UUID profileId,
  @NotNull UUID accountId,
  @NotNull UUID categoryId,
  @NotBlank String movementType,
  @NotNull LocalDate realDate,
  @NotNull LocalDate budgetDate,
  @NotNull @DecimalMin("0.01") BigDecimal amount,
  @Size(max = 255) String description
) {}
