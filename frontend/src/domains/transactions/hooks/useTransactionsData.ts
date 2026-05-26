import { useMemo } from "react";
import { useQuery } from "@tanstack/react-query";
import { listAccounts } from "../../../api/accountsApi";
import { listCategories } from "../../../api/categoriesApi";
import { listTransactions } from "../../../api/transactionsApi";
import { queryKeys } from "../../../domain/queryKeys";
import type {
  Account,
  Category,
  MoneyTransaction,
} from "../../../domain/types";

export function useTransactionsData(
  profileId: string,
  year: number,
  month: number,
) {
  const accountsQuery = useQuery<Account[]>({
    queryKey: queryKeys.accounts(profileId),
    queryFn: () => listAccounts(profileId),
    enabled: Boolean(profileId),
  });

  const categoriesQuery = useQuery<Category[]>({
    queryKey: queryKeys.categories(profileId, true),
    queryFn: () => listCategories(profileId, true),
    enabled: Boolean(profileId),
  });

  const transactionsQuery = useQuery<MoneyTransaction[]>({
    queryKey: queryKeys.transactions(profileId, year, month),
    queryFn: () => listTransactions(profileId, year, month),
    enabled: Boolean(profileId),
  });

  const accounts = accountsQuery.data ?? [];
  const categories = categoriesQuery.data ?? [];
  const transactions = transactionsQuery.data ?? [];

  const accountsById = useMemo(
    () => new Map(accounts.map((account) => [account.id, account])),
    [accounts],
  );

  const categoriesById = useMemo(
    () => new Map(categories.map((category) => [category.id, category])),
    [categories],
  );

  return {
    accountsQuery,
    categoriesQuery,
    transactionsQuery,
    accounts,
    categories,
    transactions,
    accountsById,
    categoriesById,
  };
}
