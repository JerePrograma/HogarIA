import { jsx as _jsx, jsxs as _jsxs, Fragment as _Fragment } from "react/jsx-runtime";
import { Fragment, useMemo, useState } from 'react';
import { labelOrValue, monthlyPlanPriorityLabels, monthlyPlanStatusLabels, monthlyPlanTypeLabels } from '../../../domain/financeLabels';
import { canConvertPlanItem, formatPlanNet, isCancelledPlanItem, isDonePlanItem, isPendingPlanItem } from '../planningUtils';
import { StatusBadge } from '../../../components/ui/StatusBadge';
import { getApiErrorMessage } from '../../../api/http';
import { getMonthlyPlanSuggestions } from '../../../api/monthlyPlanSuggestionsApi';
const toForm = (item) => ({
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
const parseNullableNumber = (value) => {
    if (!value.trim())
        return null;
    const parsed = Number(value);
    return Number.isFinite(parsed) ? parsed : null;
};
export function MonthlyPlanItemsTable({ items, accounts, categories, onConvert, onCancel, onDelete, onUpdate, onMarkPaid, onMarkCollected, pendingActionId, actionError, profileId }) {
    const [statusFilter, setStatusFilter] = useState('ALL');
    const [typeFilter, setTypeFilter] = useState('ALL');
    const [sortBy, setSortBy] = useState('DATE');
    const [editingId, setEditingId] = useState(null);
    const [editMode, setEditMode] = useState('FULL');
    const [form, setForm] = useState(null);
    const [editError, setEditError] = useState(null);
    const [savingId, setSavingId] = useState(null);
    const [suggestion, setSuggestion] = useState(null);
    const filtered = useMemo(() => items
        .filter((i) => (statusFilter === 'ALL' ||
        (statusFilter === 'PENDING'
            ? isPendingPlanItem(i)
            : statusFilter === 'DONE'
                ? isDonePlanItem(i)
                : isCancelledPlanItem(i))) &&
        (typeFilter === 'ALL' || i.type === typeFilter))
        .sort((a, b) => sortBy === 'DATE'
        ? (a.expectedDate ?? '').localeCompare(b.expectedDate ?? '')
        : sortBy === 'PRIORITY'
            ? a.priority.localeCompare(b.priority)
            : (b.netMax ?? 0) - (a.netMax ?? 0)), [items, statusFilter, typeFilter, sortBy]);
    const startEdit = (item, mode) => {
        setEditingId(item.id);
        setEditMode(mode);
        setForm(toForm(item));
        setEditError(null);
        setSuggestion(null);
    };
    const loadSuggestion = async (item) => {
        const target = form ?? toForm(item);
        try {
            const res = await getMonthlyPlanSuggestions(profileId, { type: item.type, title: target.title, counterparty: target.counterparty || null, amount: parseNullableNumber(target.amount), minAmount: parseNullableNumber(target.minAmount), maxAmount: parseNullableNumber(target.maxAmount), expectedRecoveryAmount: parseNullableNumber(target.expectedRecoveryAmount), expectedRecoveryPercent: parseNullableNumber(target.expectedRecoveryPercent) });
            setSuggestion(res);
        }
        catch {
            setEditError('No se pudo obtener sugerencias en este momento.');
        }
    };
    const saveEdit = async (item) => {
        if (!form)
            return;
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
        const payload = {
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
        }
        catch (error) {
            setEditError(getApiErrorMessage(error));
        }
        finally {
            setSavingId(null);
        }
    };
    return (_jsxs("section", { className: 'card', children: [_jsx("h3", { className: 'section-title', children: "Items planificados" }), _jsxs("div", { className: 'form-row', children: [_jsxs("select", { className: 'select', value: statusFilter, onChange: (e) => setStatusFilter(e.target.value), children: [_jsx("option", { value: 'ALL', children: "Todos" }), _jsx("option", { value: 'PENDING', children: "Pendientes" }), _jsx("option", { value: 'DONE', children: "Pagados-Cobrados" }), _jsx("option", { value: 'CANCELLED', children: "Cancelados" })] }), _jsxs("select", { className: 'select', value: typeFilter, onChange: (e) => setTypeFilter(e.target.value), children: [_jsx("option", { value: 'ALL', children: "Todos" }), _jsx("option", { value: 'INCOME', children: "Ingresos" }), _jsx("option", { value: 'EXPENSE', children: "Egresos" }), _jsx("option", { value: 'RECOVERY', children: "Recuperos" }), _jsx("option", { value: 'TODO', children: "TODO" })] }), _jsxs("select", { className: 'select', value: sortBy, onChange: (e) => setSortBy(e.target.value), children: [_jsx("option", { value: 'DATE', children: "Fecha" }), _jsx("option", { value: 'PRIORITY', children: "Prioridad" }), _jsx("option", { value: 'AMOUNT', children: "Monto" })] })] }), actionError ? _jsx("p", { className: 'compact-muted', role: 'alert', children: actionError }) : null, _jsxs("table", { className: 'table', children: [_jsx("thead", { children: _jsxs("tr", { children: [_jsx("th", { children: "Fecha" }), _jsx("th", { children: "T\u00EDtulo" }), _jsx("th", { children: "Tipo" }), _jsx("th", { children: "Neto" }), _jsx("th", { children: "Estado" }), _jsx("th", { children: "Prioridad" }), _jsx("th", { children: "Acciones" })] }) }), _jsx("tbody", { children: filtered.map((i) => {
                            const canConvert = canConvertPlanItem(i);
                            const isEditing = editingId === i.id && form;
                            const isBusy = pendingActionId === i.id || savingId === i.id;
                            const showMarkPaid = ['EXPENSE', 'SAVING', 'DEBT', 'TRANSFER'].includes(i.type) && !['PAID', 'CANCELLED'].includes(i.status);
                            const showMarkCollected = ['INCOME', 'RECOVERY'].includes(i.type) && !['COLLECTED', 'CANCELLED'].includes(i.status);
                            const showCompleteAmount = i.amount == null && i.minAmount == null && i.maxAmount == null && i.status !== 'CANCELLED';
                            return (_jsxs(Fragment, { children: [_jsxs("tr", { children: [_jsx("td", { children: i.expectedDate ?? '-' }), _jsxs("td", { children: [i.title, " ", i.transactionId && _jsx("span", { className: 'compact-muted', children: "\u00B7 Convertido" })] }), _jsx("td", { children: labelOrValue(monthlyPlanTypeLabels, i.type) }), _jsx("td", { children: formatPlanNet(i) }), _jsx("td", { children: _jsx(StatusBadge, { label: labelOrValue(monthlyPlanStatusLabels, i.status) }) }), _jsx("td", { children: _jsx(StatusBadge, { label: labelOrValue(monthlyPlanPriorityLabels, i.priority) }) }), _jsxs("td", { className: 'table-actions', children: [_jsx("button", { className: 'button-secondary', onClick: () => startEdit(i, 'FULL'), disabled: isBusy, children: "Editar" }), showMarkPaid ? _jsx("button", { className: 'button-secondary', onClick: () => onMarkPaid(i.id), disabled: isBusy, children: "Marcar pagado" }) : null, showMarkCollected ? _jsx("button", { className: 'button-secondary', onClick: () => onMarkCollected(i.id), disabled: isBusy, children: "Marcar cobrado" }) : null, showCompleteAmount ? _jsx("button", { className: 'button-secondary', onClick: () => startEdit(i, 'AMOUNT'), disabled: isBusy, children: "Completar monto" }) : null, canConvert ? _jsx("button", { className: 'button-primary', onClick: () => onConvert(i.id), disabled: isBusy, children: "Convertir" }) : _jsx("button", { className: 'button-secondary', onClick: () => startEdit(i, 'CONVERT'), disabled: isBusy || i.status === 'CANCELLED' || Boolean(i.transactionId), children: "Preparar conversi\u00F3n" }), _jsx("button", { className: 'button-secondary', onClick: () => onCancel(i.id), disabled: isBusy || i.status === 'CANCELLED', children: "Cancelar" }), _jsx("button", { className: 'button-danger', onClick: () => onDelete(i.id), disabled: isBusy, children: "Eliminar" })] })] }), isEditing ? (_jsx("tr", { children: _jsx("td", { colSpan: 7, children: _jsxs("div", { className: 'card', children: [editMode !== 'AMOUNT' ? _jsx("input", { className: 'input', placeholder: 'T\u00EDtulo', value: form.title, onChange: (e) => setForm({ ...form, title: e.target.value }) }) : null, _jsxs("div", { className: 'form-row', children: [editMode !== 'CONVERT' ? _jsx("input", { className: 'input', type: 'date', value: form.expectedDate, onChange: (e) => setForm({ ...form, expectedDate: e.target.value }) }) : null, _jsx("input", { className: 'input', placeholder: 'Monto exacto', value: form.amount, onChange: (e) => setForm({ ...form, amount: e.target.value }) }), editMode !== 'CONVERT' ? _jsxs(_Fragment, { children: [_jsx("input", { className: 'input', placeholder: 'Monto m\u00EDnimo', value: form.minAmount, onChange: (e) => setForm({ ...form, minAmount: e.target.value }) }), _jsx("input", { className: 'input', placeholder: 'Monto m\u00E1ximo', value: form.maxAmount, onChange: (e) => setForm({ ...form, maxAmount: e.target.value }) })] }) : null] }), editMode === 'FULL' ? _jsxs("div", { className: 'form-row', children: [_jsx("input", { className: 'input', placeholder: 'Recupero monto', value: form.expectedRecoveryAmount, onChange: (e) => setForm({ ...form, expectedRecoveryAmount: e.target.value }) }), _jsx("input", { className: 'input', placeholder: 'Recupero %', value: form.expectedRecoveryPercent, onChange: (e) => setForm({ ...form, expectedRecoveryPercent: e.target.value }) }), _jsx("input", { className: 'input', placeholder: 'Contraparte', value: form.counterparty, onChange: (e) => setForm({ ...form, counterparty: e.target.value }) })] }) : null, editMode === 'FULL' ? _jsxs("div", { className: 'form-row', children: [_jsx("input", { className: 'input', placeholder: 'N\u00B0 cuota', value: form.installmentNumber, onChange: (e) => setForm({ ...form, installmentNumber: e.target.value }) }), _jsx("input", { className: 'input', placeholder: 'Total cuotas', value: form.installmentTotal, onChange: (e) => setForm({ ...form, installmentTotal: e.target.value }) })] }) : null, _jsxs("div", { className: 'form-row', children: [_jsxs("select", { className: 'select', value: form.accountId, onChange: (e) => setForm({ ...form, accountId: e.target.value }), children: [_jsx("option", { value: '', children: "Sin cuenta" }), accounts.map((a) => _jsx("option", { value: a.id, children: a.name }, a.id))] }), _jsxs("select", { className: 'select', value: form.categoryId, onChange: (e) => setForm({ ...form, categoryId: e.target.value }), children: [_jsx("option", { value: '', children: "Sin categor\u00EDa" }), categories.map((c) => _jsx("option", { value: c.id, children: c.name }, c.id))] }), editMode === 'FULL' ? _jsxs(_Fragment, { children: [_jsx("select", { className: 'select', value: form.status, onChange: (e) => setForm({ ...form, status: e.target.value }), children: Object.entries(monthlyPlanStatusLabels).map(([value, label]) => _jsx("option", { value: value, children: label }, value)) }), _jsx("select", { className: 'select', value: form.priority, onChange: (e) => setForm({ ...form, priority: e.target.value }), children: Object.entries(monthlyPlanPriorityLabels).map(([value, label]) => _jsx("option", { value: value, children: label }, value)) })] }) : null] }), editError ? _jsx("p", { className: 'compact-muted', role: 'alert', children: editError }) : null, suggestion ? _jsxs("div", { children: [_jsx("p", { children: suggestion.confidence === 'NONE' ? 'No encontré historial suficiente para sugerir cuenta o categoría.' : 'Sugerencias disponibles.' }), suggestion.confidence !== 'NONE' ? _jsx("button", { className: 'button-secondary', onClick: () => setForm({ ...form, accountId: suggestion.accountSuggestion?.id ?? form.accountId, categoryId: suggestion.categorySuggestion?.id ?? form.categoryId }), children: "Aplicar" }) : null] }) : null, _jsxs("div", { className: 'table-actions', children: [_jsx("button", { className: 'button-secondary', onClick: () => void loadSuggestion(i), disabled: savingId === i.id, children: "Sugerir cuenta/categor\u00EDa" }), _jsx("button", { className: 'button-primary', onClick: () => void saveEdit(i), disabled: savingId === i.id, children: "Guardar" }), _jsx("button", { className: 'button-secondary', onClick: () => { setEditingId(null); setForm(null); setEditError(null); }, disabled: savingId === i.id, children: "Cancelar" })] })] }) }) })) : null] }, i.id));
                        }) })] })] }));
}
