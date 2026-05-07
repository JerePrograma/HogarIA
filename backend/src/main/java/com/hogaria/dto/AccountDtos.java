package com.hogaria.dto;

import com.hogaria.entity.Account;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public class AccountDtos {
  public record AccountCreateRequest(
      @NotBlank @Size(max = 120) String name,
      @NotNull Account.AccountType accountType,
      @NotBlank @Pattern(regexp = "^[A-Z]{3}$") String currency,
      @DecimalMin("0.00") BigDecimal creditLimit,
      @Min(1) @Max(31) Integer statementCloseDay,
      @Min(1) @Max(31) Integer dueDay) {}

  public record AccountUpdateRequest(
      @Size(max = 120) String name,
      Account.AccountType accountType,
      @Pattern(regexp = "^[A-Z]{3}$") String currency,
      @DecimalMin("0.00") BigDecimal creditLimit,
      @Min(1) @Max(31) Integer statementCloseDay,
      @Min(1) @Max(31) Integer dueDay,
      Boolean active) {}

  public record AccountResponse(
      UUID id, UUID profileId, String name, Account.AccountType accountType, String currency,
      BigDecimal creditLimit, Integer statementCloseDay, Integer dueDay, Boolean active,
      LocalDateTime createdAt, LocalDateTime updatedAt) {}
}
