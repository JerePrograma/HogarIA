import { MetricCard } from '../../../components/ui/MetricCard';
import { formatMoney } from '../../../domain/formatters';
import type { ExternalLoanDashboard } from '../types';

type Props = {
  dashboard: ExternalLoanDashboard;
};

export function ExternalLoansSummaryCards({ dashboard }: Props) {
  return (
    <section>
      <div className="section-title">
        <div>
          <p className="eyebrow">Cartera</p>
          <h2>Resumen de préstamos</h2>
        </div>
      </div>

      <div className="metric-grid">
        <MetricCard
          title="Capital invertido"
          value={formatMoney(dashboard.investedAmount)}
          helper="Monto total colocado en préstamos activos o históricos."
          primary
          tone="info"
        />

        <MetricCard
          title="Deuda total"
          value={formatMoney(dashboard.totalDebt)}
          helper="Capital pendiente de devolución."
          tone={dashboard.totalDebt > 0 ? 'warning' : 'success'}
        />

        <MetricCard
          title="Ganancia realizada"
          value={formatMoney(dashboard.earnedAmount)}
          helper="Intereses ya cobrados."
          tone="success"
        />

        <MetricCard
          title="Ganancia proyectada"
          value={formatMoney(dashboard.amountToEarn)}
          helper="Intereses esperados a futuro."
          tone="success"
        />

        <MetricCard
          title="Préstamos activos"
          value={dashboard.activeLoans}
          helper="Operaciones vigentes."
          tone={dashboard.activeLoans > 0 ? 'info' : 'neutral'}
        />
      </div>
    </section>
  );
}