import { StatusBadge } from '../../components/ui/StatusBadge';
import type { Category } from '../../domain/types';
import { movementTypeTones } from '../../domain/financeLabels';
import { formatMoney } from '../../domain/formatters';
import type { TransactionImportMovementType, TransactionImportRow } from './types';
import { ImportRowCategorySelect } from './ImportRowCategorySelect';
import { getImportRowIssueMessage, getSuggestedCategoryName, importMovementLabels, getImportRowStatusTone, importRowStatusLabels } from './utils/importUtils';


interface Props {
  rows: TransactionImportRow[];
  categories: Category[];
  categoriesById: Map<string, Category>;
  createMissingFallbackCategory: boolean;
  onUpdateRow: (rowNumber: number, patch: Partial<TransactionImportRow>) => void;
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
                label={importMovementLabels[row.movementType] ?? row.movementType}
              />

              <StatusBadge
                tone={getImportRowStatusTone(row.status)}
                label={importRowStatusLabels[row.status] ?? row.status}
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
            {issueMessage ? <p className="import-row-note">{issueMessage}</p> : null}
          </article>
        );
      })}
    </div>
  );
}