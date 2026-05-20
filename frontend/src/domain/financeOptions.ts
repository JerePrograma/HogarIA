// src/domain/financeOptions.ts

import type {
  AccountType,
  CategoryScope,
  CategoryType,
  GoalStatus,
  GoalType,
  HabitFrequency,
  MonthlyPlanItemType,
  MonthlyPlanPriority,
  MonthlyPlanSource,
  MonthlyPlanStatus,
  MovementType,
  PaymentChannel,
  ProfileType,
  TransactionClassificationStatus,
  TransactionOrigin,
  TransactionStatus,
} from "./types";

import {
  accountTypeLabels,
  categoryScopeLabels,
  categoryTypeLabels,
  classificationStatusLabels,
  goalStatusLabels,
  goalTypeLabels,
  habitFrequencyLabels,
  monthlyPlanPriorityLabels,
  monthlyPlanSourceLabels,
  monthlyPlanStatusLabels,
  monthlyPlanTypeLabels,
  movementTypeLabels,
  paymentChannelLabels,
  profileTypeLabels,
  transactionOriginLabels,
  transactionStatusLabels,
} from "./financeLabels";

export interface SelectOption<T extends string = string> {
  value: T;
  label: string;
}

export function toOptions<T extends string>(
  labels: Record<T, string>,
): SelectOption<T>[] {
  return Object.entries(labels).map(([value, label]) => ({
    value: value as T,
    label: String(label),
  }));
}

// ============================================================
// Core
// ============================================================

export const profileTypeOptions = toOptions<ProfileType>(profileTypeLabels);

export const accountTypeOptions = toOptions<AccountType>(accountTypeLabels);

export const categoryTypeOptions = toOptions<CategoryType>(categoryTypeLabels);

export const categoryScopeOptions =
  toOptions<CategoryScope>(categoryScopeLabels);

// ============================================================
// Transactions
// ============================================================

export const movementTypeOptions = toOptions<MovementType>(movementTypeLabels);

export const transactionStatusOptions = toOptions<TransactionStatus>(
  transactionStatusLabels,
);

export const transactionOriginOptions = toOptions<TransactionOrigin>(
  transactionOriginLabels,
);

export const paymentChannelOptions =
  toOptions<PaymentChannel>(paymentChannelLabels);

export const classificationStatusOptions =
  toOptions<TransactionClassificationStatus>(classificationStatusLabels);

// ============================================================
// Goals / habits
// ============================================================

export const goalTypeOptions = toOptions<GoalType>(goalTypeLabels);

export const goalStatusOptions = toOptions<GoalStatus>(goalStatusLabels);

export const habitFrequencyOptions =
  toOptions<HabitFrequency>(habitFrequencyLabels);

// ============================================================
// Monthly planning
// ============================================================

export const monthlyPlanTypeOptions = toOptions<MonthlyPlanItemType>(
  monthlyPlanTypeLabels,
);

export const monthlyPlanPriorityOptions = toOptions<MonthlyPlanPriority>(
  monthlyPlanPriorityLabels,
);

export const monthlyPlanStatusOptions = toOptions<MonthlyPlanStatus>(
  monthlyPlanStatusLabels,
);

export const monthlyPlanSourceOptions = toOptions<MonthlyPlanSource>(
  monthlyPlanSourceLabels,
);

// ============================================================
// Generic
// ============================================================

export const currencyOptions: SelectOption<string>[] = [
  { value: "ARS", label: "ARS" },
  { value: "USD", label: "USD" },
];
