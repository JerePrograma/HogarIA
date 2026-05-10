import { jsx as _jsx, jsxs as _jsxs } from "react/jsx-runtime";
import { useState } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useParams } from 'react-router-dom';
import { createAccount, deleteAccount, listAccounts, updateAccount } from '../../api/accountsApi';
import { AppLayout } from '../../components/layout/AppLayout';
export function AccountsPage() {
    const { profileId = '' } = useParams();
    const qc = useQueryClient();
    const [name, setName] = useState('');
    const q = useQuery({ queryKey: ['accounts', profileId], queryFn: () => listAccounts(profileId) });
    const c = useMutation({ mutationFn: () => createAccount(profileId, { name, accountType: 'CASH', currency: 'ARS' }), onSuccess: () => { qc.invalidateQueries({ queryKey: ['accounts', profileId] }); setName(''); } });
    const u = useMutation({ mutationFn: (a) => updateAccount(a.id, { name: a.name, accountType: a.accountType, currency: a.currency, creditLimit: a.creditLimit, statementCloseDay: a.statementCloseDay, dueDay: a.dueDay, active: !a.active }), onSuccess: () => qc.invalidateQueries({ queryKey: ['accounts', profileId] }) });
    const d = useMutation({ mutationFn: (id) => deleteAccount(id), onSuccess: () => qc.invalidateQueries({ queryKey: ['accounts', profileId] }) });
    return _jsx(AppLayout, { children: _jsxs("div", { className: 'card', children: [_jsx("h1", { children: "Cuentas" }), _jsxs("div", { className: 'form-row', children: [_jsx("input", { className: 'input', value: name, onChange: (e) => setName(e.target.value), placeholder: 'Nombre cuenta' }), _jsx("button", { className: 'button-primary', onClick: () => c.mutate(), children: "Crear" })] }), !q.data?.length ? _jsx("p", { className: 'empty-state', children: "Sin cuentas." }) : _jsxs("table", { className: 'table', children: [_jsx("thead", { children: _jsxs("tr", { children: [_jsx("th", { children: "Nombre" }), _jsx("th", { children: "Tipo" }), _jsx("th", { children: "Moneda" }), _jsx("th", { children: "L\u00EDmite" }), _jsx("th", { children: "Cierre/Venc." }), _jsx("th", { children: "Activa" }), _jsx("th", {})] }) }), _jsx("tbody", { children: q.data.map((a) => _jsxs("tr", { children: [_jsx("td", { children: a.name }), _jsx("td", { children: a.accountType }), _jsx("td", { children: a.currency }), _jsx("td", { children: a.creditLimit ?? '-' }), _jsxs("td", { children: [a.statementCloseDay ?? '-', " / ", a.dueDay ?? '-'] }), _jsx("td", { children: a.active ? 'Sí' : 'No' }), _jsxs("td", { children: [_jsx("button", { onClick: () => u.mutate(a), children: "Editar/Toggle" }), _jsx("button", { className: 'button-danger', onClick: () => window.confirm('Desactivar cuenta?') && d.mutate(a.id), children: "Desactivar" })] })] }, a.id)) })] })] }) });
}
