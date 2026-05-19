import { useMemo } from 'react';
import { StatusBadge } from '../../../components/ui/StatusBadge';
import type { Category } from '../../../domain/types';
import { formatMoney } from '../../../domain/formatters';
import type {
  TransactionImportMovementType,
  TransactionImportRow,
  TransactionImportRowStatus,
} from './types';

interface Props {
  rows: TransactionImportRow[];
  categories: Category[];
  onRowsChange: (rows: TransactionImportRow[]) => void;
  createMissingFallbackCategory: boolean;
}

const MOVEMENT_LABELS: Record<TransactionImportMovementType, string> = {
  INCOME: 'Ingreso',
  EXPENSE: 'Gasto',
  SAVING: 'Ahorro / inversión',
  TRANSFER: 'Transferencia',
  ADJUSTMENT: 'Ajuste',
};

const STATUS_LABELS: Record<TransactionImportRowStatus, string> = {
  READY: 'Lista',
  NEEDS_CATEGORY: 'Requiere categoría',
  DUPLICATE: 'Duplicada',
  SKIPPED: 'Omitida',
  ERROR: 'Error',
};

function getRowStatusTone(
  status: TransactionImportRowStatus,
): 'ok' | 'watch' | 'critical' | 'neutral' {
  if (status === 'READY') return 'ok';
  if (status === 'NEEDS_CATEGORY') return 'watch';
  if (status === 'ERROR') return 'critical';

  return 'neutral';
}

function getMovementTone(
  type: TransactionImportMovementType,
): 'ok' | 'watch' | 'risk' | 'critical' | 'neutral' {
  if (type === 'INCOME') return 'ok';
  if (type === 'SAVING') return 'ok';
  if (type === 'TRANSFER') return 'neutral';
  if (type === 'ADJUSTMENT') return 'watch';

  return 'critical';
}

function getRowIssueMessage(
  row: TransactionImportRow,
  createMissingFallbackCategory: boolean,
) {
  if (row.status === 'DUPLICATE') {
    return row.suggestedCategoryName
      ? `Movimiento ya existente. Categoría sugerida para revisar: ${row.suggestedCategoryName}.`
      : 'Movimiento ya existente. Se omitirá al confirmar.';
  }

  if (
    row.status === 'NEEDS_CATEGORY' &&
    !row.suggestedCategoryId &&
    row.suggestedCategoryName &&
    createMissingFallbackCategory
  ) {
    return `Sugerida: ${row.suggestedCategoryName}. Se creará al confirmar.`;
  }

  if (
    row.status === 'NEEDS_CATEGORY' &&
    !row.suggestedCategoryId &&
    !createMissingFallbackCategory
  ) {
    return 'Necesita una categoría para poder importarse.';
  }

  if (row.status === 'SKIPPED' && row.skipReason) {
    return `Motivo de omisión: ${row.skipReason}`;
  }

  if (row.status === 'ERROR') {
    return row.warning ?? 'La fila tiene un error y no se importará.';
  }

  return '';
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

  const updateRow = (index: number, patch: Partial<TransactionImportRow>) => {
    onRowsChange(
      rows.map((item, itemIndex) =>
        itemIndex === index
          ? {
              ...item,
              ...patch,
            }
          : item,
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
            {rows.map((row, index) => {
              const issueMessage = getRowIssueMessage(row, createMissingFallbackCategory);

              const categoryName =
                row.suggestedCategoryName ??
                categoriesById.get(row.suggestedCategoryId ?? '')?.name;

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

                      {row.rawDescription &&
                      row.rawDescription !== row.normalizedDescription ? (
                        <span className="muted">
                          Original: {row.rawDescription}
                        </span>
                      ) : null}
                    </div>
                  </td>

                  <td className="amount-cell">
                    {formatMoney(Number(row.amount ?? 0))}
                  </td>

                  <td>
                    <div className="import-row-type-cell">
                      <StatusBadge
                        tone={getMovementTone(row.movementType)}
                        label={MOVEMENT_LABELS[row.movementType] ?? row.movementType}
                      />

                      <select
                        className="input-ui"
                        value={row.movementType}
                        onChange={(event) =>
                          updateRow(index, {
                            movementType: event.target.value as TransactionImportMovementType,
                          })
                        }
                      >
                        {Object.entries(MOVEMENT_LABELS).map(([value, label]) => (
                          <option key={value} value={value}>
                            {label}
                          </option>
                        ))}
                      </select>
                    </div>
                  </td>

                  <td>
                    <div className="import-row-category-cell">
                      <select
                        className="input-ui"
                        value={row.suggestedCategoryId ?? ''}
                        onChange={(event) =>
                          updateRow(index, {
                            suggestedCategoryId: event.target.value || null,
                          })
                        }
                      >
                        <option value="">Sin categoría asignada</option>

                        {categories.map((category) => (
                          <option key={category.id} value={category.id}>
                            {category.name}
                          </option>
                        ))}
                      </select>

                      {categoryName ? (
                        <span className="muted">
                          Sugerida: {categoryName}
                        </span>
                      ) : null}

                      {issueMessage ? (
                        <small className="import-row-note">
                          {issueMessage}
                        </small>
                      ) : null}
                    </div>
                  </td>

                  <td>
                    <StatusBadge
                      tone={getRowStatusTone(row.status)}
                      label={STATUS_LABELS[row.status] ?? row.status}
                    />
                  </td>
                </tr>
              );
            })}
          </tbody>
        </table>
      </div>

      <div className="import-rows-mobile">
        {rows.map((row, index) => {
          const issueMessage = getRowIssueMessage(row, createMissingFallbackCategory);

          const categoryName =
            row.suggestedCategoryName ??
            categoriesById.get(row.suggestedCategoryId ?? '')?.name;

          return (
            <article key={row.rowNumber} className="import-row-card">
              <header>
                <div>
                  <span className="badge badge-muted">Fila #{row.rowNumber}</span>
                  <strong>
                    {row.normalizedDescription || row.rawDescription || 'Sin descripción'}
                  </strong>
                  <p className="muted">{row.realDate || '-'}</p>
                </div>

                <span className="import-row-card-amount">
                  {formatMoney(Number(row.amount ?? 0))}
                </span>
              </header>

              <div className="import-row-card-badges">
                <StatusBadge
                  tone={getMovementTone(row.movementType)}
                  label={MOVEMENT_LABELS[row.movementType] ?? row.movementType}
                />

                <StatusBadge
                  tone={getRowStatusTone(row.status)}
                  label={STATUS_LABELS[row.status] ?? row.status}
                />
              </div>

              <div className="form-grid">
                <label>
                  Tipo
                  <select
                    className="input-ui"
                    value={row.movementType}
                    onChange={(event) =>
                      updateRow(index, {
                        movementType: event.target.value as TransactionImportMovementType,
                      })
                    }
                  >
                    {Object.entries(MOVEMENT_LABELS).map(([value, label]) => (
                      <option key={value} value={value}>
                        {label}
                      </option>
                    ))}
                  </select>
                </label>

                <label>
                  Categoría
                  <select
                    className="input-ui"
                    value={row.suggestedCategoryId ?? ''}
                    onChange={(event) =>
                      updateRow(index, {
                        suggestedCategoryId: event.target.value || null,
                      })
                    }
                  >
                    <option value="">Sin categoría asignada</option>

                    {categories.map((category) => (
                      <option key={category.id} value={category.id}>
                        {category.name}
                      </option>
                    ))}
                  </select>
                </label>
              </div>

              {categoryName ? (
                <p className="muted">Sugerida: {categoryName}</p>
              ) : null}

              {issueMessage ? (
                <p className="import-row-note">{issueMessage}</p>
              ) : null}
            </article>
          );
        })}
      </div>
    </div>
  );
}