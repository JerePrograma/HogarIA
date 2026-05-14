import { Fragment, useMemo, useState } from 'react';
import { getApiErrorMessage } from '../../../api/http';
import { getMonthlyPlanSuggestions } from '../../../api/monthlyPlanSuggestionsApi';
import { StatusBadge } from '../../../components/ui/StatusBadge';
import {
  labelOrValue,
  monthlyPlanPriorityLabels,
  monthlyPlanStatusLabels,
  monthlyPlanTypeLabels,
} from '../../../domain/financeLabels';
import type {
  Account,
  Category,
  MonthlyPlanItem,
  MonthlyPlanItemUpdatePayload,
  MonthlyPlanPriority,
  MonthlyPlanStatus,
  PlanningSuggestionResponse,
} from '../../../domain/types';
import {
  canConvertPlanItem,
  formatPlanNet,
  getPlanItemMissingLabels,
  getPlanItemNextAction,
  isCancelledPlanItem,
  isDonePlanItem,
  isPendingPlanItem,
} from '../planningUtils';

export type TableFilterKey =
  | 'UNPRICED'
  | 'MISSING_CLASSIFICATION'
  | 'READY_TO_CONVERT'
  | 'DUE_NEXT_7_DAYS'
  | 'ALL';

type Props = {
  items: MonthlyPlanItem[];
  accounts: Account[];
  categories: Category[];
  onConvert: (id: string) => void;
  onCancel: (id: string) => void;
  onDelete: (id: string) => void;
  onUpdate: (id: string, payload: MonthlyPlanItemUpdatePayload) => Promise<void>;
  onMarkPaid: (id: string) => void;
  onMarkCollected: (id: string) => void;
  pendingActionId?: string | null;
  actionError?: string | null;
  profileId: string;
  externalFilterKey?: TableFilterKey;
};

type EditMode = 'FULL' | 'AMOUNT' | 'CONVERT';

type EditForm = {
  title: string;
  expectedDate: string;
  amount: string;
  minAmount: string;
  maxAmount: string;
  expectedRecoveryAmount: string;
  expectedRecoveryPercent: string;
  counterparty: string;
  status: MonthlyPlanStatus;
  priority: MonthlyPlanPriority;
  accountId: string;
  categoryId: string;
  installmentNumber: string;
  installmentTotal: string;
};

const toForm = (item: MonthlyPlanItem): EditForm => ({
  title: item.title,
  expectedDate: item.expectedDate ?? '',
  amount: item.amount == null ? '' : String(item.amount),
  minAmount: item.minAmount == null ? '' : String(item.minAmount),
  maxAmount: item.maxAmount == null ? '' : String(item.maxAmount),
  expectedRecoveryAmount:
    item.expectedRecoveryAmount == null ? '' : String(item.expectedRecoveryAmount),
  expectedRecoveryPercent:
    item.expectedRecoveryPercent == null ? '' : String(item.expectedRecoveryPercent),
  counterparty: item.counterparty ?? '',
  status: item.status,
  priority: item.priority,
  accountId: item.accountId ?? '',
  categoryId: item.categoryId ?? '',
  installmentNumber: item.installmentNumber == null ? '' : String(item.installmentNumber),
  installmentTotal: item.installmentTotal == null ? '' : String(item.installmentTotal),
});

const parseNullableNumber = (value: string): number | null => {
  if (!value.trim()) return null;
  const parsed = Number(value);
  return Number.isFinite(parsed) ? parsed : null;
};

const matchesExternalFilter = (item: MonthlyPlanItem, filter?: TableFilterKey): boolean => {
  if (!filter || filter === 'ALL') return true;

  if (filter === 'UNPRICED') {
    return item.amount == null && item.minAmount == null && item.maxAmount == null;
  }

  if (filter === 'MISSING_CLASSIFICATION') {
    return (
      !item.transactionId &&
      item.status !== 'CANCELLED' &&
      (!item.accountId || !item.categoryId)
    );
  }

  if (filter === 'READY_TO_CONVERT') {
    return canConvertPlanItem(item);
  }

  if (filter === 'DUE_NEXT_7_DAYS') {
    if (!item.expectedDate) return false;

    const today = new Date();
    const limit = new Date();
    limit.setDate(today.getDate() + 7);

    const expected = new Date(`${item.expectedDate}T00:00:00`);

    return expected >= new Date(today.toDateString()) && expected <= limit;
  }

  return true;
};

export function MonthlyPlanItemsTable({
  items,
  accounts,
  categories,
  onConvert,
  onCancel,
  onDelete,
  onUpdate,
  onMarkPaid,
  onMarkCollected,
  pendingActionId,
  actionError,
  profileId,
  externalFilterKey,
}: Props) {
  const [statusFilter, setStatusFilter] = useState('ALL');
  const [typeFilter, setTypeFilter] = useState('ALL');
  const [sortBy, setSortBy] = useState<'DATE' | 'PRIORITY' | 'AMOUNT'>('DATE');
  const [editingId, setEditingId] = useState<string | null>(null);
  const [editMode, setEditMode] = useState<EditMode>('FULL');
  const [form, setForm] = useState<EditForm | null>(null);
  const [editError, setEditError] = useState<string | null>(null);
  const [savingId, setSavingId] = useState<string | null>(null);
  const [suggestion, setSuggestion] = useState<PlanningSuggestionResponse | null>(null);
  const [expandedId, setExpandedId] = useState<string | null>(null);

  const filtered = useMemo(
    () =>
      items
        .filter(
          (item) =>
            matchesExternalFilter(item, externalFilterKey) &&
            (statusFilter === 'ALL' ||
              (statusFilter === 'PENDING'
                ? isPendingPlanItem(item)
                : statusFilter === 'DONE'
                  ? isDonePlanItem(item)
                  : isCancelledPlanItem(item))) &&
            (typeFilter === 'ALL' || item.type === typeFilter),
        )
        .sort((a, b) =>
          sortBy === 'DATE'
            ? (a.expectedDate ?? '').localeCompare(b.expectedDate ?? '')
            : sortBy === 'PRIORITY'
              ? a.priority.localeCompare(b.priority)
              : (b.netMax ?? 0) - (a.netMax ?? 0),
        ),
    [items, statusFilter, typeFilter, sortBy, externalFilterKey],
  );

  const startEdit = (item: MonthlyPlanItem, mode: EditMode) => {
    setEditingId(item.id);
    setEditMode(mode);
    setForm(toForm(item));
    setEditError(null);
    setSuggestion(null);
  };

  const closeEdit = () => {
    setEditingId(null);
    setForm(null);
    setEditError(null);
    setSuggestion(null);
  };

  const loadSuggestion = async (item: MonthlyPlanItem) => {
    const target = form ?? toForm(item);

    try {
      const response = await getMonthlyPlanSuggestions(profileId, {
        type: item.type,
        title: target.title,
        counterparty: target.counterparty || null,
        amount: parseNullableNumber(target.amount),
        minAmount: parseNullableNumber(target.minAmount),
        maxAmount: parseNullableNumber(target.maxAmount),
        expectedRecoveryAmount: parseNullableNumber(target.expectedRecoveryAmount),
        expectedRecoveryPercent: parseNullableNumber(target.expectedRecoveryPercent),
      });

      setSuggestion(response);
    } catch {
      setEditError('No se pudo obtener sugerencias en este momento.');
    }
  };

  const saveEdit = async (item: MonthlyPlanItem) => {
    if (!form) return;

    const amount = parseNullableNumber(form.amount);
    const minAmount = parseNullableNumber(form.minAmount);
    const maxAmount = parseNullableNumber(form.maxAmount);
    const expectedRecoveryAmount = parseNullableNumber(form.expectedRecoveryAmount);
    const expectedRecoveryPercent = parseNullableNumber(form.expectedRecoveryPercent);
    const installmentNumber = parseNullableNumber(form.installmentNumber);
    const installmentTotal = parseNullableNumber(form.installmentTotal);

    if (minAmount != null && maxAmount != null && minAmount > maxAmount) {
      setEditError('El monto mínimo no puede superar al máximo.');
      return;
    }

    if (
      expectedRecoveryPercent != null &&
      (expectedRecoveryPercent < 0 || expectedRecoveryPercent > 100)
    ) {
      setEditError('El recupero porcentual debe estar entre 0 y 100.');
      return;
    }

    if (installmentNumber != null && installmentTotal != null && installmentNumber > installmentTotal) {
      setEditError('La cuota no puede superar el total de cuotas.');
      return;
    }

    const clearInstallment =
      form.installmentNumber.trim() === '' &&
      form.installmentTotal.trim() === '' &&
      (item.installmentNumber != null || item.installmentTotal != null);

    const payload: MonthlyPlanItemUpdatePayload = {
      title: form.title.trim() || item.title,
      status: form.status,
      priority: form.priority,
      amount,
      minAmount,
      maxAmount,
      expectedRecoveryAmount,
      expectedRecoveryPercent,
      expectedDate: form.expectedDate || undefined,
      counterparty: form.counterparty || undefined,
      accountId: form.accountId || undefined,
      categoryId: form.categoryId || undefined,
      installmentNumber,
      installmentTotal,
      clearAmount: form.amount.trim() === '' && item.amount != null,
      clearRange:
        form.minAmount.trim() === '' &&
        form.maxAmount.trim() === '' &&
        (item.minAmount != null || item.maxAmount != null),
      clearRecovery:
        form.expectedRecoveryAmount.trim() === '' &&
        form.expectedRecoveryPercent.trim() === '' &&
        (item.expectedRecoveryAmount != null || item.expectedRecoveryPercent != null),
      clearExpectedDate: form.expectedDate.trim() === '' && Boolean(item.expectedDate),
      clearCounterparty: form.counterparty.trim() === '' && Boolean(item.counterparty),
      clearAccount: form.accountId.trim() === '' && Boolean(item.accountId),
      clearCategory: form.categoryId.trim() === '' && Boolean(item.categoryId),
      ...(clearInstallment ? { clearInstallment: true } : {}),
    };

    try {
      setSavingId(item.id);
      setEditError(null);
      await onUpdate(item.id, payload);
      closeEdit();
    } catch (error) {
      setEditError(getApiErrorMessage(error));
    } finally {
      setSavingId(null);
    }
  };

  return (
    <section className="panel">
      <div className="section-title">
        <div>
          <p className="eyebrow">Detalle</p>
          <h2>Ítems planificados</h2>
          <p className="secondary-text">
            Revisá, completá y convertí cada ítem cuando esté confirmado.
          </p>
        </div>

        <span className="badge-count">{filtered.length}</span>
      </div>

      <div className="form-row">
        <label>
          Estado
          <select
            className="input-ui"
            value={statusFilter}
            onChange={(event) => setStatusFilter(event.target.value)}
          >
            <option value="ALL">Todos</option>
            <option value="PENDING">Pendientes</option>
            <option value="DONE">Pagados/Cobrados</option>
            <option value="CANCELLED">Cancelados</option>
          </select>
        </label>

        <label>
          Tipo
          <select
            className="input-ui"
            value={typeFilter}
            onChange={(event) => setTypeFilter(event.target.value)}
          >
            <option value="ALL">Todos</option>
            <option value="INCOME">Ingresos</option>
            <option value="EXPENSE">Egresos</option>
            <option value="RECOVERY">Recuperos</option>
            <option value="TODO">TODO</option>
          </select>
        </label>

        <label>
          Orden
          <select
            className="input-ui"
            value={sortBy}
            onChange={(event) => setSortBy(event.target.value as 'DATE' | 'PRIORITY' | 'AMOUNT')}
          >
            <option value="DATE">Fecha</option>
            <option value="PRIORITY">Prioridad</option>
            <option value="AMOUNT">Monto</option>
          </select>
        </label>
      </div>

      {actionError ? <p className="mensaje-error">{actionError}</p> : null}

      <div className="tabla-ui mt-4">
        <table>
          <thead>
            <tr>
              <th>Fecha</th>
              <th>Título</th>
              <th>Tipo</th>
              <th>Neto</th>
              <th>Estado</th>
              <th>Prioridad</th>
              <th>Falta</th>
              <th>Acciones</th>
            </tr>
          </thead>

          <tbody>
            {filtered.map((item) => {
              const isEditing = editingId === item.id && form;
              const isBusy = pendingActionId === item.id || savingId === item.id;
              const nextAction = getPlanItemNextAction(item);

              return (
                <Fragment key={item.id}>
                  <tr>
                    <td>{item.expectedDate ?? '-'}</td>

                    <td>
                      <strong>{item.title}</strong>
                      {item.transactionId ? (
                        <span className="compact-muted"> · Convertido</span>
                      ) : null}
                    </td>

                    <td>{labelOrValue(monthlyPlanTypeLabels, item.type)}</td>

                    <td className="amount-cell">{formatPlanNet(item)}</td>

                    <td>
                      <StatusBadge label={labelOrValue(monthlyPlanStatusLabels, item.status)} />
                    </td>

                    <td>
                      <StatusBadge
                        label={labelOrValue(monthlyPlanPriorityLabels, item.priority)}
                      />
                    </td>

                    <td>
                      {getPlanItemMissingLabels(item).map((label) => (
                        <span key={label} className="missing-badge">
                          {label}
                        </span>
                      ))}
                    </td>

                    <td>
                      <div className="action-row">
                        <button
                          type="button"
                          className="boton-secundario"
                          onClick={() => startEdit(item, 'FULL')}
                          disabled={isBusy}
                        >
                          Editar
                        </button>

                        {nextAction === 'COMPLETE_AMOUNT' ? (
                          <button
                            type="button"
                            className="boton-secundario"
                            onClick={() => startEdit(item, 'AMOUNT')}
                            disabled={isBusy}
                          >
                            Completar monto
                          </button>
                        ) : null}

                        {nextAction === 'PREPARE_CONVERSION' ? (
                          <button
                            type="button"
                            className="boton-secundario"
                            onClick={() => startEdit(item, 'CONVERT')}
                            disabled={isBusy}
                          >
                            Preparar conversión
                          </button>
                        ) : null}

                        {nextAction === 'CONVERT' ? (
                          <button
                            type="button"
                            className="boton-principal"
                            onClick={() => onConvert(item.id)}
                            disabled={isBusy}
                          >
                            Convertir
                          </button>
                        ) : null}

                        {nextAction === 'MARK_PAID' ? (
                          <button
                            type="button"
                            className="boton-secundario"
                            onClick={() => onMarkPaid(item.id)}
                            disabled={isBusy}
                          >
                            Marcar pagado
                          </button>
                        ) : null}

                        {nextAction === 'MARK_COLLECTED' ? (
                          <button
                            type="button"
                            className="boton-secundario"
                            onClick={() => onMarkCollected(item.id)}
                            disabled={isBusy}
                          >
                            Marcar cobrado
                          </button>
                        ) : null}

                        <button
                          type="button"
                          className="boton-fantasma"
                          onClick={() => setExpandedId(expandedId === item.id ? null : item.id)}
                        >
                          Más
                        </button>
                      </div>
                    </td>
                  </tr>

                  {expandedId === item.id ? (
                    <tr>
                      <td colSpan={8}>
                        <div className="row-expanded-panel">
                          <button
                            type="button"
                            className="boton-secundario"
                            onClick={() => onCancel(item.id)}
                            disabled={isBusy || item.status === 'CANCELLED'}
                          >
                            Cancelar
                          </button>

                          <button
                            type="button"
                            className="boton-danger"
                            onClick={() => onDelete(item.id)}
                            disabled={isBusy}
                          >
                            Eliminar
                          </button>

                          <button
                            type="button"
                            className="boton-secundario"
                            onClick={() => void loadSuggestion(item)}
                            disabled={isBusy}
                          >
                            Sugerir cuenta/categoría
                          </button>

                          <button
                            type="button"
                            className="boton-secundario"
                            onClick={() => startEdit(item, 'FULL')}
                            disabled={isBusy}
                          >
                            Editar completo
                          </button>
                        </div>
                      </td>
                    </tr>
                  ) : null}

                  {isEditing ? (
                    <tr>
                      <td colSpan={8}>
                        <div className="panel-muted">
                          <div className="section-title">
                            <div>
                              <p className="eyebrow">Edición</p>
                              <h3>
                                {editMode === 'AMOUNT'
                                  ? 'Completar monto'
                                  : editMode === 'CONVERT'
                                    ? 'Preparar conversión'
                                    : 'Editar ítem'}
                              </h3>
                            </div>
                          </div>

                          {editMode !== 'AMOUNT' ? (
                            <label>
                              Título
                              <input
                                className="input-ui"
                                placeholder="Título"
                                value={form.title}
                                onChange={(event) =>
                                  setForm({ ...form, title: event.target.value })
                                }
                              />
                            </label>
                          ) : null}

                          <div className="form-row">
                            {editMode !== 'CONVERT' ? (
                              <label>
                                Fecha esperada
                                <input
                                  className="input-ui"
                                  type="date"
                                  value={form.expectedDate}
                                  onChange={(event) =>
                                    setForm({ ...form, expectedDate: event.target.value })
                                  }
                                />
                              </label>
                            ) : null}

                            <label>
                              Monto exacto
                              <input
                                className="input-ui"
                                placeholder="Monto exacto"
                                value={form.amount}
                                onChange={(event) =>
                                  setForm({ ...form, amount: event.target.value })
                                }
                              />
                            </label>

                            {editMode !== 'CONVERT' ? (
                              <>
                                <label>
                                  Monto mínimo
                                  <input
                                    className="input-ui"
                                    placeholder="Monto mínimo"
                                    value={form.minAmount}
                                    onChange={(event) =>
                                      setForm({ ...form, minAmount: event.target.value })
                                    }
                                  />
                                </label>

                                <label>
                                  Monto máximo
                                  <input
                                    className="input-ui"
                                    placeholder="Monto máximo"
                                    value={form.maxAmount}
                                    onChange={(event) =>
                                      setForm({ ...form, maxAmount: event.target.value })
                                    }
                                  />
                                </label>
                              </>
                            ) : null}
                          </div>

                          {editMode === 'FULL' ? (
                            <>
                              <div className="form-row">
                                <label>
                                  Recupero monto
                                  <input
                                    className="input-ui"
                                    placeholder="Recupero monto"
                                    value={form.expectedRecoveryAmount}
                                    onChange={(event) =>
                                      setForm({
                                        ...form,
                                        expectedRecoveryAmount: event.target.value,
                                      })
                                    }
                                  />
                                </label>

                                <label>
                                  Recupero %
                                  <input
                                    className="input-ui"
                                    placeholder="Recupero %"
                                    value={form.expectedRecoveryPercent}
                                    onChange={(event) =>
                                      setForm({
                                        ...form,
                                        expectedRecoveryPercent: event.target.value,
                                      })
                                    }
                                  />
                                </label>

                                <label>
                                  Contraparte
                                  <input
                                    className="input-ui"
                                    placeholder="Contraparte"
                                    value={form.counterparty}
                                    onChange={(event) =>
                                      setForm({ ...form, counterparty: event.target.value })
                                    }
                                  />
                                </label>
                              </div>

                              <div className="form-row">
                                <label>
                                  N° cuota
                                  <input
                                    className="input-ui"
                                    placeholder="N° cuota"
                                    value={form.installmentNumber}
                                    onChange={(event) =>
                                      setForm({ ...form, installmentNumber: event.target.value })
                                    }
                                  />
                                </label>

                                <label>
                                  Total cuotas
                                  <input
                                    className="input-ui"
                                    placeholder="Total cuotas"
                                    value={form.installmentTotal}
                                    onChange={(event) =>
                                      setForm({ ...form, installmentTotal: event.target.value })
                                    }
                                  />
                                </label>
                              </div>
                            </>
                          ) : null}

                          <div className="form-row">
                            <label>
                              Cuenta
                              <select
                                className="input-ui"
                                value={form.accountId}
                                onChange={(event) =>
                                  setForm({ ...form, accountId: event.target.value })
                                }
                              >
                                <option value="">Sin cuenta</option>
                                {accounts.map((account) => (
                                  <option key={account.id} value={account.id}>
                                    {account.name}
                                  </option>
                                ))}
                              </select>
                            </label>

                            <label>
                              Categoría
                              <select
                                className="input-ui"
                                value={form.categoryId}
                                onChange={(event) =>
                                  setForm({ ...form, categoryId: event.target.value })
                                }
                              >
                                <option value="">Sin categoría</option>
                                {categories.map((category) => (
                                  <option key={category.id} value={category.id}>
                                    {category.name}
                                  </option>
                                ))}
                              </select>
                            </label>

                            {editMode === 'FULL' ? (
                              <>
                                <label>
                                  Estado
                                  <select
                                    className="input-ui"
                                    value={form.status}
                                    onChange={(event) =>
                                      setForm({
                                        ...form,
                                        status: event.target.value as MonthlyPlanStatus,
                                      })
                                    }
                                  >
                                    {Object.entries(monthlyPlanStatusLabels).map(
                                      ([value, label]) => (
                                        <option key={value} value={value}>
                                          {label}
                                        </option>
                                      ),
                                    )}
                                  </select>
                                </label>

                                <label>
                                  Prioridad
                                  <select
                                    className="input-ui"
                                    value={form.priority}
                                    onChange={(event) =>
                                      setForm({
                                        ...form,
                                        priority: event.target.value as MonthlyPlanPriority,
                                      })
                                    }
                                  >
                                    {Object.entries(monthlyPlanPriorityLabels).map(
                                      ([value, label]) => (
                                        <option key={value} value={value}>
                                          {label}
                                        </option>
                                      ),
                                    )}
                                  </select>
                                </label>
                              </>
                            ) : null}
                          </div>

                          {editError ? <p className="mensaje-error">{editError}</p> : null}

                          {suggestion ? (
                            <div className="surface-inset">
                              <p>
                                {suggestion.confidence === 'NONE'
                                  ? 'No encontré historial suficiente para sugerir cuenta o categoría.'
                                  : 'Sugerencias disponibles.'}
                              </p>

                              {suggestion.confidence !== 'NONE' ? (
                                <button
                                  type="button"
                                  className="boton-secundario"
                                  onClick={() =>
                                    setForm({
                                      ...form,
                                      accountId: suggestion.accountSuggestion?.id ?? form.accountId,
                                      categoryId:
                                        suggestion.categorySuggestion?.id ?? form.categoryId,
                                    })
                                  }
                                >
                                  Aplicar sugerencia
                                </button>
                              ) : null}
                            </div>
                          ) : null}

                          <div className="table-actions">
                            <button
                              type="button"
                              className="boton-secundario"
                              onClick={() => void loadSuggestion(item)}
                              disabled={savingId === item.id}
                            >
                              Sugerir cuenta/categoría
                            </button>

                            <button
                              type="button"
                              className="boton-principal"
                              onClick={() => void saveEdit(item)}
                              disabled={savingId === item.id}
                            >
                              {savingId === item.id ? 'Guardando...' : 'Guardar'}
                            </button>

                            <button
                              type="button"
                              className="boton-secundario"
                              onClick={closeEdit}
                              disabled={savingId === item.id}
                            >
                              Cancelar
                            </button>
                          </div>
                        </div>
                      </td>
                    </tr>
                  ) : null}
                </Fragment>
              );
            })}
          </tbody>
        </table>
      </div>

      {filtered.length === 0 ? (
        <p className="mensaje-info mt-4">
          No hay ítems que coincidan con los filtros actuales.
        </p>
      ) : null}
    </section>
  );
}