import type { UseMutationResult, UseQueryResult } from "@tanstack/react-query";
import { ErrorState } from "../../../shared/ui/ErrorState";
import {
  getApiErrorMessage,
  isBudgetYearAlreadyExistsError,
} from "../budgetErrors";

type Props = {
  budgetYearQuery: UseQueryResult<unknown>;
  budgetMonthQuery: UseQueryResult<unknown>;
  comparisonQuery: UseQueryResult<unknown>;
  preparePeriodMutation: UseMutationResult<unknown, unknown, void, unknown>;
  budgetYearMissing: boolean;
  budgetMonthMissing: boolean;
};

export function BudgetFeedback({
  budgetYearQuery,
  budgetMonthQuery,
  comparisonQuery,
  preparePeriodMutation,
  budgetYearMissing,
  budgetMonthMissing,
}: Props) {
  return (
    <>
      {budgetYearQuery.isError && !budgetYearMissing ? (
        <ErrorState message="No se pudo consultar el presupuesto anual." />
      ) : null}

      {budgetMonthQuery.isError && !budgetMonthMissing ? (
        <ErrorState message="No se pudo consultar el presupuesto mensual." />
      ) : null}

      {comparisonQuery.isError ? (
        <ErrorState message="No se pudo consultar la comparación presupuesto vs real." />
      ) : null}

      {preparePeriodMutation.isError &&
      !isBudgetYearAlreadyExistsError(preparePeriodMutation.error) ? (
        <p className="mensaje-error">
          {getApiErrorMessage(preparePeriodMutation.error)}
        </p>
      ) : null}
    </>
  );
}
