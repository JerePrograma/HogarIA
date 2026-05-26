import { MetricCard } from "../../../shared/ui/MetricCard";
import { formatMoney } from "../../../domain/formatters";
import type { TransactionTotals } from "../hooks/useTransactionTotals";

interface Props {
  totals: TransactionTotals;
}

export function TransactionMetrics({ totals }: Props) {
  return (
    <section className="metric-grid">
      <MetricCard
        title="Ingresos confirmados"
        value={formatMoney(totals.income)}
        helper="Entradas operativas confirmadas del período."
        tone="success"
      />

      <MetricCard
        title="Gastos confirmados"
        value={formatMoney(totals.expenses)}
        helper="Egresos operativos confirmados para el mes."
        tone="danger"
      />

      <MetricCard
        title="Ahorro confirmado"
        value={formatMoney(totals.saving)}
        helper="Movimientos confirmados destinados a ahorro."
        tone="info"
      />

      <MetricCard
        title="Balance operativo"
        value={formatMoney(totals.operationalBalance)}
        helper="Ingresos menos gastos y ahorro, sólo con movimientos confirmados."
        tone={totals.operationalBalance >= 0 ? "success" : "danger"}
      />
    </section>
  );
}
