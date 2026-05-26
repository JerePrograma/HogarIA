import { useMemo } from "react";
import {
  classificationStatusLabels,
  labelOrValue,
  paymentChannelLabels,
} from "../../../domain/financeLabels";
import type {
  Account,
  Category,
  MoneyTransaction,
} from "../../../domain/types";
import { sortTransactionsForLedger } from "../../../domain/sorting";
import {
  getDefaultClassificationStatus,
  getSignedOperationalAmount,
  shouldCountTransactionInOperationalBalance,
} from "../../../domain/transactionRules";
import { normalizeSearch } from "../utils/transactionUtils";
import { ALL, WITHOUT_CATEGORY, type TransactionFilters } from "../types";

export function useTransactionFilters(
  transactions: MoneyTransaction[],
  filters: TransactionFilters,
  accountsById: Map<string, Account>,
  categoriesById: Map<string, Category>,
) {
  const filteredTransactions = useMemo(() => {
    const search = normalizeSearch(filters.search);

    const visibleTransactions = transactions.filter((transaction) => {
        const accountName = accountsById.get(transaction.accountId)?.name ?? "";

        const categoryName = transaction.categoryId
          ? (categoriesById.get(transaction.categoryId)?.name ??
            "Categoría no encontrada")
          : "Sin categoría";

        const classificationStatus =
          getDefaultClassificationStatus(transaction);

        const classificationLabel = labelOrValue(
          classificationStatusLabels,
          classificationStatus,
        );

        const paymentChannelLabel = transaction.paymentChannel
          ? labelOrValue(paymentChannelLabels, transaction.paymentChannel)
          : "";

        const matchesSearch =
          !search ||
          normalizeSearch(transaction.description).includes(search) ||
          normalizeSearch(accountName).includes(search) ||
          normalizeSearch(categoryName).includes(search) ||
          normalizeSearch(transaction.currency).includes(search) ||
          normalizeSearch(transaction.paymentChannel).includes(search) ||
          normalizeSearch(paymentChannelLabel).includes(search) ||
          normalizeSearch(classificationStatus).includes(search) ||
          normalizeSearch(classificationLabel).includes(search) ||
          normalizeSearch(transaction.classificationReason).includes(search);

        const matchesAccount =
          filters.accountId === ALL ||
          transaction.accountId === filters.accountId;

        const matchesCategory =
          filters.categoryId === ALL ||
          (filters.categoryId === WITHOUT_CATEGORY &&
            !transaction.categoryId) ||
          transaction.categoryId === filters.categoryId;

        const matchesMovement =
          filters.movementType === ALL ||
          transaction.movementType === filters.movementType;

        const matchesStatus =
          filters.status === ALL || transaction.status === filters.status;

        const matchesClassification =
          filters.classificationStatus === ALL ||
          classificationStatus === filters.classificationStatus;

        return (
          matchesSearch &&
          matchesAccount &&
          matchesCategory &&
          matchesMovement &&
          matchesStatus &&
          matchesClassification
        );
      });

    return sortTransactionsForLedger(visibleTransactions);
  }, [accountsById, categoriesById, filters, transactions]);

  const filteredTotal = useMemo(
    () =>
      filteredTransactions.reduce((acc, transaction) => {
        if (
          transaction.status !== "CONFIRMED" ||
          !shouldCountTransactionInOperationalBalance(transaction)
        ) {
          return acc;
        }

        return acc + getSignedOperationalAmount(transaction);
      }, 0),
    [filteredTransactions],
  );

  const activeFilterCount = [
    filters.search,
    filters.accountId !== ALL,
    filters.categoryId !== ALL,
    filters.movementType !== ALL,
    filters.status !== ALL,
    filters.classificationStatus !== ALL,
  ].filter(Boolean).length;

  return {
    filteredTransactions,
    filteredTotal,
    activeFilterCount,
  };
}
