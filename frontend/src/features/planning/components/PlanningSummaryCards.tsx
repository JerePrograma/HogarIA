import { MetricCard } from '../../../components/ui/MetricCard';
import { formatMoney } from '../../../domain/formatters';
import type { MonthlyPlanSummary } from '../../../domain/types';

type Props = {
  summary?: MonthlyPlanSummary;
};

export function PlanningSummaryCards({ summary }: Props) {
  const netMin = summary?.netMin ?? 0;

  return (
    <section>
      <div className="section-title">
        <div>
          <p className="eyebrow">Resumen</p>
          <h2>Proyección mensual</h2>
        </div>
      </div>

      <div className="metric-grid">
        <MetricCard title="Ingresos estimados" value={`${formatMoney(summary?.totalIncomeMin ?? 0)} – ${formatMoney(summary?.totalIncomeMax ?? 0)}`} helper="Rango de ingresos planificados." tone="success" />
        <MetricCard title="Egresos estimados" value={`${formatMoney(summary?.totalExpenseMin ?? 0)} – ${formatMoney(summary?.totalExpenseMax ?? 0)}`} helper="Rango de pagos o gastos esperados." tone="danger" />
        <MetricCard title="Recuperos esperados" value={`${formatMoney(summary?.totalRecoveryMin ?? 0)} – ${formatMoney(summary?.totalRecoveryMax ?? 0)}`} helper="Reintegros o recuperos previstos." tone="info" />
        <MetricCard title="Neto proyectado" primary value={`${formatMoney(summary?.netMin ?? 0)} – ${formatMoney(summary?.netMax ?? 0)}`} helper="Resultado estimado del período." tone={netMin >= 0 ? 'success' : 'danger'} />
      </div>
    </section>
  );
}
