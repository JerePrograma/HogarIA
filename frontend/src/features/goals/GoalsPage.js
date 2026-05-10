import { jsx as _jsx, jsxs as _jsxs, Fragment as _Fragment } from "react/jsx-runtime";
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useState } from 'react';
import { useParams } from 'react-router-dom';
import { createEmergencyFund, createGoal, deleteGoal, listGoals, } from '../../api/goalsApi';
import { AppLayout } from '../../components/layout/AppLayout';
const moneyFormatter = new Intl.NumberFormat('es-AR', {
    style: 'currency',
    currency: 'ARS',
    maximumFractionDigits: 0,
});
const percentFormatter = new Intl.NumberFormat('es-AR', {
    maximumFractionDigits: 2,
});
const goalTypeLabels = {
    EMERGENCY_FUND: 'Fondo de emergencia',
    DEBT_PAYOFF: 'Cancelación de deuda',
    SAVING_TARGET: 'Meta de ahorro',
    INVESTMENT: 'Inversión',
    BUSINESS: 'Negocio',
    TRAVEL: 'Viaje',
    EDUCATION: 'Educación',
    OTHER: 'Otro',
};
const goalStatusLabels = {
    ACTIVE: 'Activo',
    PAUSED: 'Pausado',
    COMPLETED: 'Completado',
    CANCELLED: 'Cancelado',
};
const goalTypeOptions = [
    { value: 'EMERGENCY_FUND', label: 'Fondo de emergencia' },
    { value: 'DEBT_PAYOFF', label: 'Cancelación de deuda' },
    { value: 'SAVING_TARGET', label: 'Meta de ahorro' },
    { value: 'INVESTMENT', label: 'Inversión' },
    { value: 'BUSINESS', label: 'Negocio' },
    { value: 'TRAVEL', label: 'Viaje' },
    { value: 'EDUCATION', label: 'Educación' },
    { value: 'OTHER', label: 'Otro' },
];
const formatMoney = (value) => {
    const numberValue = Number(value ?? 0);
    return Number.isFinite(numberValue) ? moneyFormatter.format(numberValue) : '-';
};
const formatPercent = (value) => {
    const numberValue = Number(value ?? 0);
    return Number.isFinite(numberValue)
        ? `${percentFormatter.format(numberValue)}%`
        : '-';
};
const formatDate = (value) => {
    if (!value)
        return 'Sin fecha límite';
    return new Intl.DateTimeFormat('es-AR').format(new Date(value));
};
const getGoalTypeLabel = (type) => {
    return goalTypeLabels[type] ?? type;
};
const getGoalStatusLabel = (status) => {
    return goalStatusLabels[status] ?? status;
};
export function GoalsPage() {
    const { profileId = '' } = useParams();
    const queryClient = useQueryClient();
    const [name, setName] = useState('');
    const [targetAmount, setTargetAmount] = useState(0);
    const [currentAmount, setCurrentAmount] = useState(0);
    const [monthlyContribution, setMonthlyContribution] = useState(0);
    const [targetDate, setTargetDate] = useState('');
    const [notes, setNotes] = useState('');
    const [goalType, setGoalType] = useState('OTHER');
    const goalsQuery = useQuery({
        queryKey: ['goals', profileId],
        queryFn: () => listGoals(profileId),
        enabled: Boolean(profileId),
    });
    const createMutation = useMutation({
        mutationFn: () => createGoal(profileId, {
            name,
            goalType,
            targetAmount,
            currentAmount,
            monthlyContribution: monthlyContribution > 0 ? monthlyContribution : null,
            targetDate: targetDate || null,
            notes: notes || null,
        }),
        onSuccess: () => {
            setName('');
            setTargetAmount(0);
            setCurrentAmount(0);
            setMonthlyContribution(0);
            setTargetDate('');
            setNotes('');
            setGoalType('OTHER');
            queryClient.invalidateQueries({ queryKey: ['goals', profileId] });
        },
    });
    const emergencyMutation = useMutation({
        mutationFn: (coverageMonths) => createEmergencyFund(profileId, coverageMonths),
        onSuccess: () => {
            queryClient.invalidateQueries({ queryKey: ['goals', profileId] });
        },
    });
    const deleteMutation = useMutation({
        mutationFn: (goalId) => deleteGoal(profileId, goalId),
        onSuccess: () => {
            queryClient.invalidateQueries({ queryKey: ['goals', profileId] });
        },
    });
    const canCreate = name.trim().length > 0 &&
        targetAmount > 0 &&
        Boolean(profileId) &&
        !createMutation.isPending;
    return (_jsx(AppLayout, { children: _jsxs("div", { className: "card", children: [_jsx("h1", { children: "Objetivos financieros" }), _jsx("p", { className: "empty-state", children: "Defin\u00ED metas concretas para ordenar ahorro, deuda, inversi\u00F3n y fondo de emergencia." }), _jsxs("div", { className: "card", children: [_jsx("h3", { children: "Crear objetivo" }), _jsxs("div", { className: "form-row", children: [_jsxs("label", { children: ["Nombre", _jsx("input", { className: "input", placeholder: "Ej: Fondo de emergencia", value: name, onChange: (event) => setName(event.target.value) })] }), _jsxs("label", { children: ["Tipo", _jsx("select", { className: "select", value: goalType, onChange: (event) => setGoalType(event.target.value), children: goalTypeOptions.map((option) => (_jsx("option", { value: option.value, children: option.label }, option.value))) })] }), _jsxs("label", { children: ["Monto objetivo", _jsx("input", { className: "input", type: "number", min: 0, value: targetAmount, onChange: (event) => setTargetAmount(Number(event.target.value)) })] }), _jsxs("label", { children: ["Monto actual", _jsx("input", { className: "input", type: "number", min: 0, value: currentAmount, onChange: (event) => setCurrentAmount(Number(event.target.value)) })] }), _jsxs("label", { children: ["Aporte mensual", _jsx("input", { className: "input", type: "number", min: 0, value: monthlyContribution, onChange: (event) => setMonthlyContribution(Number(event.target.value)) })] }), _jsxs("label", { children: ["Fecha l\u00EDmite", _jsx("input", { className: "input", type: "date", value: targetDate, onChange: (event) => setTargetDate(event.target.value) })] })] }), _jsxs("label", { children: ["Notas", _jsx("textarea", { className: "input", placeholder: "Detalle opcional del objetivo", value: notes, onChange: (event) => setNotes(event.target.value) })] }), _jsx("div", { className: "form-row", children: _jsx("button", { className: "button-primary", disabled: !canCreate, onClick: () => createMutation.mutate(), children: createMutation.isPending ? 'Creando...' : 'Crear objetivo' }) }), createMutation.isError && (_jsx("p", { className: "error-box", children: "No se pudo crear el objetivo. Revis\u00E1 los datos ingresados." }))] }), _jsxs("div", { className: "card", children: [_jsx("h3", { children: "Crear fondo de emergencia sugerido" }), _jsx("p", { children: "Gener\u00E1 autom\u00E1ticamente un objetivo para cubrir entre 3 y 6 meses de gastos." }), _jsx("div", { className: "form-row", children: [3, 4, 5, 6].map((months) => (_jsxs("button", { className: "button-secondary", disabled: emergencyMutation.isPending, onClick: () => emergencyMutation.mutate(months), children: [months, " meses"] }, months))) }), emergencyMutation.isError && (_jsx("p", { className: "error-box", children: "No se pudo crear el fondo de emergencia sugerido." }))] }), goalsQuery.isLoading && (_jsx("p", { className: "empty-state", children: "Cargando objetivos financieros..." })), goalsQuery.isError && (_jsx("p", { className: "error-box", children: "No se pudieron cargar los objetivos financieros." })), !goalsQuery.isLoading &&
                    !goalsQuery.isError &&
                    goalsQuery.data?.length === 0 && (_jsx("p", { className: "empty-state", children: "Todav\u00EDa no ten\u00E9s objetivos cargados." })), _jsx("div", { className: "grid", children: goalsQuery.data?.map((goal) => {
                        const progressPercent = Number(goal.progressPercent ?? 0);
                        const safeProgress = Math.min(Math.max(progressPercent, 0), 100);
                        return (_jsxs("div", { className: "card", children: [_jsx("b", { children: goal.name }), _jsxs("p", { children: [getGoalTypeLabel(goal.goalType), " \u00B7", ' ', getGoalStatusLabel(goal.status)] }), _jsxs("p", { children: ["Objetivo: ", formatMoney(goal.targetAmount), _jsx("br", {}), "Actual: ", formatMoney(goal.currentAmount), _jsx("br", {}), "Aporte mensual: ", formatMoney(goal.monthlyContribution), _jsx("br", {}), "Fecha l\u00EDmite: ", formatDate(goal.targetDate)] }), _jsx("div", { className: "progress-track", children: _jsx("div", { className: "progress-fill", style: { width: `${safeProgress}%` } }) }), _jsxs("p", { children: ["Progreso: ", formatPercent(goal.progressPercent), goal.monthsRemaining != null && (_jsxs(_Fragment, { children: [_jsx("br", {}), "Meses estimados restantes: ", goal.monthsRemaining] }))] }), goal.notes && _jsx("p", { children: goal.notes }), _jsx("button", { className: "button-danger", disabled: deleteMutation.isPending, onClick: () => deleteMutation.mutate(goal.id), children: "Eliminar" })] }, goal.id));
                    }) })] }) }));
}
