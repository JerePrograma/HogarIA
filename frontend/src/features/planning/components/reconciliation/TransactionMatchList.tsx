import { EmptyState } from '../../../../components/ui/EmptyState';
import { formatMoney } from '../../../../domain/formatters';
import type { TransactionMatch } from './types';

const matchTypeLabel: Record<string, string> = { MANUAL: 'Manual', SYSTEM_CONVERSION: 'Conversión automática', SUGGESTED: 'Sugerido' };

export function TransactionMatchList({ matches, isDeleting, onDelete }: { matches: TransactionMatch[]; isDeleting: boolean; onDelete: (id: string) => void }) {
  if (!matches.length) return <EmptyState title='Sin conciliaciones' message='Todavía no hay conciliaciones para este ítem.'/>
  return <ul>{matches.map((m) => <li key={m.id}>{matchTypeLabel[m.matchType] ?? m.matchType} · {formatMoney(m.matchedAmount)} <button type='button' onClick={() => onDelete(m.id)} disabled={isDeleting}>{isDeleting ? 'Eliminando...' : 'Eliminar'}</button></li>)}</ul>;
}
