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
import { useDryRunExternalLoans, useExternalLoanSyncConfig, useSaveExternalLoanSyncConfig, useSyncExternalLoans } from './hooks/useExternalLoanSync';
import { useExternalLoansSummary } from './hooks/useExternalLoansSummary';
import type { ExternalLoanSyncConfigPayload } from './types';

export function ExternalLoansPage() {
  const { profileId = '' } = useParams();
  const summaryQuery = useExternalLoansSummary(profileId);
  const syncConfigQuery = useExternalLoanSyncConfig(profileId);
  const saveConfigMutation = useSaveExternalLoanSyncConfig(profileId);
  const syncMutation = useSyncExternalLoans(profileId);
  const dryRunMutation = useDryRunExternalLoans(profileId);
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
  const [form, setForm] = useState<ExternalLoanSyncConfigPayload | null>(null);
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
  const canSync =
    !readOnlyMode &&
    Boolean(form?.enabled) &&
    Boolean(form?.accountId) &&
    Boolean(form?.loanDisbursementCategoryId) &&
    Boolean(form?.principalRecoveryCategoryId) &&
    Boolean(form?.interestIncomeCategoryId);

  const handleSaveConfig = () => {
    if (form) saveConfigMutation.mutate(form);
  };

  return (
    <AppLayout>
      <div className='page-stack'>
        <section className='card page-header'>
          <h1>Préstamos externos</h1>
          {summary && <p className='muted'>Estado de integración: {summary.status}</p>}
        </section>

        {summaryQuery.isLoading && <EmptyState message='Cargando resumen de préstamos externos...' />}
        {summaryQuery.isError && <ErrorState message='No se pudo cargar el resumen externo de préstamos.' />}
        {integrationDisabled && <EmptyState message='La integración de préstamos externos está deshabilitada para este perfil.' />}

        <section className='card'>
          <h2>Configuración de sincronización</h2>
          {syncConfigQuery.isLoading && <p className='muted'>Cargando configuración...</p>}
          {syncConfigQuery.isError && <ErrorState message='No se pudo cargar la configuración de sincronización.' />}
          {form && (
            <div className='stack-sm'>
              <label className='field'>
                <span>Cuenta destino</span>
                <select
                  value={form.accountId ?? ''}
                  onChange={(event) => setForm((current) => (current ? { ...current, accountId: event.target.value || null } : current))}
                >
                  <option value=''>Seleccionar cuenta</option>
                  {(accountsQuery.data ?? []).map((account) => (
                    <option key={account.id} value={account.id}>
                      {account.name}
                    </option>
                  ))}
                </select>
              </label>
              <label className='field'>
                <span>Categoría capital prestado</span>
                <select value={form.loanDisbursementCategoryId ?? ''} onChange={(event) => setForm((current) => (current ? { ...current, loanDisbursementCategoryId: event.target.value || null } : current))}>
                  <option value=''>Seleccionar categoría</option>
                  {(categoriesQuery.data ?? []).map((category) => (
                    <option key={category.id} value={category.id}>
                      {category.name}
                    </option>
                  ))}
                </select>
              </label>
              <label className='field'>
                <span>Categoría capital recuperado</span>
                <select value={form.principalRecoveryCategoryId ?? ''} onChange={(event) => setForm((current) => (current ? { ...current, principalRecoveryCategoryId: event.target.value || null } : current))}>
                  <option value=''>Seleccionar categoría</option>
                  {(categoriesQuery.data ?? []).map((category) => (
                    <option key={category.id} value={category.id}>
                      {category.name}
                    </option>
                  ))}
                </select>
              </label>
              <label className='field'>
                <span>Categoría interés cobrado</span>
                <select value={form.interestIncomeCategoryId ?? ''} onChange={(event) => setForm((current) => (current ? { ...current, interestIncomeCategoryId: event.target.value || null } : current))}>
                  <option value=''>Seleccionar categoría</option>
                  {(categoriesQuery.data ?? []).map((category) => (
                    <option key={category.id} value={category.id}>
                      {category.name}
                    </option>
                  ))}
                </select>
              </label>
              <label className='field' style={{ display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
                <input
                  type='checkbox'
                  checked={form.enabled}
                  onChange={(event) => {
                    setForm((current) => (current ? { ...current, enabled: event.target.checked } : current));
                  }}
                />
                <span>Habilitar sincronización</span>
              </label>
              <p className='muted'>El capital recuperado no se registra como ingreso económico; el interés cobrado sí.</p>
              {readOnlyMode && (
                <p>Modo solo lectura: los préstamos externos se consultan, pero no se crean movimientos en HogarIA.</p>
              )}
              <div className='page-actions'>
                <button type='button' onClick={handleSaveConfig} disabled={saveConfigMutation.isPending || !hasSyncConfig}>
                  Guardar configuración
                </button>
                <button type='button' onClick={() => dryRunMutation.mutate()} disabled={dryRunMutation.isPending || !hasSyncConfig}>
                  Simular sincronización
                </button>
                <button type='button' onClick={() => syncMutation.mutate()} disabled={syncMutation.isPending || !canSync}>
                  Sincronizar movimientos
                </button>
              </div>

              {dryRunMutation.data && (
                <div className='stack-sm'>
                  <p><strong>Simulación</strong>: {dryRunMutation.data.movementsCreated} movimientos potenciales.</p>
                  <p>Préstamos detectados: {dryRunMutation.data.detectedLoans.join(', ') || 'Ninguno'}</p>
                  <p>Pagos detectados: {dryRunMutation.data.detectedPayments.join(', ') || 'Ninguno'}</p>
                  <p>Duplicados ya procesados: {dryRunMutation.data.skippedDuplicates}</p>
                  <p>Por tipo: DISBURSEMENT={dryRunMutation.data.summaryByType.DISBURSEMENT ?? 0}, PAYMENT_PRINCIPAL_RECOVERY={dryRunMutation.data.summaryByType.PAYMENT_PRINCIPAL_RECOVERY ?? 0}, PAYMENT_INTEREST_INCOME={dryRunMutation.data.summaryByType.PAYMENT_INTEREST_INCOME ?? 0}</p>
                  <p>Errores que bloquearían sync real: {dryRunMutation.data.errors.length > 0 ? dryRunMutation.data.errors.join(' | ') : 'Sin errores'}</p>
                </div>
              )}
              {syncMutation.data && (
                <div className='stack-sm'>
                  <p>Préstamos sincronizados: {syncMutation.data.loansSynced}</p>
                  <p>Pagos sincronizados: {syncMutation.data.paymentsSynced}</p>
                  <p>Movimientos creados: {syncMutation.data.movementsCreated}</p>
                  <p>Duplicados omitidos: {syncMutation.data.skippedDuplicates}</p>
                  <p>Errores: {syncMutation.data.errors.length > 0 ? syncMutation.data.errors.join(' | ') : 'Sin errores'}</p>
                </div>
              )}
            </div>
          )}
        </section>

        {summary && !integrationDisabled && (
          <>
            <ExternalLoansSummaryCards dashboard={summary.dashboard} />
            <ExternalLoansCashControlCards cashControl={summary.cashControl} />
            <ExternalLoansActiveTable loans={summary.activeLoans} />
          </>
        )}
      </div>
    </AppLayout>
  );
}
