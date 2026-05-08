import { jsx as _jsx, jsxs as _jsxs } from "react/jsx-runtime";
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useState } from 'react';
import { useParams } from 'react-router-dom';
import { createHabit, listHabits, upsertHabitCheckin } from '../../api/habitsApi';
import { AppLayout } from '../../components/layout/AppLayout';
import { habitFrequencyLabels, labelOrMissing } from '../../domain/financeLabels';
import { habitFrequencyOptions } from '../../domain/financeOptions';
export function HabitsPage() {
    const { profileId = '' } = useParams();
    const qc = useQueryClient();
    const today = new Date().toISOString().slice(0, 10);
    const [name, setName] = useState('');
    const [frequency, setFrequency] = useState('DAILY');
    const habitsQuery = useQuery({
        queryKey: ['habits', profileId],
        queryFn: () => listHabits(profileId),
        enabled: Boolean(profileId),
    });
    const createHabitMutation = useMutation({
        mutationFn: () => createHabit(profileId, { name, frequency }),
        onSuccess: () => {
            setName('');
            qc.invalidateQueries({ queryKey: ['habits', profileId] });
        },
    });
    const checkHabitMutation = useMutation({
        mutationFn: ({ habitId, done }) => upsertHabitCheckin(profileId, habitId, today, done),
        onSuccess: () => qc.invalidateQueries({ queryKey: ['habits', profileId] }),
    });
    return (_jsxs(AppLayout, { children: [_jsx("div", { className: "page-header", children: _jsxs("div", { children: [_jsx("p", { className: "eyebrow", children: "Disciplina financiera" }), _jsx("h1", { children: "H\u00E1bitos financieros" }), _jsx("p", { className: "muted", children: "Registr\u00E1 rutinas que sostienen la salud financiera." })] }) }), _jsxs("section", { className: "card", children: [_jsx("h2", { children: "Crear h\u00E1bito" }), _jsxs("div", { className: "form-grid", children: [_jsxs("label", { className: "field", children: [_jsx("span", { children: "Nombre del h\u00E1bito" }), _jsx("input", { value: name, placeholder: "Ej: Registrar gastos, revisar presupuesto", onChange: (event) => setName(event.target.value) })] }), _jsxs("label", { className: "field", children: [_jsx("span", { children: "Frecuencia" }), _jsx("select", { value: frequency, onChange: (event) => setFrequency(event.target.value), children: habitFrequencyOptions.map((option) => (_jsx("option", { value: option.value, children: option.label }, option.value))) })] })] }), _jsx("div", { className: "actions", children: _jsx("button", { type: "button", className: "button primary", onClick: () => createHabitMutation.mutate(), disabled: !name.trim() || createHabitMutation.isPending, children: "Crear h\u00E1bito" }) })] }), _jsxs("section", { className: "card", children: [_jsx("h2", { children: "H\u00E1bitos registrados" }), habitsQuery.isLoading ? (_jsx("p", { className: "muted", children: "Cargando h\u00E1bitos..." })) : !habitsQuery.data?.length ? (_jsx("p", { className: "muted", children: "No hay h\u00E1bitos creados." })) : (_jsx("div", { className: "table-wrap", children: _jsxs("table", { className: "table", children: [_jsx("thead", { children: _jsxs("tr", { children: [_jsx("th", { children: "H\u00E1bito" }), _jsx("th", { children: "Frecuencia" }), _jsx("th", { children: "Estado" }), _jsx("th", { children: "Hoy" })] }) }), _jsx("tbody", { children: habitsQuery.data.map((habit) => (_jsxs("tr", { children: [_jsx("td", { children: habit.name }), _jsx("td", { children: labelOrMissing(habitFrequencyLabels, habit.frequency) }), _jsx("td", { children: _jsx("span", { className: `badge ${habit.active ? 'good' : 'muted'}`, children: habit.active ? 'Activo' : 'Inactivo' }) }), _jsx("td", { children: _jsx("button", { type: "button", className: "button secondary", "aria-label": "Marcar h\u00E1bito como completado", onClick: () => checkHabitMutation.mutate({ habitId: habit.id, done: true }), children: "Completar" }) })] }, habit.id))) })] }) }))] })] }));
}
