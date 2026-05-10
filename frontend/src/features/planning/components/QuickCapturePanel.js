import { jsx as _jsx, jsxs as _jsxs } from "react/jsx-runtime";
const examples = [
    '05/06 95000 Juliana cuota 3/5',
    'hostel 550000 recupero 50% Agus',
    'escuela Megu 150000 a 180000',
    'inflables Megu reservar fecha',
    '18/06 sueldo programación 1450000'
];
export function QuickCapturePanel({ input, onChange, onAnalyze, canAnalyze, error, children, onDiscard, showDiscard, onClear, isAnalyzing = false }) {
    return _jsxs("section", { className: 'card quick-capture-card', children: [_jsx("h3", { className: 'section-title', children: "Cargar r\u00E1pido" }), _jsx("p", { className: 'secondary-text', children: "Escrib\u00ED como lo anotar\u00EDas en WhatsApp. Despu\u00E9s pod\u00E9s revisar antes de guardar." }), _jsx("div", { className: 'quick-examples', children: examples.map((example) => _jsx("button", { type: 'button', className: 'button-secondary', onClick: () => onChange(example), children: example }, example)) }), _jsx("textarea", { className: 'input', placeholder: 'Ej: 05/06 95000 Juliana cuota 3/5', value: input, onChange: (e) => onChange(e.target.value) }), _jsxs("div", { className: 'form-row', children: [_jsx("button", { className: 'button-primary', onClick: onAnalyze, disabled: !canAnalyze || isAnalyzing, children: isAnalyzing ? 'Analizando...' : 'Analizar' }), _jsx("button", { className: 'button-secondary', onClick: onClear, children: "Limpiar" }), showDiscard && _jsx("button", { className: 'button-secondary', onClick: onDiscard, children: "Descartar an\u00E1lisis" })] }), error && _jsx("p", { className: 'error-box', children: error }), children] });
}
