import { jsx as _jsx, jsxs as _jsxs } from "react/jsx-runtime";
export function MonthlyPlanningGuide({ summary }) {
    const items = summary?.items ?? [];
    const hasItems = items.length > 0;
    const hasMissingAccountOrCategory = items.some((item) => !item.transactionId && item.status !== 'CANCELLED' && (item.accountId == null || item.categoryId == null));
    const hasConvertibleOrPending = items.some((item) => item.status !== 'CANCELLED' && !item.transactionId && item.amount != null && item.accountId && item.categoryId) || (summary?.pendingExpense ?? 0) > 0 || (summary?.pendingIncome ?? 0) > 0;
    const hasUnpriced = (summary?.unpricedCount ?? 0) > 0;
    let activeStep = 1;
    let doneLabel = null;
    if (!hasItems)
        activeStep = 1;
    else if (hasUnpriced)
        activeStep = 2;
    else if (hasMissingAccountOrCategory)
        activeStep = 3;
    else if (hasConvertibleOrPending)
        activeStep = 4;
    else
        doneLabel = 'Mes ordenado';
    const steps = [
        { title: 'Cargar', text: 'Anotá ingresos, gastos o pendientes con una frase simple.' },
        { title: 'Revisar', text: 'Confirmá fecha, monto, estado y prioridad.' },
        { title: 'Completar', text: 'Agregá cuenta/categoría o usá sugerencias.' },
        { title: 'Confirmar', text: 'Marcá pagado/cobrado o convertí a movimiento real.' }
    ];
    return _jsxs("section", { className: 'card', children: [_jsx("h3", { className: 'section-title', children: "Paso a paso del mes" }), _jsx("div", { className: 'guide-steps', children: steps.map((step, idx) => {
                    const stepNum = idx + 1;
                    const className = `guide-step ${!doneLabel && activeStep === stepNum ? 'guide-step-active' : ''}`;
                    return _jsxs("div", { className: className, children: [_jsxs("strong", { children: [stepNum, ". ", step.title] }), _jsx("p", { className: 'secondary-text', children: step.text })] }, step.title);
                }) }), doneLabel ? _jsx("p", { className: 'success-box', children: doneLabel }) : null] });
}
