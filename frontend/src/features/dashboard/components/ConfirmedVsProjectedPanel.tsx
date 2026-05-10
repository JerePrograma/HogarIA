import { formatMoney } from '../../../domain/formatters';
import type { DashboardSummary } from '../../../domain/types';

type Props = { planning: NonNullable<DashboardSummary['planningSummary']>; operational: NonNullable<DashboardSummary['operationalSummary']> };
export function ConfirmedVsProjectedPanel({ planning, operational }: Props) {
  return <section className='grid'><article className='card'><h3 className='section-title'>Confirmado</h3><p>Ingresos: {formatMoney(operational.confirmedIncome)}</p><p>Egresos: {formatMoney(operational.confirmedExpenses)}</p><p>Ahorro: {formatMoney(operational.confirmedSavings)}</p></article><article className='card'><h3 className='section-title'>Proyectado</h3><p>Ingresos estimados: {formatMoney(planning.totalIncomeMin)} – {formatMoney(planning.totalIncomeMax)}</p><p>Egresos estimados: {formatMoney(planning.totalExpenseMin)} – {formatMoney(planning.totalExpenseMax)}</p><p>Neto proyectado: {formatMoney(planning.projectedNetMin)} – {formatMoney(planning.projectedNetMax)}</p><p>Diferencia vs saldo confirmado: {formatMoney(operational.deltaProjectedMinVsConfirmed)} – {formatMoney(operational.deltaProjectedMaxVsConfirmed)}</p></article></section>;
}
