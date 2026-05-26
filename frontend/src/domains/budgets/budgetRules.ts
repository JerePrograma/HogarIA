import type { Category, CategoryType } from "../../domain/types";

export type StatusTone = "ok" | "watch" | "critical";

const BUDGETABLE_CATEGORY_TYPES = new Set<CategoryType>([
  "FIXED_EXPENSE",
  "VARIABLE_EXPENSE",
  "SAVING",
  "DEBT",
  "INVESTMENT",
]);

export function isBudgetableCategory(category: Category) {
  return (
    category.active &&
    BUDGETABLE_CATEGORY_TYPES.has(category.type as CategoryType)
  );
}

export function getBudgetComparisonStatusTone(status: string): StatusTone {
  if (status === "EXCEEDED") return "critical";
  if (status === "WARNING") return "watch";
  return "ok";
}
