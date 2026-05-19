import type { Category } from '../../../domain/types';
import type { TransactionImportMovementType, TransactionImportRow, TransactionImportRowStatus } from './types';

interface Props { rows: TransactionImportRow[]; categories: Category[]; onRowsChange: (rows: TransactionImportRow[]) => void; createMissingFallbackCategory: boolean; }

const MOVEMENT_LABELS: Record<TransactionImportMovementType, string> = {
  INCOME: 'Ingreso', EXPENSE: 'Gasto', SAVING: 'Ahorro / inversión', TRANSFER: 'Transferencia', ADJUSTMENT: 'Ajuste',
};
const STATUS_LABELS: Record<TransactionImportRowStatus, string> = {
  READY: 'Lista', NEEDS_CATEGORY: 'Requiere categoría', DUPLICATE: 'Duplicada', SKIPPED: 'Omitida', ERROR: 'Error',
};

export function ImportRowsTable({ rows, categories, onRowsChange, createMissingFallbackCategory }: Props) {
  const updateRow = (index: number, patch: Partial<TransactionImportRow>) => onRowsChange(rows.map((it, i) => i === index ? { ...it, ...patch } : it));
  return <table><thead><tr><th>Fila</th><th>Fecha</th><th>Descripción</th><th>Monto</th><th>Tipo</th><th>Categoría</th><th>Estado</th></tr></thead><tbody>
    {rows.map((row, index) => <tr key={row.rowNumber}>
      <td>{row.rowNumber}</td><td>{row.realDate}</td><td>{row.normalizedDescription}</td><td>{row.amount}</td>
      <td><select value={row.movementType} onChange={(e) => updateRow(index, { movementType: e.target.value as TransactionImportMovementType })}>{Object.entries(MOVEMENT_LABELS).map(([v,l]) => <option key={v} value={v}>{l}</option>)}</select></td>
      <td><select value={row.suggestedCategoryId ?? ''} onChange={(e) => updateRow(index, { suggestedCategoryId: e.target.value || null })}><option value="">Sin categoría asignada</option>{categories.map((c) => <option key={c.id} value={c.id}>{c.name}</option>)}</select>
      {row.status === 'DUPLICATE' ? <small>Movimiento ya existente. Puede recategorizarse desde revisión de categorías. {row.suggestedCategoryName ? `Sugerida para el movimiento existente: ${row.suggestedCategoryName}` : ''}</small> : null}
      {row.status === 'NEEDS_CATEGORY' && !row.suggestedCategoryId && row.suggestedCategoryName && createMissingFallbackCategory ? <small>Sugerida: {row.suggestedCategoryName} (se creará al confirmar)</small> : null}
      {row.status === 'SKIPPED' && row.skipReason ? <small>Motivo de omisión: {row.skipReason}</small> : null}
      </td>
      <td>{STATUS_LABELS[row.status] ?? row.status}</td>
    </tr>)}
  </tbody></table>;
}
