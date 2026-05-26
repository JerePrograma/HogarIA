import { EmptyState } from '../../../../shared/ui/EmptyState';
import { formatMoney } from '../../../../domain/formatters';
import { labelOrFallback, labelOrValue, monthlyPlanTypeLabels, movementTypeLabels } from '../../../../domain/financeLabels';
import type { Account, Category } from '../../../../domain/types';
import type { SuggestedPlanTransactionMatch } from './types';

type Props = {
  matches: SuggestedPlanTransactionMatch[];
  accounts: Account[];
  categories: Category[];
  isConfirming: boolean;
  onConfirm: (match: SuggestedPlanTransactionMatch) => void;
};

const confidenceLabel: Record<string, string> = {
  HIGH: 'Alta',
  MEDIUM: 'Media',
  LOW: 'Baja',
};

export function SuggestedMatchesPanel({ matches, accounts, categories, isConfirming, onConfirm }: Props) {
  const accountById = new Map(accounts.map((account) => [account.id, account]));
  const categoryById = new Map(categories.map((category) => [category.id, category]));

  return (
    <section className="surface-inset mt-4">
      <div className="section-title">
        <div>
          <h3>Sugerencias de conciliación</h3>
          <p className="secondary-text">
            Revisá coincidencias entre lo planificado y movimientos reales existentes. Confirmar vínculo no crea movimientos nuevos.
          </p>
        </div>
        <span className="badge-ui badge-info">{matches.length}</span>
      </div>

      {!matches.length ? (
        <EmptyState title="Sin sugerencias" message="No se detectaron coincidencias automáticas para este período." />
      ) : (
        <div className="stack-ui">
          {matches.map((match) => (
            <article key={`${match.itemId}-${match.transactionId}`} className="panel-muted">
              <div className="section-title">
                <div>
                  <h4>{match.itemTitle}</h4>
                  <p className="secondary-text">
                    {labelOrValue(monthlyPlanTypeLabels, match.itemType)} · Período {match.periodMonth}/{match.periodYear}
                  </p>
                </div>
                <span className="badge-ui badge-ok">
                  Confianza {labelOrFallback(confidenceLabel, match.confidence, 'no reconocida')}
                </span>
              </div>

              <dl className="plan-item-card-grid">
                <div>
                  <dt>Planificado</dt>
                  <dd>{formatMoney(match.plannedAmount)}</dd>
                </div>
                <div>
                  <dt>Real</dt>
                  <dd>{formatMoney(match.transactionAmount)}</dd>
                </div>
                <div>
                  <dt>Diferencia</dt>
                  <dd>{formatMoney(match.difference)}</dd>
                </div>
                <div>
                  <dt>Fecha esperada</dt>
                  <dd>{match.expectedDate ?? '-'}</dd>
                </div>
                <div>
                  <dt>Fecha real</dt>
                  <dd>{match.transactionRealDate ?? '-'}</dd>
                </div>
                <div>
                  <dt>Movimiento real</dt>
                  <dd>{labelOrValue(movementTypeLabels, match.transactionMovementType)}</dd>
                </div>
                <div>
                  <dt>Cuenta</dt>
                  <dd>{accountById.get(match.transactionAccountId ?? '')?.name ?? accountById.get(match.itemAccountId ?? '')?.name ?? 'Sin cuenta'}</dd>
                </div>
                <div>
                  <dt>Categoría</dt>
                  <dd>{categoryById.get(match.transactionCategoryId ?? '')?.name ?? categoryById.get(match.itemCategoryId ?? '')?.name ?? 'Sin categoría'}</dd>
                </div>
              </dl>

              {match.reasons.length > 0 ? (
                <ul className="planning-suggestion-reasons">
                  {match.reasons.map((reason, index) => (
                    <li key={`${reason}-${index}`}>{reason}</li>
                  ))}
                </ul>
              ) : null}

              <div className="form-actions">
                <button type="button" className="boton-principal" onClick={() => onConfirm(match)} disabled={isConfirming}>
                  {isConfirming ? 'Vinculando...' : 'Confirmar vínculo'}
                </button>
                <button type="button" className="boton-secundario" disabled title="El backend todavía no expone ignorar sugerencias.">
                  Ignorar sugerencia
                </button>
              </div>
            </article>
          ))}
        </div>
      )}
    </section>
  );
}
