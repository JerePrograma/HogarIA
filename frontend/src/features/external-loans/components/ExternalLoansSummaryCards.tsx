import { MetricCard } from '../../../components/ui/MetricCard';
import { formatMoney } from '../../../domain/formatters';
import type { ExternalLoanDashboard } from '../types';

type Props = { dashboard: ExternalLoanDashboard };

export function ExternalLoansSummaryCards({ dashboard }: Props) {
  return (
    <section className='metric-grid'>
      <MetricCard title='Capital invertido' value={formatMoney(dashboard.investedAmount)} primary />
      <MetricCard title='Deuda total' value={formatMoney(dashboard.totalDebt)} />
      <MetricCard title='Ganancia proyectada' value={formatMoney(dashboard.amountToEarn)} />
      <MetricCard title='Préstamos activos' value={dashboard.activeLoans} />
    </section>
  );
}
