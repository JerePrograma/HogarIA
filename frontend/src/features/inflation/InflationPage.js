import { jsx as _jsx, jsxs as _jsxs } from "react/jsx-runtime";
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useState } from 'react';
import { Line, LineChart, ResponsiveContainer, Tooltip, XAxis, YAxis, CartesianGrid } from 'recharts';
import { createInflation, getAccumulatedInflation, listInflation } from '../../api/inflationApi';
import { AppLayout } from '../../components/layout/AppLayout';
import { monthLabels } from '../../domain/financeLabels';
import { formatPercent } from '../../domain/formatters';
export function InflationPage() {
    const today = new Date();
    const [year, setYear] = useState(today.getFullYear());
    const [month, setMonth] = useState(today.getMonth() + 1);
    const [ratePercent, setRatePercent] = useState(3);
    const qc = useQueryClient();
    const inflationQuery = useQuery({
        queryKey: ['inflation', year],
        queryFn: () => listInflation(year),
    });
    const accumulatedQuery = useQuery({
        queryKey: ['inflation-acc', year],
        queryFn: () => getAccumulatedInflation(year, 1, year, 12),
    });
    const createInflationMutation = useMutation({
        mutationFn: () => createInflation({
            year,
            month,
            rate: ratePercent / 100,
            source: 'MANUAL',
            observed: true,
        }),
        onSuccess: () => {
            qc.invalidateQueries({ queryKey: ['inflation', year] });
            qc.invalidateQueries({ queryKey: ['inflation-acc', year] });
        },
    });
    const chartData = inflationQuery.data?.map((item) => ({
        month: monthLabels[item.month] ?? item.month,
        rate: Number(item.rate ?? 0) * 100,
    })) ?? [];
    return (_jsxs(AppLayout, { children: [_jsx("div", { className: "page-header", children: _jsxs("div", { children: [_jsx("p", { className: "eyebrow", children: "Contexto econ\u00F3mico" }), _jsx("h1", { children: "Inflaci\u00F3n y proyecciones" }), _jsx("p", { className: "muted", children: "Registr\u00E1 \u00EDndices mensuales y revis\u00E1 el acumulado anual." })] }) }), _jsxs("section", { className: "card", children: [_jsx("h2", { children: "Cargar \u00EDndice mensual" }), _jsxs("div", { className: "form-grid", children: [_jsxs("label", { className: "field", children: [_jsx("span", { children: "A\u00F1o" }), _jsx("input", { type: "number", value: year, onChange: (event) => setYear(Number(event.target.value)) })] }), _jsxs("label", { className: "field", children: [_jsx("span", { children: "Mes" }), _jsx("select", { value: month, onChange: (event) => setMonth(Number(event.target.value)), children: Object.entries(monthLabels).map(([value, label]) => (_jsx("option", { value: value, children: label }, value))) })] }), _jsxs("label", { className: "field", children: [_jsx("span", { children: "Tasa mensual de inflaci\u00F3n (%)" }), _jsx("input", { type: "number", step: "0.01", value: ratePercent, placeholder: "Ej: 12.4", onChange: (event) => setRatePercent(Number(event.target.value)) })] })] }), _jsx("div", { className: "actions", children: _jsx("button", { type: "button", className: "button primary", onClick: () => createInflationMutation.mutate(), disabled: createInflationMutation.isPending, children: "Guardar \u00EDndice" }) })] }), _jsxs("section", { className: "card", children: [_jsx("div", { className: "section-header", children: _jsxs("div", { children: [_jsx("h2", { children: "Evoluci\u00F3n anual" }), _jsxs("p", { className: "muted", children: ["Acumulada anual:", ' ', _jsx("strong", { children: formatPercent(Number(accumulatedQuery.data?.accumulatedRate ?? 0) * 100) })] })] }) }), _jsx("div", { className: "chart-box", children: _jsx(ResponsiveContainer, { width: "100%", height: 320, children: _jsxs(LineChart, { data: chartData, children: [_jsx(CartesianGrid, { strokeDasharray: "3 3" }), _jsx(XAxis, { dataKey: "month" }), _jsx(YAxis, {}), _jsx(Tooltip, { formatter: (value) => [formatPercent(Number(value)), 'Inflación mensual'], labelFormatter: (label) => `Mes: ${label}` }), _jsx(Line, { type: "monotone", dataKey: "rate", name: "Inflaci\u00F3n mensual", stroke: "var(--chart-1)", strokeWidth: 3 })] }) }) })] })] }));
}
