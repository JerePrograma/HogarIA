// src/domain/transactionRules.ts

import type {
  BalanceImpact,
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

  if (movementType === "TRANSFER") {
    return (
      categoryType === "SAVING" ||
      categoryType === "INVESTMENT" ||
      categoryType === "VARIABLE_EXPENSE"
    );
  }

  if (movementType === "ADJUSTMENT") {
    return categoryType !== "INCOME";
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
  const label = category.displayPath || category.name;
  if (category.technical) return `${label} · técnica`;
  return label;
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

const OPERATIONAL_BALANCE_IMPACTS = new Set<BalanceImpact>([
  "OPERATING_INCOME",
  "INTEREST_INCOME",
  "CONSUMPTION_EXPENSE",
  "SAVING_OUTFLOW",
  "INVESTMENT_OUTFLOW",
  "DEBT_OUTFLOW",
]);

const NON_OPERATIONAL_BALANCE_IMPACTS = new Set<BalanceImpact>([
  "RECOVERABLE_OUTFLOW",
  "PRINCIPAL_RECOVERY",
  "REFUND_OR_REIMBURSEMENT",
  "INTERNAL_TRANSFER",
  "EXTERNAL_TRANSFER",
  "NEUTRAL_ADJUSTMENT",
  "IGNORED",
  "TECHNICAL",
  "UNKNOWN",
]);

const INTERNAL_TRANSFER_REVIEW_REASONS = [
  "POSSIBLE_INTERNAL_TRANSFER",
  "INTERNAL_TRANSFER_MATCHED",
  "TRANSFER_UNMATCHED",
  "USER_MARKED_INTERNAL_TRANSFER",
];

const DUPLICATE_REVIEW_REASONS = [
  "POSSIBLE_CROSS_SOURCE_DUPLICATE",
  "USER_IGNORED_CROSS_SOURCE",
  "DUPLICATE_RESOLVED_KEEP",
  "EXACT_DUPLICATE",
  "SOURCE_DUPLICATE",
];

type OperationalBalanceTransaction = Pick<
  MoneyTransaction,
  "status" | "movementType" | "classificationStatus"
> &
  Partial<
    Pick<
      MoneyTransaction,
      | "balanceImpact"
      | "paymentChannel"
      | "internalTransferGroupId"
      | "classificationReason"
    >
  >;

export function isInternalTransferReviewReason(reason?: string | null) {
  return includesAnyReason(reason, INTERNAL_TRANSFER_REVIEW_REASONS);
}

export function isDuplicateReviewReason(reason?: string | null) {
  return includesAnyReason(reason, DUPLICATE_REVIEW_REASONS);
}

export function isTransactionInReviewForOperationalBalance(
  transaction: Pick<
    MoneyTransaction,
    "classificationStatus" | "classificationReason" | "balanceImpact"
  >,
) {
  return (
    transaction.classificationStatus === "REVIEW" ||
    isInternalTransferReviewReason(transaction.classificationReason) ||
    isDuplicateReviewReason(transaction.classificationReason)
  );
}

export function shouldCountTransactionInOperationalBalance(
  transaction: OperationalBalanceTransaction,
) {
  if (transaction.status === "IGNORED") return false;
  if (transaction.classificationStatus === "TECHNICAL") return false;
  if (transaction.classificationStatus === "IGNORED_BY_RULE") return false;
  if (transaction.internalTransferGroupId) return false;
  if (transaction.paymentChannel === "INTERNAL_TRANSFER") return false;
  if (isInternalTransferReviewReason(transaction.classificationReason)) {
    return false;
  }
  if (isDuplicateReviewReason(transaction.classificationReason)) {
    return false;
  }

  if (transaction.balanceImpact) {
    if (NON_OPERATIONAL_BALANCE_IMPACTS.has(transaction.balanceImpact)) {
      return false;
    }

    return OPERATIONAL_BALANCE_IMPACTS.has(transaction.balanceImpact);
  }

  if (transaction.classificationStatus === "REVIEW") return false;
  if (transaction.movementType === "TRANSFER") return false;
  if (transaction.movementType === "ADJUSTMENT") return false;

  return true;
}

export function getSignedOperationalAmount(
  transaction: Pick<
    MoneyTransaction,
    | "amount"
    | "status"
    | "movementType"
    | "classificationStatus"
    | "balanceImpact"
    | "paymentChannel"
    | "internalTransferGroupId"
    | "classificationReason"
  >,
) {
  if (!shouldCountTransactionInOperationalBalance(transaction)) return 0;

  const amount = Number(transaction.amount ?? 0);

  if (
    transaction.balanceImpact === "OPERATING_INCOME" ||
    transaction.balanceImpact === "INTEREST_INCOME"
  ) {
    return amount;
  }

  if (
    transaction.balanceImpact === "CONSUMPTION_EXPENSE" ||
    transaction.balanceImpact === "SAVING_OUTFLOW" ||
    transaction.balanceImpact === "INVESTMENT_OUTFLOW" ||
    transaction.balanceImpact === "DEBT_OUTFLOW"
  ) {
    return -amount;
  }

  if (transaction.movementType === "INCOME") return amount;
  if (transaction.movementType === "EXPENSE") return -amount;
  if (transaction.movementType === "SAVING") return -amount;

  return 0;
}

function includesAnyReason(reason: string | null | undefined, needles: string[]) {
  if (!reason) return false;

  const normalized = reason.toUpperCase();
  return needles.some((needle) => normalized.includes(needle));
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
