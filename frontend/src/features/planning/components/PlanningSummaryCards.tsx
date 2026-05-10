import { MetricCard } from '../../../components/ui/MetricCard';
import { formatMoney } from '../../../domain/formatters';
import type { MonthlyPlanSummary } from '../../../domain/types';

type Props = { summary?: MonthlyPlanSummary };
export function PlanningSummaryCards({ summary }: Props) {
  return <section className='metric-grid'><MetricCard title='Ingresos estimados' value={`${formatMoney(summary?.totalIncomeMin ?? 0)} – ${formatMoney(summary?.totalIncomeMax ?? 0)}`} /><MetricCard title='Egresos estimados' value={`${formatMoney(summary?.totalExpenseMin ?? 0)} – ${formatMoney(summary?.totalExpenseMax ?? 0)}`} /><MetricCard title='Recuperos esperados' value={`${formatMoney(summary?.totalRecoveryMin ?? 0)} – ${formatMoney(summary?.totalRecoveryMax ?? 0)}`} /><MetricCard title='Neto proyectado' primary value={`${formatMoney(summary?.netMin ?? 0)} – ${formatMoney(summary?.netMax ?? 0)}`} /><MetricCard title='Pendiente de cobro' value={formatMoney(summary?.pendingIncome ?? 0)} /><MetricCard title='Pendiente de pago' value={formatMoney(summary?.pendingExpense ?? 0)} /><MetricCard title='Sin cotizar' value={summary?.unpricedCount ?? 0} /><MetricCard title='Próximos 7 días' value={summary?.dueNext7DaysCount ?? 0} /></section>;
}
