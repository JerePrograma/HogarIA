import { MetricCard } from '../../../shared/ui/MetricCard';
import { StatusBadge } from '../../../shared/ui/StatusBadge';
import {
  financialRiskLevelLabels,
} from '../../../domain/financeLabels';
import { financialRiskLevelTones } from '../../../domain/financeTones';
import { formatMoney } from '../../../domain/formatters';
import type { DashboardSummary } from '../../../domain/types';

type Props = {
  summary: NonNullable<DashboardSummary['operationalSummary']>;
  realSummary?: DashboardSummary['realConfirmedSummary'];
};

export function OperationalSummaryCards({ summary, realSummary }: Props) {
  const result = realSummary?.operationalBalance ?? summary.confirmedBalance;
  const income = realSummary?.confirmedIncome ?? summary.confirmedIncome;
  const expenses = realSummary?.confirmedExpenses ?? summary.confirmedExpenses;
  const confirmedCount = realSummary?.confirmedCount ?? 0;
  const pendingCount = realSummary?.pendingCount ?? 0;
  const reviewCount = realSummary?.reviewCount ?? 0;
  const withoutCategoryCount = realSummary?.withoutCategoryCount ?? 0;
  const riskTone = toMetricTone(summary.financialRiskLevel);

  return (
    <section className="dashboard-executive-summary">
      <div className="section-title">
        <div>
          <p className="eyebrow">Lectura rápida</p>
          <h2>Lo importante del mes</h2>
          <p className="muted">
            Balance, flujo confirmado y calidad de datos para decidir por dónde seguir.
          </p>
        </div>

        <StatusBadge
          tone={financialRiskLevelTones[summary.financialRiskLevel]}
          label={financialRiskLevelLabels[summary.financialRiskLevel]}
        />
      </div>

      <div className="metric-grid">
        <MetricCard
          title="Resultado real"
          value={formatMoney(result)}
          helper="Balance operativo confirmado."
          primary
          tone={result >= 0 ? 'success' : 'danger'}
        />

        <MetricCard
          title="Ingresos"
          value={formatMoney(income)}
          helper="Ingresos operativos confirmados."
          tone="success"
        />

        <MetricCard
          title="Gastos"
          value={formatMoney(expenses)}
          helper={`Egresos operativos confirmados. Excluidos: ${formatMoney((realSummary?.excludedInternalTransferAmount ?? 0) + (realSummary?.excludedDuplicateAmount ?? 0))}.`}
          tone="danger"
        />

        <MetricCard
          title="Pendientes / revisión"
          value={pendingCount + reviewCount}
          helper={`${pendingCount} pendiente(s), ${reviewCount} en revisión, ${withoutCategoryCount} sin categoría.`}
          tone={pendingCount + reviewCount + withoutCategoryCount > 0 ? 'warning' : 'success'}
        />

        <MetricCard
          title="Salud del mes"
          value={financialRiskLevelLabels[summary.financialRiskLevel]}
          helper={`${confirmedCount} movimiento(s) confirmado(s) sostienen esta lectura.`}
          tone={riskTone}
        />
      </div>
    </section>
  );
}

function toMetricTone(
  riskLevel: NonNullable<DashboardSummary['operationalSummary']>['financialRiskLevel'],
) {
  if (riskLevel === 'OK') return 'success';
  if (riskLevel === 'WATCH') return 'warning';
  return 'danger';
}
