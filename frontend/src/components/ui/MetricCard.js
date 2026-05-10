import { jsx as _jsx, jsxs as _jsxs } from "react/jsx-runtime";
export function MetricCard({ title, value, primary = false, helper }) {
    return _jsxs("article", { className: `card metric-card ${primary ? 'metric-card-primary' : ''}`.trim(), children: [_jsx("b", { children: title }), _jsx("div", { className: 'kpi-value', children: value }), helper && _jsx("p", { className: 'compact-muted', children: helper })] });
}
