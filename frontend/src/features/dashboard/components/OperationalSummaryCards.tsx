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
          title="Saldo confirmado"
          value={formatMoney(summary.confirmedBalance)}
          helper="Balance real registrado hasta ahora."
          tone={summary.confirmedBalance >= 0 ? 'success' : 'danger'}
        />

        <MetricCard
          title="Pendiente de cobro"
          value={formatMoney(summary.pendingIncome)}
          helper="Ingresos planificados todavía no confirmados."
          tone="info"
        />

        <MetricCard
          title="Pendiente de pago"
          value={formatMoney(summary.pendingExpense)}
          helper="Egresos pendientes de registrar o pagar."
          tone={summary.pendingExpense > 0 ? 'warning' : 'neutral'}
        />

        <MetricCard
          title="Recuperos esperados"
          value={`${formatMoney(summary.expectedRecoveriesMin)} – ${formatMoney(summary.expectedRecoveriesMax)}`}
          helper="Reintegros, recuperos o retornos esperados."
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