import { useMutation, useQueryClient } from "@tanstack/react-query";
import {
  bulkCategorizeTransactions,
  bulkIgnoreTransactions,
  bulkStatusTransactions,
  createTransaction,
  deleteTransaction,
  updateTransaction,
} from "../../../api/transactionsApi";
import { queryKeys } from "../../../domain/queryKeys";
import type { MoneyTransaction } from "../../../domain/types";
import type { TransactionForm } from "../types";
import { toTransactionUpdatePayload } from "../utils/transactionUtils";

export function useTransactionMutations(
  profileId: string,
  year: number,
  month: number,
) {
  const queryClient = useQueryClient();

  const invalidateTransactionDependents = async () => {
    await Promise.all([
      queryClient.invalidateQueries({
        queryKey: queryKeys.transactions(profileId, year, month),
      }),
      queryClient.invalidateQueries({
        queryKey: queryKeys.transactions(profileId),
      }),
      queryClient.invalidateQueries({
        queryKey: queryKeys.budgetComparison(profileId, year, month),
      }),
      queryClient.invalidateQueries({
        queryKey: queryKeys.budgetComparison(profileId),
      }),
      queryClient.invalidateQueries({
        queryKey: queryKeys.dashboard(profileId),
      }),
      queryClient.invalidateQueries({
        queryKey: queryKeys.planning(profileId, year, month),
      }),
      queryClient.invalidateQueries({
        queryKey: queryKeys.monthlyPlanItems(profileId, year, month),
      }),
    ]);
  };

  const createTransactionMutation = useMutation({
    mutationFn: (form: TransactionForm) =>
      createTransaction({
        ...form,
        profileId,
        categoryId: form.categoryId || null,
        amount: Number(form.amount),
        origin: "MANUAL",
      }),
    onSuccess: invalidateTransactionDependents,
  });

  const updateTransactionMutation = useMutation({
    mutationFn: (transaction: MoneyTransaction) =>
      updateTransaction(
        transaction.id,
        toTransactionUpdatePayload(transaction),
      ),
    onSuccess: invalidateTransactionDependents,
  });

  const deleteTransactionMutation = useMutation({
    mutationFn: (transaction: MoneyTransaction) =>
      deleteTransaction(transaction.id),
    onSuccess: invalidateTransactionDependents,
  });

  const bulkCategorizeMutation = useMutation({
    mutationFn: ({
      transactionIds,
      categoryId,
    }: {
      transactionIds: string[];
      categoryId: string;
    }) => bulkCategorizeTransactions(profileId, transactionIds, categoryId),
    onSuccess: invalidateTransactionDependents,
  });

  const bulkStatusMutation = useMutation({
    mutationFn: ({
      transactionIds,
      status,
      reason,
    }: {
      transactionIds: string[];
      status: "CONFIRMED" | "PENDING";
      reason?: string;
    }) => bulkStatusTransactions(profileId, transactionIds, status, reason),
    onSuccess: invalidateTransactionDependents,
  });

  const bulkIgnoreMutation = useMutation({
    mutationFn: ({
      transactionIds,
      reason,
    }: {
      transactionIds: string[];
      reason?: string;
    }) => bulkIgnoreTransactions(profileId, transactionIds, reason),
    onSuccess: invalidateTransactionDependents,
  });

  return {
    createTransactionMutation,
    updateTransactionMutation,
    deleteTransactionMutation,
    bulkCategorizeMutation,
    bulkStatusMutation,
    bulkIgnoreMutation,
  };
}
