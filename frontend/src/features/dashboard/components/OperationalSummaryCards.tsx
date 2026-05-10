import { MetricCard } from '../../../components/ui/MetricCard';
import { StatusBadge } from '../../../components/ui/StatusBadge';
import { formatMoney } from '../../../domain/formatters';
import type { DashboardSummary, FinancialRiskLevel } from '../../../domain/types';

const riskLabels: Record<FinancialRiskLevel, string> = { OK: 'Correcto', WATCH: 'Atención', RISK: 'Riesgo', CRITICAL: 'Crítico' };
const tone: Record<FinancialRiskLevel, 'ok' | 'watch' | 'risk' | 'critical'> = { OK: 'ok', WATCH: 'watch', RISK: 'risk', CRITICAL: 'critical' };

type Props = { summary: NonNullable<DashboardSummary['operationalSummary']> };
export function OperationalSummaryCards({ summary }: Props) {
  return <section><h3 className='section-title'>Estado operativo del mes</h3><div className='metric-grid'><MetricCard title='Neto proyectado' primary value={`${formatMoney(summary.projectedNetMin)} – ${formatMoney(summary.projectedNetMax)}`} /><MetricCard title='Saldo confirmado' value={formatMoney(summary.confirmedBalance)} /><MetricCard title='Pendiente de cobro' value={formatMoney(summary.pendingIncome)} /><MetricCard title='Pendiente de pago' value={formatMoney(summary.pendingExpense)} /><MetricCard title='Recuperos esperados' value={`${formatMoney(summary.expectedRecoveriesMin)} – ${formatMoney(summary.expectedRecoveriesMax)}`} /><MetricCard title='Riesgo financiero' value={<StatusBadge tone={tone[summary.financialRiskLevel]} label={riskLabels[summary.financialRiskLevel]} />} /></div></section>;
}
