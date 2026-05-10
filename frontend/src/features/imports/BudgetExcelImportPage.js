import { jsx as _jsx, jsxs as _jsxs, Fragment as _Fragment } from "react/jsx-runtime";
import { useState } from 'react';
import { Link, useParams } from 'react-router-dom';
import { commitBudgetExcelImport, previewBudgetExcelImport } from '../../api/importsApi';
import { getApiErrorMessage } from '../../api/http';
import { AppLayout } from '../../components/layout/AppLayout';
import { importRowStatusLabels, importTargetEntityLabels, labelOrValue, profileTypeLabels } from '../../domain/financeLabels';
import { formatMoney, formatMonth, formatNumber, normalizeOptionalText } from '../../domain/formatters';
const defaultOptions = { createCategories: true, createAccounts: true, createBudgets: true, createTransactions: true, createGoals: true, createHabits: true, createInflation: true, updateExisting: true, ignoreInvalidRows: true, year: new Date().getFullYear(), currency: 'ARS', profileType: 'PERSONAL' };
export function BudgetExcelImportPage() {
    const { profileId = '' } = useParams();
    const [file, setFile] = useState(null);
    const [preview, setPreview] = useState(null);
    const [options, setOptions] = useState(defaultOptions);
    const [loading, setLoading] = useState(false);
    const [msg, setMsg] = useState('');
    const analyze = async () => { if (!file)
        return; setLoading(true); setMsg(''); try {
        setPreview(await previewBudgetExcelImport(profileId, file));
    }
    catch (e) {
        setMsg(getApiErrorMessage(e));
    }
    finally {
        setLoading(false);
    } };
    const commit = async () => { if (!preview)
        return; setLoading(true); setMsg(''); try {
        const r = await commitBudgetExcelImport(profileId, preview.batchId, options);
        setMsg(`Importación finalizada: ${r.status}.`);
    }
    catch (e) {
        setMsg(getApiErrorMessage(e));
    }
    finally {
        setLoading(false);
    } };
    return _jsx(AppLayout, { children: _jsxs("section", { className: 'card', children: [_jsx("h1", { children: "Carga guiada de Excel" }), _jsxs("ol", { children: [_jsx("li", { children: "Seleccionar archivo" }), _jsx("li", { children: "Analizar Excel" }), _jsx("li", { children: "Revisar hojas detectadas/faltantes" }), _jsx("li", { children: "Elegir opciones de importaci\u00F3n" }), _jsx("li", { children: "Ver filas detectadas" }), _jsx("li", { children: "Confirmar importaci\u00F3n" }), _jsx("li", { children: "Ver resultado y links" })] }), _jsx("input", { className: 'input', type: 'file', accept: '.xlsx', onChange: (e) => setFile(e.target.files?.[0] ?? null) }), _jsx("button", { className: 'button-primary', onClick: analyze, disabled: !file || loading, children: "Analizar Excel" }), preview && _jsxs(_Fragment, { children: [_jsxs("p", { children: ["Hojas detectadas: ", preview.detectedSheets.join(', ') || '-'] }), _jsxs("p", { children: ["Hojas faltantes: ", preview.missingSheets.join(', ') || 'Ninguna'] }), _jsxs("div", { className: 'form-row', children: [_jsxs("label", { children: [_jsx("input", { type: 'checkbox', checked: options.createCategories, onChange: e => setOptions({ ...options, createCategories: e.target.checked }) }), " Crear categor\u00EDas"] }), _jsxs("label", { children: [_jsx("input", { type: 'checkbox', checked: options.createAccounts, onChange: e => setOptions({ ...options, createAccounts: e.target.checked }) }), " Crear cuentas"] }), _jsxs("label", { children: [_jsx("input", { type: 'checkbox', checked: options.createBudgets, onChange: e => setOptions({ ...options, createBudgets: e.target.checked }) }), " Crear presupuestos"] }), _jsxs("label", { children: [_jsx("input", { type: 'checkbox', checked: options.createTransactions, onChange: e => setOptions({ ...options, createTransactions: e.target.checked }) }), " Crear movimientos"] }), _jsxs("label", { children: [_jsx("input", { type: 'checkbox', checked: options.createGoals, onChange: e => setOptions({ ...options, createGoals: e.target.checked }) }), " Crear objetivos"] }), _jsxs("label", { children: [_jsx("input", { type: 'checkbox', checked: options.createHabits, onChange: e => setOptions({ ...options, createHabits: e.target.checked }) }), " Crear h\u00E1bitos"] }), _jsxs("label", { children: [_jsx("input", { type: 'checkbox', checked: options.createInflation, onChange: e => setOptions({ ...options, createInflation: e.target.checked }) }), " Crear inflaci\u00F3n"] })] }), _jsxs("div", { className: 'form-row', children: [_jsxs("label", { children: ["A\u00F1o ", _jsx("input", { className: 'input', type: 'number', value: options.year, onChange: e => setOptions({ ...options, year: Number(e.target.value) }) })] }), _jsxs("label", { children: ["Moneda ", _jsx("input", { className: 'input', value: options.currency, onChange: e => setOptions({ ...options, currency: e.target.value.toUpperCase() }) })] }), _jsxs("label", { children: ["Tipo de perfil", _jsxs("select", { className: 'select', value: options.profileType, onChange: e => setOptions({ ...options, profileType: e.target.value }), children: [_jsx("option", { value: 'PERSONAL', children: profileTypeLabels.PERSONAL }), _jsx("option", { value: 'FAMILY', children: profileTypeLabels.FAMILY }), _jsx("option", { value: 'BUSINESS', children: profileTypeLabels.BUSINESS })] })] })] }), _jsx("button", { className: 'button-secondary', onClick: commit, disabled: loading, children: "Confirmar importaci\u00F3n" }), _jsxs("table", { className: 'table', children: [_jsx("thead", { children: _jsxs("tr", { children: [_jsx("th", { children: "Hoja" }), _jsx("th", { children: "Fila" }), _jsx("th", { children: "Concepto" }), _jsx("th", { children: "Mes" }), _jsx("th", { children: "Monto" }), _jsx("th", { children: "Entidad" }), _jsx("th", { children: "Estado" })] }) }), _jsx("tbody", { children: preview.rows.slice(0, 200).map((r) => _jsxs("tr", { children: [_jsx("td", { children: normalizeOptionalText(r.sheetName) }), _jsx("td", { children: r.rowNumber ? formatNumber(r.rowNumber) : '-' }), _jsx("td", { children: normalizeOptionalText(r.concept) }), _jsx("td", { children: formatMonth(r.month ?? undefined) }), _jsx("td", { children: formatMoney(r.amount) }), _jsx("td", { children: labelOrValue(importTargetEntityLabels, r.targetEntity ?? 'UNKNOWN') }), _jsx("td", { children: labelOrValue(importRowStatusLabels, r.status) })] }, r.id)) })] }), _jsx("p", { children: _jsx(Link, { to: `/profiles/${profileId}/dashboard`, children: "Ir al panel mensual" }) })] }), msg && _jsx("p", { className: msg.toLowerCase().includes('error') ? 'error-box' : 'empty-state', children: msg })] }) });
}
