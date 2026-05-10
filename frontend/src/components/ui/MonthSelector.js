import { jsx as _jsx, jsxs as _jsxs } from "react/jsx-runtime";
export function MonthSelector({ year, month, onYearChange, onMonthChange }) {
    return _jsxs("div", { className: 'form-row', children: [_jsxs("label", { children: ["A\u00F1o", _jsx("input", { className: 'input', type: 'number', value: year, onChange: e => onYearChange(Number(e.target.value)) })] }), _jsxs("label", { children: ["Mes", _jsx("input", { className: 'input', type: 'number', min: 1, max: 12, value: month, onChange: e => onMonthChange(Number(e.target.value)) })] })] });
}
