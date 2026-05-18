import { useQuery } from '@tanstack/react-query';
import { listAccounts } from '../api/accountsApi';
import { listCategories } from '../api/categoriesApi';
import { getMonthlyPlan } from '../api/monthlyPlanningApi';
import { queryKeys } from '../domain/queryKeys';
import { MonthlyPlanSummary } from '../domain/types';

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

  return {
    planningQuery,
    accountsQuery,
    categoriesQuery,
    summary: planningQuery.data,
    items: planningQuery.data?.items ?? [],
    accounts: accountsQuery.data ?? [],
    categories: categoriesQuery.data ?? [],
  };
}