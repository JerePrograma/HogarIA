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

  return (
    <section>
      <div className="section-title">
        <div>
          <p className="eyebrow">Situación actual</p>
          <h2>Estado operativo del mes</h2>
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
          title="Movimientos reales"
          value={confirmedCount}
          helper="Registros confirmados del período."
          tone="info"
        />
      </div>
    </section>
  );
}
