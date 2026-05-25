import { MetricCard } from '../../../components/ui/MetricCard';
import { StatusBadge } from '../../../components/ui/StatusBadge';
import { formatMoney } from '../../../domain/formatters';
import type { DashboardSummary, FinancialRiskLevel } from '../../../domain/types';

const riskLabels: Record<FinancialRiskLevel, string> = {
  OK: 'Correcto',
  WATCH: 'Atención',
  RISK: 'Riesgo',
  CRITICAL: 'Crítico',
};

const badgeTone: Record<FinancialRiskLevel, 'ok' | 'watch' | 'risk' | 'critical'> = {
  OK: 'ok',
  WATCH: 'watch',
  RISK: 'risk',
  CRITICAL: 'critical',
};

const metricTone: Record<FinancialRiskLevel, 'success' | 'warning' | 'danger'> = {
  OK: 'success',
  WATCH: 'warning',
  RISK: 'warning',
  CRITICAL: 'danger',
};

type Props = {
  summary: NonNullable<DashboardSummary['operationalSummary']>;
};

export function OperationalSummaryCards({ summary }: Props) {
  return (
    <section>
      <div className="section-title">
        <div>
          <p className="eyebrow">Situación actual</p>
          <h2>Estado operativo del mes</h2>
        </div>

        <StatusBadge
          tone={badgeTone[summary.financialRiskLevel]}
          label={riskLabels[summary.financialRiskLevel]}
        />
      </div>

      <div className="metric-grid">
        <MetricCard
          title="Neto proyectado"
          value={`${formatMoney(summary.projectedNetMin)} – ${formatMoney(summary.projectedNetMax)}`}
          helper="Rango esperado al cierre del mes."
          primary
          tone={summary.projectedNetMin >= 0 ? 'success' : 'danger'}
        />

        <MetricCard
          title="Balance consumo"
          value={formatMoney(summary.operationalBalanceExcludingRecoverables ?? summary.confirmedBalance)}
          helper="Ingresos y reintegros menos consumo y ahorro, sin capital prestado recuperable."
          tone={(summary.operationalBalanceExcludingRecoverables ?? summary.confirmedBalance) >= 0 ? 'success' : 'danger'}
        />

        <MetricCard
          title="Gasto consumo"
          value={formatMoney(summary.consumptionExpense ?? summary.confirmedExpenses)}
          helper="Consumo del mes, separado de recuperables e inversión."
          tone={(summary.consumptionExpense ?? summary.confirmedExpenses) > 0 ? 'warning' : 'neutral'}
        />

        <MetricCard
          title="Capital recuperable"
          value={formatMoney(summary.recoverableOutflow ?? 0)}
          helper="Capital prestado o salidas recuperables fuera del consumo."
          tone={(summary.recoverableOutflow ?? 0) > 0 ? 'info' : 'neutral'}
        />

        <MetricCard
          title="Flujo caja neto"
          value={formatMoney(summary.netCashFlowIncludingRecoverables ?? summary.confirmedBalance)}
          helper="Incluye salidas recuperables y recuperos de capital."
          tone={(summary.netCashFlowIncludingRecoverables ?? summary.confirmedBalance) >= 0 ? 'success' : 'danger'}
        />

        <MetricCard
          title="Recupero capital"
          value={formatMoney(summary.principalRecovery ?? 0)}
          helper="Capital recuperado separado de ingresos operativos."
          tone="info"
        />

        <MetricCard
          title="Riesgo financiero"
          value={
            <StatusBadge
              tone={badgeTone[summary.financialRiskLevel]}
              label={riskLabels[summary.financialRiskLevel]}
            />
          }
          helper="Lectura consolidada del riesgo mensual."
          tone={metricTone[summary.financialRiskLevel]}
        />
      </div>
    </section>
  );
}
