import { jsx as _jsx, jsxs as _jsxs } from "react/jsx-runtime";
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useState } from 'react';
import { useParams } from 'react-router-dom';
import { createEmergencyFund, createGoal, deleteGoal, listGoals } from '../../api/goalsApi';
import { AppLayout } from '../../components/layout/AppLayout';
import { goalStatusLabels, goalTypeLabels, labelOrMissing } from '../../domain/financeLabels';
import { goalTypeOptions } from '../../domain/financeOptions';
import { formatMoney } from '../../domain/formatters';
export function GoalsPage() {
    const { profileId = '' } = useParams();
    const qc = useQueryClient();
    const [name, setName] = useState('');
    const [targetAmount, setTargetAmount] = useState(0);
    const [goalType, setGoalType] = useState('OTHER');
    const goalsQuery = useQuery({
        queryKey: ['goals', profileId],
        queryFn: () => listGoals(profileId),
        enabled: Boolean(profileId),
    });
    const createGoalMutation = useMutation({
        mutationFn: () => createGoal(profileId, {
            name,
            goalType,
            targetAmount,
            currentAmount: 0,
            priority: 3,
        }),
        onSuccess: () => {
            setName('');
            setTargetAmount(0);
            qc.invalidateQueries({ queryKey: ['goals', profileId] });
        },
    });
    const emergencyFundMutation = useMutation({
        mutationFn: (coverageMonths) => createEmergencyFund(profileId, coverageMonths),
        onSuccess: () => qc.invalidateQueries({ queryKey: ['goals', profileId] }),
    });
    const deleteGoalMutation = useMutation({
        mutationFn: (goalId) => deleteGoal(profileId, goalId),
        onSuccess: () => qc.invalidateQueries({ queryKey: ['goals', profileId] }),
    });
    const canCreate = Boolean(name.trim()) && targetAmount > 0;
    return (_jsxs(AppLayout, { children: [_jsx("div", { className: "page-header", children: _jsxs("div", { children: [_jsx("p", { className: "eyebrow", children: "Planificaci\u00F3n" }), _jsx("h1", { children: "Objetivos financieros" }), _jsx("p", { className: "muted", children: "Convert\u00ED metas abstractas en progreso medible." })] }) }), _jsxs("section", { className: "card", children: [_jsx("h2", { children: "Crear objetivo" }), _jsxs("div", { className: "form-grid", children: [_jsxs("label", { className: "field", children: [_jsx("span", { children: "Nombre del objetivo" }), _jsx("input", { value: name, placeholder: "Ej: Fondo de emergencia, viaje, inversi\u00F3n", onChange: (event) => setName(event.target.value) })] }), _jsxs("label", { className: "field", children: [_jsx("span", { children: "Tipo de objetivo" }), _jsx("select", { value: goalType, onChange: (event) => setGoalType(event.target.value), children: goalTypeOptions.map((option) => (_jsx("option", { value: option.value, children: option.label }, option.value))) })] }), _jsxs("label", { className: "field", children: [_jsx("span", { children: "Monto objetivo" }), _jsx("input", { type: "number", min: "0", value: targetAmount, placeholder: "Ej: 500000", onChange: (event) => setTargetAmount(Number(event.target.value)) })] })] }), _jsx("div", { className: "actions", children: _jsx("button", { type: "button", className: "button primary", onClick: () => createGoalMutation.mutate(), disabled: !canCreate || createGoalMutation.isPending, children: "Crear objetivo" }) })] }), _jsxs("section", { className: "card", children: [_jsx("h2", { children: "Fondo de emergencia sugerido" }), _jsx("p", { className: "muted", children: "Cre\u00E1 una meta autom\u00E1tica seg\u00FAn cantidad de meses de cobertura." }), _jsx("div", { className: "actions", children: [3, 4, 5, 6].map((months) => (_jsxs("button", { type: "button", className: "button secondary", onClick: () => emergencyFundMutation.mutate(months), disabled: emergencyFundMutation.isPending, children: [months, " meses"] }, months))) })] }), _jsx("section", { className: "goal-grid", children: goalsQuery.isLoading ? (_jsx("p", { className: "muted", children: "Cargando objetivos..." })) : !goalsQuery.data?.length ? (_jsx("article", { className: "card", children: _jsx("p", { className: "muted", children: "No hay objetivos financieros creados." }) })) : (goalsQuery.data.map((goal) => {
                    const progress = Math.min(100, Math.max(0, Number(goal.progress ?? 0)));
                    return (_jsxs("article", { className: "card goal-card", children: [_jsxs("div", { className: "section-header", children: [_jsxs("div", { children: [_jsx("h2", { children: goal.name }), _jsxs("p", { className: "muted", children: [labelOrMissing(goalTypeLabels, goal.goalType), " \u00B7 ", labelOrMissing(goalStatusLabels, goal.status)] })] }), _jsxs("span", { className: "badge good", children: [progress.toFixed(1), "%"] })] }), _jsxs("div", { className: "goal-amounts", children: [_jsxs("span", { children: ["Meta: ", _jsx("strong", { children: formatMoney(goal.targetAmount) })] }), _jsxs("span", { children: ["Ahorrado: ", _jsx("strong", { children: formatMoney(goal.currentAmount) })] })] }), _jsx("div", { className: "progress", "aria-label": `Progreso ${progress.toFixed(1)}%`, children: _jsx("div", { className: "progress-bar", style: { width: `${progress}%` } }) }), _jsx("div", { className: "actions", children: _jsx("button", { type: "button", className: "button danger", onClick: () => window.confirm('¿Eliminar objetivo?') && deleteGoalMutation.mutate(goal.id), children: "Eliminar" }) })] }, goal.id));
                })) })] }));
}
