import { jsx as _jsx, jsxs as _jsxs } from "react/jsx-runtime";
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useMemo, useState } from 'react';
import { useParams } from 'react-router-dom';
import { createHabit, listHabits, upsertHabitCheckin, } from '../../api/habitsApi';
import { getApiErrorMessage } from '../../api/http';
import { AppLayout } from '../../components/layout/AppLayout';
import { habitFrequencyLabels, labelOrValue, } from '../../domain/financeLabels';
import { normalizeOptionalText } from '../../domain/formatters';
const frequencyOptions = [
    { value: 'DAILY', label: 'Diario' },
    { value: 'WEEKLY', label: 'Semanal' },
    { value: 'MONTHLY', label: 'Mensual' },
];
function getTodayIsoDate() {
    return new Date().toISOString().slice(0, 10);
}
function getHabitStatusBadgeClass(active) {
    return active ? 'badge badge-ok' : 'badge badge-muted';
}
export function HabitsPage() {
    const { profileId = '' } = useParams();
    const queryClient = useQueryClient();
    const today = getTodayIsoDate();
    const [description, setDescription] = useState('');
    const [frequency, setFrequency] = useState('DAILY');
    const [area, setArea] = useState('FINANZAS');
    const [feedback, setFeedback] = useState('');
    const habitsQuery = useQuery({
        queryKey: ['habits', profileId],
        queryFn: () => listHabits(profileId),
        enabled: Boolean(profileId),
    });
    const habits = habitsQuery.data ?? [];
    const activeHabits = useMemo(() => habits.filter((habit) => habit.active), [habits]);
    const inactiveHabits = useMemo(() => habits.filter((habit) => !habit.active), [habits]);
    const createHabitMutation = useMutation({
        mutationFn: () => createHabit(profileId, {
            description: description.trim(),
            area: area.trim() || 'FINANZAS',
            frequency,
        }),
        onSuccess: () => {
            setDescription('');
            setArea('FINANZAS');
            setFrequency('DAILY');
            setFeedback('Hábito creado correctamente.');
            queryClient.invalidateQueries({ queryKey: ['habits', profileId] });
        },
        onError: (error) => {
            setFeedback(getApiErrorMessage(error));
        },
    });
    const checkinMutation = useMutation({
        mutationFn: ({ habitId, completed, }) => upsertHabitCheckin(profileId, habitId, today, {
            completed,
            note: completed
                ? 'Completado desde la web'
                : 'Marcado como pendiente desde la web',
        }),
        onSuccess: () => {
            setFeedback('Check-in actualizado.');
            queryClient.invalidateQueries({ queryKey: ['habits', profileId] });
        },
        onError: (error) => {
            setFeedback(getApiErrorMessage(error));
        },
    });
    const canCreate = description.trim().length > 0 &&
        Boolean(profileId) &&
        !createHabitMutation.isPending;
    return (_jsx(AppLayout, { children: _jsxs("div", { className: "page-stack", children: [_jsx("section", { className: "page-header", children: _jsxs("div", { children: [_jsx("p", { className: "eyebrow", children: "Seguimiento financiero" }), _jsx("h1", { children: "H\u00E1bitos financieros" }), _jsx("p", { className: "muted", children: "Registr\u00E1 rutinas simples para sostener el control mensual: revisar gastos, anotar movimientos, controlar presupuesto y evitar desv\u00EDos." })] }) }), _jsxs("section", { className: "summary-grid", children: [_jsxs("div", { className: "metric-card metric-income", children: [_jsx("span", { children: "H\u00E1bitos activos" }), _jsx("strong", { children: activeHabits.length })] }), _jsxs("div", { className: "metric-card", children: [_jsx("span", { children: "Total cargados" }), _jsx("strong", { children: habits.length })] }), _jsxs("div", { className: "metric-card metric-saving", children: [_jsx("span", { children: "Frecuencia diaria" }), _jsx("strong", { children: habits.filter((habit) => habit.frequency === 'DAILY').length })] }), _jsxs("div", { className: "metric-card metric-expense", children: [_jsx("span", { children: "Pausados" }), _jsx("strong", { children: inactiveHabits.length })] })] }), _jsxs("section", { className: "card", children: [_jsx("div", { className: "section-title", children: _jsxs("div", { children: [_jsx("h2", { children: "Crear h\u00E1bito" }), _jsx("p", { className: "muted", children: "Carg\u00E1 h\u00E1bitos concretos y medibles. Si no se puede marcar como hecho, est\u00E1 mal definido." })] }) }), _jsxs("div", { className: "form-grid habits-form-grid", children: [_jsxs("label", { className: "form-field-wide", children: ["Descripci\u00F3n", _jsx("input", { className: "input", placeholder: "Ej: revisar gastos del d\u00EDa", value: description, onChange: (event) => setDescription(event.target.value) })] }), _jsxs("label", { children: ["\u00C1rea", _jsx("input", { className: "input", placeholder: "Ej: FINANZAS", value: area, onChange: (event) => setArea(event.target.value) })] }), _jsxs("label", { children: ["Frecuencia", _jsx("select", { className: "select", value: frequency, onChange: (event) => setFrequency(event.target.value), children: frequencyOptions.map((option) => (_jsx("option", { value: option.value, children: option.label }, option.value))) })] })] }), _jsxs("div", { className: "form-actions", children: [_jsx("button", { className: "button-primary", disabled: !canCreate, onClick: () => createHabitMutation.mutate(), children: createHabitMutation.isPending ? 'Creando...' : 'Crear hábito' }), !canCreate && (_jsx("span", { className: "muted", children: "Complet\u00E1 una descripci\u00F3n para crear el h\u00E1bito." }))] }), feedback && (_jsx("p", { className: feedback.includes('correctamente') ||
                                feedback.includes('actualizado')
                                ? 'success-box'
                                : 'error-box', children: feedback }))] }), _jsxs("section", { className: "card", children: [_jsx("div", { className: "section-title", children: _jsxs("div", { children: [_jsx("h2", { children: "H\u00E1bitos cargados" }), _jsxs("p", { className: "muted", children: [habits.length, " h\u00E1bito", habits.length === 1 ? '' : 's', " registrado", habits.length === 1 ? '' : 's', "."] })] }) }), habitsQuery.isLoading && (_jsx("p", { className: "empty-state", children: "Cargando h\u00E1bitos financieros..." })), habitsQuery.isError && (_jsx("p", { className: "error-box", children: "No se pudieron cargar los h\u00E1bitos financieros." })), !habitsQuery.isLoading &&
                            !habitsQuery.isError &&
                            habits.length === 0 && (_jsx("p", { className: "empty-state", children: "Todav\u00EDa no ten\u00E9s h\u00E1bitos cargados. Empez\u00E1 con uno simple: \u201Canotar gastos del d\u00EDa\u201D." })), habits.length > 0 && (_jsx("div", { className: "table-wrapper", children: _jsxs("table", { className: "table table-compact", children: [_jsx("thead", { children: _jsxs("tr", { children: [_jsx("th", { children: "H\u00E1bito" }), _jsx("th", { children: "\u00C1rea" }), _jsx("th", { children: "Frecuencia" }), _jsx("th", { children: "Estado" }), _jsx("th", { children: "Check-in de hoy" })] }) }), _jsx("tbody", { children: habits.map((habit) => (_jsxs("tr", { children: [_jsx("td", { children: _jsx("strong", { children: habit.description }) }), _jsx("td", { children: normalizeOptionalText(habit.area) }), _jsx("td", { children: labelOrValue(habitFrequencyLabels, habit.frequency) }), _jsx("td", { children: _jsx("span", { className: getHabitStatusBadgeClass(habit.active), children: habit.active ? 'Activo' : 'Pausado' }) }), _jsx("td", { children: _jsxs("div", { className: "row-actions", children: [_jsx("button", { className: "button-secondary", disabled: checkinMutation.isPending || !habit.active, onClick: () => checkinMutation.mutate({
                                                                    habitId: habit.id,
                                                                    completed: true,
                                                                }), children: "Marcar completo" }), _jsx("button", { disabled: checkinMutation.isPending || !habit.active, onClick: () => checkinMutation.mutate({
                                                                    habitId: habit.id,
                                                                    completed: false,
                                                                }), children: "Marcar pendiente" })] }) })] }, habit.id))) })] }) }))] })] }) }));
}
