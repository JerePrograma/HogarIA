import type { UseQueryResult } from "@tanstack/react-query";
import { EmptyState } from "../../../components/ui/EmptyState";
import { ErrorState } from "../../../components/ui/ErrorState";
import type { BudgetComparison } from "../../../api/budgetsApi";
import { BudgetComparisonTable } from "./BudgetComparisonTable";

type Props = {
  budgetMonthExists: boolean;
  comparisonQuery: UseQueryResult<BudgetComparison>;
};

export function BudgetComparisonPanel({
  budgetMonthExists,
  comparisonQuery,
}: Props) {
  return (
    <section className="panel">
      <div className="section-title">
        <div>
          <p className="eyebrow">Ejecución</p>
          <h2>Presupuesto vs real</h2>
          <p className="secondary-text">
            Comparación entre lo presupuestado y los movimientos confirmados del
            período.
          </p>
        </div>
      </div>

      {!budgetMonthExists ? (
        <EmptyState
          title="Sin comparación"
          message="Creá el mes presupuestario para ver la ejecución contra movimientos reales."
        />
      ) : null}

      {budgetMonthExists && comparisonQuery.isLoading ? (
        <p className="secondary-text">Calculando comparación...</p>
      ) : null}

      {comparisonQuery.isError ? (
        <ErrorState message="No se pudo consultar la comparación presupuesto vs real." />
      ) : null}

      {budgetMonthExists &&
      !comparisonQuery.isLoading &&
      !comparisonQuery.isError &&
      !comparisonQuery.data?.items?.length ? (
        <EmptyState
          title="Sin datos para comparar"
          message="Todavía no hay importes presupuestados ni movimientos confirmados para este período."
        />
      ) : null}

      {comparisonQuery.data?.items?.length ? (
        <BudgetComparisonTable items={comparisonQuery.data.items} />
      ) : null}
    </section>
  );
}
