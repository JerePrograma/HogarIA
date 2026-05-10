import { jsx as _jsx, jsxs as _jsxs } from "react/jsx-runtime";
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { createDevUser, listDevUsers } from '../../api/devUsersApi';
import { getApiErrorMessage } from '../../api/http';
export function DevUserPage() {
    const nav = useNavigate();
    const qc = useQueryClient();
    const [form, setForm] = useState({ fullName: '', email: '', password: '' });
    const q = useQuery({ queryKey: ['dev-users'], queryFn: listDevUsers });
    const c = useMutation({ mutationFn: () => createDevUser(form), onSuccess: (u) => { qc.invalidateQueries({ queryKey: ['dev-users'] }); localStorage.setItem('devUserId', u.id); nav('/profiles'); } });
    return _jsxs("div", { className: 'card', children: [_jsx("h1", { children: "Dev users" }), q.isError && _jsx("div", { className: 'error-box', children: getApiErrorMessage(q.error) }), _jsxs("div", { className: 'form-row', children: [_jsx("input", { className: 'input', placeholder: 'Nombre completo', value: form.fullName, onChange: (e) => setForm({ ...form, fullName: e.target.value }) }), _jsx("input", { className: 'input', placeholder: 'Email', value: form.email, onChange: (e) => setForm({ ...form, email: e.target.value }) }), _jsx("input", { className: 'input', type: 'password', placeholder: 'Password', value: form.password, onChange: (e) => setForm({ ...form, password: e.target.value }) }), _jsx("button", { className: 'button-primary', onClick: () => c.mutate(), disabled: c.isPending, children: "Crear y seleccionar" })] }), q.isLoading ? _jsx("p", { children: "Cargando..." }) : !q.data?.length ? _jsx("p", { className: 'empty-state', children: "No hay usuarios creados." }) : _jsxs("table", { className: 'table', children: [_jsx("thead", { children: _jsxs("tr", { children: [_jsx("th", { children: "Nombre" }), _jsx("th", { children: "Email" }), _jsx("th", {})] }) }), _jsx("tbody", { children: q.data.map((u) => _jsxs("tr", { children: [_jsx("td", { children: u.fullName }), _jsx("td", { children: u.email }), _jsx("td", { children: _jsx("button", { onClick: () => { localStorage.setItem('devUserId', u.id); nav('/profiles'); }, children: "Seleccionar" }) })] }, u.id)) })] })] });
}
