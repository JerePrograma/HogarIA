package com.hogaria.service;

import com.hogaria.dto.CategoryDtos.CategoryCreateRequest;
import com.hogaria.dto.CategoryDtos.CategoryResponse;
import com.hogaria.dto.CategoryDtos.CategoryUpdateRequest;
import com.hogaria.entity.Category;
import com.hogaria.exception.BadRequestException;
import com.hogaria.exception.DomainConflictException;
import com.hogaria.exception.ErrorResponse;
import com.hogaria.exception.ForbiddenException;
import com.hogaria.exception.NotFoundException;
import com.hogaria.repository.CategoryRepository;
import com.hogaria.repository.FinancialProfileRepository;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class CategoryService {

  private final CategoryRepository categoryRepository;
  private final FinancialProfileRepository profileRepository;
  private final CategoryKeyNormalizer categoryKeyNormalizer;

  public CategoryService(
          CategoryRepository categoryRepository,
          FinancialProfileRepository profileRepository,
          CategoryKeyNormalizer categoryKeyNormalizer
  ) {
    this.categoryRepository = categoryRepository;
    this.profileRepository = profileRepository;
    this.categoryKeyNormalizer = categoryKeyNormalizer;
  }

  public CategoryResponse create(UUID userId, UUID profileId, CategoryCreateRequest request) {
    ensureProfile(userId, profileId);

    if (request.scope() == Category.Scope.GLOBAL) {
      throw new BadRequestException("GLOBAL scope not allowed");
    }

    validateParent(request.parentId(), profileId, null);
    var key = categoryKeyNormalizer.normalize(request.name());
    rejectDuplicateKey(profileId, key, request.type(), null);

    var category = Category
            .builder()
            .profileId(profileId)
            .parentId(request.parentId())
            .name(request.name())
            .categoryKey(key)
            .type(request.type())
            .scope(request.scope())
            .active(true)
            .build();

    var saved = categoryRepository.save(category);
    return toResponse(saved, pathContext(List.of(saved)));
  }

  public List<CategoryResponse> list(UUID userId, UUID profileId, boolean includeGlobal) {
    ensureProfile(userId, profileId);

    var categories = loadCategories(profileId, includeGlobal);
    var context = pathContext(categories);

    return categories
            .stream()
            .map(category -> toResponse(category, context))
            .sorted(categoryComparator())
            .toList();
  }

  public CategoryResponse get(UUID userId, UUID id) {
    var category = categoryRepository
            .findById(id)
            .orElseThrow(() -> new NotFoundException("Category not found"));

    if (category.getProfileId() != null) {
      ensureProfile(userId, category.getProfileId());
    }

    return toResponse(category, loadPathContext(category.getProfileId(), true));
  }

  public CategoryResponse update(UUID userId, UUID id, CategoryUpdateRequest request) {
    var category = categoryRepository
            .findById(id)
            .orElseThrow(() -> new NotFoundException("Category not found"));

    if (category.getProfileId() == null) {
      throw new ForbiddenException("Global categories cannot be edited");
    }

    ensureProfile(userId, category.getProfileId());

    if (request.parentIdPresent()) {
      validateParent(request.parentId(), category.getProfileId(), category.getId());
      category.setParentId(request.parentId());
    }

    var nextName = request.name() != null ? request.name() : category.getName();
    var nextType = request.type() != null ? request.type() : category.getType();
    var nextKey = categoryKeyNormalizer.normalize(nextName);

    if (!Boolean.FALSE.equals(request.active())) {
      rejectDuplicateKey(category.getProfileId(), nextKey, nextType, category.getId());
    }

    if (request.name() != null) {
      category.setName(request.name());
      category.setCategoryKey(nextKey);
    }

    if (request.type() != null) {
      category.setType(request.type());
    }

    if (request.scope() != null && request.scope() != Category.Scope.GLOBAL) {
      category.setScope(request.scope());
    }

    if (request.active() != null) {
      category.setActive(request.active());
    }

    return toResponse(categoryRepository.save(category), loadPathContext(category.getProfileId(), true));
  }

  public void deactivate(UUID userId, UUID id) {
    var category = categoryRepository
            .findById(id)
            .orElseThrow(() -> new NotFoundException("Category not found"));

    if (category.getProfileId() == null) {
      throw new ForbiddenException("Global categories cannot be deactivated");
    }

    ensureProfile(userId, category.getProfileId());
    category.setActive(false);
    categoryRepository.save(category);
  }

  private List<Category> loadCategories(UUID profileId, boolean includeGlobal) {
    var categories = new ArrayList<Category>();
    addAll(categories, categoryRepository.findByProfileId(profileId));

    if (includeGlobal) {
      addAll(categories, categoryRepository.findByProfileIdIsNullAndActiveTrue());
    }

    return categories;
  }

  private Map<UUID, CategoryPath> loadPathContext(UUID profileId, boolean includeGlobal) {
    return pathContext(loadCategories(profileId, includeGlobal));
  }

  private void addAll(List<Category> target, List<Category> source) {
    if (source != null) {
      target.addAll(source);
    }
  }

  private Map<UUID, CategoryPath> pathContext(List<Category> categories) {
    var byId = new HashMap<UUID, Category>();

    for (var category : categories) {
      if (category.getId() != null) {
        byId.put(category.getId(), category);
      }
    }

    var paths = new HashMap<UUID, CategoryPath>();

    for (var category : categories) {
      if (category.getId() != null) {
        paths.put(category.getId(), buildPath(category, byId, new HashSet<>()));
      }
    }

    return paths;
  }

  private CategoryPath buildPath(Category category, Map<UUID, Category> byId, HashSet<UUID> visited) {
    if (category.getId() == null || !visited.add(category.getId())) {
      return new CategoryPath(category.getName(), 0);
    }

    if (category.getParentId() == null || !byId.containsKey(category.getParentId())) {
      return new CategoryPath(category.getName(), 0);
    }

    var parent = byId.get(category.getParentId());
    var parentPath = buildPath(parent, byId, visited);

    return new CategoryPath(parentPath.displayPath() + " / " + category.getName(), parentPath.depth() + 1);
  }

  private void validateParent(UUID parentId, UUID profileId, UUID categoryId) {
    if (parentId == null) {
      return;
    }

    if (Objects.equals(parentId, categoryId)) {
      throw new BadRequestException("Una categoría no puede ser hija de sí misma.");
    }

    var parent = categoryRepository
            .findById(parentId)
            .orElseThrow(() -> new BadRequestException("Parent category not found"));

    if (parent.getProfileId() != null && !Objects.equals(parent.getProfileId(), profileId)) {
      throw new BadRequestException("Parent category must belong to same profile or be global");
    }

    if (categoryId != null && wouldCreateCycle(parentId, categoryId)) {
      throw new BadRequestException("La jerarquía de categorías no puede tener ciclos.");
    }
  }

  private boolean wouldCreateCycle(UUID candidateParentId, UUID categoryId) {
    var byId = new HashMap<UUID, Category>();

    for (var category : categoryRepository.findAll()) {
      if (category.getId() != null) {
        byId.put(category.getId(), category);
      }
    }

    var currentId = candidateParentId;
    var visited = new HashSet<UUID>();

    while (currentId != null && visited.add(currentId)) {
      if (Objects.equals(currentId, categoryId)) {
        return true;
      }

      var current = byId.get(currentId);
      currentId = current == null ? null : current.getParentId();
    }

    return false;
  }

  private void rejectDuplicateKey(UUID profileId, String key, Category.Type type, UUID excludeId) {
    if (key != null && categoryRepository.existsActiveProfileDuplicateKey(profileId, key, type, excludeId)) {
      throw new DomainConflictException(
              "Ya existe una categoría activa equivalente para ese perfil y tipo.",
              "CATEGORY_DUPLICATE_NORMALIZED",
              List.of(
                      new ErrorResponse.Detail("categoryKey", key),
                      new ErrorResponse.Detail("type", type.name())
              )
      );
    }
  }

  private void ensureProfile(UUID userId, UUID profileId) {
    profileRepository
            .findByIdAndUserId(profileId, userId)
            .orElseThrow(() -> new ForbiddenException("Profile does not belong to user"));
  }

  private CategoryResponse toResponse(Category category, Map<UUID, CategoryPath> paths) {
    var path = category.getId() == null
            ? new CategoryPath(category.getName(), 0)
            : paths.getOrDefault(category.getId(), new CategoryPath(category.getName(), 0));

    return new CategoryResponse(
            category.getId(),
            category.getProfileId(),
            category.getParentId(),
            category.getName(),
            category.getCategoryKey(),
            category.getType(),
            category.getScope(),
            category.getDefaultMovementType(),
            category.getBudgetable(),
            category.getTechnical(),
            category.getActive(),
            path.displayPath(),
            path.depth(),
            category.getCreatedAt(),
            category.getUpdatedAt()
    );
  }

  private Comparator<CategoryResponse> categoryComparator() {
    return Comparator
            .comparing((CategoryResponse category) -> !Boolean.TRUE.equals(category.active()))
            .thenComparing(category -> Boolean.TRUE.equals(category.technical()))
            .thenComparing(CategoryResponse::displayPath, String.CASE_INSENSITIVE_ORDER)
            .thenComparing(category -> category.id() == null ? "" : category.id().toString());
  }

  private record CategoryPath(String displayPath, int depth) {
  }
}
