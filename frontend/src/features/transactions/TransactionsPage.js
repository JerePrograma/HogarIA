import { jsx as _jsx, jsxs as _jsxs } from "react/jsx-runtime";
// src/features/transactions/TransactionsPage.tsx
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useMemo, useState } from 'react';
import { Link, useParams } from 'react-router-dom';
import { listAccounts } from '../../api/accountsApi';
import { listCategories } from '../../api/categoriesApi';
import { createTransaction, deleteTransaction, listTransactions, updateTransaction, } from '../../api/transactionsApi';
import { AppLayout } from '../../components/layout/AppLayout';
import { labelOrValue, movementTypeLabels, transactionStatusLabels, } from '../../domain/financeLabels';
import { movementTypeOptions, transactionStatusOptions, } from '../../domain/financeOptions';
import { formatMoney } from '../../domain/formatters';
const monthOptions = [
    { value: 1, label: 'Enero' },
    { value: 2, label: 'Febrero' },
    { value: 3, label: 'Marzo' },
    { value: 4, label: 'Abril' },
    { value: 5, label: 'Mayo' },
    { value: 6, label: 'Junio' },
    { value: 7, label: 'Julio' },
    { value: 8, label: 'Agosto' },
    { value: 9, label: 'Septiembre' },
    { value: 10, label: 'Octubre' },
    { value: 11, label: 'Noviembre' },
    { value: 12, label: 'Diciembre' },
];
function getDefaultDate(year, month) {
    return `${year}-${String(month).padStart(2, '0')}-01`;
}
function formatDate(value) {
    if (!value)
        return '-';
    return new Intl.DateTimeFormat('es-AR', {
        day: '2-digit',
        month: '2-digit',
        year: 'numeric',
    }).format(new Date(`${value}T00:00:00`));
}
function getStatusBadgeClass(status) {
    if (status === 'CONFIRMED')
        return 'badge badge-ok';
    if (status === 'PENDING')
        return 'badge badge-warning';
    return 'badge badge-danger';
}
function getMovementBadgeClass(type) {
    if (type === 'INCOME')
        return 'badge badge-ok';
    if (type === 'SAVING')
        return 'badge badge-info';
    if (type === 'TRANSFER')
        return 'badge badge-muted';
    if (type === 'ADJUSTMENT')
        return 'badge badge-warning';
    return 'badge badge-danger';
}
function toTransactionUpdatePayload(transaction) {
    return {
        accountId: transaction.accountId,
        categoryId: transaction.categoryId,
        movementType: transaction.movementType,
        realDate: transaction.realDate,
        budgetDate: transaction.budgetDate,
        amount: transaction.amount,
        currency: transaction.currency,
        description: transaction.description ?? '',
        status: transaction.status === 'CONFIRMED' ? 'PENDING' : 'CONFIRMED',
    };
}
export function TransactionsPage() {
    const { profileId = '' } = useParams();
    const queryClient = useQueryClient();
    const today = new Date();
    const initialYear = today.getFullYear();
    const initialMonth = today.getMonth() + 1;
    const [year, setYear] = useState(initialYear);
    const [month, setMonth] = useState(initialMonth);
    const [form, setForm] = useState({
        accountId: '',
        categoryId: '',
        movementType: 'EXPENSE',
        realDate: getDefaultDate(initialYear, initialMonth),
        budgetDate: getDefaultDate(initialYear, initialMonth),
        amount: 0,
        currency: 'ARS',
        description: '',
        status: 'CONFIRMED',
    });
    const accountsQuery = useQuery({
        queryKey: ['accounts', profileId],
        queryFn: () => listAccounts(profileId),
        enabled: Boolean(profileId),
    });
    const categoriesQuery = useQuery({
        queryKey: ['categories', profileId],
        queryFn: () => listCategories(profileId, true),
        enabled: Boolean(profileId),
    });
    const transactionsQuery = useQuery({
        queryKey: ['tx', profileId, year, month],
        queryFn: () => listTransactions(profileId, year, month),
        enabled: Boolean(profileId),
    });
    const accountsById = useMemo(() => {
        return new Map((accountsQuery.data ?? []).map((account) => [account.id, account]));
    }, [accountsQuery.data]);
    const categoriesById = useMemo(() => {
        return new Map((categoriesQuery.data ?? []).map((category) => [category.id, category]));
    }, [categoriesQuery.data]);
    const transactions = transactionsQuery.data ?? [];
    const totals = useMemo(() => {
        return transactions.reduce((acc, transaction) => {
            if (transaction.status === 'IGNORED') {
                acc.ignored += Number(transaction.amount ?? 0);
                return acc;
            }
            if (transaction.movementType === 'INCOME') {
                acc.income += Number(transaction.amount ?? 0);
                return acc;
            }
            if (transaction.movementType === 'SAVING') {
                acc.saving += Number(transaction.amount ?? 0);
                return acc;
            }
            if (transaction.movementType === 'EXPENSE') {
                acc.expenses += Number(transaction.amount ?? 0);
                return acc;
            }
            return acc;
        }, {
            income: 0,
            expenses: 0,
            saving: 0,
            ignored: 0,
        });
    }, [transactions]);
    const createTransactionMutation = useMutation({
        mutationFn: () => createTransaction({
            ...form,
            profileId,
            amount: Number(form.amount),
            origin: 'MANUAL',
        }),
        onSuccess: () => {
            queryClient.invalidateQueries({ queryKey: ['tx', profileId, year, month] });
            queryClient.invalidateQueries({ queryKey: ['budget-comp', profileId, year, month] });
            setForm((current) => ({
                ...current,
                amount: 0,
                description: '',
            }));
        },
    });
    const updateTransactionMutation = useMutation({
        mutationFn: (transaction) => updateTransaction(transaction.id, toTransactionUpdatePayload(transaction)),
        onSuccess: () => {
            queryClient.invalidateQueries({ queryKey: ['tx', profileId, year, month] });
            queryClient.invalidateQueries({ queryKey: ['budget-comp', profileId, year, month] });
        },
    });
    const deleteTransactionMutation = useMutation({
        mutationFn: (id) => deleteTransaction(id),
        onSuccess: () => {
            queryClient.invalidateQueries({ queryKey: ['tx', profileId, year, month] });
            queryClient.invalidateQueries({ queryKey: ['budget-comp', profileId, year, month] });
        },
    });
    const canSave = Boolean(form.accountId) &&
        Boolean(form.categoryId) &&
        form.amount > 0 &&
        Boolean(form.realDate) &&
        Boolean(form.budgetDate) &&
        !createTransactionMutation.isPending;
    const handlePeriodChange = (nextYear, nextMonth) => {
        setYear(nextYear);
        setMonth(nextMonth);
        const nextDefaultDate = getDefaultDate(nextYear, nextMonth);
        setForm((current) => ({
            ...current,
            realDate: nextDefaultDate,
            budgetDate: nextDefaultDate,
        }));
    };
    return (_jsx(AppLayout, { children: _jsxs("div", { className: "page-stack", children: [_jsxs("section", { className: "page-header", children: [_jsxs("div", { children: [_jsx("p", { className: "eyebrow", children: "Gesti\u00F3n diaria" }), _jsx("h1", { children: "Movimientos" }), _jsx("p", { className: "muted", children: "Carg\u00E1 ingresos, gastos, ahorros y ajustes para alimentar el presupuesto y el panel mensual." })] }), _jsxs("div", { className: "period-selector", children: [_jsxs("label", { children: ["A\u00F1o", _jsx("input", { className: "input", type: "number", value: year, min: 2000, max: 2100, onChange: (event) => handlePeriodChange(Number(event.target.value), month) })] }), _jsxs("label", { children: ["Mes", _jsx("select", { className: "select", value: month, onChange: (event) => handlePeriodChange(year, Number(event.target.value)), children: monthOptions.map((option) => (_jsx("option", { value: option.value, children: option.label }, option.value))) })] })] })] }), _jsxs("section", { className: "summary-grid", children: [_jsxs("div", { className: "metric-card metric-income", children: [_jsx("span", { children: "Ingresos" }), _jsx("strong", { children: formatMoney(totals.income) })] }), _jsxs("div", { className: "metric-card metric-expense", children: [_jsx("span", { children: "Gastos" }), _jsx("strong", { children: formatMoney(totals.expenses) })] }), _jsxs("div", { className: "metric-card metric-saving", children: [_jsx("span", { children: "Ahorro" }), _jsx("strong", { children: formatMoney(totals.saving) })] }), _jsxs("div", { className: "metric-card", children: [_jsx("span", { children: "Balance operativo" }), _jsx("strong", { children: formatMoney(totals.income - totals.expenses - totals.saving) })] })] }), !accountsQuery.isLoading && !accountsQuery.data?.length && (_jsxs("p", { className: "empty-state", children: ["No hay cuentas cargadas.", ' ', _jsx(Link, { to: `/profiles/${profileId}/accounts`, children: "Crear cuenta" })] })), !categoriesQuery.isLoading && !categoriesQuery.data?.length && (_jsxs("p", { className: "empty-state", children: ["No hay categor\u00EDas cargadas.", ' ', _jsx(Link, { to: `/profiles/${profileId}/categories`, children: "Crear categor\u00EDa" })] })), _jsxs("section", { className: "card", children: [_jsx("div", { className: "section-title", children: _jsxs("div", { children: [_jsx("h2", { children: "Cargar movimiento" }), _jsx("p", { className: "muted", children: "Los movimientos confirmados impactan en reportes y comparaci\u00F3n presupuesto vs real." })] }) }), _jsxs("div", { className: "form-grid transaction-form-grid", children: [_jsxs("label", { children: ["Cuenta", _jsxs("select", { className: "select", value: form.accountId, onChange: (event) => setForm({
                                                ...form,
                                                accountId: event.target.value,
                                            }), children: [_jsx("option", { value: "", children: "Seleccionar cuenta" }), accountsQuery.data?.map((account) => (_jsx("option", { value: account.id, children: account.name }, account.id)))] })] }), _jsxs("label", { children: ["Categor\u00EDa", _jsxs("select", { className: "select", value: form.categoryId, onChange: (event) => setForm({
                                                ...form,
                                                categoryId: event.target.value,
                                            }), children: [_jsx("option", { value: "", children: "Seleccionar categor\u00EDa" }), categoriesQuery.data?.map((category) => (_jsx("option", { value: category.id, children: category.name }, category.id)))] })] }), _jsxs("label", { children: ["Tipo de movimiento", _jsx("select", { className: "select", value: form.movementType, onChange: (event) => setForm({
                                                ...form,
                                                movementType: event.target.value,
                                            }), children: movementTypeOptions.map((option) => (_jsx("option", { value: option.value, children: option.label }, option.value))) })] }), _jsxs("label", { children: ["Estado", _jsx("select", { className: "select", value: form.status, onChange: (event) => setForm({
                                                ...form,
                                                status: event.target.value,
                                            }), children: transactionStatusOptions.map((option) => (_jsx("option", { value: option.value, children: option.label }, option.value))) })] }), _jsxs("label", { children: ["Fecha real", _jsx("input", { className: "input", type: "date", value: form.realDate, onChange: (event) => setForm({
                                                ...form,
                                                realDate: event.target.value,
                                            }) })] }), _jsxs("label", { children: ["Fecha de presupuesto", _jsx("input", { className: "input", type: "date", value: form.budgetDate, onChange: (event) => setForm({
                                                ...form,
                                                budgetDate: event.target.value,
                                            }) })] }), _jsxs("label", { children: ["Monto", _jsx("input", { className: "input", type: "number", min: 0, value: form.amount, onChange: (event) => setForm({
                                                ...form,
                                                amount: Number(event.target.value),
                                            }) })] }), _jsxs("label", { className: "form-field-wide", children: ["Descripci\u00F3n", _jsx("input", { className: "input", value: form.description, placeholder: "Ej: supermercado, sueldo, alquiler", onChange: (event) => setForm({
                                                ...form,
                                                description: event.target.value,
                                            }) })] })] }), _jsxs("div", { className: "form-actions", children: [_jsx("button", { className: "button-primary", onClick: () => createTransactionMutation.mutate(), disabled: !canSave, children: createTransactionMutation.isPending ? 'Guardando...' : 'Guardar movimiento' }), !canSave && (_jsx("span", { className: "muted", children: "Complet\u00E1 cuenta, categor\u00EDa, monto y fechas para guardar." }))] }), createTransactionMutation.isError && (_jsx("p", { className: "error-box", children: "No se pudo guardar el movimiento. Revis\u00E1 los datos ingresados." }))] }), _jsxs("section", { className: "card", children: [_jsx("div", { className: "section-title", children: _jsxs("div", { children: [_jsx("h2", { children: "Movimientos del per\u00EDodo" }), _jsxs("p", { className: "muted", children: [transactions.length, " movimiento", transactions.length === 1 ? '' : 's', " registrado", transactions.length === 1 ? '' : 's', "."] })] }) }), transactionsQuery.isLoading && (_jsx("p", { className: "empty-state", children: "Cargando movimientos..." })), transactionsQuery.isError && (_jsx("p", { className: "error-box", children: "No se pudieron cargar los movimientos del per\u00EDodo." })), !transactionsQuery.isLoading &&
                            !transactionsQuery.isError &&
                            transactions.length === 0 && (_jsx("p", { className: "empty-state", children: "Todav\u00EDa no hay movimientos cargados para este mes." })), transactions.length > 0 && (_jsx("div", { className: "table-wrapper", children: _jsxs("table", { className: "table table-compact", children: [_jsx("thead", { children: _jsxs("tr", { children: [_jsx("th", { children: "Fecha" }), _jsx("th", { children: "Movimiento" }), _jsx("th", { children: "Cuenta" }), _jsx("th", { children: "Categor\u00EDa" }), _jsx("th", { children: "Monto" }), _jsx("th", { children: "Estado" }), _jsx("th", { children: "Acciones" })] }) }), _jsx("tbody", { children: transactions.map((transaction) => {
                                            const accountName = accountsById.get(transaction.accountId)?.name ?? 'Cuenta no encontrada';
                                            const categoryName = categoriesById.get(transaction.categoryId)?.name ?? 'Categoría no encontrada';
                                            return (_jsxs("tr", { children: [_jsxs("td", { children: [_jsx("strong", { children: formatDate(transaction.realDate) }), _jsx("br", {}), _jsxs("span", { className: "muted", children: ["Presupuesto: ", formatDate(transaction.budgetDate)] })] }), _jsxs("td", { children: [_jsx("span", { className: getMovementBadgeClass(transaction.movementType), children: labelOrValue(movementTypeLabels, transaction.movementType) }), _jsx("br", {}), _jsx("span", { className: "muted", children: transaction.description || 'Sin descripción' })] }), _jsx("td", { children: accountName }), _jsx("td", { children: categoryName }), _jsx("td", { className: "amount-cell", children: formatMoney(transaction.amount, transaction.currency) }), _jsx("td", { children: _jsx("span", { className: getStatusBadgeClass(transaction.status), children: labelOrValue(transactionStatusLabels, transaction.status) }) }), _jsx("td", { children: _jsxs("div", { className: "row-actions", children: [_jsx("button", { className: "button-secondary", disabled: updateTransactionMutation.isPending, onClick: () => updateTransactionMutation.mutate(transaction), children: transaction.status === 'CONFIRMED'
                                                                        ? 'Pasar a pendiente'
                                                                        : 'Confirmar' }), _jsx("button", { className: "button-danger", disabled: deleteTransactionMutation.isPending, onClick: () => window.confirm('¿Eliminar este movimiento?') &&
                                                                        deleteTransactionMutation.mutate(transaction.id), children: "Eliminar" })] }) })] }, transaction.id));
                                        }) })] }) }))] })] }) }));
}
