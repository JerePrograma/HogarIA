import { useQuery } from '@tanstack/react-query';
import { listAccounts } from '../api/accountsApi';
import { listCategories } from '../api/categoriesApi';
import { getMonthlyPlan } from '../api/monthlyPlanningApi';
import { listTransactions } from '../api/transactionsApi';
import {
  buildClosingProjection,
  buildFinancialAlerts,
  buildRealConfirmedSummary,
  buildRealVsPlannedSummary,
} from '../domain/financialSemantics';
import { queryKeys } from '../domain/queryKeys';
import { MonthlyPlanSummary, MoneyTransaction } from '../domain/types';

export function useMonthlyPlanningQueries(profileId: string, year: number, month: number) {
  const enabled = Boolean(profileId);

  const planningQuery = useQuery<MonthlyPlanSummary>({
    queryKey: queryKeys.planning(profileId, year, month),
    queryFn: () => getMonthlyPlan(profileId, year, month),
    enabled,
  });

  const accountsQuery = useQuery({
    queryKey: queryKeys.accounts(profileId),
    queryFn: () => listAccounts(profileId),
    enabled,
  });

  const categoriesQuery = useQuery({
    queryKey: queryKeys.categories(profileId, true),
    queryFn: () => listCategories(profileId, true),
    enabled,
  });

  const transactionsQuery = useQuery<MoneyTransaction[]>({
    queryKey: queryKeys.transactions(profileId, year, month),
    queryFn: () => listTransactions(profileId, year, month),
    enabled,
  });

  const transactions = transactionsQuery.data ?? [];
  const items = planningQuery.data?.items ?? [];
  const categories = categoriesQuery.data ?? [];
  const realSummary = buildRealConfirmedSummary(transactions);
  const realVsPlanned = buildRealVsPlannedSummary(transactions, items, categories);
  const closingProjection = buildClosingProjection(transactions, items);
  const financialAlerts = buildFinancialAlerts(
    realSummary,
    realVsPlanned,
    closingProjection,
  );

  return {
    planningQuery,
    accountsQuery,
    categoriesQuery,
    transactionsQuery,
    summary: planningQuery.data,
    items,
    accounts: accountsQuery.data ?? [],
    categories,
    transactions,
    realSummary,
    realVsPlanned,
    closingProjection,
    financialAlerts,
  };
}
