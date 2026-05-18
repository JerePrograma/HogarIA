import { MetricCard } from '../../../components/ui/MetricCard';
import { formatMoney } from '../../../domain/formatters';
import type { DashboardSummary } from '../../../domain/types';

type Props = {
  planning: NonNullable<DashboardSummary['planningSummary']>;
  operational: NonNullable<DashboardSummary['operationalSummary']>;
  cashFlow: NonNullable<DashboardSummary['monthlyCashFlowSummary']>;
};

export function ConfirmedVsProjectedPanel({ planning, operational, cashFlow }: Props) {
  return (
    <section className="grid">
      <article className="panel">
        <div className="section-title"><div><p className="eyebrow">Datos reales</p><h2>Confirmado (semántica financiera)</h2></div></div>
        <div className="grid">
          <MetricCard title="Flujo neto de caja" value={formatMoney(cashFlow.netCashFlow)} tone={cashFlow.netCashFlow >= 0 ? 'success' : 'danger'} />
          <MetricCard title="Gasto de consumo" value={formatMoney(cashFlow.consumptionExpense)} tone="danger" helper={`Fijo: ${formatMoney(cashFlow.fixedExpense)} · Variable: ${formatMoney(cashFlow.variableExpense)}`} />
          <MetricCard title="Ahorro + inversión" value={formatMoney(cashFlow.savingOutflow + cashFlow.investmentOutflow)} tone="info" />
          <MetricCard title="Deuda y egresos recuperables" value={formatMoney(cashFlow.debtOutflow + cashFlow.recoverableOutflow)} tone="warning" />
          <MetricCard title="Transferencias" value={`Internas ${formatMoney(cashFlow.internalTransfers)} · Externas ${formatMoney(cashFlow.externalTransfers)}`} tone="info" />
        </div>
      </article>
      <article className="panel">
        <div className="section-title"><div><p className="eyebrow">Escenario esperado</p><h2>Proyectado</h2></div></div>
        <div className="grid">
          <MetricCard title="Neto proyectado" value={`${formatMoney(planning.projectedNetMin)} – ${formatMoney(planning.projectedNetMax)}`} tone={planning.projectedNetMin >= 0 ? 'success' : 'danger'} />
          <MetricCard title="Diferencia vs flujo confirmado" value={`${formatMoney(operational.deltaProjectedMinVsConfirmed)} – ${formatMoney(operational.deltaProjectedMaxVsConfirmed)}`} tone="warning" />
        </div>
      </article>
    </section>
  );
}
