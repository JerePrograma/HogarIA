import { StatusBadge } from '../../shared/ui/StatusBadge';
import type { Category } from '../../domain/types';
import { labelOrFallback, movementTypeTones } from '../../domain/financeLabels';
import { formatMoney } from '../../domain/formatters';
import type { TransactionImportMovementType, TransactionImportRow } from './types';
import { ImportRowCategorySelect } from './ImportRowCategorySelect';
import {
  getClassificationExplanationSummary,
  getImportRowIssueMessage,
  getSuggestedCategoryName,
  importBalanceImpactLabels,
  importClassificationStatusLabels,
  importConfidenceLabels,
  importMovementLabels,
  importPaymentChannelLabels,
  getImportRowStatusTone,
  importRowStatusLabels,
} from './utils/importUtils';


interface Props {
  rows: TransactionImportRow[];
  categories: Category[];
  categoriesById: Map<string, Category>;
  createMissingFallbackCategory: boolean;
  onUpdateRow: (rowNumber: number, patch: Partial<TransactionImportRow>) => void;
}

function compactMeta(row: TransactionImportRow) {
  return [
    row.paymentChannel
      ? `Canal: ${importPaymentChannelLabels[row.paymentChannel] ?? row.paymentChannel}`
      : null,
    row.balanceImpact
      ? `Impacto: ${importBalanceImpactLabels[row.balanceImpact] ?? row.balanceImpact}`
      : null,
    row.classificationStatus
      ? `Clasificación: ${importClassificationStatusLabels[row.classificationStatus] ?? row.classificationStatus}`
      : null,
    row.confidence
      ? `Confianza: ${importConfidenceLabels[row.confidence] ?? row.confidence}`
      : null,
    row.classificationLayer ? `Capa: ${row.classificationLayer}` : null,
  ].filter(Boolean).join(' · ');
}

export function ImportRowsMobileList({
  rows,
  categories,
  categoriesById,
  createMissingFallbackCategory,
  onUpdateRow,
}: Props) {
  return (
    <div className="import-rows-mobile">
      {rows.map((row) => {
        const issueMessage = getImportRowIssueMessage(row, createMissingFallbackCategory);
        const categoryName = getSuggestedCategoryName(row, categoriesById);

        return (
          <article key={row.rowNumber} className="import-row-card">
            <header>
              <div>
                <span className="badge badge-muted">Fila #{row.rowNumber}</span>
                <strong>{row.normalizedDescription || row.rawDescription || 'Sin descripción'}</strong>
                <p className="muted">{row.realDate || '-'}</p>
              </div>

              <span className="import-row-card-amount">
                {formatMoney(Number(row.amount ?? 0))}
              </span>
            </header>

            <div className="import-row-card-badges">
              <StatusBadge
                tone={movementTypeTones[row.movementType]}
                label={labelOrFallback(importMovementLabels, row.movementType, 'Tipo no reconocido')}
              />

              <StatusBadge
                tone={getImportRowStatusTone(row.status)}
                label={labelOrFallback(importRowStatusLabels, row.status, 'Estado no reconocido')}
              />
            </div>

            <div className="form-grid">
              <label>
                Tipo
                <select
                  className="input-ui"
                  value={row.movementType}
                  onChange={(event) =>
                    onUpdateRow(row.rowNumber, {
                      movementType: event.target.value as TransactionImportMovementType,
                      suggestedCategoryId: null,
                    })
                  }
                >
                  {Object.entries(importMovementLabels).map(([value, label]) => (
                    <option key={value} value={value}>
                      {label}
                    </option>
                  ))}
                </select>
              </label>

              <label>
                Categoría
                <ImportRowCategorySelect
                  row={row}
                  categories={categories}
                  onChange={(categoryId) =>
                    onUpdateRow(row.rowNumber, { suggestedCategoryId: categoryId })
                  }
                />
              </label>
            </div>

            {categoryName ? <p className="muted">Sugerida: {categoryName}</p> : null}
            {row.merchantName ? <p className="muted">Comercio: {row.merchantName}</p> : null}
            {row.counterparty && row.counterparty !== row.merchantName ? (
              <p className="muted">Contraparte: {row.counterparty}</p>
            ) : null}
            {compactMeta(row) ? <p className="muted">{compactMeta(row)}</p> : null}
            {row.classificationReason ? (
              <p className="muted">Regla: {row.classificationReason}</p>
            ) : null}
            {getClassificationExplanationSummary(row) ? (
              <p className="muted">Match: {getClassificationExplanationSummary(row)}</p>
            ) : null}
            {issueMessage ? <p className="import-row-note">{issueMessage}</p> : null}
          </article>
        );
      })}
    </div>
  );
}
