import { MetricCard } from '../../../shared/ui/MetricCard';
import { StatusBadge } from '../../../shared/ui/StatusBadge';
import {
  getFinancialExecutionLabel,
  getFinancialExecutionRiskLevel,
} from '../../../domain/financialSemantics';
import { financialRiskLevelTones } from '../../../domain/financeTones';
import { formatMoney } from '../../../domain/formatters';
import type { DashboardSummary } from '../../../domain/types';

type Props = {
  planning: NonNullable<DashboardSummary['planningSummary']>;
  operational: NonNullable<DashboardSummary['operationalSummary']>;
  cashFlow: NonNullable<DashboardSummary['monthlyCashFlowSummary']>;
  realVsPlanned?: DashboardSummary['realVsPlanned'];
  closingProjection?: DashboardSummary['closingProjection'];
};

export function ConfirmedVsProjectedPanel({
  planning,
  operational,
  cashFlow,
  realVsPlanned,
  closingProjection,
}: Props) {
  const statusRisk = realVsPlanned
    ? getFinancialExecutionRiskLevel(realVsPlanned.status)
    : 'OK';

  return (
    <section className="grid">
      <article className="panel">
        <div className="section-title">
          <div>
            <p className="eyebrow">Real vs planificado</p>
            <h2>Ejecución del período</h2>
          </div>
          {realVsPlanned ? (
            <StatusBadge
              tone={financialRiskLevelTones[statusRisk]}
              label={getFinancialExecutionLabel(realVsPlanned.status)}
            />
          ) : null}
        </div>
        <div className="grid">
          <MetricCard
            title="Planificado"
            value={formatMoney(realVsPlanned?.totalPlanned ?? 0)}
            tone="info"
          />
          <MetricCard
            title="Real confirmado"
            value={formatMoney(realVsPlanned?.totalRealConfirmed ?? 0)}
            tone="success"
          />
          <MetricCard
            title="Desvío absoluto"
            value={formatMoney(realVsPlanned?.totalDifference ?? 0)}
            tone={(realVsPlanned?.totalDifference ?? 0) > 0 ? 'warning' : 'neutral'}
          />
          <MetricCard
            title="Real no planificado"
            value={formatMoney(realVsPlanned?.realUnplannedAmount ?? 0)}
            tone={(realVsPlanned?.realUnplannedAmount ?? 0) > 0 ? 'warning' : 'neutral'}
          />
        </div>
      </article>
      <article className="panel">
        <div className="section-title"><div><p className="eyebrow">Proyección de cierre</p><h2>Estimado</h2></div></div>
        <div className="grid">
          <MetricCard
            title="Real acumulado"
            value={formatMoney(closingProjection?.realAccumulated ?? operational.confirmedBalance)}
            tone={(closingProjection?.realAccumulated ?? operational.confirmedBalance) >= 0 ? 'success' : 'danger'}
          />
          <MetricCard
            title="Pendiente planificado"
            value={formatMoney(closingProjection?.pendingPlannedNet ?? planning.pendingIncome - planning.pendingExpense)}
            tone={(closingProjection?.pendingPlannedNet ?? 0) >= 0 ? 'success' : 'warning'}
          />
          <MetricCard
            title="Estimación de cierre"
            value={formatMoney(closingProjection?.estimatedClosing ?? planning.projectedNetMax)}
            tone={(closingProjection?.estimatedClosing ?? planning.projectedNetMax) >= 0 ? 'success' : 'danger'}
            primary
          />
          <MetricCard
            title="Diferencia vs plan"
            value={formatMoney(closingProjection?.estimatedDifferenceVsPlan ?? operational.deltaProjectedMaxVsConfirmed)}
            tone="warning"
          />
          <MetricCard title="Flujo neto de caja" value={formatMoney(cashFlow.netCashFlow)} tone={cashFlow.netCashFlow >= 0 ? 'success' : 'danger'} />
        </div>
      </article>

      {realVsPlanned && realVsPlanned.categories.length > 0 ? (
        <article className="panel">
          <div className="section-title">
            <div>
              <p className="eyebrow">Desvíos</p>
              <h2>Categorías con mayor prioridad</h2>
            </div>
          </div>

          <div className="tabla-ui">
            <table className="table-compact">
              <thead>
                <tr>
                  <th>Categoría</th>
                  <th className="amount-cell">Planificado</th>
                  <th className="amount-cell">Real</th>
                  <th className="amount-cell">Desvío</th>
                  <th>Estado</th>
                </tr>
              </thead>
              <tbody>
                {realVsPlanned.categories.slice(0, 8).map((item) => {
                  const itemRisk = getFinancialExecutionRiskLevel(item.status);

                  return (
                    <tr key={item.categoryId ?? item.categoryName}>
                      <td>{item.categoryName}</td>
                      <td className="amount-cell">{formatMoney(item.plannedAmount)}</td>
                      <td className="amount-cell">{formatMoney(item.realConfirmedAmount)}</td>
                      <td className="amount-cell">{formatMoney(item.difference)}</td>
                      <td>
                        <StatusBadge
                          tone={financialRiskLevelTones[itemRisk]}
                          label={getFinancialExecutionLabel(item.status)}
                        />
                      </td>
                    </tr>
                  );
                })}
              </tbody>
            </table>
          </div>
        </article>
      ) : null}
    </section>
  );
}
