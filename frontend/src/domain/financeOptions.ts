import {
  accountTypeLabels,
  categoryScopeLabels,
  categoryTypeLabels,
  goalTypeLabels,
  habitFrequencyLabels,
  movementTypeLabels,
  profileTypeLabels,
  transactionStatusLabels,
  type GoalType,
  type HabitFrequency,
} from './financeLabels';

import type {
  AccountType,
  CategoryScope,
  CategoryType,
  MovementType,
  ProfileType,
  TransactionStatus,
} from './types';

function toOptions<T extends string | number>(labels: Partial<Record<T, string>>) {
  return Object.entries(labels).map(([value, label]) => ({
    value: value as unknown as T,
    label: String(label),
  }));
}

export const profileTypeOptions = toOptions<ProfileType>(profileTypeLabels);
export const accountTypeOptions = toOptions<AccountType>(accountTypeLabels);
export const categoryTypeOptions = toOptions<CategoryType>(categoryTypeLabels);
export const categoryScopeOptions = toOptions<CategoryScope>(categoryScopeLabels);
export const movementTypeOptions = toOptions<MovementType>(movementTypeLabels);
export const transactionStatusOptions = toOptions<TransactionStatus>(transactionStatusLabels);
export const goalTypeOptions = toOptions<GoalType>(goalTypeLabels);
export const habitFrequencyOptions = toOptions<HabitFrequency>(habitFrequencyLabels);