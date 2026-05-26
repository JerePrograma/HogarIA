import { MetricCard } from "../../../shared/ui/MetricCard";
import { formatMoney } from "../../../domain/formatters";
import type { BudgetComparison } from "../../../api/budgetsApi";

type Props = {
  comparison?: BudgetComparison;
};

export function BudgetSummaryMetrics({ comparison }: Props) {
  const totalDifference = comparison?.totalDifference ?? 0;

  return (
    <section className="metric-grid">
      <MetricCard
        title="Presupuesto total"
        value={formatMoney(comparison?.totalBudget)}
        helper="Suma presupuestada del período."
        tone="info"
      />

      <MetricCard
        title="Real total"
        value={formatMoney(comparison?.totalReal)}
        helper="Movimientos confirmados del período."
        tone="warning"
      />

      <MetricCard
        title={totalDifference >= 0 ? "Disponible" : "Excedido"}
        value={formatMoney(Math.abs(totalDifference))}
        helper="Diferencia entre presupuesto y realidad."
        tone={totalDifference >= 0 ? "success" : "danger"}
      />
    </section>
  );
}