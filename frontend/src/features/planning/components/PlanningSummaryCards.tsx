import { MetricCard } from '../../../components/ui/MetricCard';
import { formatMoney } from '../../../domain/formatters';
import type { MonthlyPlanReconciliationSummary, MonthlyPlanSummary } from '../../../domain/types';

type Props = {
  summary?: MonthlyPlanSummary;
  reconciliation?: MonthlyPlanReconciliationSummary;
};

export function PlanningSummaryCards({ summary, reconciliation }: Props) {
  const netMin = summary?.netMin ?? 0;
  const netMax = summary?.netMax ?? 0;
  const netTone = netMin >= 0 && netMax >= 0 ? 'success' : 'danger';

  return (
    <section>
      <div className="section-title">
        <div>
          <p className="eyebrow">Resumen</p>
          <h2>Proyección y ejecución mensual</h2>
          <p className="muted">
            Compará lo planificado contra movimientos reales conciliados.
          </p>
        </div>
      </div>

      <div className="metric-grid">
        <MetricCard
          title="Planificado"
          value={formatMoney(reconciliation?.plannedTotal ?? 0)}
          helper="Monto objetivo cargado en planificación."
          tone="info"
        />

        <MetricCard
          title="Ejecutado"
          value={formatMoney(reconciliation?.matchedTotal ?? 0)}
          helper="Movimientos reales vinculados al plan."
          tone="success"
        />

        <MetricCard
          title="Pendiente"
          value={formatMoney(reconciliation?.remainingTotal ?? 0)}
          helper="Saldo planificado todavía no cubierto."
          tone={(reconciliation?.remainingTotal ?? 0) > 0 ? 'warning' : 'success'}
        />

        <MetricCard
          title="No planificado"
          value={formatMoney(reconciliation?.unplannedTransactionsTotal ?? 0)}
          helper="Movimientos reales sin vínculo con planificación."
          tone={(reconciliation?.unplannedTransactionsCount ?? 0) > 0 ? 'danger' : 'neutral'}
        />

        <MetricCard
          title="Neto proyectado"
          primary
          value={formatRange(summary?.netMin, summary?.netMax)}
          helper="Resultado estimado del período."
          tone={netTone}
        />
      </div>
    </section>
  );
}

function formatRange(min?: number, max?: number): string {
  return `${formatMoney(min ?? 0)} – ${formatMoney(max ?? 0)}`;
}