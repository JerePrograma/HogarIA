import { jsx as _jsx, jsxs as _jsxs } from "react/jsx-runtime";
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { createProfile, deleteProfile, listProfiles, updateProfile } from '../../api/profilesApi';
import { getApiErrorMessage } from '../../api/http';
import { AppLayout } from '../../components/layout/AppLayout';
import { labelOrMissing, profileTypeLabels } from '../../domain/financeLabels';
import { profileTypeOptions } from '../../domain/financeOptions';
export function ProfilesPage() {
    const nav = useNavigate();
    const qc = useQueryClient();
    const [form, setForm] = useState({
        name: '',
        type: 'PERSONAL',
        baseCurrency: 'ARS',
        activeYear: new Date().getFullYear(),
    });
    const profilesQuery = useQuery({
        queryKey: ['profiles'],
        queryFn: listProfiles,
    });
    const createProfileMutation = useMutation({
        mutationFn: () => createProfile(form),
        onSuccess: () => {
            qc.invalidateQueries({ queryKey: ['profiles'] });
            setForm({ ...form, name: '' });
        },
    });
    const updateProfileMutation = useMutation({
        mutationFn: (profile) => updateProfile(profile.id, {
            name: profile.name,
            type: profile.type,
            baseCurrency: profile.baseCurrency,
            activeYear: profile.activeYear,
            active: !profile.active,
        }),
        onSuccess: () => qc.invalidateQueries({ queryKey: ['profiles'] }),
    });
    const deleteProfileMutation = useMutation({
        mutationFn: (id) => deleteProfile(id),
        onSuccess: () => qc.invalidateQueries({ queryKey: ['profiles'] }),
    });
    if (profilesQuery.isLoading) {
        return (_jsx(AppLayout, { children: _jsx("p", { className: "muted", children: "Cargando perfiles..." }) }));
    }
    if (profilesQuery.isError) {
        return (_jsx(AppLayout, { children: _jsx("div", { className: "alert danger", children: getApiErrorMessage(profilesQuery.error) }) }));
    }
    return (_jsxs(AppLayout, { children: [_jsx("div", { className: "page-header", children: _jsxs("div", { children: [_jsx("p", { className: "eyebrow", children: "Configuraci\u00F3n" }), _jsx("h1", { children: "Perfiles financieros" }), _jsx("p", { className: "muted", children: "Separ\u00E1 tus finanzas personales, familiares o de negocio." })] }) }), _jsxs("section", { className: "card", children: [_jsx("h2", { children: "Crear perfil" }), _jsxs("div", { className: "form-grid", children: [_jsxs("label", { className: "field", children: [_jsx("span", { children: "Nombre del perfil" }), _jsx("input", { value: form.name, placeholder: "Ej: Hogar, Personal, Negocio", onChange: (event) => setForm({ ...form, name: event.target.value }) })] }), _jsxs("label", { className: "field", children: [_jsx("span", { children: "Tipo" }), _jsx("select", { value: form.type, onChange: (event) => setForm({ ...form, type: event.target.value }), children: profileTypeOptions.map((option) => (_jsx("option", { value: option.value, children: option.label }, option.value))) })] }), _jsxs("label", { className: "field", children: [_jsx("span", { children: "Moneda base" }), _jsx("input", { value: form.baseCurrency, maxLength: 3, onChange: (event) => setForm({ ...form, baseCurrency: event.target.value.toUpperCase() }) })] }), _jsxs("label", { className: "field", children: [_jsx("span", { children: "A\u00F1o activo" }), _jsx("input", { type: "number", value: form.activeYear, onChange: (event) => setForm({ ...form, activeYear: Number(event.target.value) }) })] })] }), _jsx("div", { className: "actions", children: _jsx("button", { type: "button", className: "button primary", onClick: () => createProfileMutation.mutate(), disabled: !form.name.trim() || createProfileMutation.isPending, children: "Crear perfil" }) })] }), _jsxs("section", { className: "card", children: [_jsx("h2", { children: "Perfiles disponibles" }), !profilesQuery.data?.length ? (_jsx("p", { className: "muted", children: "No hay perfiles." })) : (_jsx("div", { className: "table-wrap", children: _jsxs("table", { className: "table", children: [_jsx("thead", { children: _jsxs("tr", { children: [_jsx("th", { children: "Nombre" }), _jsx("th", { children: "Tipo" }), _jsx("th", { children: "Moneda" }), _jsx("th", { children: "A\u00F1o" }), _jsx("th", { children: "Estado" }), _jsx("th", { children: "Acciones" })] }) }), _jsx("tbody", { children: profilesQuery.data.map((profile) => (_jsxs("tr", { children: [_jsx("td", { children: profile.name }), _jsx("td", { children: labelOrMissing(profileTypeLabels, profile.type) }), _jsx("td", { children: profile.baseCurrency }), _jsx("td", { children: profile.activeYear }), _jsx("td", { children: _jsx("span", { className: `badge ${profile.active ? 'good' : 'muted'}`, children: profile.active ? 'Activo' : 'Inactivo' }) }), _jsx("td", { children: _jsxs("div", { className: "actions compact", children: [_jsx("button", { type: "button", className: "button secondary", onClick: () => {
                                                                localStorage.setItem('selectedProfileId', profile.id);
                                                                nav(`/profiles/${profile.id}/dashboard`);
                                                            }, children: "Entrar" }), _jsx("button", { type: "button", className: "button ghost", onClick: () => updateProfileMutation.mutate(profile), children: profile.active ? 'Desactivar' : 'Activar' }), _jsx("button", { type: "button", className: "button danger", onClick: () => window.confirm('¿Eliminar perfil?') && deleteProfileMutation.mutate(profile.id), children: "Eliminar" })] }) })] }, profile.id))) })] }) }))] })] }));
}
