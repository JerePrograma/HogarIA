import { StatusBadge } from '../../../components/ui/StatusBadge';
import { formatMoney } from '../../../domain/formatters';
import { suggestionConfidenceLabels } from '../../../domain/financeLabels';
import type { ApplyBudgetSuggestion, BudgetSuggestion, SuggestionConfidence } from '../../../domain/types';

export type BudgetSuggestionDraft = ApplyBudgetSuggestion & {
  categoryName: string;
  realAmount: number;
  transactionCount: number;
  confidence: SuggestionConfidence;
  outlierDetected: boolean;
  outlierAffectsSuggestedAmount: boolean;
  sourceTransactionIds: string[];
};

type Props = {
  rows: BudgetSuggestionDraft[];
  onRowsChange: (rows: BudgetSuggestionDraft[]) => void;
};

export const toBudgetDrafts = (suggestions: BudgetSuggestion[]): BudgetSuggestionDraft[] =>
  suggestions.map((suggestion) => ({
    categoryId: suggestion.categoryId,
    categoryName: suggestion.categoryName,
    realAmount: suggestion.realAmount,
    suggestedBudgetAmount: suggestion.suggestedBudgetAmount,
    transactionCount: suggestion.transactionCount,
    confidence: suggestion.confidence,
    reason: suggestion.reason,
    outlierDetected: suggestion.outlierDetected,
    outlierAffectsSuggestedAmount: suggestion.outlierAffectsSuggestedAmount,
    apply: suggestion.applyByDefault,
    sourceTransactionIds: suggestion.sourceTransactionIds,
  }));

export function BudgetSuggestionsTable({ rows, onRowsChange }: Props) {
  const updateRow = (index: number, patch: Partial<BudgetSuggestionDraft>) => {
    onRowsChange(rows.map((row, rowIndex) => (rowIndex === index ? { ...row, ...patch } : row)));
  };

  if (rows.length === 0) {
    return <p className="mensaje-info">No hay sugerencias de presupuesto para los filtros actuales.</p>;
  }

  return (
    <section className="panel suggestion-table-panel">
      <div className="section-title">
        <div>
          <p className="eyebrow">Presupuesto</p>
          <h2>Categorías sugeridas</h2>
        </div>
      </div>

      <div className="tabla-ui suggestion-table-desktop">
        <table>
          <thead>
            <tr>
              <th>Aplicar</th>
              <th>Categoría</th>
              <th>Real detectado</th>
              <th>Sugerido</th>
              <th>Confianza</th>
              <th>Motivo</th>
            </tr>
          </thead>
          <tbody>
            {rows.map((row, index) => (
              <tr key={row.categoryId}>
                <td>
                  <input
                    type="checkbox"
                    checked={row.apply}
                    onChange={(event) => updateRow(index, { apply: event.target.checked })}
                    aria-label={`Aplicar ${row.categoryName}`}
                  />
                </td>
                <td>
                  <strong>{row.categoryName}</strong>
                  <span className="compact-muted">{row.transactionCount} movimientos</span>
                </td>
                <td>{formatMoney(row.realAmount)}</td>
                <td>
                  <input
                    className="input-ui suggestion-amount-input"
                    type="number"
                    min="0"
                    inputMode="decimal"
                    value={row.suggestedBudgetAmount}
                    onChange={(event) =>
                      updateRow(index, { suggestedBudgetAmount: parseAmount(event.target.value) })
                    }
                  />
                </td>
                <td>
                  <ConfidenceBadge confidence={row.confidence} />
                </td>
                <td>
                  <span>{row.reason}</span>
                  {row.outlierDetected ? <StatusBadge label="Atípico" tone="watch" /> : null}
                  {row.outlierAffectsSuggestedAmount ? (
                    <StatusBadge label="Importe ajustado" tone="watch" />
                  ) : null}
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>

      <div className="suggestion-mobile-list">
        {rows.map((row, index) => (
          <article key={row.categoryId} className="suggestion-mobile-card">
            <header>
              <div>
                <strong>{row.categoryName}</strong>
                <span className="compact-muted">{row.transactionCount} movimientos</span>
              </div>
              <input
                type="checkbox"
                checked={row.apply}
                onChange={(event) => updateRow(index, { apply: event.target.checked })}
                aria-label={`Aplicar ${row.categoryName}`}
              />
            </header>
            <div className="suggestion-mobile-grid">
              <span>Real {formatMoney(row.realAmount)}</span>
              <label>
                Sugerido
                <input
                  type="number"
                  min="0"
                  inputMode="decimal"
                  value={row.suggestedBudgetAmount}
                  onChange={(event) =>
                    updateRow(index, { suggestedBudgetAmount: parseAmount(event.target.value) })
                  }
                />
              </label>
            </div>
            <ConfidenceBadge confidence={row.confidence} />
            <p className="secondary-text">{row.reason}</p>
            <div className="suggestion-badge-row">
              {row.outlierDetected ? <StatusBadge label="Atípico" tone="watch" /> : null}
              {row.outlierAffectsSuggestedAmount ? (
                <StatusBadge label="Importe ajustado" tone="watch" />
              ) : null}
            </div>
          </article>
        ))}
      </div>
    </section>
  );
}

function ConfidenceBadge({ confidence }: { confidence: SuggestionConfidence }) {
  const tone = confidence === 'HIGH' ? 'ok' : confidence === 'MEDIUM' ? 'watch' : 'risk';
  return <StatusBadge label={suggestionConfidenceLabels[confidence]} tone={tone} />;
}

function parseAmount(value: string): number {
  const parsed = Number(value.replace(',', '.'));
  return Number.isFinite(parsed) && parsed >= 0 ? parsed : 0;
}
