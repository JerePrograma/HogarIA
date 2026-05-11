import { jsx as _jsx, jsxs as _jsxs, Fragment as _Fragment } from "react/jsx-runtime";
import { useParams } from 'react-router-dom';
import { AppLayout } from '../../components/layout/AppLayout';
import { EmptyState } from '../../components/ui/EmptyState';
import { ErrorState } from '../../components/ui/ErrorState';
import { ExternalLoansActiveTable } from './components/ExternalLoansActiveTable';
import { ExternalLoansCashControlCards } from './components/ExternalLoansCashControlCards';
import { ExternalLoansSummaryCards } from './components/ExternalLoansSummaryCards';
import { useExternalLoansSummary } from './hooks/useExternalLoansSummary';
export function ExternalLoansPage() {
    const { profileId = '' } = useParams();
    const summaryQuery = useExternalLoansSummary(profileId);
    const summary = summaryQuery.data;
    const integrationDisabled = summary?.status === 'DISABLED';
    return (_jsx(AppLayout, { children: _jsxs("div", { className: 'page-stack', children: [_jsxs("section", { className: 'card page-header', children: [_jsx("h1", { children: "Pr\u00E9stamos externos" }), summary && _jsxs("p", { className: 'muted', children: ["Estado de integraci\u00F3n: ", summary.status] })] }), summaryQuery.isLoading && _jsx(EmptyState, { message: 'Cargando resumen de pr\u00E9stamos externos...' }), summaryQuery.isError && _jsx(ErrorState, { message: 'No se pudo cargar el resumen externo de pr\u00E9stamos.' }), integrationDisabled && _jsx(EmptyState, { message: 'La integraci\u00F3n de pr\u00E9stamos externos est\u00E1 deshabilitada para este perfil.' }), summary && !integrationDisabled && (_jsxs(_Fragment, { children: [_jsx(ExternalLoansSummaryCards, { dashboard: summary.dashboard }), _jsx(ExternalLoansCashControlCards, { cashControl: summary.cashControl }), _jsx(ExternalLoansActiveTable, { loans: summary.activeLoans })] }))] }) }));
}
