import { MetricCard } from '../../../components/ui/MetricCard';
import { formatMoney, formatNumber, formatPercent } from '../../../domain/formatters';
import type { ExternalLoanCashControl } from '../types';

type Props = { cashControl: ExternalLoanCashControl };

export function ExternalLoansCashControlCards({ cashControl }: Props) {
  return (
    <section className='metric-grid'>
      <MetricCard title='Caja disponible' value={formatMoney(cashControl.availableCash)} />
      <MetricCard title='Capital recuperado' value={formatMoney(cashControl.recoveredCapital)} />
      <MetricCard title='Capital pendiente' value={formatMoney(cashControl.pendingCapital)} />
      <MetricCard title='Ganancia realizada' value={formatMoney(cashControl.realizedProfit)} />
      <MetricCard title='Ganancia proyectada' value={formatMoney(cashControl.projectedProfit)} />
      <MetricCard title='Cuotas pendientes' value={formatNumber(cashControl.pendingInstallments)} />
      <MetricCard title='Cartera en mora' value={formatMoney(cashControl.overduePortfolio)} />
      <MetricCard title='Cobro proyectado a 30 días' value={formatMoney(cashControl.projectedCollection30Days)} />
      <MetricCard title='Cobro proyectado a 60 días' value={formatMoney(cashControl.projectedCollection60Days)} />
      <MetricCard title='Cobro proyectado a 90 días' value={formatMoney(cashControl.projectedCollection90Days)} />
      <MetricCard title='Recupero de capital' value={formatPercent(cashControl.capitalRecoveryPercentage)} />
      <MetricCard title='Rendimiento esperado' value={formatPercent(cashControl.expectedYieldPercentage)} />
    </section>
  );
}
