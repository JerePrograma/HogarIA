import { labelOrFallback } from '../../../../domain/financeLabels';
import { formatMoney } from '../../../../domain/formatters';
import { EmptyState } from '../../../../shared/ui/EmptyState';
import type { TransactionMatch } from './types';

const matchTypeLabel: Record<string, string> = {
  MANUAL: 'Manual',
  SYSTEM_CONVERSION: 'Conversión automática',
  SUGGESTED: 'Sugerido',
};

type TransactionMatchListProps = {
  matches: TransactionMatch[];
  isDeleting: boolean;
  onDelete: (id: string) => void;
};

export function TransactionMatchList({
  matches,
  isDeleting,
  onDelete,
}: TransactionMatchListProps) {
  if (!matches.length) {
    return (
      <EmptyState
        title="Sin conciliaciones"
        message="Todavía no hay conciliaciones para este ítem."
      />
    );
  }

  return (
    <ul className="grid gap-2 p-0">
      {matches.map((match) => (
        <li key={match.id} className="surface-inset list-none">
          <span>
            {labelOrFallback(matchTypeLabel, match.matchType, 'Tipo no reconocido')} ·{' '}
            {formatMoney(match.matchedAmount)}
          </span>

          <button
            type="button"
            className="boton-fantasma"
            onClick={() => onDelete(match.id)}
            disabled={isDeleting}
          >
            {isDeleting ? 'Eliminando...' : 'Eliminar'}
          </button>
        </li>
      ))}
    </ul>
  );
}
