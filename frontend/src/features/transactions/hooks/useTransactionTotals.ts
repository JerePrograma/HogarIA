import { useMemo } from "react";
import type { MoneyTransaction } from "../../../domain/types";
import {
  getDefaultClassificationStatus,
  getSignedOperationalAmount,
  isTransactionTechnical,
  isTransactionWithoutCategory,
  shouldCountTransactionInOperationalBalance,
} from "../../../domain/transactionRules";

export interface TransactionTotals {
  income: number;
  expenses: number;
  saving: number;
  operationalBalance: number;

  ignored: number;
  transfers: number;
  adjustments: number;
  technical: number;
  nonOperational: number;

  confirmedCount: number;
  pendingCount: number;
  ignoredCount: number;
  withoutCategoryCount: number;
  reviewCount: number;
  technicalCount: number;
}

function calculateTransactionTotals(
  transactions: MoneyTransaction[],
): TransactionTotals {
  return transactions.reduce<TransactionTotals>(
    (acc, transaction) => {
      const amount = Number(transaction.amount ?? 0);
      const classificationStatus = getDefaultClassificationStatus(transaction);

      if (transaction.status === "CONFIRMED") {
        acc.confirmedCount += 1;
      }

      if (transaction.status === "PENDING") {
        acc.pendingCount += 1;
      }

      if (transaction.status === "IGNORED") {
        acc.ignored += amount;
        acc.ignoredCount += 1;
        return acc;
      }

      if (isTransactionWithoutCategory(transaction)) {
        acc.withoutCategoryCount += 1;
      }

      if (classificationStatus === "REVIEW") {
        acc.reviewCount += 1;
      }

      if (isTransactionTechnical(transaction)) {
        acc.technical += amount;
        acc.technicalCount += 1;
      }

      if (transaction.movementType === "TRANSFER") {
        acc.transfers += amount;
      }

      if (transaction.movementType === "ADJUSTMENT") {
        acc.adjustments += amount;
      }

      if (!shouldCountTransactionInOperationalBalance(transaction)) {
        acc.nonOperational += amount;
        return acc;
      }

      if (transaction.movementType === "INCOME") {
        acc.income += amount;
      }

      if (transaction.movementType === "EXPENSE") {
        acc.expenses += amount;
      }

      if (transaction.movementType === "SAVING") {
        acc.saving += amount;
      }

      acc.operationalBalance += getSignedOperationalAmount(transaction);

      return acc;
    },
    {
      income: 0,
      expenses: 0,
      saving: 0,
      operationalBalance: 0,

      ignored: 0,
      transfers: 0,
      adjustments: 0,
      technical: 0,
      nonOperational: 0,

      confirmedCount: 0,
      pendingCount: 0,
      ignoredCount: 0,
      withoutCategoryCount: 0,
      reviewCount: 0,
      technicalCount: 0,
    },
  );
}

export function useTransactionTotals(transactions: MoneyTransaction[]) {
  return useMemo(
    () => calculateTransactionTotals(transactions),
    [transactions],
  );
}
