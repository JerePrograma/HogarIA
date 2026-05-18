import { useMemo } from "react";
import { useQuery } from "@tanstack/react-query";
import {
  getBudgetYear,
  getBudgetMonth,
  getBudgetComparison,
} from "../../../api/budgetsApi";
import { listCategories } from "../../../api/categoriesApi";
import { Category, BudgetCategoryItem } from "../../../domain/types";
import { isNotFound } from "../budgetErrors";
import { budgetQueryKeys } from "../budgetQueryKeys";
import { isBudgetableCategory } from "../budgetRules";

export function useBudgetWorkspace(
  profileId: string,
  year: number,
  month: number,
) {
  const budgetYearQuery = useQuery({
    queryKey: budgetQueryKeys.year(profileId, year),
    queryFn: () => getBudgetYear(profileId, year),
    retry: false,
    enabled: Boolean(profileId),
  });

  const budgetMonthQuery = useQuery({
    queryKey: budgetQueryKeys.month(profileId, year, month),
    queryFn: () => getBudgetMonth(profileId, year, month),
    retry: false,
    enabled: Boolean(profileId),
  });

  const categoriesQuery = useQuery<Category[]>({
    queryKey: budgetQueryKeys.categories(profileId),
    queryFn: () => listCategories(profileId, true),
    enabled: Boolean(profileId),
  });

  const budgetYearMissing =
    budgetYearQuery.isError && isNotFound(budgetYearQuery.error);

  const budgetMonthMissing =
    budgetMonthQuery.isError && isNotFound(budgetMonthQuery.error);

  const canReadComparison = Boolean(profileId && budgetMonthQuery.data?.id);

  const comparisonQuery = useQuery({
    queryKey: budgetQueryKeys.comparison(profileId, year, month),
    queryFn: () => getBudgetComparison(profileId, year, month),
    retry: false,
    enabled: canReadComparison,
  });

  const budgetableCategories = useMemo(
    () => (categoriesQuery.data ?? []).filter(isBudgetableCategory),
    [categoriesQuery.data],
  );

  const budgetAmountByCategoryId = useMemo(() => {
    const map = new Map<string, number>();

    budgetMonthQuery.data?.items?.forEach((item: BudgetCategoryItem) => {
      map.set(item.categoryId, item.budgetAmount ?? 0);
    });

    return map;
  }, [budgetMonthQuery.data?.items]);

  return {
    budgetYearQuery,
    budgetMonthQuery,
    categoriesQuery,
    comparisonQuery,

    budgetYearMissing,
    budgetMonthMissing,

    budgetableCategories,
    budgetAmountByCategoryId,

    hasBudgetYear: Boolean(budgetYearQuery.data),
    hasBudgetMonth: Boolean(budgetMonthQuery.data),
    canEditBudget: Boolean(budgetMonthQuery.data?.id),
    canCompareBudget: Boolean(comparisonQuery.data),
  };
}
