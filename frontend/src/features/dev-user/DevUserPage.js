import { jsx as _jsx, jsxs as _jsxs } from "react/jsx-runtime";
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { createDevUser, listDevUsers } from '../../api/devUsersApi';
import { getApiErrorMessage } from '../../api/http';
export function DevUserPage() {
    const nav = useNavigate();
    const qc = useQueryClient();
    const [form, setForm] = useState({
        fullName: '',
        email: '',
        password: '',
    });
    const usersQuery = useQuery({
        queryKey: ['dev-users'],
        queryFn: listDevUsers,
    });
    const createUserMutation = useMutation({
        mutationFn: () => createDevUser(form),
        onSuccess: (user) => {
            qc.invalidateQueries({ queryKey: ['dev-users'] });
            localStorage.setItem('devUserId', user.id);
            nav('/profiles');
        },
    });
    const canCreate = Boolean(form.fullName.trim()) && Boolean(form.email.trim()) && Boolean(form.password);
    return (_jsx("main", { className: "auth-page", children: _jsxs("section", { className: "auth-card", children: [_jsx("div", { className: "page-header", children: _jsxs("div", { children: [_jsx("p", { className: "eyebrow", children: "Entorno de desarrollo" }), _jsx("h1", { children: "Usuarios de desarrollo" }), _jsx("p", { className: "muted", children: "Seleccion\u00E1 un usuario existente o cre\u00E1 uno nuevo para continuar." })] }) }), usersQuery.isError && _jsx("div", { className: "alert danger", children: getApiErrorMessage(usersQuery.error) }), _jsxs("div", { className: "card", children: [_jsx("h2", { children: "Crear usuario" }), _jsxs("div", { className: "form-grid", children: [_jsxs("label", { className: "field", children: [_jsx("span", { children: "Nombre completo" }), _jsx("input", { value: form.fullName, placeholder: "Ej: Jerem\u00EDas Rivelli", onChange: (event) => setForm({ ...form, fullName: event.target.value }) })] }), _jsxs("label", { className: "field", children: [_jsx("span", { children: "Correo electr\u00F3nico" }), _jsx("input", { type: "email", value: form.email, placeholder: "usuario@correo.com", onChange: (event) => setForm({ ...form, email: event.target.value }) })] }), _jsxs("label", { className: "field", children: [_jsx("span", { children: "Contrase\u00F1a" }), _jsx("input", { type: "password", value: form.password, placeholder: "Contrase\u00F1a", onChange: (event) => setForm({ ...form, password: event.target.value }) })] })] }), _jsx("div", { className: "actions", children: _jsx("button", { type: "button", className: "button primary", onClick: () => createUserMutation.mutate(), disabled: !canCreate || createUserMutation.isPending, children: "Crear y seleccionar" }) })] }), _jsxs("div", { className: "card", children: [_jsx("h2", { children: "Usuarios disponibles" }), usersQuery.isLoading ? (_jsx("p", { className: "muted", children: "Cargando usuarios..." })) : !usersQuery.data?.length ? (_jsx("p", { className: "muted", children: "No hay usuarios creados." })) : (_jsx("div", { className: "table-wrap", children: _jsxs("table", { className: "table", children: [_jsx("thead", { children: _jsxs("tr", { children: [_jsx("th", { children: "Nombre" }), _jsx("th", { children: "Correo electr\u00F3nico" }), _jsx("th", { children: "Acciones" })] }) }), _jsx("tbody", { children: usersQuery.data.map((user) => (_jsxs("tr", { children: [_jsx("td", { children: user.fullName }), _jsx("td", { children: user.email }), _jsx("td", { children: _jsx("button", { type: "button", className: "button secondary", onClick: () => {
                                                            localStorage.setItem('devUserId', user.id);
                                                            nav('/profiles');
                                                        }, children: "Seleccionar" }) })] }, user.id))) })] }) }))] })] }) }));
}
