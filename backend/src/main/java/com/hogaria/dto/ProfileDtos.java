package com.hogaria.dto;

import com.hogaria.entity.FinancialProfile;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import java.time.LocalDateTime;
import java.util.UUID;

public class ProfileDtos {
  public record ProfileCreateRequest(@NotBlank String name, @NotNull FinancialProfile.Type type, @Pattern(regexp = "^[A-Z]{3}$") String baseCurrency, @Min(2000) @Max(2100) Integer activeYear) {}
  public record ProfileUpdateRequest(String name, FinancialProfile.Type type, @Pattern(regexp = "^[A-Z]{3}$") String baseCurrency, @Min(2000) @Max(2100) Integer activeYear, Boolean active) {}
  public record ProfileResponse(UUID id, String name, FinancialProfile.Type type, String baseCurrency, Integer activeYear, Boolean active, LocalDateTime createdAt, LocalDateTime updatedAt) {}
}
