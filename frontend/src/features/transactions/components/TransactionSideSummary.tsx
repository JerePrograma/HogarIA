import { formatMoney } from "../../../domain/formatters";
import type { TransactionTotals } from "../hooks/useTransactionTotals";

interface Props {
  visibleCount: number;
  filteredTotal: number;
  totals: TransactionTotals;
}

export function TransactionSideSummary({
  visibleCount,
  filteredTotal,
  totals,
}: Props) {
  return (
    <aside className="panel-soft transactions-side-panel">
      <p className="eyebrow">Lectura del período</p>
      <h3>Resumen operativo</h3>

      <div className="transactions-side-list">
        <div>
          <span>Movimientos visibles</span>
          <strong>{visibleCount}</strong>
        </div>

        <div>
          <span>Resultado filtrado</span>
          <strong>{formatMoney(filteredTotal)}</strong>
        </div>

        <div>
          <span>Ignorados</span>
          <strong>{formatMoney(totals.ignored)}</strong>
        </div>

        <div>
          <span>Transferencias</span>
          <strong>{formatMoney(totals.transfers)}</strong>
        </div>

        <div>
          <span>Técnicos</span>
          <strong>{formatMoney(totals.technical)}</strong>
        </div>
      </div>

      <p className="muted">
        El balance principal excluye técnicos, transferencias, ajustes e
        ignorados.
      </p>
    </aside>
  );
}
