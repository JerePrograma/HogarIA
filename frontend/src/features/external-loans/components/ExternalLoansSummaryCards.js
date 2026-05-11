import { jsx as _jsx, jsxs as _jsxs } from "react/jsx-runtime";
import { MetricCard } from '../../../components/ui/MetricCard';
import { formatMoney } from '../../../domain/formatters';
export function ExternalLoansSummaryCards({ dashboard }) {
    return (_jsxs("section", { className: 'metric-grid', children: [_jsx(MetricCard, { title: 'Capital invertido', value: formatMoney(dashboard.investedAmount), primary: true }), _jsx(MetricCard, { title: 'Deuda total', value: formatMoney(dashboard.totalDebt) }), _jsx(MetricCard, { title: 'Ganancia proyectada', value: formatMoney(dashboard.amountToEarn) }), _jsx(MetricCard, { title: 'Pr\u00E9stamos activos', value: dashboard.activeLoans })] }));
}
