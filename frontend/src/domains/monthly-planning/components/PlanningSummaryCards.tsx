import { MetricCard } from '../../../shared/ui/MetricCard';
import type { ClosingProjection, RealConfirmedSummary } from '../../../domain/financialSemantics';
import { formatMoney } from '../../../domain/formatters';
import type { MonthlyPlanSummary } from '../../../domain/types';

type Props = {
  summary?: MonthlyPlanSummary;
  realSummary?: RealConfirmedSummary;
  closingProjection?: ClosingProjection;
};

export function PlanningSummaryCards({
  summary,
  realSummary,
  closingProjection,
}: Props) {
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
          title="Neto planificado"
          primary
          value={formatRange(summary?.netMin, summary?.netMax)}
          helper="Resultado esperado antes de cruzar movimientos reales."
          tone={netTone}
        />

        {realSummary ? (
          <MetricCard
            title="Real confirmado"
            value={formatMoney(realSummary.operationalBalance)}
            helper="Balance operativo desde movimientos confirmados."
            tone={realSummary.operationalBalance >= 0 ? 'success' : 'danger'}
          />
        ) : null}

        {closingProjection ? (
          <MetricCard
            title="Estimado de cierre"
            value={formatMoney(closingProjection.estimatedClosing)}
            helper="Real acumulado más pendientes planificados."
            tone={closingProjection.estimatedClosing >= 0 ? 'success' : 'danger'}
          />
        ) : null}
      </div>
    </section>
  );
}

function formatRange(min?: number, max?: number): string {
  return `${formatMoney(min ?? 0)} – ${formatMoney(max ?? 0)}`;
}
