import { jsx as _jsx, jsxs as _jsxs } from "react/jsx-runtime";
import { useState } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useParams } from 'react-router-dom';
import { createAccount, deleteAccount, listAccounts, updateAccount } from '../../api/accountsApi';
import { AppLayout } from '../../components/layout/AppLayout';
import { accountTypeLabels, labelOrMissing } from '../../domain/financeLabels';
import { accountTypeOptions } from '../../domain/financeOptions';
import { formatMoney } from '../../domain/formatters';
export function AccountsPage() {
    const { profileId = '' } = useParams();
    const qc = useQueryClient();
    const [form, setForm] = useState({
        name: '',
        accountType: 'CASH',
        currency: 'ARS',
    });
    const accountsQuery = useQuery({
        queryKey: ['accounts', profileId],
        queryFn: () => listAccounts(profileId),
        enabled: Boolean(profileId),
    });
    const createAccountMutation = useMutation({
        mutationFn: () => createAccount(profileId, form),
        onSuccess: () => {
            qc.invalidateQueries({ queryKey: ['accounts', profileId] });
            setForm({ ...form, name: '' });
        },
    });
    const updateAccountMutation = useMutation({
        mutationFn: (account) => updateAccount(String(account.id), {
            ...account,
            active: !account.active,
        }),
        onSuccess: () => qc.invalidateQueries({ queryKey: ['accounts', profileId] }),
    });
    const deleteAccountMutation = useMutation({
        mutationFn: (id) => deleteAccount(id),
        onSuccess: () => qc.invalidateQueries({ queryKey: ['accounts', profileId] }),
    });
    return (_jsxs(AppLayout, { children: [_jsx("div", { className: "page-header", children: _jsxs("div", { children: [_jsx("p", { className: "eyebrow", children: "Estructura financiera" }), _jsx("h1", { children: "Cuentas" }), _jsx("p", { className: "muted", children: "Registr\u00E1 efectivo, bancos, tarjetas y billeteras virtuales." })] }) }), _jsxs("section", { className: "card", children: [_jsx("h2", { children: "Crear cuenta" }), _jsxs("div", { className: "form-grid", children: [_jsxs("label", { className: "field", children: [_jsx("span", { children: "Nombre de la cuenta" }), _jsx("input", { value: form.name, placeholder: "Ej: Cuenta sueldo, Mercado Pago, Efectivo", onChange: (event) => setForm({ ...form, name: event.target.value }) })] }), _jsxs("label", { className: "field", children: [_jsx("span", { children: "Tipo de cuenta" }), _jsx("select", { value: form.accountType, onChange: (event) => setForm({ ...form, accountType: event.target.value }), children: accountTypeOptions.map((option) => (_jsx("option", { value: option.value, children: option.label }, option.value))) })] }), _jsxs("label", { className: "field", children: [_jsx("span", { children: "Moneda" }), _jsx("input", { value: form.currency, maxLength: 3, onChange: (event) => setForm({ ...form, currency: event.target.value.toUpperCase() }) })] })] }), _jsx("div", { className: "actions", children: _jsx("button", { type: "button", className: "button primary", onClick: () => createAccountMutation.mutate(), disabled: !form.name.trim() || createAccountMutation.isPending, children: "Crear cuenta" }) })] }), _jsxs("section", { className: "card", children: [_jsx("h2", { children: "Cuentas registradas" }), accountsQuery.isLoading ? (_jsx("p", { className: "muted", children: "Cargando cuentas..." })) : !accountsQuery.data?.length ? (_jsx("p", { className: "muted", children: "Sin cuentas." })) : (_jsx("div", { className: "table-wrap", children: _jsxs("table", { className: "table", children: [_jsx("thead", { children: _jsxs("tr", { children: [_jsx("th", { children: "Nombre" }), _jsx("th", { children: "Tipo" }), _jsx("th", { children: "Moneda" }), _jsx("th", { className: "amount", children: "L\u00EDmite" }), _jsx("th", { children: "Cierre/Vencimiento" }), _jsx("th", { children: "Estado" }), _jsx("th", { children: "Acciones" })] }) }), _jsx("tbody", { children: accountsQuery.data.map((account) => (_jsxs("tr", { children: [_jsx("td", { children: account.name }), _jsx("td", { children: labelOrMissing(accountTypeLabels, account.accountType) }), _jsx("td", { children: account.currency }), _jsx("td", { className: "amount", children: account.creditLimit ? formatMoney(account.creditLimit, account.currency) : '-' }), _jsx("td", { children: account.statementCloseDay || account.dueDay
                                                    ? `${account.statementCloseDay ?? '-'} / ${account.dueDay ?? '-'}`
                                                    : '-' }), _jsx("td", { children: _jsx("span", { className: `badge ${account.active ? 'good' : 'muted'}`, children: account.active ? 'Activa' : 'Inactiva' }) }), _jsx("td", { children: _jsxs("div", { className: "actions compact", children: [_jsx("button", { type: "button", className: "button ghost", onClick: () => updateAccountMutation.mutate(account), children: account.active ? 'Desactivar' : 'Activar' }), _jsx("button", { type: "button", className: "button danger", onClick: () => window.confirm('¿Eliminar cuenta?') && deleteAccountMutation.mutate(account.id), children: "Eliminar" })] }) })] }, account.id))) })] }) }))] })] }));
}
