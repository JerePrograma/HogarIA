import type { PlanItemReconciliation, MonthlyPlanReconciliationSummary, SuggestedPlanTransactionMatch, TransactionMatch, UnplannedTransaction } from '../../../../domain/types';

export type ReconciliationTabKey = 'overview' | 'unplanned' | 'suggestions' | 'items';

export interface ReconciliationTabOption { key: ReconciliationTabKey; label: string; }

export const reconciliationTabs: ReconciliationTabOption[] = [
  { key: 'overview', label: 'Resumen' },
  { key: 'unplanned', label: 'Sin planificar' },
  { key: 'suggestions', label: 'Sugerencias' },
  { key: 'items', label: 'Ítems' },
];

export type { PlanItemReconciliation, MonthlyPlanReconciliationSummary, SuggestedPlanTransactionMatch, TransactionMatch, UnplannedTransaction };
