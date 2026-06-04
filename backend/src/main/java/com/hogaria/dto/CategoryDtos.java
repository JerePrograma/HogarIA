package com.hogaria.dto;

import com.hogaria.entity.Category;
import com.fasterxml.jackson.annotation.JsonSetter;
import jakarta.validation.constraints.*;
import java.time.LocalDateTime;
import java.util.UUID;

public class CategoryDtos {
  public record CategoryCreateRequest(
      UUID parentId,
      @NotBlank @Size(max = 120) String name,
      @NotNull Category.Type type,
      @NotNull Category.Scope scope) {}

  public static class CategoryUpdateRequest {
    private UUID parentId;
    private boolean parentIdPresent;
    @Size(max = 120)
    private String name;
    private Category.Type type;
    private Category.Scope scope;
    private Boolean active;

    public CategoryUpdateRequest() {
    }

    public CategoryUpdateRequest(UUID parentId, String name, Category.Type type, Category.Scope scope, Boolean active) {
      this.parentId = parentId;
      this.parentIdPresent = true;
      this.name = name;
      this.type = type;
      this.scope = scope;
      this.active = active;
    }

    public UUID parentId() { return parentId; }
    public boolean parentIdPresent() { return parentIdPresent; }
    public String name() { return name; }
    public Category.Type type() { return type; }
    public Category.Scope scope() { return scope; }
    public Boolean active() { return active; }

    @JsonSetter("parentId")
    public void setParentId(UUID parentId) {
      this.parentId = parentId;
      this.parentIdPresent = true;
    }

    public void setName(String name) { this.name = name; }
    public void setType(Category.Type type) { this.type = type; }
    public void setScope(Category.Scope scope) { this.scope = scope; }
    public void setActive(Boolean active) { this.active = active; }
  }

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
      String displayPath,
      int depth,
      LocalDateTime createdAt,
      LocalDateTime updatedAt) {}
}
