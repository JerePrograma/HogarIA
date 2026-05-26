import { EmptyState } from '../../../../components/ui/EmptyState';
import { formatMoney } from '../../../../domain/formatters';
import { labelOrValue, monthlyPlanStatusLabels, monthlyPlanTypeLabels } from '../../../../domain/financeLabels';
import type { Account, Category } from '../../../../domain/types';
import type { PlanItemReconciliation } from './types';
import { TransactionMatchList } from './TransactionMatchList';

type Props = {
  title: string;
  items: PlanItemReconciliation[];
  accounts: Account[];
  categories: Category[];
  isDeleting: boolean;
  onDelete: (id: string) => void;
};

const statusLabel: Record<string, string> = {
  PENDING: 'Sin vínculo',
  PARTIAL: 'Parcial',
  MATCHED: 'Conciliado',
};

export function PlanItemsReconciliationPanel({ title, items, accounts, categories, isDeleting, onDelete }: Props) {
  const accountById = new Map(accounts.map((account) => [account.id, account]));
  const categoryById = new Map(categories.map((category) => [category.id, category]));

  return (
    <section className="surface-inset mt-4">
      <div className="section-title">
        <div>
          <h3>{title}</h3>
          <p className="secondary-text">
            Acá se ve qué parte de la planificación ya tiene movimiento real vinculado y qué sigue pendiente.
          </p>
        </div>
        <span className="badge-ui badge-info">{items.length}</span>
      </div>

      {!items.length ? (
        <EmptyState title="Sin ítems para mostrar" message="No hay compromisos en esta sección para el período." />
      ) : (
        <div className="tabla-ui">
          <table className="table-compact">
            <thead>
              <tr>
                <th>Compromiso</th>
                <th>Tipo</th>
                <th>Fecha esperada</th>
                <th>Período</th>
                <th>Planificado</th>
                <th>Vinculado</th>
                <th>Falta</th>
                <th>Cuenta/Categoría</th>
                <th>Qué falta</th>
                <th>Vínculos</th>
              </tr>
            </thead>
            <tbody>
              {items.map((item) => (
                <tr key={item.itemId}>
                  <td>
                    <strong>{item.title}</strong>
                    <p className="compact-muted">{labelOrValue(monthlyPlanStatusLabels, item.status)}</p>
                  </td>
                  <td>{labelOrValue(monthlyPlanTypeLabels, item.type)}</td>
                  <td>{item.expectedDate ?? '-'}</td>
                  <td>{item.periodMonth}/{item.periodYear}</td>
                  <td>{formatMoney(item.plannedAmount)}</td>
                  <td>{formatMoney(item.matchedAmount)}</td>
                  <td>{formatMoney(item.remainingAmount)}</td>
                  <td>
                    {accountById.get(item.accountId ?? '')?.name ?? 'Sin cuenta'} · {categoryById.get(item.categoryId ?? '')?.name ?? 'Sin categoría'}
                  </td>
                  <td>{missingReason(item).join(', ')}</td>
                  <td>
                    <strong>{statusLabel[item.executionStatus] ?? item.executionStatus}</strong>
                    <TransactionMatchList matches={item.matches} onDelete={onDelete} isDeleting={isDeleting} />
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
    </section>
  );
}

function missingReason(item: PlanItemReconciliation): string[] {
  if (item.executionStatus === 'MATCHED') return ['Conciliado'];

  const reasons: string[] = [];

  if (item.expectedDate) {
    reasons.push(item.expectedDate < todayLocalIsoDate() ? 'Vencido sin movimiento' : 'Todavía no venció');
  }

  if (!item.accountId) reasons.push('Falta cuenta');
  if (!item.categoryId) reasons.push('Falta categoría');
  if (!item.plannedAmount || item.plannedAmount <= 0) reasons.push('Falta monto exacto');

  if (canBeConverted(item)) {
    reasons.push('Puede convertirse');
  }

  reasons.push('Puede vincularse si el movimiento real ya existe');

  return reasons;
}

function canBeConverted(item: PlanItemReconciliation): boolean {
  return (
    item.plannedAmount > 0 &&
    Boolean(item.accountId) &&
    Boolean(item.categoryId) &&
    !['TODO', 'TRANSFER', 'RECOVERY'].includes(String(item.type)) &&
    item.status !== 'CANCELLED'
  );
}

function todayLocalIsoDate(): string {
  const now = new Date();
  return `${now.getFullYear()}-${String(now.getMonth() + 1).padStart(2, '0')}-${String(now.getDate()).padStart(2, '0')}`;
}
