import { jsx as _jsx, jsxs as _jsxs, Fragment as _Fragment } from "react/jsx-runtime";
import { useParams } from 'react-router-dom';
import { useQuery } from '@tanstack/react-query';
import { useEffect, useState } from 'react';
import { listAccounts } from '../../api/accountsApi';
import { listCategories } from '../../api/categoriesApi';
import { AppLayout } from '../../components/layout/AppLayout';
import { EmptyState } from '../../components/ui/EmptyState';
import { ErrorState } from '../../components/ui/ErrorState';
import { ExternalLoansActiveTable } from './components/ExternalLoansActiveTable';
import { ExternalLoansCashControlCards } from './components/ExternalLoansCashControlCards';
import { ExternalLoansSummaryCards } from './components/ExternalLoansSummaryCards';
import { useExternalLoanSyncConfig, useSaveExternalLoanSyncConfig, useSyncExternalLoans } from './hooks/useExternalLoanSync';
import { useExternalLoansSummary } from './hooks/useExternalLoansSummary';
export function ExternalLoansPage() {
    const { profileId = '' } = useParams();
    const summaryQuery = useExternalLoansSummary(profileId);
    const syncConfigQuery = useExternalLoanSyncConfig(profileId);
    const saveConfigMutation = useSaveExternalLoanSyncConfig(profileId);
    const syncMutation = useSyncExternalLoans(profileId);
    const accountsQuery = useQuery({
        queryKey: ['accounts', profileId],
        queryFn: () => listAccounts(profileId),
        enabled: Boolean(profileId),
    });
    const categoriesQuery = useQuery({
        queryKey: ['categories', profileId, true],
        queryFn: () => listCategories(profileId, true),
        enabled: Boolean(profileId),
    });
    const summary = summaryQuery.data;
    const integrationDisabled = summary?.status === 'DISABLED';
    const syncConfig = syncConfigQuery.data;
    const readOnlyMode = Boolean(summary?.readOnly);
    const [form, setForm] = useState(null);
    useEffect(() => {
        if (!syncConfig) {
            setForm(null);
            return;
        }
        setForm({
            accountId: syncConfig.accountId ?? null,
            loanDisbursementCategoryId: syncConfig.loanDisbursementCategoryId ?? null,
            principalRecoveryCategoryId: syncConfig.principalRecoveryCategoryId ?? null,
            interestIncomeCategoryId: syncConfig.interestIncomeCategoryId ?? null,
            enabled: Boolean(syncConfig.enabled),
        });
    }, [syncConfig]);
    const hasSyncConfig = Boolean(form);
    const canSync = !readOnlyMode &&
        Boolean(form?.enabled) &&
        Boolean(form?.accountId) &&
        Boolean(form?.loanDisbursementCategoryId) &&
        Boolean(form?.principalRecoveryCategoryId) &&
        Boolean(form?.interestIncomeCategoryId);
    const handleSaveConfig = () => {
        if (form)
            saveConfigMutation.mutate(form);
    };
    return (_jsx(AppLayout, { children: _jsxs("div", { className: 'page-stack', children: [_jsxs("section", { className: 'card page-header', children: [_jsx("h1", { children: "Pr\u00E9stamos externos" }), summary && _jsxs("p", { className: 'muted', children: ["Estado de integraci\u00F3n: ", summary.status] })] }), summaryQuery.isLoading && _jsx(EmptyState, { message: 'Cargando resumen de pr\u00E9stamos externos...' }), summaryQuery.isError && _jsx(ErrorState, { message: 'No se pudo cargar el resumen externo de pr\u00E9stamos.' }), integrationDisabled && _jsx(EmptyState, { message: 'La integraci\u00F3n de pr\u00E9stamos externos est\u00E1 deshabilitada para este perfil.' }), _jsxs("section", { className: 'card', children: [_jsx("h2", { children: "Configuraci\u00F3n de sincronizaci\u00F3n" }), syncConfigQuery.isLoading && _jsx("p", { className: 'muted', children: "Cargando configuraci\u00F3n..." }), syncConfigQuery.isError && _jsx(ErrorState, { message: 'No se pudo cargar la configuraci\u00F3n de sincronizaci\u00F3n.' }), form && (_jsxs("div", { className: 'stack-sm', children: [_jsxs("label", { className: 'field', children: [_jsx("span", { children: "Cuenta destino" }), _jsxs("select", { value: form.accountId ?? '', onChange: (event) => setForm((current) => (current ? { ...current, accountId: event.target.value || null } : current)), children: [_jsx("option", { value: '', children: "Seleccionar cuenta" }), (accountsQuery.data ?? []).map((account) => (_jsx("option", { value: account.id, children: account.name }, account.id)))] })] }), _jsxs("label", { className: 'field', children: [_jsx("span", { children: "Categor\u00EDa capital prestado" }), _jsxs("select", { value: form.loanDisbursementCategoryId ?? '', onChange: (event) => setForm((current) => (current ? { ...current, loanDisbursementCategoryId: event.target.value || null } : current)), children: [_jsx("option", { value: '', children: "Seleccionar categor\u00EDa" }), (categoriesQuery.data ?? []).map((category) => (_jsx("option", { value: category.id, children: category.name }, category.id)))] })] }), _jsxs("label", { className: 'field', children: [_jsx("span", { children: "Categor\u00EDa capital recuperado" }), _jsxs("select", { value: form.principalRecoveryCategoryId ?? '', onChange: (event) => setForm((current) => (current ? { ...current, principalRecoveryCategoryId: event.target.value || null } : current)), children: [_jsx("option", { value: '', children: "Seleccionar categor\u00EDa" }), (categoriesQuery.data ?? []).map((category) => (_jsx("option", { value: category.id, children: category.name }, category.id)))] })] }), _jsxs("label", { className: 'field', children: [_jsx("span", { children: "Categor\u00EDa inter\u00E9s cobrado" }), _jsxs("select", { value: form.interestIncomeCategoryId ?? '', onChange: (event) => setForm((current) => (current ? { ...current, interestIncomeCategoryId: event.target.value || null } : current)), children: [_jsx("option", { value: '', children: "Seleccionar categor\u00EDa" }), (categoriesQuery.data ?? []).map((category) => (_jsx("option", { value: category.id, children: category.name }, category.id)))] })] }), _jsxs("label", { className: 'field', style: { display: 'flex', alignItems: 'center', gap: '0.5rem' }, children: [_jsx("input", { type: 'checkbox', checked: form.enabled, onChange: (event) => {
                                                setForm((current) => (current ? { ...current, enabled: event.target.checked } : current));
                                            } }), _jsx("span", { children: "Habilitar sincronizaci\u00F3n" })] }), _jsx("p", { className: 'muted', children: "El capital recuperado no se registra como ingreso econ\u00F3mico; el inter\u00E9s cobrado s\u00ED." }), readOnlyMode && (_jsx("p", { children: "Modo solo lectura: los pr\u00E9stamos externos se consultan, pero no se crean movimientos en HogarIA." })), _jsxs("div", { className: 'page-actions', children: [_jsx("button", { type: 'button', onClick: handleSaveConfig, disabled: saveConfigMutation.isPending || !hasSyncConfig, children: "Guardar configuraci\u00F3n" }), _jsx("button", { type: 'button', onClick: () => syncMutation.mutate(), disabled: syncMutation.isPending || !canSync, children: "Sincronizar movimientos" })] }), syncMutation.data && (_jsxs("div", { className: 'stack-sm', children: [_jsxs("p", { children: ["Pr\u00E9stamos sincronizados: ", syncMutation.data.loansSynced] }), _jsxs("p", { children: ["Pagos sincronizados: ", syncMutation.data.paymentsSynced] }), _jsxs("p", { children: ["Movimientos creados: ", syncMutation.data.movementsCreated] }), _jsxs("p", { children: ["Duplicados omitidos: ", syncMutation.data.skippedDuplicates] }), _jsxs("p", { children: ["Errores: ", syncMutation.data.errors.length > 0 ? syncMutation.data.errors.join(' | ') : 'Sin errores'] })] }))] }))] }), summary && !integrationDisabled && (_jsxs(_Fragment, { children: [_jsx(ExternalLoansSummaryCards, { dashboard: summary.dashboard }), _jsx(ExternalLoansCashControlCards, { cashControl: summary.cashControl }), _jsx(ExternalLoansActiveTable, { loans: summary.activeLoans })] }))] }) }));
}
