import { useQuery } from '@tanstack/react-query';
import { useEffect, useMemo, useState } from 'react';
import { useParams } from 'react-router-dom';
import { listAccounts } from '../../api/accountsApi';
import { listCategories } from '../../api/categoriesApi';
import { getApiErrorMessage } from '../../api/http';
import { AppLayout } from '../../app/shell/AppShell';
import { EmptyState } from '../../shared/ui/EmptyState';
import { ErrorState } from '../../shared/ui/ErrorState';
import { StatusBadge } from '../../shared/ui/StatusBadge';
import { queryKeys } from '../../domain/queryKeys';
import type { Category } from '../../domain/types';
import { ExternalLoansActiveTable } from './components/ExternalLoansActiveTable';
import { ExternalLoansCashControlCards } from './components/ExternalLoansCashControlCards';
import { ExternalLoanOperationsPanel } from './components/ExternalLoanOperationsPanel';
import { ExternalLoansSummaryCards } from './components/ExternalLoansSummaryCards';
import {
  useApplyExternalLoanBackfill,
  useBackfillDryRunExternalLoans,
  useDryRunExternalLoans,
  useExternalLoanHealth,
  useExternalLoanIdempotencyDiagnostics,
  useExternalLoanSyncConfig,
  useSaveExternalLoanSyncConfig,
  useSyncExternalLoans,
} from './hooks/useExternalLoanSync';
import { useExternalLoansSummary } from './hooks/useExternalLoansSummary';
import type { ExternalLoanSyncConfigPayload } from './types';

const EXPENSE_CATEGORY_TYPES: Category['type'][] = [
  'FIXED_EXPENSE',
  'VARIABLE_EXPENSE',
  'DEBT',
  'INVESTMENT',
];

const INCOME_CATEGORY_TYPES: Category['type'][] = ['INCOME'];

const createEmptySyncConfig = (): ExternalLoanSyncConfigPayload => ({
  accountId: null,
  loanDisbursementCategoryId: null,
  principalRecoveryCategoryId: null,
  interestIncomeCategoryId: null,
  enabled: false,
});

const statusLabel = (status?: string) => {
  if (status === 'ENABLED') return 'Habilitada';
  if (status === 'DISABLED') return 'Deshabilitada';
  return status ?? 'Sin estado';
};

const statusTone = (status?: string): 'ok' | 'watch' | 'critical' | 'neutral' => {
  if (status === 'ENABLED') return 'ok';
  if (status === 'DISABLED') return 'critical';
  return 'watch';
};

export function ExternalLoansPage() {
  const { profileId = '' } = useParams();

  const summaryQuery = useExternalLoansSummary(profileId);
  const healthQuery = useExternalLoanHealth(profileId);
  const syncConfigQuery = useExternalLoanSyncConfig(profileId);
  const saveConfigMutation = useSaveExternalLoanSyncConfig(profileId);
  const syncMutation = useSyncExternalLoans(profileId);
  const dryRunMutation = useDryRunExternalLoans(profileId);
  const diagnosticsMutation = useExternalLoanIdempotencyDiagnostics(profileId);
  const backfillDryRunMutation = useBackfillDryRunExternalLoans(profileId);
  const backfillApplyMutation = useApplyExternalLoanBackfill(profileId);

  const accountsQuery = useQuery({
    queryKey: queryKeys.accounts(profileId),
    queryFn: () => listAccounts(profileId),
    enabled: Boolean(profileId),
  });

  const categoriesQuery = useQuery({
    queryKey: queryKeys.categories(profileId, true),
    queryFn: () => listCategories(profileId, true),
    enabled: Boolean(profileId),
  });

  const [form, setForm] = useState<ExternalLoanSyncConfigPayload | null>(null);

  const summary = summaryQuery.data;
  const syncConfig = syncConfigQuery.data;
  const categories = categoriesQuery.data ?? [];
  const accounts = accountsQuery.data ?? [];

  const integrationDisabled = summary?.status === 'DISABLED';
  const readOnlyMode = Boolean(summary?.readOnly);

  const expenseCategories = useMemo(
    () => categories.filter((category) => EXPENSE_CATEGORY_TYPES.includes(category.type)),
    [categories],
  );

  const incomeCategories = useMemo(
    () => categories.filter((category) => INCOME_CATEGORY_TYPES.includes(category.type)),
    [categories],
  );

  useEffect(() => {
    if (!syncConfigQuery.isSuccess) return;

    setForm({
      accountId: syncConfig?.accountId ?? null,
      loanDisbursementCategoryId: syncConfig?.loanDisbursementCategoryId ?? null,
      principalRecoveryCategoryId: syncConfig?.principalRecoveryCategoryId ?? null,
      interestIncomeCategoryId: syncConfig?.interestIncomeCategoryId ?? null,
      enabled: Boolean(syncConfig?.enabled),
    });
  }, [syncConfig, syncConfigQuery.isSuccess]);

  const hasSyncConfig = Boolean(form);

  const missingConfigItems = [
    !form?.accountId ? 'cuenta destino' : null,
    !form?.loanDisbursementCategoryId ? 'categoría de capital prestado' : null,
    !form?.principalRecoveryCategoryId ? 'categoría de capital recuperado' : null,
    !form?.interestIncomeCategoryId ? 'categoría de interés cobrado' : null,
  ].filter(Boolean);

  const canSync =
    !readOnlyMode &&
    Boolean(form?.enabled) &&
    missingConfigItems.length === 0;

  const handleSaveConfig = () => {
    if (form) saveConfigMutation.mutate(form);
  };

  return (
    <AppLayout>
      <div className="page-stack">
        <section className="page-header">
          <div>
            <p className="eyebrow">Integración</p>
            <h1>Préstamos externos</h1>
            <p className="muted">
              Controlá cartera, recuperos, mora y sincronización de movimientos desde el módulo externo.
            </p>
          </div>

          <div className="surface-inset">
            <p className="label-ui">Estado de integración</p>
            <StatusBadge tone={statusTone(summary?.status)} label={statusLabel(summary?.status)} />
            {readOnlyMode ? <p className="mensaje-warning mt-3">Modo solo lectura activo.</p> : null}
          </div>
        </section>

        {summaryQuery.isLoading ? (
          <EmptyState
            title="Cargando préstamos externos"
            message="Estamos obteniendo el resumen de cartera externa."
          />
        ) : null}

        {summaryQuery.isError ? (
          <ErrorState message="No se pudo cargar el resumen externo de préstamos." />
        ) : null}

        {integrationDisabled ? (
          <EmptyState
            title="Integración deshabilitada"
            message="La integración de préstamos externos está deshabilitada para este perfil."
          />
        ) : null}

        <section className="panel">
          <div className="section-title">
            <div>
              <p className="eyebrow">Mapeo contable</p>
              <h2>Configuración de sincronización</h2>
              <p className="secondary-text">
                Definí cuenta y categorías para transformar préstamos/pagos externos en movimientos de HogarIA.
              </p>
            </div>

            {form?.enabled ? (
              <StatusBadge tone="ok" label="Sync habilitada" />
            ) : (
              <StatusBadge tone="watch" label="Sync deshabilitada" />
            )}
          </div>

          {syncConfigQuery.isLoading ? <p className="muted">Cargando configuración...</p> : null}

          {syncConfigQuery.isError ? (
            <ErrorState message="No se pudo cargar la configuración de sincronización." />
          ) : null}

          {!form && syncConfigQuery.isSuccess ? (
            <button
              type="button"
              className="boton-secundario"
              onClick={() => setForm(createEmptySyncConfig())}
            >
              Crear configuración
            </button>
          ) : null}

          {form ? (
            <div className="stack-ui">
              <div className="form-grid">
                <label>
                  Cuenta destino
                  <select
                    className="input-ui"
                    value={form.accountId ?? ''}
                    disabled={readOnlyMode}
                    onChange={(event) =>
                      setForm((current) =>
                        current ? { ...current, accountId: event.target.value || null } : current,
                      )
                    }
                  >
                    <option value="">Seleccionar cuenta</option>
                    {accounts.map((account) => (
                      <option key={account.id} value={account.id}>
                        {account.name}
                      </option>
                    ))}
                  </select>
                </label>

                <label>
                  Categoría capital prestado
                  <select
                    className="input-ui"
                    value={form.loanDisbursementCategoryId ?? ''}
                    disabled={readOnlyMode}
                    onChange={(event) =>
                      setForm((current) =>
                        current
                          ? { ...current, loanDisbursementCategoryId: event.target.value || null }
                          : current,
                      )
                    }
                  >
                    <option value="">Seleccionar categoría</option>
                    {expenseCategories.map((category) => (
                      <option key={category.id} value={category.id}>
                        {category.name}
                      </option>
                    ))}
                  </select>
                </label>

                <label>
                  Categoría capital recuperado
                  <select
                    className="input-ui"
                    value={form.principalRecoveryCategoryId ?? ''}
                    disabled={readOnlyMode}
                    onChange={(event) =>
                      setForm((current) =>
                        current
                          ? {
                              ...current,
                              principalRecoveryCategoryId: event.target.value || null,
                            }
                          : current,
                      )
                    }
                  >
                    <option value="">Seleccionar categoría</option>
                    {expenseCategories.map((category) => (
                      <option key={category.id} value={category.id}>
                        {category.name}
                      </option>
                    ))}
                  </select>
                </label>

                <label>
                  Categoría interés cobrado
                  <select
                    className="input-ui"
                    value={form.interestIncomeCategoryId ?? ''}
                    disabled={readOnlyMode}
                    onChange={(event) =>
                      setForm((current) =>
                        current
                          ? { ...current, interestIncomeCategoryId: event.target.value || null }
                          : current,
                      )
                    }
                  >
                    <option value="">Seleccionar categoría</option>
                    {incomeCategories.map((category) => (
                      <option key={category.id} value={category.id}>
                        {category.name}
                      </option>
                    ))}
                  </select>
                </label>
              </div>

              <label className="surface-inset cluster-ui">
                <input
                  type="checkbox"
                  checked={form.enabled}
                  disabled={readOnlyMode}
                  onChange={(event) =>
                    setForm((current) =>
                      current ? { ...current, enabled: event.target.checked } : current,
                    )
                  }
                />
                <span>Habilitar sincronización</span>
              </label>

              <p className="mensaje-info">
                Mapeo sugerido: capital prestado se exporta como gasto, capital recuperado como recupero
                y el interés cobrado como ingreso.
              </p>

              {missingConfigItems.length > 0 ? (
                <p className="mensaje-warning">
                  Configuración incompleta: falta {missingConfigItems.join(', ')}.
                </p>
              ) : null}

              {readOnlyMode ? (
                <p className="mensaje-warning">
                  Modo solo lectura: los préstamos externos se consultan, pero no se crean movimientos en HogarIA.
                </p>
              ) : null}

              <div className="page-actions">
                <button
                  type="button"
                  className="boton-secundario"
                  onClick={handleSaveConfig}
                  disabled={saveConfigMutation.isPending || !hasSyncConfig || readOnlyMode}
                >
                  {saveConfigMutation.isPending ? 'Guardando...' : 'Guardar configuración'}
                </button>
              </div>

              {saveConfigMutation.isError ? (
                <p className="mensaje-error">{getApiErrorMessage(saveConfigMutation.error)}</p>
              ) : null}

              {saveConfigMutation.isSuccess ? (
                <p className="mensaje-exito">Configuración guardada correctamente.</p>
              ) : null}
            </div>
          ) : null}
        </section>

        <ExternalLoanOperationsPanel
          health={healthQuery.data}
          healthLoading={healthQuery.isLoading}
          healthError={healthQuery.error}
          hasSyncConfig={hasSyncConfig}
          canSync={canSync}
          readOnlyMode={readOnlyMode}
          configEnabled={Boolean(form?.enabled)}
          missingConfigItems={missingConfigItems as string[]}
          dryRunResult={dryRunMutation.data}
          dryRunPending={dryRunMutation.isPending}
          dryRunError={dryRunMutation.error}
          syncResult={syncMutation.data}
          syncPending={syncMutation.isPending}
          syncError={syncMutation.error}
          diagnostics={diagnosticsMutation.data}
          diagnosticsPending={diagnosticsMutation.isPending}
          diagnosticsError={diagnosticsMutation.error}
          backfillDryRun={backfillDryRunMutation.data}
          backfillDryRunPending={backfillDryRunMutation.isPending}
          backfillDryRunError={backfillDryRunMutation.error}
          backfillApply={backfillApplyMutation.data}
          backfillApplyPending={backfillApplyMutation.isPending}
          backfillApplyError={backfillApplyMutation.error}
          onAnalyzeSync={() => dryRunMutation.mutate()}
          onSync={() => syncMutation.mutate()}
          onDiagnose={() => diagnosticsMutation.mutate()}
          onAnalyzeBackfill={() => backfillDryRunMutation.mutate()}
          onApplyBackfill={(includeLowConfidence) =>
            backfillApplyMutation.mutate({ includeLowConfidence })
          }
        />

        {summary && !integrationDisabled ? (
          <>
            <ExternalLoansSummaryCards dashboard={summary.dashboard} />
            <ExternalLoansCashControlCards cashControl={summary.cashControl} />
            <ExternalLoansActiveTable loans={summary.activeLoans} />
          </>
        ) : null}
      </div>
    </AppLayout>
  );
}
