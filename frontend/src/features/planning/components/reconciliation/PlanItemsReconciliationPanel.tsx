import { EmptyState } from '../../../../components/ui/EmptyState';
import { formatMoney } from '../../../../domain/formatters';
import type { PlanItemReconciliation } from './types';
import { TransactionMatchList } from './TransactionMatchList';

const statusLabel: Record<string, string> = { PENDING: 'Pendiente', PARTIAL: 'Parcial', MATCHED: 'Conciliado' };

export function PlanItemsReconciliationPanel({ items, isDeleting, onDelete }: { items: PlanItemReconciliation[]; isDeleting: boolean; onDelete: (id: string) => void }) {
  if (!items.length) return <EmptyState title='Sin ítems para conciliar' message='No hay ítems de planificación para este período.'/>
  return <ul>{items.map((i) => <li key={i.itemId}><strong>{i.title}</strong> · {statusLabel[i.executionStatus] ?? i.executionStatus} · {formatMoney(i.matchedAmount)}/{formatMoney(i.plannedAmount)}<TransactionMatchList matches={i.matches} onDelete={onDelete} isDeleting={isDeleting} /></li>)}</ul>;
}
