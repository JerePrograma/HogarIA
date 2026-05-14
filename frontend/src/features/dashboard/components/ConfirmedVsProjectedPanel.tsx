import { MetricCard } from '../../../components/ui/MetricCard';
import { formatMoney } from '../../../domain/formatters';
import type { DashboardSummary } from '../../../domain/types';

type Props = {
  planning: NonNullable<DashboardSummary['planningSummary']>;
  operational: NonNullable<DashboardSummary['operationalSummary']>;
};

export function ConfirmedVsProjectedPanel({ planning, operational }: Props) {
  return (
    <section className="grid">
      <article className="panel">
        <div className="section-title">
          <div>
            <p className="eyebrow">Datos reales</p>
            <h2>Confirmado</h2>
          </div>
        </div>

        <div className="grid">
          <MetricCard
            title="Ingresos"
            value={formatMoney(operational.confirmedIncome)}
            tone="success"
          />

          <MetricCard
            title="Egresos"
            value={formatMoney(operational.confirmedExpenses)}
            tone="danger"
          />

          <MetricCard
            title="Ahorro"
            value={formatMoney(operational.confirmedSavings)}
            tone="info"
          />
        </div>
      </article>

      <article className="panel">
        <div className="section-title">
          <div>
            <p className="eyebrow">Escenario esperado</p>
            <h2>Proyectado</h2>
          </div>
        </div>

        <div className="grid">
          <MetricCard
            title="Ingresos estimados"
            value={`${formatMoney(planning.totalIncomeMin)} – ${formatMoney(planning.totalIncomeMax)}`}
            tone="success"
          />

          <MetricCard
            title="Egresos estimados"
            value={`${formatMoney(planning.totalExpenseMin)} – ${formatMoney(planning.totalExpenseMax)}`}
            tone="danger"
          />

          <MetricCard
            title="Neto proyectado"
            value={`${formatMoney(planning.projectedNetMin)} – ${formatMoney(planning.projectedNetMax)}`}
            tone={planning.projectedNetMin >= 0 ? 'success' : 'danger'}
          />

          <MetricCard
            title="Diferencia vs saldo confirmado"
            value={`${formatMoney(operational.deltaProjectedMinVsConfirmed)} – ${formatMoney(operational.deltaProjectedMaxVsConfirmed)}`}
            tone={operational.deltaProjectedMinVsConfirmed >= 0 ? 'info' : 'warning'}
          />
        </div>
      </article>
    </section>
  );
}