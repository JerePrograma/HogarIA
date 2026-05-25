package com.hogaria.dto;

import com.hogaria.entity.Category;
import jakarta.validation.constraints.*;
import java.time.LocalDateTime;
import java.util.UUID;

public class CategoryDtos {
  public record CategoryCreateRequest(
      UUID parentId,
      @NotBlank @Size(max = 120) String name,
      @NotNull Category.Type type,
      @NotNull Category.Scope scope) {}

  public record CategoryUpdateRequest(
      UUID parentId,
      @Size(max = 120) String name,
      Category.Type type,
      Category.Scope scope,
      Boolean active) {}

  public record CategoryResponse(
      UUID id,
      UUID profileId,
      UUID parentId,
      String name,
      String categoryKey,
      Category.Type type,
      Category.Scope scope,
      com.hogaria.entity.MoneyTransaction.MovementType defaultMovementType,
      Boolean budgetable,
      Boolean technical,
      Boolean active,
      LocalDateTime createdAt,
      LocalDateTime updatedAt) {}
}
