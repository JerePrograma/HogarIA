import { formatMoney } from '../../../../domain/formatters';
import type { MonthlyPlanReconciliationSummary } from './types';

export function ReconciliationSummaryPanel({ summary }: { summary: MonthlyPlanReconciliationSummary }) {
  return <div><p>Movimientos sin plan: {summary.unplannedCount}</p><p>Sugerencias: {summary.suggestedCount}</p><p>Total sin planificar: {formatMoney(summary.unplannedTransactionsTotal)}</p></div>;
}
