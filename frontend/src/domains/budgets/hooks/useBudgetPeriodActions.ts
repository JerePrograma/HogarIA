import { useMutation, useQueryClient } from "@tanstack/react-query";
import {
  createBudgetYear,
  createBudgetMonth,
  upsertBudgetCategoryItem,
} from "../../../api/budgetsApi";
import { budgetQueryKeys } from "../budgetQueryKeys";
import { isBudgetYearAlreadyExistsError } from "../budgetErrors";

type SaveBudgetItemVariables = {
  categoryId: string;
  budgetAmount: number;
};

type UseBudgetPeriodActionsArgs = {
  profileId: string;
  year: number;
  month: number;
  budgetMonthId?: string;
};

export function useBudgetPeriodActions({
  profileId,
  year,
  month,
  budgetMonthId,
}: UseBudgetPeriodActionsArgs) {
  const queryClient = useQueryClient();

  const invalidateYear = (targetYear = year) => {
    queryClient.invalidateQueries({
      queryKey: budgetQueryKeys.year(profileId, targetYear),
    });
  };

  const invalidateMonth = (targetYear = year, targetMonth = month) => {
    queryClient.invalidateQueries({
      queryKey: budgetQueryKeys.month(profileId, targetYear, targetMonth),
    });
  };

  const invalidateComparison = (targetYear = year, targetMonth = month) => {
    queryClient.invalidateQueries({
      queryKey: budgetQueryKeys.comparison(profileId, targetYear, targetMonth),
    });
  };

  const preparePeriodMutation = useMutation({
    mutationFn: async () => {
      try {
        await createBudgetYear(profileId, { year });
      } catch (error) {
        if (!isBudgetYearAlreadyExistsError(error)) {
          throw error;
        }
      }

      return createBudgetMonth(profileId, year, { month });
    },

    onSuccess: () => {
      invalidateYear();
      invalidateMonth();
      invalidateComparison();
    },
  });

  const saveItemMutation = useMutation({
    mutationFn: ({ categoryId, budgetAmount }: SaveBudgetItemVariables) => {
      if (!budgetMonthId) {
        throw new Error("Primero tenés que crear el mes presupuestario.");
      }

      return upsertBudgetCategoryItem(budgetMonthId, {
        categoryId,
        budgetAmount,
      });
    },

    onSuccess: () => {
      invalidateMonth();
      invalidateComparison();
    },
  });

  return {
    preparePeriodMutation,
    saveItemMutation,
  };
}
