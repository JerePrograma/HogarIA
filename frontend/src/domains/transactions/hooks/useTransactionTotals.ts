import { useMemo } from "react";
import { buildRealConfirmedSummary } from "../../../domain/financialSemantics";
import type { MoneyTransaction } from "../../../domain/types";

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
  transferCount: number;
  adjustmentCount: number;
}

function calculateTransactionTotals(
  transactions: MoneyTransaction[],
): TransactionTotals {
  const summary = buildRealConfirmedSummary(transactions);

  return {
    income: summary.confirmedIncome,
    expenses: summary.confirmedExpenses,
    saving: summary.confirmedSavings,
    operationalBalance: summary.operationalBalance,

    ignored: summary.ignoredAmount,
    transfers: summary.transfersAmount,
    adjustments: summary.adjustmentsAmount,
    technical: summary.technicalAmount,
    nonOperational: summary.nonOperationalAmount,

    confirmedCount: summary.confirmedCount,
    pendingCount: summary.pendingCount,
    ignoredCount: summary.ignoredCount,
    withoutCategoryCount: summary.withoutCategoryCount,
    reviewCount: summary.reviewCount,
    technicalCount: summary.technicalCount,
    transferCount: summary.transferCount,
    adjustmentCount: summary.adjustmentCount,
  };
}

export function useTransactionTotals(transactions: MoneyTransaction[]) {
  return useMemo(
    () => calculateTransactionTotals(transactions),
    [transactions],
  );
}
