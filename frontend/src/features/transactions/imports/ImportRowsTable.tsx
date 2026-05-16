import type { Category } from '../../../domain/types';
import type { TransactionImportMovementType, TransactionImportRow } from './types';

interface Props {
  rows: TransactionImportRow[];
  categories: Category[];
  onRowsChange: (rows: TransactionImportRow[]) => void;
}

export function ImportRowsTable({ rows, categories, onRowsChange }: Props) {
  return (
    <table>
      <thead><tr><th>Fila</th><th>Fecha</th><th>Descripción</th><th>Monto</th><th>Tipo</th><th>Categoría</th><th>Estado</th></tr></thead>
      <tbody>
        {rows.map((row, index) => (
          <tr key={row.rowNumber}>
            <td>{row.rowNumber}</td><td>{row.realDate}</td><td>{row.normalizedDescription}</td><td>{row.amount}</td>
            <td><select value={row.movementType} onChange={(e)=>onRowsChange(rows.map((it,i)=>i===index?{...it,movementType:e.target.value as TransactionImportMovementType}:it))}><option value="INCOME">INCOME</option><option value="EXPENSE">EXPENSE</option></select></td>
            <td><select value={row.suggestedCategoryId ?? ''} onChange={(e)=>onRowsChange(rows.map((it,i)=>i===index?{...it,suggestedCategoryId:e.target.value||null}:it))}><option value="">Sin categoría</option>{categories.map((c)=><option key={c.id} value={c.id}>{c.name}</option>)}</select></td>
            <td>{row.status}</td>
          </tr>
        ))}
      </tbody>
    </table>
  );
}
