package com.hogaria.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.hogaria.dto.CategoryDtos.CategoryCreateRequest;
import com.hogaria.dto.CategoryDtos.CategoryUpdateRequest;
import com.hogaria.entity.Category;
import com.hogaria.entity.FinancialProfile;
import com.hogaria.exception.BadRequestException;
import com.hogaria.exception.DomainConflictException;
import com.hogaria.exception.ForbiddenException;
import com.hogaria.repository.CategoryRepository;
import com.hogaria.repository.FinancialProfileRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CategoryServiceTest {

  @Mock
  CategoryRepository categoryRepository;

  @Mock
  FinancialProfileRepository profileRepository;

  private CategoryService service;
  private UUID userId;
  private UUID profileId;

  @BeforeEach
  void setup() {
    service = new CategoryService(categoryRepository, profileRepository, new CategoryKeyNormalizer());
    userId = UUID.randomUUID();
    profileId = UUID.randomUUID();
  }

  @Test
  void createsPersonalCategoryWithNormalizedKey() {
    allowProfile();
    when(categoryRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

    var response = service.create(
            userId,
            profileId,
            new CategoryCreateRequest(null, "Comida diaria", Category.Type.VARIABLE_EXPENSE, Category.Scope.PERSONAL)
    );

    assertEquals("Comida diaria", response.name());
    assertEquals("comidadiaria", response.categoryKey());
    assertEquals("Comida diaria", response.displayPath());
    assertEquals(0, response.depth());
  }

  @Test
  void listsTreeWithDisplayPathAndActiveTechnicalOrdering() {
    allowProfile();
    var root = category("Alimentacion", "alimentacion", Category.Type.VARIABLE_EXPENSE, false, true, false, null);
    var child = category("Supermercado", "supermercado", Category.Type.VARIABLE_EXPENSE, false, true, false, root.getId());
    var inactive = category("Archivo", "archivo", Category.Type.VARIABLE_EXPENSE, false, false, false, null);
    var technical = category("Movimiento ignorado", "movimientoignorado", Category.Type.INVESTMENT, true, true, true, null);
    var global = category("Sueldo", "sueldo", Category.Type.INCOME, true, true, false, null);
    global.setProfileId(null);
    global.setScope(Category.Scope.GLOBAL);

    when(categoryRepository.findByProfileId(profileId)).thenReturn(List.of(inactive, child, root));
    when(categoryRepository.findByProfileIdIsNullAndActiveTrue()).thenReturn(List.of(technical, global));

    var responses = service.list(userId, profileId, true);

    assertEquals(List.of(
            "Alimentacion",
            "Alimentacion / Supermercado",
            "Sueldo",
            "Movimiento ignorado",
            "Archivo"
    ), responses.stream().map(response -> response.displayPath()).toList());
    assertEquals(1, responses.get(1).depth());
  }

  @Test
  void updateCanRemoveParentWithExplicitNullParentId() {
    allowProfile();
    var parent = category("Alimentacion", "alimentacion", Category.Type.VARIABLE_EXPENSE, false, true, false, null);
    var child = category("Supermercado", "supermercado", Category.Type.VARIABLE_EXPENSE, false, true, false, parent.getId());

    when(categoryRepository.findById(child.getId())).thenReturn(Optional.of(child));
    when(categoryRepository.findByProfileId(profileId)).thenReturn(List.of(child));
    when(categoryRepository.findByProfileIdIsNullAndActiveTrue()).thenReturn(List.of());
    when(categoryRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

    var response = service.update(userId, child.getId(), new CategoryUpdateRequest(null, null, null, null, null));

    assertNull(response.parentId());
    assertEquals("Supermercado", response.displayPath());
    assertEquals(0, response.depth());
  }

  @Test
  void rejectsCyclesWhenMovingRootUnderDescendant() {
    allowProfile();
    var root = category("Alimentacion", "alimentacion", Category.Type.VARIABLE_EXPENSE, false, true, false, null);
    var child = category("Supermercado", "supermercado", Category.Type.VARIABLE_EXPENSE, false, true, false, root.getId());

    when(categoryRepository.findById(root.getId())).thenReturn(Optional.of(root));
    when(categoryRepository.findById(child.getId())).thenReturn(Optional.of(child));
    when(categoryRepository.findAll()).thenReturn(List.of(root, child));

    assertThrows(
            BadRequestException.class,
            () -> service.update(userId, root.getId(), new CategoryUpdateRequest(child.getId(), null, null, null, null))
    );
  }

  @Test
  void rejectsDuplicateActiveKeyInSameProfileAndType() {
    allowProfile();
    when(categoryRepository.existsActiveProfileDuplicateKey(profileId, "supermercado", Category.Type.VARIABLE_EXPENSE, null))
            .thenReturn(true);

    assertThrows(
            DomainConflictException.class,
            () -> service.create(
                    userId,
                    profileId,
                    new CategoryCreateRequest(null, "Super mercado", Category.Type.VARIABLE_EXPENSE, Category.Scope.PERSONAL)
            )
    );
  }

  @Test
  void rejectsEditingGlobalCategories() {
    var category = category("Global", "global", Category.Type.FIXED_EXPENSE, true, true, false, null);
    category.setProfileId(null);
    category.setScope(Category.Scope.GLOBAL);

    when(categoryRepository.findById(category.getId())).thenReturn(Optional.of(category));

    assertThrows(
            ForbiddenException.class,
            () -> service.update(userId, category.getId(), new CategoryUpdateRequest(null, "a", null, null, null))
    );
  }

  @Test
  void profileCategoriesAndGlobalCategoriesDoNotConflictInList() {
    allowProfile();
    var personal = category("Sueldo", "sueldo", Category.Type.INCOME, false, true, false, null);
    var global = category("Sueldo", "sueldo", Category.Type.INCOME, true, true, false, null);
    global.setProfileId(null);
    global.setScope(Category.Scope.GLOBAL);

    when(categoryRepository.findByProfileId(profileId)).thenReturn(List.of(personal));
    when(categoryRepository.findByProfileIdIsNullAndActiveTrue()).thenReturn(List.of(global));

    var responses = service.list(userId, profileId, true);

    assertEquals(2, responses.size());
    assertEquals(1, responses.stream().filter(response -> response.scope() == Category.Scope.GLOBAL).count());
    assertEquals(1, responses.stream().filter(response -> response.scope() == Category.Scope.PERSONAL).count());
  }

  private void allowProfile() {
    when(profileRepository.findByIdAndUserId(profileId, userId)).thenReturn(Optional.of(new FinancialProfile()));
  }

  private Category category(
          String name,
          String key,
          Category.Type type,
          boolean global,
          boolean active,
          boolean technical,
          UUID parentId
  ) {
    return Category
            .builder()
            .id(UUID.randomUUID())
            .profileId(global ? null : profileId)
            .parentId(parentId)
            .name(name)
            .categoryKey(key)
            .type(type)
            .scope(global ? Category.Scope.GLOBAL : Category.Scope.PERSONAL)
            .active(active)
            .technical(technical)
            .budgetable(!technical)
            .build();
  }
}
