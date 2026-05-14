import { useQuery } from '@tanstack/react-query';
import { useEffect, useMemo, useState } from 'react';
import { useParams } from 'react-router-dom';
import { listAccounts } from '../../api/accountsApi';
import { listCategories } from '../../api/categoriesApi';
import { getApiErrorMessage } from '../../api/http';
import { AppLayout } from '../../components/layout/AppLayout';
import { EmptyState } from '../../components/ui/EmptyState';
import { ErrorState } from '../../components/ui/ErrorState';
import { StatusBadge } from '../../components/ui/StatusBadge';
import type { Category } from '../../domain/types';
import { ExternalLoansActiveTable } from './components/ExternalLoansActiveTable';
import { ExternalLoansCashControlCards } from './components/ExternalLoansCashControlCards';
import { ExternalLoansSummaryCards } from './components/ExternalLoansSummaryCards';
import {
  useDryRunExternalLoans,
  useExternalLoanSyncConfig,
  useSaveExternalLoanSyncConfig,
  useSyncExternalLoans,
} from './hooks/useExternalLoanSync';
import { useExternalLoansSummary } from './hooks/useExternalLoansSummary';
import type { ExternalLoanManualSyncResponse, ExternalLoanSyncConfigPayload } from './types';

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

type SyncResultPanelProps = {
  title: string;
  result: ExternalLoanManualSyncResponse;
};

function SyncResultPanel({ title, result }: SyncResultPanelProps) {
  const hasErrors = result.errors.length > 0;

  return (
    <section className="panel-muted">
      <div className="section-title">
        <div>
          <p className="eyebrow">{result.dryRun ? 'Simulación' : 'Sincronización real'}</p>
          <h3>{title}</h3>
        </div>

        <StatusBadge tone={hasErrors ? 'critical' : 'ok'} label={hasErrors ? 'Con errores' : 'Correcto'} />
      </div>

      <div className="metric-grid">
        <article className="surface-inset">
          <p className="label-ui">Movimientos creados</p>
          <strong>{result.movementsCreated}</strong>
        </article>

        <article className="surface-inset">
          <p className="label-ui">Préstamos sincronizados</p>
          <strong>{result.loansSynced}</strong>
        </article>

        <article className="surface-inset">
          <p className="label-ui">Pagos sincronizados</p>
          <strong>{result.paymentsSynced}</strong>
        </article>

        <article className="surface-inset">
          <p className="label-ui">Duplicados omitidos</p>
          <strong>{result.skippedDuplicates}</strong>
        </article>
      </div>

      <div className="grid mt-4">
        <div className="surface-inset">
          <p className="label-ui">Préstamos detectados</p>
          <p className="mb-0">{result.detectedLoans.join(', ') || 'Ninguno'}</p>
        </div>

        <div className="surface-inset">
          <p className="label-ui">Pagos detectados</p>
          <p className="mb-0">{result.detectedPayments.join(', ') || 'Ninguno'}</p>
        </div>

        <div className="surface-inset">
          <p className="label-ui">Resumen por tipo</p>
          <p className="mb-0">
            DISBURSEMENT={result.summaryByType.DISBURSEMENT ?? 0} ·
            PAYMENT_PRINCIPAL_RECOVERY={result.summaryByType.PAYMENT_PRINCIPAL_RECOVERY ?? 0} ·
            PAYMENT_INTEREST_INCOME={result.summaryByType.PAYMENT_INTEREST_INCOME ?? 0}
          </p>
        </div>
      </div>

      {hasErrors ? (
        <p className="mensaje-error mt-4">{result.errors.join(' | ')}</p>
      ) : (
        <p className="mensaje-exito mt-4">Sin errores detectados.</p>
      )}
    </section>
  );
}

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

                <button
                  type="button"
                  className="boton-secundario"
                  onClick={() => dryRunMutation.mutate()}
                  disabled={dryRunMutation.isPending || !hasSyncConfig}
                >
                  {dryRunMutation.isPending ? 'Simulando...' : 'Simular sincronización'}
                </button>

                <button
                  type="button"
                  className="boton-principal"
                  onClick={() => syncMutation.mutate()}
                  disabled={syncMutation.isPending || !canSync}
                >
                  {syncMutation.isPending ? 'Sincronizando...' : 'Sincronizar movimientos'}
                </button>
              </div>

              {saveConfigMutation.isError ? (
                <p className="mensaje-error">{getApiErrorMessage(saveConfigMutation.error)}</p>
              ) : null}

              {saveConfigMutation.isSuccess ? (
                <p className="mensaje-exito">Configuración guardada correctamente.</p>
              ) : null}

              {dryRunMutation.isError ? (
                <p className="mensaje-error">{getApiErrorMessage(dryRunMutation.error)}</p>
              ) : null}

              {syncMutation.isError ? (
                <p className="mensaje-error">{getApiErrorMessage(syncMutation.error)}</p>
              ) : null}

              {dryRunMutation.data ? (
                <SyncResultPanel title="Resultado de simulación" result={dryRunMutation.data} />
              ) : null}

              {syncMutation.data ? (
                <SyncResultPanel title="Resultado de sincronización" result={syncMutation.data} />
              ) : null}
            </div>
          ) : null}
        </section>

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