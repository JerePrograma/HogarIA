import { jsx as _jsx, jsxs as _jsxs } from "react/jsx-runtime";
import { EmptyState } from '../../../components/ui/EmptyState';
import { formatMoney } from '../../../domain/formatters';
export function ExternalLoansActiveTable({ loans }) {
    if (!loans.length)
        return _jsx(EmptyState, { message: 'No hay pr\u00E9stamos activos.' });
    return (_jsxs("section", { className: 'card', children: [_jsx("h2", { className: 'section-title', children: "Pr\u00E9stamos activos" }), _jsx("div", { className: 'table-wrapper', children: _jsxs("table", { className: 'table table-compact', children: [_jsx("thead", { children: _jsxs("tr", { children: [_jsx("th", { children: "Prestatario" }), _jsx("th", { children: "Estado" }), _jsx("th", { className: 'amount-cell', children: "Capital" }), _jsx("th", { className: 'amount-cell', children: "Cobrado" }), _jsx("th", { className: 'amount-cell', children: "Pendiente" }), _jsx("th", { className: 'amount-cell', children: "Ganancia proyectada" })] }) }), _jsx("tbody", { children: loans.map((loan) => (_jsxs("tr", { children: [_jsx("td", { children: loan.borrowerName }), _jsx("td", { children: loan.status }), _jsx("td", { className: 'amount-cell', children: formatMoney(loan.principalAmount) }), _jsx("td", { className: 'amount-cell', children: formatMoney(loan.totalCollected) }), _jsx("td", { className: 'amount-cell', children: formatMoney(loan.totalPending) }), _jsx("td", { className: 'amount-cell', children: formatMoney(loan.projectedProfit) })] }, loan.externalLoanId))) })] }) })] }));
}
