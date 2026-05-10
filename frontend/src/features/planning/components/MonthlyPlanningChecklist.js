import { jsx as _jsx, jsxs as _jsxs } from "react/jsx-runtime";
export function MonthlyPlanningChecklist({ summary, items, onApply }) {
    const missingClassification = items.filter((item) => !item.transactionId && item.status !== 'CANCELLED' && (item.accountId == null || item.categoryId == null)).length;
    const readyToConvert = items.filter((item) => !item.transactionId && item.status !== 'CANCELLED' && item.amount != null && item.accountId && item.categoryId).length;
    const cards = [
        { title: 'Sin cotizar', value: summary?.unpricedCount ?? 0, action: 'Ver items', onClick: () => onApply('UNPRICED') },
        { title: 'Sin cuenta/categoría', value: missingClassification, action: 'Usá preparar conversión', onClick: () => onApply('MISSING_CLASSIFICATION') },
        { title: 'Listos para convertir', value: readyToConvert, action: 'Filtrar', onClick: () => onApply('READY_TO_CONVERT') },
        { title: 'Próximos 7 días', value: summary?.dueNext7DaysCount ?? 0, action: 'Ordenar por fecha', onClick: () => onApply('DUE_NEXT_7_DAYS') },
        { title: 'Pendiente de cobro', value: summary?.pendingIncome ?? 0, action: 'Revisar', onClick: () => onApply('ALL') },
        { title: 'Pendiente de pago', value: summary?.pendingExpense ?? 0, action: 'Revisar', onClick: () => onApply('ALL') }
    ];
    return _jsxs("section", { className: 'card', children: [_jsx("h3", { className: 'section-title', children: "Checklist operativo del mes" }), _jsx("div", { className: 'checklist-grid', children: cards.map((card) => _jsxs("div", { className: 'checklist-item', children: [_jsx("strong", { children: card.title }), _jsx("div", { className: 'kpi-value', children: card.value }), _jsx("button", { className: 'button-secondary', onClick: card.onClick, children: card.action })] }, card.title)) })] });
}
