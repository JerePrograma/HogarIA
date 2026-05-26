import { useMemo, useState } from 'react';
import { Link } from 'react-router-dom';
import { useMonthlyPlanItemActions } from '../../hooks/useMonthlyPlanItemActions';
import { useStructuredPlanItemDraft } from '../../hooks/useStructuredPlanItemDraft';
import { formatMoney } from '../../../../domain/formatters';
import {
  labelOrValue,
  monthlyPlanStatusLabels,
  monthlyPlanTypeLabels,
  movementTypeLabels,
  transactionStatusLabels,
} from '../../../../domain/financeLabels';
import type { Account, Category, MonthlyPlanItem, MovementType, TransactionStatus } from '../../../../domain/types';
import { usePlanningData } from '../../hooks/usePlanningData';
import { canConvertPlanItem, formatPlanAmount, getPlanItemMissingLabels, hasExactAmount } from '../../planningUtils';
import { preservePlanningPeriodParams } from '../../planningSearchParams';
import { planningRoutes } from '../../planningRoutes';
import { buildPlanningPath } from '../../utils/buildPlanningPath';

const nonConvertibleTypes = new Set<MonthlyPlanItem['type']>(['TODO', 'TRANSFER', 'RECOVERY']);

export function MonthlyPlanConvertPage() {
  const { items, profileId, year, month, accounts, categories, invalidatePlanningViews, searchParams } = usePlanningData();
  const draft = useStructuredPlanItemDraft(year, month);
  const actions = useMonthlyPlanItemActions({ profileId, year, month, form: draft.form, resetFormAfterCreate: draft.resetAfterCreate, invalidatePlanningViews });
  const [confirmingItemId, setConfirmingItemId] = useState<string | null>(null);

  const accountById = useMemo(() => new Map(accounts.map((account) => [account.id, account])), [accounts]);
  const categoryById = useMemo(() => new Map(categories.map((category) => [category.id, category])), [categories]);

  const grouped = useMemo(() => {
    const ready: MonthlyPlanItem[] = [];
    const blocked: MonthlyPlanItem[] = [];
    const converted: MonthlyPlanItem[] = [];
    const notConvertible: MonthlyPlanItem[] = [];

    for (const item of items) {
      if (item.transactionId) {
        converted.push(item);
      } else if (nonConvertibleTypes.has(item.type)) {
        notConvertible.push(item);
      } else if (canConvertPlanItem(item)) {
        ready.push(item);
      } else {
        blocked.push(item);
      }
    }

    return { ready, blocked, converted, notConvertible };
  }, [items]);

  const editLink = (id: string) =>
    buildPlanningPath(planningRoutes.itemEdit(profileId, id), preservePlanningPeriodParams(searchParams));

  return (
    <section className="panel">
      <div className="section-title">
        <div>
          <p className="eyebrow">Crear movimientos</p>
          <h2>Convertir planificación en movimientos reales</h2>
          <p className="secondary-text">
            Convertir crea un movimiento nuevo desde un ítem planificado. Si el movimiento ya existe, usá Vincular reales.
          </p>
        </div>
      </div>

      {actions.actionErrorMessage ? <p className="mensaje-error">{actions.actionErrorMessage}</p> : null}

      <ConversionSection
        title="Listos para convertir"
        description="Tienen monto exacto, cuenta, categoría y un tipo convertible."
        items={grouped.ready}
        accountById={accountById}
        categoryById={categoryById}
        actionFor={(item) => (
          <button
            type="button"
            className="boton-principal"
            onClick={() => setConfirmingItemId(item.id)}
            disabled={actions.pendingActionId === item.id}
          >
            Revisar y crear
          </button>
        )}
        confirmingItemId={confirmingItemId}
        onCancelConfirmation={() => setConfirmingItemId(null)}
        onConfirm={(item) => {
          actions.convert(item.id);
          setConfirmingItemId(null);
        }}
        isBusy={(item) => actions.pendingActionId === item.id}
      />

      <ConversionSection
        title="Bloqueados"
        description="Todavía les falta un dato operativo o están cancelados."
        items={grouped.blocked}
        accountById={accountById}
        categoryById={categoryById}
        actionFor={(item) => (
          <Link className="boton-secundario" to={editLink(item.id)}>
            {recommendedAction(item)}
          </Link>
        )}
      />

      <ConversionSection
        title="Ya convertidos"
        description="Ya tienen un movimiento creado y no se pueden convertir otra vez."
        items={grouped.converted}
        accountById={accountById}
        categoryById={categoryById}
        actionFor={() => <span className="badge-ui badge-ok">Convertido</span>}
      />

      <ConversionSection
        title="No convertibles por tipo"
        description="Tareas, transferencias y recuperos quedan fuera de la conversión automática."
        items={grouped.notConvertible}
        accountById={accountById}
        categoryById={categoryById}
        actionFor={() => <span className="badge-ui badge-warning">No convertible</span>}
      />
    </section>
  );
}

type ConversionSectionProps = {
  title: string;
  description: string;
  items: MonthlyPlanItem[];
  accountById: Map<string, Account>;
  categoryById: Map<string, Category>;
  actionFor: (item: MonthlyPlanItem) => JSX.Element;
  confirmingItemId?: string | null;
  onCancelConfirmation?: () => void;
  onConfirm?: (item: MonthlyPlanItem) => void;
  isBusy?: (item: MonthlyPlanItem) => boolean;
};

function ConversionSection({
  title,
  description,
  items,
  accountById,
  categoryById,
  actionFor,
  confirmingItemId,
  onCancelConfirmation,
  onConfirm,
  isBusy,
}: ConversionSectionProps) {
  return (
    <section className="surface-inset mt-4">
      <div className="section-title">
        <div>
          <h3>{title}</h3>
          <p className="secondary-text">{description}</p>
        </div>
        <span className="badge-ui badge-info">{items.length}</span>
      </div>

      {items.length === 0 ? (
        <p className="muted">No hay ítems en esta sección.</p>
      ) : (
        <div className="tabla-ui">
          <table className="table-compact">
            <thead>
              <tr>
                <th>Ítem</th>
                <th>Tipo</th>
                <th>Fecha esperada</th>
                <th>Período operativo</th>
                <th>Monto</th>
                <th>Cuenta</th>
                <th>Categoría</th>
                <th>Estado</th>
                <th>Faltantes</th>
                <th>Acción</th>
              </tr>
            </thead>
            <tbody>
              {items.map((item) => (
                <tr key={item.id}>
                  <td>
                    <strong>{item.title}</strong>
                    {confirmingItemId === item.id && onConfirm && onCancelConfirmation ? (
                      <ConversionConfirmation
                        item={item}
                        onCancel={onCancelConfirmation}
                        onConfirm={() => onConfirm(item)}
                        isBusy={Boolean(isBusy?.(item))}
                      />
                    ) : null}
                  </td>
                  <td>{labelOrValue(monthlyPlanTypeLabels, item.type)}</td>
                  <td>{item.expectedDate ?? '-'}</td>
                  <td>{item.periodMonth}/{item.periodYear}</td>
                  <td>{formatPlanAmount(item)}</td>
                  <td>{accountById.get(item.accountId ?? '')?.name ?? 'Sin cuenta'}</td>
                  <td>{categoryById.get(item.categoryId ?? '')?.name ?? 'Sin categoría'}</td>
                  <td>{labelOrValue(monthlyPlanStatusLabels, item.status)}</td>
                  <td>{getPlanItemMissingLabels(item).join(', ')}</td>
                  <td>{actionFor(item)}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
    </section>
  );
}

function ConversionConfirmation({
  item,
  onCancel,
  onConfirm,
  isBusy,
}: {
  item: MonthlyPlanItem;
  onCancel: () => void;
  onConfirm: () => void;
  isBusy: boolean;
}) {
  const preview = conversionPreview(item);

  return (
    <div className="surface-inset mt-2">
      <p className="label-ui">Movimiento que se va a crear</p>
      <dl className="plan-item-card-grid">
        <div>
          <dt>Tipo</dt>
          <dd>{labelOrValue(movementTypeLabels, preview.movementType)}</dd>
        </div>
        <div>
          <dt>Fecha real</dt>
          <dd>{preview.realDate}</dd>
        </div>
        <div>
          <dt>Período presupuesto</dt>
          <dd>{preview.budgetDate}</dd>
        </div>
        <div>
          <dt>Monto</dt>
          <dd>{formatMoney(preview.amount, item.currency)}</dd>
        </div>
        <div>
          <dt>Estado</dt>
          <dd>{labelOrValue(transactionStatusLabels, preview.status)}</dd>
        </div>
      </dl>

      <div className="form-actions">
        <button type="button" className="boton-principal" onClick={onConfirm} disabled={isBusy}>
          {isBusy ? 'Creando...' : 'Crear movimiento'}
        </button>
        <button type="button" className="boton-secundario" onClick={onCancel} disabled={isBusy}>
          Cancelar
        </button>
      </div>
    </div>
  );
}

function recommendedAction(item: MonthlyPlanItem): string {
  if (item.status === 'CANCELLED') return 'Editar ítem';
  if (!hasExactAmount(item)) return 'Completar monto';
  if (!item.accountId || !item.categoryId) return 'Agregar cuenta/categoría';
  return 'Editar ítem';
}

function conversionPreview(item: MonthlyPlanItem): {
  movementType: MovementType;
  realDate: string;
  budgetDate: string;
  amount: number;
  status: TransactionStatus;
} {
  return {
    movementType: movementTypeForPlanItem(item),
    realDate: item.expectedDate ?? todayLocalIsoDate(),
    budgetDate: `${item.periodYear}-${pad2(item.periodMonth)}-01`,
    amount: exactAmount(item),
    status: item.status === 'PAID' || item.status === 'COLLECTED' ? 'CONFIRMED' : 'PENDING',
  };
}

function movementTypeForPlanItem(item: MonthlyPlanItem): MovementType {
  if (item.type === 'INCOME') return 'INCOME';
  if (item.type === 'SAVING') return 'SAVING';
  return 'EXPENSE';
}

function exactAmount(item: MonthlyPlanItem): number {
  if (item.amount != null) return item.amount;
  if (item.minAmount != null && item.maxAmount != null && item.minAmount === item.maxAmount) {
    return item.minAmount;
  }
  return 0;
}

function todayLocalIsoDate(): string {
  const now = new Date();
  return `${now.getFullYear()}-${pad2(now.getMonth() + 1)}-${pad2(now.getDate())}`;
}

function pad2(value: number): string {
  return String(value).padStart(2, '0');
}
