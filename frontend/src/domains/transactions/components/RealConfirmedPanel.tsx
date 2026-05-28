import { MetricCard } from "../../../shared/ui/MetricCard";
import { formatMoney } from "../../../domain/formatters";
import type { TransactionTotals } from "../hooks/useTransactionTotals";

type Props = {
  totals: TransactionTotals;
};

export function RealConfirmedPanel({ totals }: Props) {
  const technicalReading =
    totals.technicalCount +
    totals.transferCount +
    totals.adjustmentCount +
    totals.ignoredCount;

  return (
    <section className="panel">
      <div className="section-title">
        <div>
          <p className="eyebrow">Datos reales</p>
          <h2>Confirmado · Semántica financiera</h2>
          <p className="muted">
            El balance principal excluye movimientos ignorados, técnicos,
            transferencias internas y ajustes no operativos.
          </p>
        </div>
      </div>

      <div className="metric-grid">
        <MetricCard
          title="Ingresos reales confirmados"
          value={formatMoney(totals.income)}
          helper="Solo movimientos confirmados que representan ingreso operativo."
          tone="success"
        />

        <MetricCard
          title="Gastos reales confirmados"
          value={formatMoney(totals.expenses)}
          helper="Solo egresos operativos confirmados del período."
          tone="danger"
        />

        <MetricCard
          title="Ahorro real confirmado"
          value={formatMoney(totals.saving)}
          helper="Ahorro confirmado separado del gasto de consumo."
          tone="info"
        />

        <MetricCard
          title="Balance operativo"
          value={formatMoney(totals.operationalBalance)}
          helper="Ingresos reales menos gastos reales y ahorro real."
          primary
          tone={totals.operationalBalance >= 0 ? "success" : "danger"}
        />
      </div>

      <div className="metric-grid mt-4">
        <MetricCard
          title="Movimientos confirmados"
          value={totals.confirmedCount}
          helper="Impactan en la lectura real del período."
          tone="success"
        />

        <MetricCard
          title="Pendientes"
          value={totals.pendingCount}
          helper="No impactan en el resultado confirmado."
          tone={totals.pendingCount > 0 ? "warning" : "neutral"}
        />

        <MetricCard
          title="Sin categoría"
          value={totals.withoutCategoryCount}
          helper="Necesitan clasificación para mejorar reportes y desvíos."
          tone={totals.withoutCategoryCount > 0 ? "warning" : "neutral"}
        />

        <MetricCard
          title="En revisión"
          value={totals.reviewCount}
          helper="Requieren validación antes de cerrar el período."
          tone={totals.reviewCount > 0 ? "warning" : "neutral"}
        />

        <MetricCard
          title="Ignorados"
          value={totals.ignoredCount}
          helper={`Monto ignorado: ${formatMoney(totals.ignored)}.`}
          tone={totals.ignoredCount > 0 ? "warning" : "neutral"}
        />

        <MetricCard
          title="Técnicos / transferencias / ajustes"
          value={technicalReading}
          helper={`Lectura secundaria: ${formatMoney(totals.nonOperational)} no operativo.`}
          tone={technicalReading > 0 ? "info" : "neutral"}
        />

        <MetricCard
          title="Transferencias internas excluidas"
          value={formatMoney(totals.excludedInternalTransfers)}
          helper="No suman como gasto ni ingreso real."
          tone={totals.excludedInternalTransfers > 0 ? "info" : "neutral"}
        />

        <MetricCard
          title="Duplicados excluidos"
          value={formatMoney(totals.excludedDuplicates)}
          helper="Cross-source o exactos, fuera del balance operativo."
          tone={totals.excludedDuplicates > 0 ? "warning" : "neutral"}
        />

        <MetricCard
          title="Monto en revisión"
          value={formatMoney(totals.reviewAmount)}
          helper="Pendiente de decisión antes de sumarlo como real definitivo."
          tone={totals.reviewAmount > 0 ? "warning" : "neutral"}
        />
      </div>
    </section>
  );
}
