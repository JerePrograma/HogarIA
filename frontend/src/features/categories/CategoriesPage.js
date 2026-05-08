import { jsx as _jsx, jsxs as _jsxs, Fragment as _Fragment } from "react/jsx-runtime";
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useState } from 'react';
import { useParams } from 'react-router-dom';
import { createCategory, deleteCategory, listCategories, updateCategory, } from '../../api/categoriesApi';
import { AppLayout } from '../../components/layout/AppLayout';
import { categoryScopeLabels, categoryTypeLabels, labelOrValue, } from '../../domain/financeLabels';
export function CategoriesPage() {
    const { profileId = '' } = useParams();
    const qc = useQueryClient();
    const [includeGlobal, setIncludeGlobal] = useState(true);
    const [name, setName] = useState('');
    const q = useQuery({
        queryKey: ['categories', profileId, includeGlobal],
        queryFn: () => listCategories(profileId, includeGlobal),
        enabled: Boolean(profileId),
    });
    const c = useMutation({
        mutationFn: () => createCategory(profileId, {
            name,
            type: 'VARIABLE_EXPENSE',
            scope: 'PERSONAL',
        }),
        onSuccess: () => {
            qc.invalidateQueries({ queryKey: ['categories', profileId] });
            setName('');
        },
    });
    const u = useMutation({
        mutationFn: (cat) => updateCategory(cat.id, {
            name: cat.name,
            type: cat.type,
            scope: cat.scope,
            parentId: cat.parentId ?? undefined,
            active: !cat.active,
        }),
        onSuccess: () => qc.invalidateQueries({ queryKey: ['categories', profileId] }),
    });
    const d = useMutation({
        mutationFn: (id) => deleteCategory(id),
        onSuccess: () => qc.invalidateQueries({ queryKey: ['categories', profileId] }),
    });
    return (_jsx(AppLayout, { children: _jsxs("div", { className: "card", children: [_jsx("h1", { children: "Categor\u00EDas" }), _jsxs("label", { children: [_jsx("input", { type: "checkbox", checked: includeGlobal, onChange: (e) => setIncludeGlobal(e.target.checked) }), ' ', "Incluir categor\u00EDas globales"] }), _jsxs("div", { className: "form-row", children: [_jsx("input", { className: "input", value: name, onChange: (e) => setName(e.target.value), placeholder: "Nombre de la categor\u00EDa" }), _jsx("button", { className: "button-primary", onClick: () => c.mutate(), disabled: !name.trim() || c.isPending, children: "Crear" })] }), q.isLoading ? (_jsx("p", { className: "empty-state", children: "Cargando categor\u00EDas..." })) : q.isError ? (_jsx("p", { className: "empty-state", children: "No se pudieron cargar las categor\u00EDas." })) : !q.data?.length ? (_jsx("p", { className: "empty-state", children: "Sin categor\u00EDas." })) : (_jsxs("table", { className: "table", children: [_jsx("thead", { children: _jsxs("tr", { children: [_jsx("th", { children: "Nombre" }), _jsx("th", { children: "Tipo" }), _jsx("th", { children: "Alcance" }), _jsx("th", { children: "Activa" }), _jsx("th", {})] }) }), _jsx("tbody", { children: q.data.map((cat) => (_jsxs("tr", { children: [_jsx("td", { children: cat.name }), _jsx("td", { children: labelOrValue(categoryTypeLabels, cat.type) }), _jsx("td", { children: labelOrValue(categoryScopeLabels, cat.scope) }), _jsx("td", { children: cat.active ? 'Sí' : 'No' }), _jsx("td", { children: cat.scope === 'GLOBAL' ? ('-') : (_jsxs(_Fragment, { children: [_jsx("button", { onClick: () => u.mutate(cat), children: cat.active ? 'Desactivar' : 'Activar' }), _jsx("button", { className: "button-danger", onClick: () => window.confirm('¿Desactivar categoría?') && d.mutate(cat.id), children: "Eliminar" })] })) })] }, cat.id))) })] }))] }) }));
}
