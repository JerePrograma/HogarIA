import { EmptyState } from '../../../../shared/ui/EmptyState';
import { formatMoney } from '../../../../domain/formatters';
import { labelOrValue, movementTypeLabels, transactionStatusLabels } from '../../../../domain/financeLabels';
import type { Account, Category } from '../../../../domain/types';
import type { UnplannedTransaction } from './types';

type Props = {
  transactions: UnplannedTransaction[];
  accounts: Account[];
  categories: Category[];
};

const operationalKindLabel: Record<string, string> = {
  GASTO_NO_PLANIFICADO: 'Gasto no planificado',
  INGRESO_NO_PLANIFICADO: 'Ingreso no planificado',
  AHORRO_NO_PLANIFICADO: 'Ahorro no planificado',
  EXCLUIDO: 'Excluido',
};

export function UnplannedTransactionsPanel({ transactions, accounts, categories }: Props) {
  const accountById = new Map(accounts.map((account) => [account.id, account]));
  const categoryById = new Map(categories.map((category) => [category.id, category]));

  return (
    <section className="surface-inset mt-4">
      <div className="section-title">
        <div>
          <h3>Movimientos reales sin plan</h3>
          <p className="secondary-text">
            Son movimientos confirmados del período que todavía no tienen vínculo con un compromiso planificado. Transferencias, ajustes y técnicos quedan fuera de esta lista.
          </p>
        </div>
        <span className="badge-ui badge-info">{transactions.length}</span>
      </div>

      {!transactions.length ? (
        <EmptyState title="Sin movimientos reales sin plan" message="No hay movimientos operativos pendientes de vincular en este período." />
      ) : (
        <div className="tabla-ui">
          <table className="table-compact">
            <thead>
              <tr>
                <th>Movimiento</th>
                <th>Tipo</th>
                <th>Fecha real</th>
                <th>Período</th>
                <th>Monto</th>
                <th>Cuenta</th>
                <th>Categoría</th>
                <th>Estado</th>
              </tr>
            </thead>
            <tbody>
              {transactions.map((transaction) => (
                <tr key={transaction.transactionId}>
                  <td>
                    <strong>{transaction.description || 'Sin descripción'}</strong>
                    <p className="compact-muted">{operationalKindLabel[transaction.operationalKind ?? ''] ?? 'Movimiento operativo'}</p>
                  </td>
                  <td>{labelOrValue(movementTypeLabels, transaction.movementType)}</td>
                  <td>{transaction.realDate ?? '-'}</td>
                  <td>{transaction.budgetDate}</td>
                  <td>{formatMoney(transaction.amount)}</td>
                  <td>{accountById.get(transaction.accountId)?.name ?? 'Sin cuenta'}</td>
                  <td>{categoryById.get(transaction.categoryId ?? '')?.name ?? 'Sin categoría'}</td>
                  <td>{labelOrValue(transactionStatusLabels, transaction.status)}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
    </section>
  );
}
