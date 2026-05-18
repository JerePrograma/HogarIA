import { EmptyState } from '../../../../components/ui/EmptyState';
import { formatMoney } from '../../../../domain/formatters';
import type { SuggestedPlanTransactionMatch } from './types';

export function SuggestedMatchesPanel({ matches, isConfirming, onConfirm }: { matches: SuggestedPlanTransactionMatch[]; isConfirming: boolean; onConfirm: (m: SuggestedPlanTransactionMatch) => void }) {
  if (!matches.length) return <EmptyState title='Sin sugerencias' message='No se detectaron coincidencias automáticas para este período.'/>
  return <ul>{matches.map((m) => <li key={`${m.itemId}-${m.transactionId}`}><strong>{m.reasons.join(', ') || 'Sugerencia automática'}</strong> · {formatMoney(m.suggestedAmount)} <button type='button' onClick={() => onConfirm(m)} disabled={isConfirming}>{isConfirming ? 'Confirmando...' : 'Aceptar'}</button></li>)}</ul>;
}
