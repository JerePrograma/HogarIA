import { Fragment, useMemo, useState } from 'react';
import { labelOrValue, monthlyPlanPriorityLabels, monthlyPlanStatusLabels, monthlyPlanTypeLabels } from '../../../domain/financeLabels';
import type { Account, Category, MonthlyPlanItem, MonthlyPlanItemUpdatePayload, MonthlyPlanPriority, MonthlyPlanStatus } from '../../../domain/types';
import { canConvertPlanItem, formatPlanNet, isCancelledPlanItem, isDonePlanItem, isPendingPlanItem } from '../planningUtils';
import { StatusBadge } from '../../../components/ui/StatusBadge';
import { getApiErrorMessage } from '../../../api/http';
import { getMonthlyPlanSuggestions } from '../../../api/monthlyPlanSuggestionsApi';
import type { PlanningSuggestionResponse } from '../../../domain/types';

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
  expectedRecoveryAmount: item.expectedRecoveryAmount == null ? '' : String(item.expectedRecoveryAmount),
  expectedRecoveryPercent: item.expectedRecoveryPercent == null ? '' : String(item.expectedRecoveryPercent),
  counterparty: item.counterparty ?? '',
  status: item.status,
  priority: item.priority,
  accountId: item.accountId ?? '',
  categoryId: item.categoryId ?? '',
  installmentNumber: item.installmentNumber == null ? '' : String(item.installmentNumber),
  installmentTotal: item.installmentTotal == null ? '' : String(item.installmentTotal)
});

const parseNullableNumber = (value: string): number | null => {
  if (!value.trim()) return null;
  const parsed = Number(value);
  return Number.isFinite(parsed) ? parsed : null;
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
  profileId
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

  const filtered = useMemo(
    () =>
      items
        .filter(
          (i) =>
            (statusFilter === 'ALL' ||
              (statusFilter === 'PENDING'
                ? isPendingPlanItem(i)
                : statusFilter === 'DONE'
                  ? isDonePlanItem(i)
                  : isCancelledPlanItem(i))) &&
            (typeFilter === 'ALL' || i.type === typeFilter)
        )
        .sort((a, b) =>
          sortBy === 'DATE'
            ? (a.expectedDate ?? '').localeCompare(b.expectedDate ?? '')
            : sortBy === 'PRIORITY'
              ? a.priority.localeCompare(b.priority)
              : (b.netMax ?? 0) - (a.netMax ?? 0)
        ),
    [items, statusFilter, typeFilter, sortBy]
  );

  const startEdit = (item: MonthlyPlanItem, mode: EditMode) => {
    setEditingId(item.id);
    setEditMode(mode);
    setForm(toForm(item));
    setEditError(null);
    setSuggestion(null);
  };


  const loadSuggestion = async (item: MonthlyPlanItem) => {
    const target = form ?? toForm(item);
    try {
      const res = await getMonthlyPlanSuggestions(profileId, { type: item.type, title: target.title, counterparty: target.counterparty || null, amount: parseNullableNumber(target.amount), minAmount: parseNullableNumber(target.minAmount), maxAmount: parseNullableNumber(target.maxAmount), expectedRecoveryAmount: parseNullableNumber(target.expectedRecoveryAmount), expectedRecoveryPercent: parseNullableNumber(target.expectedRecoveryPercent) });
      setSuggestion(res);
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

    if (expectedRecoveryPercent != null && (expectedRecoveryPercent < 0 || expectedRecoveryPercent > 100)) {
      setEditError('El recupero porcentual debe estar entre 0 y 100.');
      return;
    }

    if (installmentNumber != null && installmentTotal != null && installmentNumber > installmentTotal) {
      setEditError('La cuota no puede superar el total de cuotas.');
      return;
    }

    const hadAmount = item.amount != null;
    const hadRange = item.minAmount != null || item.maxAmount != null;
    const hadRecovery = item.expectedRecoveryAmount != null || item.expectedRecoveryPercent != null;
    const hadExpectedDate = Boolean(item.expectedDate);
    const hadCounterparty = Boolean(item.counterparty);
    const hadAccount = Boolean(item.accountId);
    const hadCategory = Boolean(item.categoryId);
    const hadInstallment = item.installmentNumber != null || item.installmentTotal != null;

    const clearInstallment = form.installmentNumber.trim() === '' && form.installmentTotal.trim() === '' && hadInstallment;

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
      clearAmount: form.amount.trim() === '' && hadAmount,
      clearRange: form.minAmount.trim() === '' && form.maxAmount.trim() === '' && hadRange,
      clearRecovery: form.expectedRecoveryAmount.trim() === '' && form.expectedRecoveryPercent.trim() === '' && hadRecovery,
      clearExpectedDate: form.expectedDate.trim() === '' && hadExpectedDate,
      clearCounterparty: form.counterparty.trim() === '' && hadCounterparty,
      clearAccount: form.accountId.trim() === '' && hadAccount,
      clearCategory: form.categoryId.trim() === '' && hadCategory,
      ...(clearInstallment ? { clearInstallment: true } : {})
    };

    try {
      setSavingId(item.id);
      setEditError(null);
      await onUpdate(item.id, payload);
      setEditingId(null);
      setForm(null);
    } catch (error) {
      setEditError(getApiErrorMessage(error));
    } finally {
      setSavingId(null);
    }
  };

  return (
    <section className='card'>
      <h3 className='section-title'>Items planificados</h3>
      <div className='form-row'>
        <select className='select' value={statusFilter} onChange={(e) => setStatusFilter(e.target.value)}>
          <option value='ALL'>Todos</option>
          <option value='PENDING'>Pendientes</option>
          <option value='DONE'>Pagados-Cobrados</option>
          <option value='CANCELLED'>Cancelados</option>
        </select>
        <select className='select' value={typeFilter} onChange={(e) => setTypeFilter(e.target.value)}>
          <option value='ALL'>Todos</option><option value='INCOME'>Ingresos</option><option value='EXPENSE'>Egresos</option><option value='RECOVERY'>Recuperos</option><option value='TODO'>TODO</option>
        </select>
        <select className='select' value={sortBy} onChange={(e) => setSortBy(e.target.value as 'DATE' | 'PRIORITY' | 'AMOUNT')}>
          <option value='DATE'>Fecha</option><option value='PRIORITY'>Prioridad</option><option value='AMOUNT'>Monto</option>
        </select>
      </div>
      {actionError ? <p className='compact-muted' role='alert'>{actionError}</p> : null}
      <table className='table'>
        <thead><tr><th>Fecha</th><th>Título</th><th>Tipo</th><th>Neto</th><th>Estado</th><th>Prioridad</th><th>Acciones</th></tr></thead>
        <tbody>
          {filtered.map((i) => {
            const canConvert = canConvertPlanItem(i);
            const isEditing = editingId === i.id && form;
            const isBusy = pendingActionId === i.id || savingId === i.id;
            const showMarkPaid = ['EXPENSE', 'SAVING', 'DEBT', 'TRANSFER'].includes(i.type) && !['PAID', 'CANCELLED'].includes(i.status);
            const showMarkCollected = ['INCOME', 'RECOVERY'].includes(i.type) && !['COLLECTED', 'CANCELLED'].includes(i.status);
            const showCompleteAmount = i.amount == null && i.minAmount == null && i.maxAmount == null && i.status !== 'CANCELLED';

            return (
              <Fragment key={i.id}>
                <tr>
                  <td>{i.expectedDate ?? '-'}</td><td>{i.title} {i.transactionId && <span className='compact-muted'>· Convertido</span>}</td><td>{labelOrValue(monthlyPlanTypeLabels, i.type)}</td><td>{formatPlanNet(i)}</td><td><StatusBadge label={labelOrValue(monthlyPlanStatusLabels, i.status)} /></td><td><StatusBadge label={labelOrValue(monthlyPlanPriorityLabels, i.priority)} /></td>
                  <td className='table-actions'>
                    <button className='button-secondary' onClick={() => startEdit(i, 'FULL')} disabled={isBusy}>Editar</button>
                    {showMarkPaid ? <button className='button-secondary' onClick={() => onMarkPaid(i.id)} disabled={isBusy}>Marcar pagado</button> : null}
                    {showMarkCollected ? <button className='button-secondary' onClick={() => onMarkCollected(i.id)} disabled={isBusy}>Marcar cobrado</button> : null}
                    {showCompleteAmount ? <button className='button-secondary' onClick={() => startEdit(i, 'AMOUNT')} disabled={isBusy}>Completar monto</button> : null}
                    {canConvert ? <button className='button-primary' onClick={() => onConvert(i.id)} disabled={isBusy}>Convertir</button> : <button className='button-secondary' onClick={() => startEdit(i, 'CONVERT')} disabled={isBusy || i.status === 'CANCELLED' || Boolean(i.transactionId)}>Preparar conversión</button>}
                    <button className='button-secondary' onClick={() => onCancel(i.id)} disabled={isBusy || i.status === 'CANCELLED'}>Cancelar</button>
                    <button className='button-danger' onClick={() => onDelete(i.id)} disabled={isBusy}>Eliminar</button>
                  </td>
                </tr>
                {isEditing ? (
                  <tr>
                    <td colSpan={7}>
                      <div className='card'>
                        {editMode !== 'AMOUNT' ? <input className='input' placeholder='Título' value={form.title} onChange={(e) => setForm({ ...form, title: e.target.value })} /> : null}
                        <div className='form-row'>
                          {editMode !== 'CONVERT' ? <input className='input' type='date' value={form.expectedDate} onChange={(e) => setForm({ ...form, expectedDate: e.target.value })} /> : null}
                          <input className='input' placeholder='Monto exacto' value={form.amount} onChange={(e) => setForm({ ...form, amount: e.target.value })} />
                          {editMode !== 'CONVERT' ? <><input className='input' placeholder='Monto mínimo' value={form.minAmount} onChange={(e) => setForm({ ...form, minAmount: e.target.value })} /><input className='input' placeholder='Monto máximo' value={form.maxAmount} onChange={(e) => setForm({ ...form, maxAmount: e.target.value })} /></> : null}
                        </div>
                        {editMode === 'FULL' ? <div className='form-row'><input className='input' placeholder='Recupero monto' value={form.expectedRecoveryAmount} onChange={(e) => setForm({ ...form, expectedRecoveryAmount: e.target.value })} /><input className='input' placeholder='Recupero %' value={form.expectedRecoveryPercent} onChange={(e) => setForm({ ...form, expectedRecoveryPercent: e.target.value })} /><input className='input' placeholder='Contraparte' value={form.counterparty} onChange={(e) => setForm({ ...form, counterparty: e.target.value })} /></div> : null}
                        {editMode === 'FULL' ? <div className='form-row'><input className='input' placeholder='N° cuota' value={form.installmentNumber} onChange={(e) => setForm({ ...form, installmentNumber: e.target.value })} /><input className='input' placeholder='Total cuotas' value={form.installmentTotal} onChange={(e) => setForm({ ...form, installmentTotal: e.target.value })} /></div> : null}
                        <div className='form-row'>
                          <select className='select' value={form.accountId} onChange={(e) => setForm({ ...form, accountId: e.target.value })}><option value=''>Sin cuenta</option>{accounts.map((a) => <option key={a.id} value={a.id}>{a.name}</option>)}</select>
                          <select className='select' value={form.categoryId} onChange={(e) => setForm({ ...form, categoryId: e.target.value })}><option value=''>Sin categoría</option>{categories.map((c) => <option key={c.id} value={c.id}>{c.name}</option>)}</select>
                          {editMode === 'FULL' ? <><select className='select' value={form.status} onChange={(e) => setForm({ ...form, status: e.target.value as MonthlyPlanStatus })}>{Object.entries(monthlyPlanStatusLabels).map(([value, label]) => <option key={value} value={value}>{label}</option>)}</select><select className='select' value={form.priority} onChange={(e) => setForm({ ...form, priority: e.target.value as MonthlyPlanPriority })}>{Object.entries(monthlyPlanPriorityLabels).map(([value, label]) => <option key={value} value={value}>{label}</option>)}</select></> : null}
                        </div>
                        {editError ? <p className='compact-muted' role='alert'>{editError}</p> : null}{suggestion ? <div><p>{suggestion.confidence === 'NONE' ? 'No encontré historial suficiente para sugerir cuenta o categoría.' : 'Sugerencias disponibles.'}</p>{suggestion.confidence !== 'NONE' ? <button className='button-secondary' onClick={() => setForm({ ...form, accountId: suggestion.accountSuggestion?.id ?? form.accountId, categoryId: suggestion.categorySuggestion?.id ?? form.categoryId })}>Aplicar</button> : null}</div> : null}
                        <div className='table-actions'>
                          <button className='button-secondary' onClick={() => void loadSuggestion(i)} disabled={savingId === i.id}>Sugerir cuenta/categoría</button>
                          <button className='button-primary' onClick={() => void saveEdit(i)} disabled={savingId === i.id}>Guardar</button>
                          <button className='button-secondary' onClick={() => { setEditingId(null); setForm(null); setEditError(null); }} disabled={savingId === i.id}>Cancelar</button>
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
    </section>
  );
}
