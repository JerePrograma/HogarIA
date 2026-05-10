import { jsx as _jsx, jsxs as _jsxs } from "react/jsx-runtime";
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useState } from 'react';
import { Line, LineChart, ResponsiveContainer, Tooltip, XAxis, YAxis } from 'recharts';
import { createInflation, getAccumulatedInflation, listInflation } from '../../api/inflationApi';
import { getApiErrorMessage } from '../../api/http';
import { AppLayout } from '../../components/layout/AppLayout';
import { formatPercent, formatMonth } from '../../domain/formatters';
export function InflationPage() {
    const d = new Date();
    const [year, setYear] = useState(d.getFullYear());
    const [month, setMonth] = useState(d.getMonth() + 1);
    const [monthlyRate, setMonthlyRate] = useState(0.03);
    const [projection, setProjection] = useState(false);
    const [message, setMessage] = useState('');
    const qc = useQueryClient();
    const q = useQuery({ queryKey: ['inflation', year], queryFn: () => listInflation(year) });
    const acc = useQuery({ queryKey: ['inflation-acc', year], queryFn: () => getAccumulatedInflation(year, 1, year, 12) });
    const create = useMutation({ mutationFn: () => createInflation({ year, month, monthlyRate, source: 'MANUAL', projection }), onSuccess: () => { setMessage('Índice guardado.'); qc.invalidateQueries({ queryKey: ['inflation', year] }); }, onError: (e) => setMessage(getApiErrorMessage(e)) });
    return _jsx(AppLayout, { children: _jsxs("div", { className: 'card', children: [_jsx("h1", { children: "Inflaci\u00F3n y proyecciones" }), _jsxs("div", { className: 'form-row', children: [_jsx("input", { className: 'input', type: 'number', value: year, onChange: e => setYear(Number(e.target.value)) }), _jsx("input", { className: 'input', type: 'number', value: month, min: 1, max: 12, onChange: e => setMonth(Number(e.target.value)) }), _jsx("input", { className: 'input', type: 'number', step: '0.0001', value: monthlyRate, onChange: e => setMonthlyRate(Number(e.target.value)) }), _jsxs("label", { children: [_jsx("input", { type: 'checkbox', checked: projection, onChange: e => setProjection(e.target.checked) }), " Proyectado"] }), _jsx("button", { className: 'button-primary', disabled: create.isPending, onClick: () => create.mutate(), children: "Guardar \u00EDndice" })] }), _jsxs("p", { children: ["Acumulada anual: ", formatPercent(Number(acc.data?.accumulatedRate ?? 0) * 100)] }), message && _jsx("p", { className: 'empty-state', children: message }), _jsx("div", { style: { height: 260 }, children: _jsx(ResponsiveContainer, { children: _jsxs(LineChart, { data: (q.data ?? []).map((x) => ({ mes: formatMonth(x.month), porcentaje: Number(x.monthlyRate) * 100 })), children: [_jsx(XAxis, { dataKey: 'mes' }), _jsx(YAxis, {}), _jsx(Tooltip, { formatter: (value) => formatPercent(value) }), _jsx(Line, { type: 'monotone', dataKey: 'porcentaje', stroke: '#0f766e' })] }) }) }), _jsxs("table", { className: 'table', children: [_jsx("thead", { children: _jsxs("tr", { children: [_jsx("th", { children: "Mes" }), _jsx("th", { children: "Tasa mensual" }), _jsx("th", { children: "Tipo" }), _jsx("th", { children: "Fuente" })] }) }), _jsx("tbody", { children: (q.data ?? []).map((x) => _jsxs("tr", { children: [_jsx("td", { children: formatMonth(x.month) }), _jsx("td", { children: formatPercent(Number(x.monthlyRate) * 100) }), _jsx("td", { children: x.projection ? 'Proyectado' : 'Real' }), _jsx("td", { children: x.source ?? 'Sin fuente' })] }, x.id)) })] })] }) });
}
