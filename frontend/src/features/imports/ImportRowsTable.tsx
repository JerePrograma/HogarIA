import { useMemo } from 'react';
import { StatusBadge } from '../../components/ui/StatusBadge';
import type { Category } from '../../domain/types';
import { formatMoney } from '../../domain/formatters';
import type {
  TransactionImportMovementType,
  TransactionImportRow,
} from './types';
import { ImportRowCategorySelect } from './ImportRowCategorySelect';
import { getImportRowIssueMessage, getSuggestedCategoryName, importMovementLabels, getImportRowStatusTone, importRowStatusLabels } from '../../imports/importUtils';

interface Props {
  rows: TransactionImportRow[];
  categories: Category[];
  onRowsChange: (rows: TransactionImportRow[]) => void;
  createMissingFallbackCategory: boolean;
}

export function ImportRowsTable({
  rows,
  categories,
  onRowsChange,
  createMissingFallbackCategory,
}: Props) {
  const categoriesById = useMemo(
    () => new Map(categories.map((category) => [category.id, category])),
    [categories],
  );

  const updateRow = (rowNumber: number, patch: Partial<TransactionImportRow>) => {
    onRowsChange(
      rows.map((row) =>
        row.rowNumber === rowNumber
          ? { ...row, ...patch }
          : row,
      ),
    );
  };

  return (
    <div className="import-rows">
      <div className="tabla-ui import-rows-table">
        <table className="table-compact">
          <thead>
            <tr>
              <th>Fila</th>
              <th>Fecha</th>
              <th>Descripción</th>
              <th className="amount-cell">Monto</th>
              <th>Tipo</th>
              <th>Categoría</th>
              <th>Estado</th>
            </tr>
          </thead>

          <tbody>
            {rows.map((row) => {
              const issueMessage = getImportRowIssueMessage(
                row,
                createMissingFallbackCategory,
              );

              const categoryName = getSuggestedCategoryName(row, categoriesById);

              return (
                <tr key={row.rowNumber}>
                  <td>
                    <strong>#{row.rowNumber}</strong>
                  </td>

                  <td>
                    <strong>{row.realDate || '-'}</strong>
                  </td>

                  <td>
                    <div className="import-row-description">
                      <strong>
                        {row.normalizedDescription || row.rawDescription || 'Sin descripción'}
                      </strong>

                      {row.rawDescription && row.rawDescription !== row.normalizedDescription ? (
                        <span className="muted">Original: {row.rawDescription}</span>
                      ) : null}
                    </div>
                  </td>

                  <td className="amount-cell">
                    {formatMoney(Number(row.amount ?? 0))}
                  </td>

                  <td>
                    <div className="import-row-type-cell">
                      <StatusBadge
                        tone={movementTypeTones[row.movementType]}
                        label={importMovementLabels[row.movementType] ?? row.movementType}
                      />

                      <select
                        className="input-ui"
                        value={row.movementType}
                        onChange={(event) =>
                          updateRow(row.rowNumber, {
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
                    </div>
                  </td>

                  <td>
                    <div className="import-row-category-cell">
                      <ImportRowCategorySelect
                        row={row}
                        categories={categories}
                        onChange={(categoryId) =>
                          updateRow(row.rowNumber, { suggestedCategoryId: categoryId })
                        }
                      />

                      {categoryName ? (
                        <span className="muted">Sugerida: {categoryName}</span>
                      ) : null}

                      {issueMessage ? (
                        <small className="import-row-note">{issueMessage}</small>
                      ) : null}
                    </div>
                  </td>

                  <td>
                    <StatusBadge
                      tone={getImportRowStatusTone(row.status)}
                      label={importRowStatusLabels[row.status] ?? row.status}
                    />
                  </td>
                </tr>
              );
            })}
          </tbody>
        </table>
      </div>

      <ImportRowsMobileList
        rows={rows}
        categories={categories}
        categoriesById={categoriesById}
        createMissingFallbackCategory={createMissingFallbackCategory}
        onUpdateRow={updateRow}
      />
    </div>
  );
}