import { jsx as _jsx, jsxs as _jsxs } from "react/jsx-runtime";
// src/features/transactions/TransactionsPage.tsx
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useState } from 'react';
import { Link, useParams } from 'react-router-dom';
import { listAccounts } from '../../api/accountsApi';
import { listCategories } from '../../api/categoriesApi';
import { createTransaction, deleteTransaction, listTransactions, updateTransaction, } from '../../api/transactionsApi';
import { AppLayout } from '../../components/layout/AppLayout';
import { labelOrValue, movementTypeLabels, transactionStatusLabels, } from '../../domain/financeLabels';
import { movementTypeOptions, transactionStatusOptions, } from '../../domain/financeOptions';
import { formatMoney } from '../../domain/formatters';
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
        status: transaction.status === 'CONFIRMED'
            ? 'PENDING'
            : 'CONFIRMED',
    };
}
export function TransactionsPage() {
    const { profileId = '' } = useParams();
    const qc = useQueryClient();
    const today = new Date();
    const yyyy = today.getFullYear();
    const mm = String(today.getMonth() + 1).padStart(2, '0');
    const [year, setYear] = useState(yyyy);
    const [month, setMonth] = useState(today.getMonth() + 1);
    const [form, setForm] = useState({
        accountId: '',
        categoryId: '',
        movementType: 'EXPENSE',
        realDate: `${yyyy}-${mm}-01`,
        budgetDate: `${yyyy}-${mm}-01`,
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
    const createTransactionMutation = useMutation({
        mutationFn: () => createTransaction({
            ...form,
            profileId,
            amount: Number(form.amount),
            origin: 'MANUAL',
        }),
        onSuccess: () => {
            qc.invalidateQueries({ queryKey: ['tx', profileId, year, month] });
            qc.invalidateQueries({ queryKey: ['budget-comp', profileId, year, month] });
        },
    });
    const updateTransactionMutation = useMutation({
        mutationFn: (transaction) => updateTransaction(transaction.id, toTransactionUpdatePayload(transaction)),
        onSuccess: () => {
            qc.invalidateQueries({ queryKey: ['tx', profileId, year, month] });
            qc.invalidateQueries({ queryKey: ['budget-comp', profileId, year, month] });
        },
    });
    const deleteTransactionMutation = useMutation({
        mutationFn: (id) => deleteTransaction(id),
        onSuccess: () => {
            qc.invalidateQueries({ queryKey: ['tx', profileId, year, month] });
            qc.invalidateQueries({ queryKey: ['budget-comp', profileId, year, month] });
        },
    });
    const nameById = (items, id) => items?.find((item) => item.id === id)?.name ?? id;
    const canSave = Boolean(form.accountId) &&
        Boolean(form.categoryId) &&
        form.amount > 0 &&
        Boolean(form.realDate) &&
        Boolean(form.budgetDate);
    return (_jsx(AppLayout, { children: _jsxs("div", { className: "card", children: [_jsx("h1", { children: "Movimientos" }), !accountsQuery.data?.length && (_jsxs("p", { className: "empty-state", children: ["No hay cuentas.", ' ', _jsx(Link, { to: `/profiles/${profileId}/accounts`, children: "Crear cuenta" })] })), !categoriesQuery.data?.length && (_jsxs("p", { className: "empty-state", children: ["No hay categor\u00EDas.", ' ', _jsx(Link, { to: `/profiles/${profileId}/categories`, children: "Crear categor\u00EDa" })] })), _jsxs("div", { className: "form-row", children: [_jsx("input", { className: "input", type: "number", value: year, min: 2000, max: 2100, onChange: (e) => setYear(Number(e.target.value)) }), _jsx("input", { className: "input", type: "number", value: month, min: 1, max: 12, onChange: (e) => setMonth(Number(e.target.value)) })] }), _jsxs("div", { className: "form-row", children: [_jsxs("select", { className: "select", value: form.accountId, onChange: (e) => setForm({
                                ...form,
                                accountId: e.target.value,
                            }), children: [_jsx("option", { value: "", children: "Cuenta" }), accountsQuery.data?.map((account) => (_jsx("option", { value: account.id, children: account.name }, account.id)))] }), _jsxs("select", { className: "select", value: form.categoryId, onChange: (e) => setForm({
                                ...form,
                                categoryId: e.target.value,
                            }), children: [_jsx("option", { value: "", children: "Categor\u00EDa" }), categoriesQuery.data?.map((category) => (_jsx("option", { value: category.id, children: category.name }, category.id)))] }), _jsx("select", { className: "select", value: form.movementType, onChange: (e) => setForm({
                                ...form,
                                movementType: e.target.value,
                            }), children: movementTypeOptions.map((option) => (_jsx("option", { value: option.value, children: option.label }, option.value))) }), _jsx("select", { className: "select", value: form.status, onChange: (e) => setForm({
                                ...form,
                                status: e.target.value,
                            }), children: transactionStatusOptions.map((option) => (_jsx("option", { value: option.value, children: option.label }, option.value))) })] }), _jsxs("div", { className: "form-row", children: [_jsx("input", { className: "input", type: "date", value: form.realDate, onChange: (e) => setForm({
                                ...form,
                                realDate: e.target.value,
                            }) }), _jsx("input", { className: "input", type: "date", value: form.budgetDate, onChange: (e) => setForm({
                                ...form,
                                budgetDate: e.target.value,
                            }) }), _jsx("input", { className: "input", type: "number", min: 0, value: form.amount, onChange: (e) => setForm({
                                ...form,
                                amount: Number(e.target.value),
                            }) }), _jsx("input", { className: "input", value: form.description, placeholder: "Descripci\u00F3n", onChange: (e) => setForm({
                                ...form,
                                description: e.target.value,
                            }) }), _jsx("button", { className: "button-primary", onClick: () => createTransactionMutation.mutate(), disabled: !canSave || createTransactionMutation.isPending, children: "Guardar" })] }), _jsxs("table", { className: "table", children: [_jsx("thead", { children: _jsxs("tr", { children: [_jsx("th", { children: "Fecha real" }), _jsx("th", { children: "Fecha presupuesto" }), _jsx("th", { children: "Tipo" }), _jsx("th", { children: "Cuenta" }), _jsx("th", { children: "Categor\u00EDa" }), _jsx("th", { children: "Monto" }), _jsx("th", { children: "Estado" }), _jsx("th", { children: "Descripci\u00F3n" }), _jsx("th", {})] }) }), _jsx("tbody", { children: transactionsQuery.data?.map((transaction) => (_jsxs("tr", { children: [_jsx("td", { children: transaction.realDate }), _jsx("td", { children: transaction.budgetDate }), _jsx("td", { children: labelOrValue(movementTypeLabels, transaction.movementType) }), _jsx("td", { children: nameById(accountsQuery.data, transaction.accountId) }), _jsx("td", { children: nameById(categoriesQuery.data, transaction.categoryId) }), _jsx("td", { children: formatMoney(transaction.amount, transaction.currency) }), _jsx("td", { children: labelOrValue(transactionStatusLabels, transaction.status) }), _jsx("td", { children: transaction.description }), _jsxs("td", { children: [_jsx("button", { onClick: () => updateTransactionMutation.mutate(transaction), children: transaction.status === 'CONFIRMED'
                                                    ? 'Pasar a pendiente'
                                                    : 'Confirmar' }), _jsx("button", { className: "button-danger", onClick: () => window.confirm('¿Eliminar movimiento?') &&
                                                    deleteTransactionMutation.mutate(transaction.id), children: "Eliminar" })] })] }, transaction.id))) })] })] }) }));
}
