import { useMemo, useState } from 'react';
import type {
  Account,
  Category,
  MoneyTransaction,
  MovementType,
  TransactionStatus,
} from '../../../domain/types';

export type AllOption = 'ALL';

export interface TransactionFilters {
  search: string;
  accountId: string | AllOption;
  categoryId: string | AllOption;
  movementType: MovementType | AllOption;
  status: TransactionStatus | AllOption;
}

const ALL: AllOption = 'ALL';

function normalizeSearch(value: string) {
  return value.trim().toLowerCase();
}

export function useTransactionFilters(
  transactions: MoneyTransaction[],
  accountsById: Map<string, Account>,
  categoriesById: Map<string, Category>,
) {
  const [filters, setFilters] = useState<TransactionFilters>({
    search: '',
    accountId: ALL,
    categoryId: ALL,
    movementType: ALL,
    status: ALL,
  });

  const filteredTransactions = useMemo(() => {
    const search = normalizeSearch(filters.search);

    return transactions
      .filter((transaction) => {
        const accountName = accountsById.get(transaction.accountId)?.name ?? '';
        const categoryName = transaction.categoryId
          ? categoriesById.get(transaction.categoryId)?.name ?? ''
          : 'sin categoría';

        const matchesSearch =
          !search
          || normalizeSearch(transaction.description ?? '').includes(search)
          || normalizeSearch(accountName).includes(search)
          || normalizeSearch(categoryName).includes(search)
          || normalizeSearch(transaction.currency).includes(search)
          || normalizeSearch(transaction.paymentChannel ?? '').includes(search)
          || normalizeSearch(transaction.classificationStatus ?? '').includes(search);

        const matchesAccount =
          filters.accountId === ALL || transaction.accountId === filters.accountId;

        const matchesCategory =
          filters.categoryId === ALL
          || (filters.categoryId === '__WITHOUT_CATEGORY__' && !transaction.categoryId)
          || transaction.categoryId === filters.categoryId;

        const matchesMovement =
          filters.movementType === ALL || transaction.movementType === filters.movementType;

        const matchesStatus =
          filters.status === ALL || transaction.status === filters.status;

        return matchesSearch
          && matchesAccount
          && matchesCategory
          && matchesMovement
          && matchesStatus;
      })
      .sort((a, b) => {
        const byDate = b.realDate.localeCompare(a.realDate);
        if (byDate !== 0) return byDate;

        return (b.createdAt ?? '').localeCompare(a.createdAt ?? '');
      });
  }, [accountsById, categoriesById, filters, transactions]);

  const activeFilterCount = [
    filters.search,
    filters.accountId !== ALL,
    filters.categoryId !== ALL,
    filters.movementType !== ALL,
    filters.status !== ALL,
  ].filter(Boolean).length;

  const resetFilters = () => {
    setFilters({
      search: '',
      accountId: ALL,
      categoryId: ALL,
      movementType: ALL,
      status: ALL,
    });
  };

  return {
    allValue: ALL,
    filters,
    setFilters,
    filteredTransactions,
    activeFilterCount,
    resetFilters,
  };
}