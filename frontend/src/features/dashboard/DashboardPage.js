import { jsx as _jsx, jsxs as _jsxs, Fragment as _Fragment } from "react/jsx-runtime";
import { useQuery } from '@tanstack/react-query';
import { useState } from 'react';
import { Link, useParams } from 'react-router-dom';
import { Bar, BarChart, CartesianGrid, Cell, Legend, Pie, PieChart, ResponsiveContainer, Tooltip, XAxis, YAxis, } from 'recharts';
import { getMonthlyDashboard } from '../../api/dashboardApi';
import { AppLayout } from '../../components/layout/AppLayout';
import { categoryTypeLabels, financialHealthLabels, labelOrMissing, monthLabels } from '../../domain/financeLabels';
import { formatMoney, formatPercent } from '../../domain/formatters';
const chartColors = [
    'var(--chart-1)',
    'var(--chart-2)',
    'var(--chart-3)',
    'var(--chart-4)',
];
export function DashboardPage() {
    const { profileId = '' } = useParams();
    const today = new Date();
    const [year, setYear] = useState(today.getFullYear());
    const [month, setMonth] = useState(today.getMonth() + 1);
    const dashboardQuery = useQuery({
        queryKey: ['dash', profileId, year, month],
        queryFn: () => getMonthlyDashboard(profileId, year, month),
        enabled: Boolean(profileId),
    });
    const summary = dashboardQuery.data;
    const distributionData = [
        { name: 'Gastos fijos', value: Number(summary?.fixedExpenses ?? 0) },
        { name: 'Gastos variables', value: Number(summary?.variableExpenses ?? 0) },
        { name: 'Ahorro', value: Number(summary?.monthlyBalance?.savings ?? 0) },
    ];
    const budgetData = [
        { name: 'Presupuestado', monto: Number(summary?.budgetSummary?.totalBudget ?? 0) },
        { name: 'Ejecutado', monto: Number(summary?.budgetSummary?.totalReal ?? 0) },
    ];
    const ruleData = [
        { name: 'Gastos fijos', porcentaje: Number(summary?.fiftyThirtyTwenty?.fixedPercent ?? 0) },
        { name: 'Gastos variables', porcentaje: Number(summary?.fiftyThirtyTwenty?.variablePercent ?? 0) },
        { name: 'Ahorro', porcentaje: Number(summary?.fiftyThirtyTwenty?.savingPercent ?? 0) },
    ];
    const kpis = [
        { label: 'Ingresos', value: formatMoney(summary?.monthlyBalance?.totalIncome), tone: 'good' },
        { label: 'Gastos fijos', value: formatMoney(summary?.fixedExpenses), tone: 'warning' },
        { label: 'Gastos variables', value: formatMoney(summary?.variableExpenses), tone: 'warning' },
        { label: 'Ahorro', value: formatMoney(summary?.monthlyBalance?.savings), tone: 'good' },
        { label: 'Balance', value: formatMoney(summary?.monthlyBalance?.balance), tone: 'neutral' },
        {
            label: 'Salud financiera',
            value: labelOrMissing(financialHealthLabels, summary?.financialHealth),
            tone: summary?.financialHealth === 'CRITICAL' ? 'danger' : summary?.financialHealth === 'WARNING' ? 'warning' : 'good',
        },
    ];
    return (_jsxs(AppLayout, { children: [_jsxs("div", { className: "page-header", children: [_jsxs("div", { children: [_jsx("p", { className: "eyebrow", children: "Resumen mensual" }), _jsx("h1", { children: "Panel financiero" }), _jsxs("p", { className: "muted", children: [monthLabels[month] ?? month, " ", year, ": estado general, desv\u00EDos y composici\u00F3n del mes."] })] }), _jsxs("div", { className: "toolbar", children: [_jsxs("label", { className: "field compact-field", children: [_jsx("span", { children: "A\u00F1o" }), _jsx("input", { type: "number", value: year, onChange: (event) => setYear(Number(event.target.value)) })] }), _jsxs("label", { className: "field compact-field", children: [_jsx("span", { children: "Mes" }), _jsx("select", { value: month, onChange: (event) => setMonth(Number(event.target.value)), children: Object.entries(monthLabels).map(([value, label]) => (_jsx("option", { value: value, children: label }, value))) })] })] })] }), dashboardQuery.isLoading ? (_jsx("p", { className: "muted", children: "Cargando panel financiero..." })) : dashboardQuery.isError ? (_jsx("div", { className: "alert danger", children: "No se pudo cargar el panel mensual." })) : (_jsxs(_Fragment, { children: [_jsx("section", { className: "kpi-grid", children: kpis.map((kpi) => (_jsxs("article", { className: "kpi-card", children: [_jsx("span", { children: kpi.label }), _jsx("strong", { className: kpi.label === 'Salud financiera' ? '' : 'amount', children: kpi.value }), _jsx("small", { className: `badge ${kpi.tone}`, children: kpi.label })] }, kpi.label))) }), _jsxs("section", { className: "chart-grid", children: [_jsxs("article", { className: "card", children: [_jsx("h2", { children: "Distribuci\u00F3n del mes" }), _jsx("div", { className: "chart-box", children: _jsx(ResponsiveContainer, { width: "100%", height: 260, children: _jsxs(PieChart, { children: [_jsx(Pie, { data: distributionData, dataKey: "value", nameKey: "name", outerRadius: 90, label: true, children: distributionData.map((entry, index) => (_jsx(Cell, { fill: chartColors[index % chartColors.length] }, entry.name))) }), _jsx(Tooltip, { formatter: (value) => formatMoney(Number(value)) }), _jsx(Legend, {})] }) }) })] }), _jsxs("article", { className: "card", children: [_jsx("h2", { children: "Presupuesto vs ejecutado" }), _jsx("div", { className: "chart-box", children: _jsx(ResponsiveContainer, { width: "100%", height: 260, children: _jsxs(BarChart, { data: budgetData, children: [_jsx(CartesianGrid, { strokeDasharray: "3 3" }), _jsx(XAxis, { dataKey: "name" }), _jsx(YAxis, {}), _jsx(Tooltip, { formatter: (value) => formatMoney(Number(value)) }), _jsx(Bar, { dataKey: "monto", name: "Monto", fill: "var(--chart-1)", radius: [8, 8, 0, 0] })] }) }) })] }), _jsxs("article", { className: "card", children: [_jsx("h2", { children: "Regla 50/30/20" }), _jsx("p", { className: "muted", children: "Comparaci\u00F3n r\u00E1pida entre gastos fijos, variables y ahorro." }), _jsx("div", { className: "chart-box", children: _jsx(ResponsiveContainer, { width: "100%", height: 260, children: _jsxs(BarChart, { data: ruleData, children: [_jsx(CartesianGrid, { strokeDasharray: "3 3" }), _jsx(XAxis, { dataKey: "name" }), _jsx(YAxis, {}), _jsx(Tooltip, { formatter: (value) => formatPercent(Number(value)) }), _jsx(Bar, { dataKey: "porcentaje", name: "Porcentaje", fill: "var(--chart-2)", radius: [8, 8, 0, 0] })] }) }) })] })] }), _jsxs("section", { className: "card", children: [_jsx("div", { className: "section-header", children: _jsxs("div", { children: [_jsx("h2", { children: "Desglose por categor\u00EDa" }), _jsx("p", { className: "muted", children: "Lectura r\u00E1pida de peso relativo y cantidad de movimientos." })] }) }), !summary?.categoryBreakdown?.length ? (_jsx("p", { className: "muted", children: "No hay movimientos para mostrar en este per\u00EDodo." })) : (_jsx("div", { className: "table-wrap", children: _jsxs("table", { className: "table", children: [_jsx("thead", { children: _jsxs("tr", { children: [_jsx("th", { children: "Categor\u00EDa" }), _jsx("th", { children: "Tipo" }), _jsx("th", { className: "amount", children: "Total" }), _jsx("th", { className: "amount", children: "% ingreso" }), _jsx("th", { className: "amount", children: "Movimientos" })] }) }), _jsx("tbody", { children: summary.categoryBreakdown.map((item) => (_jsxs("tr", { children: [_jsx("td", { children: item.categoryName }), _jsx("td", { children: labelOrMissing(categoryTypeLabels, item.categoryType) }), _jsx("td", { className: "amount", children: formatMoney(item.totalAmount) }), _jsx("td", { className: "amount", children: formatPercent(item.percentOfIncome) }), _jsx("td", { className: "amount", children: item.movementCount })] }, item.categoryId))) })] }) }))] }), _jsxs("section", { className: "dashboard-links", children: [_jsx(Link, { to: `../transactions`, className: "quick-link", children: "Movimientos" }), _jsx(Link, { to: `../budgets`, className: "quick-link", children: "Presupuesto" }), _jsx(Link, { to: `../goals`, className: "quick-link", children: "Objetivos" }), _jsx(Link, { to: `../habits`, className: "quick-link", children: "H\u00E1bitos" })] })] }))] }));
}
