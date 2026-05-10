import { jsx as _jsx, jsxs as _jsxs } from "react/jsx-runtime";
import { MetricCard } from '../../../components/ui/MetricCard';
import { StatusBadge } from '../../../components/ui/StatusBadge';
import { formatMoney } from '../../../domain/formatters';
const riskLabels = { OK: 'Correcto', WATCH: 'Atención', RISK: 'Riesgo', CRITICAL: 'Crítico' };
const tone = { OK: 'ok', WATCH: 'watch', RISK: 'risk', CRITICAL: 'critical' };
export function OperationalSummaryCards({ summary }) {
    return _jsxs("section", { children: [_jsx("h3", { className: 'section-title', children: "Estado operativo del mes" }), _jsxs("div", { className: 'metric-grid', children: [_jsx(MetricCard, { title: 'Neto proyectado', primary: true, value: `${formatMoney(summary.projectedNetMin)} – ${formatMoney(summary.projectedNetMax)}` }), _jsx(MetricCard, { title: 'Saldo confirmado', value: formatMoney(summary.confirmedBalance) }), _jsx(MetricCard, { title: 'Pendiente de cobro', value: formatMoney(summary.pendingIncome) }), _jsx(MetricCard, { title: 'Pendiente de pago', value: formatMoney(summary.pendingExpense) }), _jsx(MetricCard, { title: 'Recuperos esperados', value: `${formatMoney(summary.expectedRecoveriesMin)} – ${formatMoney(summary.expectedRecoveriesMax)}` }), _jsx(MetricCard, { title: 'Riesgo financiero', value: _jsx(StatusBadge, { tone: tone[summary.financialRiskLevel], label: riskLabels[summary.financialRiskLevel] }) })] })] });
}
