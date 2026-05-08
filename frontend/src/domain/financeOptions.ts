// src/domain/financeOptions.ts

import {
  accountTypeLabels,
  categoryScopeLabels,
  categoryTypeLabels,
  movementTypeLabels,
  profileTypeLabels,
  transactionStatusLabels,
} from './financeLabels';

import type {
  AccountType,
  CategoryScope,
  CategoryType,
  MovementType,
  ProfileType,
  TransactionStatus,
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