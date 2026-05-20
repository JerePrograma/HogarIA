import { MetricCard } from "../../../components/ui/MetricCard";
import { formatMoney } from "../../../domain/formatters";
import type { TransactionTotals } from "../hooks/useTransactionTotals";

interface Props {
  totals: TransactionTotals;
}

export function TransactionMetrics({ totals }: Props) {
  return (
    <section className="metric-grid">
      <MetricCard
        title="Ingresos"
        value={formatMoney(totals.income)}
        helper="Entradas operativas no ignoradas del período."
        tone="success"
      />

      <MetricCard
        title="Gastos"
        value={formatMoney(totals.expenses)}
        helper="Egresos operativos registrados para el mes."
        tone="danger"
      />

      <MetricCard
        title="Ahorro"
        value={formatMoney(totals.saving)}
        helper="Movimientos destinados a ahorro."
        tone="info"
      />

      <MetricCard
        title="Balance operativo"
        value={formatMoney(totals.operationalBalance)}
        helper="Ingresos menos gastos y ahorro, excluyendo técnicos/transferencias."
        tone={totals.operationalBalance >= 0 ? "success" : "danger"}
      />
    </section>
  );
}
