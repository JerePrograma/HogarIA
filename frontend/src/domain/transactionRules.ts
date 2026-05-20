// src/domain/transactionRules.ts

import type {
  Category,
  CategoryType,
  MoneyTransaction,
  MovementType,
  TransactionClassificationStatus,
  TransactionStatus,
} from "./types";

export interface CategoryCompatibilityOptions {
  includeTechnical?: boolean;
  includeInactive?: boolean;

  /**
   * El backend hoy permite EXPENSE con cualquier categoría que no sea INCOME.
   * Para UI manual conviene ser más estricto y evitar SAVING/INVESTMENT.
   */
  expenseCanUseSavingOrInvestment?: boolean;
}

export function isCategoryTypeCompatibleWithMovement(
  categoryType: CategoryType,
  movementType: MovementType,
  options?: Pick<
    CategoryCompatibilityOptions,
    "expenseCanUseSavingOrInvestment"
  >,
) {
  if (movementType === "INCOME") {
    return categoryType === "INCOME";
  }

  if (movementType === "SAVING") {
    return categoryType === "SAVING" || categoryType === "INVESTMENT";
  }

  if (movementType === "EXPENSE") {
    if (categoryType === "INCOME") return false;

    if (options?.expenseCanUseSavingOrInvestment) {
      return true;
    }

    return (
      categoryType === "FIXED_EXPENSE" ||
      categoryType === "VARIABLE_EXPENSE" ||
      categoryType === "DEBT"
    );
  }

  /**
   * TRANSFER y ADJUSTMENT tienen semántica más técnica.
   * El backend los acepta con mayor libertad.
   */
  if (movementType === "TRANSFER" || movementType === "ADJUSTMENT") {
    return true;
  }

  return false;
}

export function isCategoryCompatibleWithMovement(
  category: Category,
  movementType: MovementType,
  options?: CategoryCompatibilityOptions,
) {
  if (!options?.includeInactive && !category.active) return false;
  if (!options?.includeTechnical && category.technical) return false;

  return isCategoryTypeCompatibleWithMovement(
    category.type,
    movementType,
    options,
  );
}

export function getCompatibleCategories(
  categories: Category[],
  movementType: MovementType,
  options?: CategoryCompatibilityOptions,
) {
  return categories.filter((category) =>
    isCategoryCompatibleWithMovement(category, movementType, options),
  );
}

export function getCategoryDisplayName(category?: Category | null) {
  if (!category) return "Sin categoría";
  if (category.technical) return `${category.name} · técnica`;
  return category.name;
}

export function isTransactionConfirmed(
  transaction: Pick<MoneyTransaction, "status">,
) {
  return transaction.status === "CONFIRMED";
}

export function isTransactionIgnored(
  transaction: Pick<MoneyTransaction, "status">,
) {
  return transaction.status === "IGNORED";
}

export function isTransactionPending(
  transaction: Pick<MoneyTransaction, "status">,
) {
  return transaction.status === "PENDING";
}

export function isTransactionTechnical(
  transaction: Pick<MoneyTransaction, "classificationStatus">,
) {
  return transaction.classificationStatus === "TECHNICAL";
}

export function isTransactionWithoutCategory(
  transaction: Pick<MoneyTransaction, "categoryId" | "classificationStatus">,
) {
  return (
    !transaction.categoryId ||
    transaction.classificationStatus === "NEEDS_CATEGORY"
  );
}

export function shouldCountTransactionInOperationalBalance(
  transaction: Pick<
    MoneyTransaction,
    "status" | "movementType" | "classificationStatus"
  >,
) {
  if (transaction.status === "IGNORED") return false;
  if (transaction.classificationStatus === "TECHNICAL") return false;
  if (transaction.classificationStatus === "IGNORED_BY_RULE") return false;
  if (transaction.movementType === "TRANSFER") return false;
  if (transaction.movementType === "ADJUSTMENT") return false;

  return true;
}

export function getSignedOperationalAmount(
  transaction: Pick<
    MoneyTransaction,
    "amount" | "status" | "movementType" | "classificationStatus"
  >,
) {
  if (!shouldCountTransactionInOperationalBalance(transaction)) return 0;

  const amount = Number(transaction.amount ?? 0);

  if (transaction.movementType === "INCOME") return amount;
  if (transaction.movementType === "EXPENSE") return -amount;
  if (transaction.movementType === "SAVING") return -amount;

  return 0;
}

export function getDefaultClassificationStatus(
  transaction: Pick<MoneyTransaction, "categoryId" | "classificationStatus">,
): TransactionClassificationStatus {
  if (transaction.classificationStatus) return transaction.classificationStatus;
  if (!transaction.categoryId) return "NEEDS_CATEGORY";

  return "CLASSIFIED";
}

export function getDefaultTransactionStatus(
  status: TransactionStatus | null | undefined,
): TransactionStatus {
  return status ?? "CONFIRMED";
}
