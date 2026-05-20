import type { Category, CategoryType, MovementType } from './types';

export function isCategoryCompatibleWithMovement(
  category: Category,
  movementType: MovementType,
) {
  return isCategoryTypeCompatibleWithMovement(category.type, movementType);
}

export function isCategoryTypeCompatibleWithMovement(
  categoryType: CategoryType,
  movementType: MovementType,
) {
  if (movementType === 'INCOME') {
    return categoryType === 'INCOME';
  }

  if (movementType === 'SAVING') {
    return categoryType === 'SAVING' || categoryType === 'INVESTMENT';
  }

  if (movementType === 'EXPENSE') {
    return categoryType === 'FIXED_EXPENSE'
      || categoryType === 'VARIABLE_EXPENSE'
      || categoryType === 'DEBT';
  }

  if (movementType === 'TRANSFER') {
    return categoryType === 'SAVING'
      || categoryType === 'INVESTMENT'
      || categoryType === 'VARIABLE_EXPENSE';
  }

  if (movementType === 'ADJUSTMENT') {
    return categoryType !== 'INCOME';
  }

  return false;
}

export function getCompatibleCategories(
  categories: Category[],
  movementType: MovementType,
  options?: {
    includeTechnical?: boolean;
    includeInactive?: boolean;
  },
) {
  return categories.filter((category) => {
    if (!options?.includeInactive && !category.active) return false;
    if (!options?.includeTechnical && category.technical) return false;

    return isCategoryCompatibleWithMovement(category, movementType);
  });
}

export function getCategoryDisplayName(category?: Category | null) {
  if (!category) return 'Sin categoría';
  if (category.technical) return `${category.name} · técnica`;
  return category.name;
}