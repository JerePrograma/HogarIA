import { jsx as _jsx, jsxs as _jsxs } from "react/jsx-runtime";
import { Link } from 'react-router-dom';
export function NextBestActionCard({ profileId, summary }) {
    const planning = summary.planningSummary;
    const operational = summary.operationalSummary;
    if (!planning || !operational)
        return null;
    let message = 'El mes está ordenado. Revisá movimientos o cargá novedades.';
    let cta = null;
    let href = null;
    if (planning.unpricedCount > 0) {
        message = `Tenés ${planning.unpricedCount} ítems sin cotizar. Completalos para mejorar la proyección.`;
        cta = 'Ir a planificación';
        href = `/profiles/${profileId}/planning`;
    }
    else if (planning.dueNext7DaysCount > 0) {
        message = `Hay ${planning.dueNext7DaysCount} vencimientos/cobros próximos.`;
    }
    else if (planning.pendingExpense > 0) {
        message = `Hay ${planning.pendingExpense} pagos pendientes.`;
    }
    else if (planning.pendingIncome > 0) {
        message = `Hay ${planning.pendingIncome} cobros pendientes.`;
    }
    else if (operational.alerts.length > 0) {
        message = operational.alerts[0];
    }
    return _jsxs("section", { className: 'card next-action-card', children: [_jsx("h3", { className: 'section-title', children: "Siguiente mejor acci\u00F3n" }), _jsx("p", { children: message }), cta && href ? _jsx(Link, { to: href, className: 'button-primary', children: cta }) : null] });
}
