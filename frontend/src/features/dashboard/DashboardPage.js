import { jsx as _jsx, jsxs as _jsxs, Fragment as _Fragment } from "react/jsx-runtime";
import { useQuery } from '@tanstack/react-query';
import { useState } from 'react';
import { Link, useParams } from 'react-router-dom';
import { getMonthlyDashboard } from '../../api/dashboardApi';
import { AppLayout } from '../../components/layout/AppLayout';
import { EmptyState } from '../../components/ui/EmptyState';
import { ErrorState } from '../../components/ui/ErrorState';
import { MonthSelector } from '../../components/ui/MonthSelector';
import { MetricCard } from '../../components/ui/MetricCard';
import { ConfirmedVsProjectedPanel } from './components/ConfirmedVsProjectedPanel';
import { DashboardCharts } from './components/DashboardCharts';
import { OperationalAlerts } from './components/OperationalAlerts';
import { OperationalSummaryCards } from './components/OperationalSummaryCards';
import { NextBestActionCard } from './components/NextBestActionCard';
export function DashboardPage() {
    const { profileId = '' } = useParams();
    const d = new Date();
    const [year, setYear] = useState(d.getFullYear());
    const [month, setMonth] = useState(d.getMonth() + 1);
    const q = useQuery({ queryKey: ['dashboard', profileId, year, month], queryFn: () => getMonthlyDashboard(profileId, year, month), enabled: Boolean(profileId) });
    const s = q.data;
    const planning = s?.planningSummary;
    const operational = s?.operationalSummary;
    return _jsx(AppLayout, { children: _jsxs("div", { className: 'page-stack', children: [_jsxs("section", { className: 'card page-header', children: [_jsx("h1", { children: "Panel mensual" }), _jsx(MonthSelector, { year: year, month: month, onYearChange: setYear, onMonthChange: setMonth }), _jsxs("div", { className: 'page-actions', children: [_jsx(Link, { to: `/profiles/${profileId}/planning`, children: "Planificar" }), _jsx(Link, { to: `/profiles/${profileId}/transactions`, children: "Cargar movimiento" })] })] }), q.isLoading && _jsx(EmptyState, { message: 'Cargando informaci\u00F3n financiera...' }), q.isError && _jsx(ErrorState, { message: 'No se pudo cargar el panel mensual.' }), s && planning && operational && _jsxs(_Fragment, { children: [_jsx(OperationalSummaryCards, { summary: operational }), _jsx(NextBestActionCard, { profileId: profileId, summary: s }), _jsx(OperationalAlerts, { alerts: operational.alerts }), _jsx(ConfirmedVsProjectedPanel, { planning: planning, operational: operational }), _jsxs("section", { className: 'metric-grid', children: [_jsx(MetricCard, { title: 'Sin cotizar', value: planning.unpricedCount }), _jsx(MetricCard, { title: 'Pr\u00F3ximos 7 d\u00EDas', value: planning.dueNext7DaysCount }), _jsx(MetricCard, { title: 'Items convertidos', value: planning.convertedItemsCount }), _jsx(MetricCard, { title: 'Items cancelados', value: planning.cancelledItemsCount })] }), _jsxs("section", { className: 'card', children: [_jsx("h3", { className: 'section-title', children: "Regla 50/30/20" }), _jsxs("p", { children: ["Gastos fijos: ", s.fiftyThirtyTwenty?.fixedPercent ?? 0, "% | Gastos variables: ", s.fiftyThirtyTwenty?.variablePercent ?? 0, "% | Ahorro: ", s.fiftyThirtyTwenty?.savingPercent ?? 0, "%"] })] }), _jsx(DashboardCharts, { summary: s })] })] }) });
}
