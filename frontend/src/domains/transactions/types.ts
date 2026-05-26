import type {
  MovementType,
  TransactionClassificationStatus,
  TransactionStatus,
} from "../../domain/types";

export const ALL = "ALL" as const;
export type AllOption = typeof ALL;

export const WITHOUT_CATEGORY = "__WITHOUT_CATEGORY__" as const;
export type WithoutCategoryOption = typeof WITHOUT_CATEGORY;

export interface TransactionForm {
  accountId: string;
  categoryId: string;
  movementType: MovementType;
  realDate: string;
  budgetDate: string;
  amount: number;
  currency: string;
  description: string;
  status: TransactionStatus;
}

export interface TransactionFilters {
  search: string;
  accountId: string | AllOption;
  categoryId: string | AllOption | WithoutCategoryOption;
  movementType: MovementType | AllOption;
  status: TransactionStatus | AllOption;
  classificationStatus: TransactionClassificationStatus | AllOption;
}
