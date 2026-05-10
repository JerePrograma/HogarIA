// src/domain/financeOptions.ts

import {
  accountTypeLabels,
  categoryScopeLabels,
  categoryTypeLabels,
  movementTypeLabels,
  profileTypeLabels,
  transactionStatusLabels,
  monthlyPlanTypeLabels,
  monthlyPlanPriorityLabels,
  monthlyPlanStatusLabels,
  monthlyPlanSourceLabels,
} from './financeLabels';

import type {
  AccountType,
  CategoryScope,
  CategoryType,
  MovementType,
  ProfileType,
  TransactionStatus,
  MonthlyPlanItemType,
  MonthlyPlanPriority,
  MonthlyPlanStatus,
  MonthlyPlanSource,
} from './types';

function toOptions<T extends string>(labels: Record<T, string>) {
  return Object.entries(labels).map(([value, label]) => ({
    value: value as T,
    label: label as string,
  }));
}

export const profileTypeOptions = toOptions<ProfileType>(profileTypeLabels);
export const accountTypeOptions = toOptions<AccountType>(accountTypeLabels);
export const categoryTypeOptions = toOptions<CategoryType>(categoryTypeLabels);
export const categoryScopeOptions = toOptions<CategoryScope>(categoryScopeLabels);
export const movementTypeOptions = toOptions<MovementType>(movementTypeLabels);
export const transactionStatusOptions =
  toOptions<TransactionStatus>(transactionStatusLabels);
export const monthlyPlanTypeOptions = toOptions<MonthlyPlanItemType>(monthlyPlanTypeLabels);
export const monthlyPlanPriorityOptions = toOptions<MonthlyPlanPriority>(monthlyPlanPriorityLabels);
export const monthlyPlanStatusOptions = toOptions<MonthlyPlanStatus>(monthlyPlanStatusLabels);
export const monthlyPlanSourceOptions = toOptions<MonthlyPlanSource>(monthlyPlanSourceLabels);
