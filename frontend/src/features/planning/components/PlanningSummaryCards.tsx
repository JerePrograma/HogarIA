import { MetricCard } from '../../../components/ui/MetricCard';
import { formatMoney } from '../../../domain/formatters';
import type { MonthlyPlanSummary } from '../../../domain/types';

type Props = {
  summary?: MonthlyPlanSummary;
};

export function PlanningSummaryCards({ summary }: Props) {
  const netMin = summary?.netMin ?? 0;
  const netMax = summary?.netMax ?? 0;
  const netTone = netMin >= 0 && netMax >= 0 ? 'success' : 'danger';

  return (
    <section>
      <div className="section-title">
        <div>
          <p className="eyebrow">Resumen</p>
          <h2>Proyección mensual</h2>
          <p className="muted">
            Lectura rápida del rango planificado antes de confirmar movimientos reales.
          </p>
        </div>
      </div>

      <div className="metric-grid">
        <MetricCard
          title="Ingresos estimados"
          value={formatRange(summary?.totalIncomeMin, summary?.totalIncomeMax)}
          helper="Rango de ingresos planificados."
          tone="success"
        />

        <MetricCard
          title="Egresos estimados"
          value={formatRange(summary?.totalExpenseMin, summary?.totalExpenseMax)}
          helper="Rango de pagos o gastos esperados."
          tone="danger"
        />

        <MetricCard
          title="Recuperos esperados"
          value={formatRange(summary?.totalRecoveryMin, summary?.totalRecoveryMax)}
          helper="Reintegros o recuperos previstos."
          tone="info"
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