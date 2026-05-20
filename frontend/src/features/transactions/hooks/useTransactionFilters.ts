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
import { getDefaultClassificationStatus } from "../../../domain/transactionRules";
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

    return transactions
      .filter((transaction) => {
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
      })
      .sort((a, b) => {
        const byDate = b.realDate.localeCompare(a.realDate);
        if (byDate !== 0) return byDate;

        return (b.createdAt ?? "").localeCompare(a.createdAt ?? "");
      });
  }, [accountsById, categoriesById, filters, transactions]);

  const filteredTotal = useMemo(
    () =>
      filteredTransactions.reduce((acc, transaction) => {
        if (transaction.status === "IGNORED") return acc;

        if (transaction.movementType === "INCOME") {
          return acc + Number(transaction.amount ?? 0);
        }

        if (
          transaction.movementType === "EXPENSE" ||
          transaction.movementType === "SAVING"
        ) {
          return acc - Number(transaction.amount ?? 0);
        }

        return acc;
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
