import { jsx as _jsx, jsxs as _jsxs } from "react/jsx-runtime";
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { createProfile, deleteProfile, listProfiles, updateProfile } from '../../api/profilesApi';
import { getApiErrorMessage } from '../../api/http';
import { AppLayout } from '../../components/layout/AppLayout';
export function ProfilesPage() {
    const nav = useNavigate();
    const qc = useQueryClient();
    const [form, setForm] = useState({ name: '', type: 'PERSONAL', baseCurrency: 'ARS', activeYear: new Date().getFullYear() });
    const q = useQuery({ queryKey: ['profiles'], queryFn: listProfiles });
    const c = useMutation({ mutationFn: () => createProfile(form), onSuccess: () => { qc.invalidateQueries({ queryKey: ['profiles'] }); setForm({ ...form, name: '' }); } });
    const u = useMutation({ mutationFn: ({ id, payload }) => updateProfile(id, payload), onSuccess: () => qc.invalidateQueries({ queryKey: ['profiles'] }) });
    const d = useMutation({ mutationFn: (id) => deleteProfile(id), onSuccess: () => qc.invalidateQueries({ queryKey: ['profiles'] }) });
    if (q.isLoading)
        return _jsx("p", { children: "Cargando perfiles..." });
    if (q.isError)
        return _jsx("p", { children: getApiErrorMessage(q.error) });
    return _jsx(AppLayout, { children: _jsxs("div", { className: 'card', children: [_jsx("h1", { children: "Perfiles" }), _jsxs("div", { children: [_jsx("input", { className: 'input', value: form.name, placeholder: 'Nombre', onChange: (e) => setForm({ ...form, name: e.target.value }) }), _jsxs("select", { className: 'select', value: form.type, onChange: (e) => setForm({ ...form, type: e.target.value }), children: [_jsx("option", { children: "PERSONAL" }), _jsx("option", { children: "FAMILY" }), _jsx("option", { children: "BUSINESS" })] }), _jsx("input", { className: 'input', type: 'number', value: form.activeYear, onChange: (e) => setForm({ ...form, activeYear: Number(e.target.value) }) }), _jsx("button", { onClick: () => c.mutate(), disabled: c.isPending, children: "Crear perfil" })] }), !q.data?.length ? _jsx("p", { children: "No hay perfiles." }) : _jsxs("table", { className: 'table', children: [_jsx("thead", { children: _jsxs("tr", { children: [_jsx("th", { children: "Nombre" }), _jsx("th", { children: "Tipo" }), _jsx("th", { children: "Moneda" }), _jsx("th", { children: "A\u00F1o" }), _jsx("th", { children: "Activo" }), _jsx("th", { children: "Acciones" })] }) }), _jsx("tbody", { children: q.data.map((p) => _jsxs("tr", { children: [_jsx("td", { children: p.name }), _jsx("td", { children: p.type }), _jsx("td", { children: p.baseCurrency }), _jsx("td", { children: p.activeYear }), _jsx("td", { children: p.active ? 'Sí' : 'No' }), _jsxs("td", { children: [_jsx("button", { onClick: () => { localStorage.setItem('selectedProfileId', p.id); nav(`/profiles/${p.id}/dashboard`); }, children: "Entrar" }), _jsx("button", { onClick: () => u.mutate({ id: p.id, payload: { name: p.name, type: p.type, baseCurrency: p.baseCurrency, activeYear: p.activeYear, active: !p.active } }), children: "Toggle" }), _jsx("button", { onClick: () => window.confirm('Desactivar perfil?') && d.mutate(p.id), children: "Desactivar" })] })] }, p.id)) })] })] }) });
}
