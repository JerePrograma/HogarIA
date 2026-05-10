import { jsx as _jsx, jsxs as _jsxs } from "react/jsx-runtime";
import { EmptyState } from '../../../components/ui/EmptyState';
export function OperationalAlerts({ alerts }) { return _jsxs("section", { className: 'card', children: [_jsx("h3", { className: 'section-title', children: "Alertas operativas" }), alerts.length === 0 ? _jsx(EmptyState, { message: 'Sin alertas operativas para este per\u00EDodo.' }) : _jsx("ul", { children: alerts.map((a) => _jsx("li", { children: a }, a)) })] }); }
