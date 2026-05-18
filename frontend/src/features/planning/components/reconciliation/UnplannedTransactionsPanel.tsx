import { EmptyState } from '../../../../components/ui/EmptyState';
import { formatMoney } from '../../../../domain/formatters';
import type { UnplannedTransaction } from './types';

const statusLabel: Record<string, string> = { PENDING: 'Pendiente', PARTIAL: 'Parcial', MATCHED: 'Conciliado' };

export function UnplannedTransactionsPanel({ transactions }: { transactions: UnplannedTransaction[] }) {
  if (!transactions.length) return <EmptyState title='Sin movimientos sin planificar' message='No hay movimientos pendientes de conciliación en este período.'/>
  return <ul>{transactions.map((t) => <li key={t.transactionId}><strong>{t.description || 'Sin descripción'}</strong> · {t.budgetDate} · {formatMoney(t.amount)} · Estado: {statusLabel[t.status] ?? t.status}</li>)}</ul>;
}
