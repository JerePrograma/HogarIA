import { useMemo, useState } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { Link, useParams, useSearchParams } from 'react-router-dom';
import { listAccounts } from '../../api/accountsApi';
import {
  applyBulkRecategorize,
  previewBulkRecategorize,
  type BulkRecategorizePreviewPayload,
} from '../../api/bulkRecategorizeApi';
import { createCategory, listCategories } from '../../api/categoriesApi';
import { getApiErrorMessage } from '../../api/http';
import { AppLayout } from '../../components/layout/AppLayout';
import { EmptyState } from '../../components/ui/EmptyState';
import { ErrorState } from '../../components/ui/ErrorState';
import { StatusBadge } from '../../components/ui/StatusBadge';
import { formatMoney } from '../../domain/formatters';
import { queryKeys } from '../../domain/queryKeys';

type MovementType = NonNullable<BulkRecategorizePreviewPayload['movementType']>;

type CandidateStatusFilter =
  | 'ALL'
  | 'READY'
  | 'AMBIGUOUS'
  | 'SKIPPED'
  | 'ERROR'
  | string;

interface CandidateFilters {
  search: string;
  status: CandidateStatusFilter;
}

const ALL = 'ALL';

const MOVEMENT_LABELS: Record<string, string> = {
  INCOME: 'Ingreso',
  EXPENSE: 'Gasto',
  SAVING: 'Ahorro',
  TRANSFER: 'Transferencia',
  ADJUSTMENT: 'Ajuste',
};

const PREVIEW_STATUS_LABELS: Record<string, string> = {
  READY: 'Actualizable',
  AMBIGUOUS: 'Ambiguo',
  SKIPPED: 'Omitido',
  NEEDS_CATEGORY: 'Sin sugerencia',
  ERROR: 'Error',
};

function normalize(value: string | number | null | undefined) {
  return String(value ?? '').trim().toLowerCase();
}

function getPreviewStatusLabel(status: string) {
  return PREVIEW_STATUS_LABELS[status] ?? status;
}

function getPreviewStatusTone(status: string): 'ok' | 'watch' | 'critical' | 'neutral' {
  if (status === 'READY') return 'ok';
  if (status === 'AMBIGUOUS') return 'watch';
  if (status === 'ERROR') return 'critical';

  return 'neutral';
}

function getMovementLabel(type: string | null | undefined) {
  if (!type) return '-';

  return MOVEMENT_LABELS[type] ?? type;
}

function getMovementTone(type: string | null | undefined): 'ok' | 'watch' | 'risk' | 'critical' | 'neutral' {
  if (type === 'INCOME') return 'ok';
  if (type === 'SAVING') return 'ok';
  if (type === 'TRANSFER') return 'neutral';
  if (type === 'ADJUSTMENT') return 'watch';
  if (type === 'EXPENSE') return 'critical';

  return 'neutral';
}

function toNullableString(value: string) {
  return value.trim() ? value : null;
}

function toNullableNumber(value: string) {
  return value.trim() ? Number(value) : null;
}

function countByStatus(candidates: Array<{ previewStatus: string }>) {
  return candidates.reduce<Record<string, number>>((acc, candidate) => {
    acc[candidate.previewStatus] = (acc[candidate.previewStatus] ?? 0) + 1;
    return acc;
  }, {});
}

export function TransactionRecategorizationPage() {
  const { profileId = '' } = useParams();
  const [search] = useSearchParams();
  const qc = useQueryClient();

  const [form, setForm] = useState<BulkRecategorizePreviewPayload>({
    accountId: search.get('accountId'),
    from: search.get('from'),
    to: search.get('to'),
    fromCategoryId: null,
    onlyWithoutCategory: search.get('onlyWithoutCategory') === 'true' ? true : null,
    targetMode: (search.get('targetMode') as 'MANUAL' | 'AUTO_BY_IMPORT_RULES') ?? 'MANUAL',
    toCategoryId: search.get('toCategoryId') ?? '',
    movementType:
      (search.get('movementType') as BulkRecategorizePreviewPayload['movementType']) ?? null,
    descriptionContains: search.get('descriptionContains'),
    exactAmount: null,
    minAmount: null,
    maxAmount: null,
    onlyImported: search.get('onlyImported') === 'true' ? true : null,
    transactionIds: (search.get('transactionIds')?.split(',').filter(Boolean) ?? null),
  });

  const [missingCategoryName, setMissingCategoryName] = useState(
    search.get('suggestedCategoryName') ?? '',
  );

  const [candidateFilters, setCandidateFilters] = useState<CandidateFilters>({
    search: '',
    status: ALL,
  });

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

  const accounts = accountsQuery.data ?? [];
  const categories = categoriesQuery.data ?? [];

  const accountsById = useMemo(
    () => new Map(accounts.map((account) => [account.id, account])),
    [accounts],
  );

  const categoriesById = useMemo(
    () => new Map(categories.map((category) => [category.id, category])),
    [categories],
  );

  const selectedAccount = form.accountId ? accountsById.get(form.accountId) : null;
  const selectedTargetCategory = form.toCategoryId
    ? categoriesById.get(form.toCategoryId)
    : null;
  const isAutoTargetMode = form.targetMode === 'AUTO_BY_IMPORT_RULES';

  const previewMutation = useMutation({
    mutationFn: () => previewBulkRecategorize(profileId, form),
    onSuccess: () => {
      setCandidateFilters({
        search: '',
        status: ALL,
      });
    },
  });

  const createCategoryMutation = useMutation({
    mutationFn: () =>
      createCategory(profileId, {
        name: missingCategoryName.trim(),
        type: (previewMutation.data?.candidates.find((c) => c.targetCategoryName === missingCategoryName.trim() && c.targetCategoryType)?.targetCategoryType as any),
        scope: 'PERSONAL',
      }),
    onSuccess: async (created) => {
      setForm((prev) => ({
        ...prev,
        toCategoryId: created.id,
      }));

      await qc.invalidateQueries({ queryKey: queryKeys.categories(profileId, true) });
    },
  });

  const candidates = previewMutation.data?.candidates ?? [];

  const readyIds = useMemo(
    () =>
      candidates
        .filter((candidate) => candidate.previewStatus === 'READY')
        .map((candidate) => candidate.transactionId),
    [candidates],
  );

  const statusCounters = useMemo(() => countByStatus(candidates), [candidates]);

  const visibleCandidates = useMemo(() => {
    const searchValue = normalize(candidateFilters.search);

    return candidates.filter((candidate) => {
      const matchesSearch =
        !searchValue ||
        normalize(candidate.description).includes(searchValue) ||
        normalize(candidate.realDate).includes(searchValue) ||
        normalize(candidate.amount).includes(searchValue) ||
        normalize(candidate.movementType).includes(searchValue) ||
        normalize(candidate.warning).includes(searchValue);

      const matchesStatus =
        candidateFilters.status === ALL ||
        candidate.previewStatus === candidateFilters.status;

      return matchesSearch && matchesStatus;
    });
  }, [candidateFilters, candidates]);

  const applyMutation = useMutation({
    mutationFn: () =>
      applyBulkRecategorize(profileId, {
        targetMode: form.targetMode ?? 'MANUAL',
        toCategoryId: isAutoTargetMode ? null : form.toCategoryId,
        transactionIds: isAutoTargetMode ? [] : readyIds,
        updates: isAutoTargetMode
          ? candidates
              .filter((candidate) => candidate.previewStatus === 'READY' && candidate.targetCategoryId)
              .map((candidate) => ({ transactionId: candidate.transactionId, targetCategoryId: candidate.targetCategoryId! }))
          : [],
      }),
    onSuccess: async () => {
      await Promise.all([
        qc.invalidateQueries({ queryKey: queryKeys.transactions(profileId) }),
        qc.invalidateQueries({ queryKey: queryKeys.categories(profileId, true) }),
        qc.invalidateQueries({ queryKey: queryKeys.dashboard(profileId) }),
        qc.invalidateQueries({ queryKey: queryKeys.budgetComparison(profileId) }),
      ]);
    },
  });

  const hasAnySearchCriteria = Boolean(
    form.accountId ||
      form.from ||
      form.to ||
      form.fromCategoryId ||
      form.onlyWithoutCategory != null ||
      form.movementType ||
      form.descriptionContains ||
      form.exactAmount != null ||
      form.minAmount != null ||
      form.maxAmount != null ||
      form.onlyImported != null || (form.transactionIds?.length ?? 0) > 0,
  );

  const activeCandidateFilterCount = [
    candidateFilters.search,
    candidateFilters.status !== ALL,
  ].filter(Boolean).length;

  const inferredCategoryType = previewMutation.data?.candidates.find((c) => c.targetCategoryName === missingCategoryName.trim() && c.targetCategoryType)?.targetCategoryType;
  const canCreateMissingCategory =
    Boolean(missingCategoryName.trim()) && Boolean(inferredCategoryType) && !createCategoryMutation.isPending;

  const canPreview =
    (isAutoTargetMode || Boolean(form.toCategoryId)) &&
    hasAnySearchCriteria &&
    !previewMutation.isPending;

  const canApply =
    readyIds.length > 0 &&
    !applyMutation.isPending;

  const updateForm = (patch: Partial<BulkRecategorizePreviewPayload>) => {
    setForm((current) => ({
      ...current,
      ...patch,
    }));

    previewMutation.reset();
    applyMutation.reset();
  };

  const resetFilters = () => {
    setCandidateFilters({
      search: '',
      status: ALL,
    });
  };

  const clearCriteria = () => {
    updateForm({
      accountId: null,
      from: null,
      to: null,
      fromCategoryId: null,
      onlyWithoutCategory: null,
      movementType: null,
      descriptionContains: null,
      exactAmount: null,
      minAmount: null,
      maxAmount: null,
      onlyImported: search.get('onlyImported') === 'true' ? true : null,
    transactionIds: (search.get('transactionIds')?.split(',').filter(Boolean) ?? null),
    });
  };

  return (
    <AppLayout>
      <div className="page-stack recategorization-page">
        <section className="page-header recategorization-hero">
          <div className="recategorization-hero-copy">
            <p className="eyebrow">Movimientos</p>
            <h1>Recategorizar movimientos</h1>
            <p className="muted">
              Buscá movimientos existentes, previsualizá el impacto y aplicá una categoría destino
              solo sobre candidatos seguros.
            </p>

            <div className="recategorization-hero-badges">
              <span className="badge badge-info">
                {selectedAccount?.name ?? 'Todas las cuentas'}
              </span>

              <span className={selectedTargetCategory ? 'badge badge-ok' : 'badge badge-warning'}>
                {selectedTargetCategory?.name ?? 'Sin categoría destino'}
              </span>

              {form.descriptionContains ? (
                <span className="badge badge-muted">
                  Texto: {form.descriptionContains}
                </span>
              ) : null}
            </div>
          </div>

          <aside className="recategorization-status-card">
            <span className="label-ui">Estado</span>
            <strong>
              {applyMutation.data
                ? 'Recategorización aplicada'
                : previewMutation.data
                  ? readyIds.length > 0
                    ? 'Lista para aplicar'
                    : 'Sin candidatos aplicables'
                  : 'Esperando previsualización'}
            </strong>

            <p className="muted">
              {hasAnySearchCriteria
                ? 'Hay criterios cargados para buscar candidatos.'
                : 'Cargá al menos un criterio para evitar cambios masivos accidentales.'}
            </p>

            <div className="recategorization-status-actions">
              <Link
                className="boton-secundario"
                to={`/profiles/${profileId}/transactions`}
              >
                Volver a movimientos
              </Link>
            </div>
          </aside>
        </section>

        <section className="recategorization-layout">
          <div className="recategorization-main">
            <section className="panel recategorization-filter-panel">
              <div className="section-title">
                <div>
                  <p className="eyebrow">Paso 1</p>
                  <h2>Definir búsqueda y destino</h2>
                  <p className="muted">
                    Elegí qué movimientos querés encontrar y a qué categoría querés moverlos.
                  </p>
                </div>

                <button
                  type="button"
                  className="boton-fantasma"
                  onClick={clearCriteria}
                  disabled={previewMutation.isPending || applyMutation.isPending}
                >
                  Limpiar criterios
                </button>
              </div>

              <div className="form-grid">
                <label>
                  Cuenta
                  <select
                    className="input-ui"
                    value={form.accountId ?? ''}
                    onChange={(event) =>
                      updateForm({
                        accountId: toNullableString(event.target.value),
                      })
                    }
                  >
                    <option value="">Todas</option>

                    {accounts.map((account) => (
                      <option key={account.id} value={account.id}>
                        {account.name}
                      </option>
                    ))}
                  </select>
                </label>

                <label>
                  Desde
                  <input
                    className="input-ui"
                    type="date"
                    value={form.from ?? ''}
                    onChange={(event) =>
                      updateForm({
                        from: toNullableString(event.target.value),
                      })
                    }
                  />
                </label>

                <label>
                  Hasta
                  <input
                    className="input-ui"
                    type="date"
                    value={form.to ?? ''}
                    onChange={(event) =>
                      updateForm({
                        to: toNullableString(event.target.value),
                      })
                    }
                  />
                </label>

                <label>
                  Categoría actual
                  <select
                    className="input-ui"
                    value={form.onlyWithoutCategory ? '__WITHOUT_CATEGORY__' : (form.fromCategoryId ?? '')}
                    onChange={(event) =>
                      updateForm(
                        event.target.value === '__WITHOUT_CATEGORY__'
                          ? { fromCategoryId: null, onlyWithoutCategory: true }
                          : { fromCategoryId: toNullableString(event.target.value), onlyWithoutCategory: null },
                      )
                    }
                  >
                    <option value="">Todas</option>
                    <option value="__WITHOUT_CATEGORY__">Sin categoría</option>

                    {categories.map((category) => (
                      <option key={category.id} value={category.id}>
                        {category.name}
                      </option>
                    ))}
                  </select>
                </label>

                <label>
                  Modo destino
                  <select
                    className="input-ui"
                    value={form.targetMode ?? 'MANUAL'}
                    onChange={(event) =>
                      updateForm({
                        targetMode: event.target.value as 'MANUAL' | 'AUTO_BY_IMPORT_RULES',
                      })
                    }
                  >
                    <option value="MANUAL">Categoría fija</option>
                    <option value="AUTO_BY_IMPORT_RULES">Automático según importación</option>
                  </select>
                </label>

                <label>
                  Categoría destino
                  <select
                    className="input-ui"
                    value={form.toCategoryId ?? ''}
                    disabled={isAutoTargetMode}
                    onChange={(event) =>
                      updateForm({
                        toCategoryId: event.target.value,
                      })
                    }
                  >
                    <option value="">Seleccionar</option>

                    {categories.map((category) => (
                      <option key={category.id} value={category.id}>
                        {category.name}
                      </option>
                    ))}
                  </select>
                </label>
                {isAutoTargetMode ? <p className="muted">Se intentará resolver la categoría usando las mismas reglas del importador.</p> : null}

                <label>
                  Tipo
                  <select
                    className="input-ui"
                    value={form.movementType ?? ''}
                    onChange={(event) =>
                      updateForm({
                        movementType: (event.target.value || null) as MovementType | null,
                      })
                    }
                  >
                    <option value="">Todos</option>
                    <option value="INCOME">Ingreso</option>
                    <option value="EXPENSE">Gasto</option>
                    <option value="SAVING">Ahorro</option>
                    <option value="TRANSFER">Transferencia</option>
                    <option value="ADJUSTMENT">Ajuste</option>
                  </select>
                </label>

                <label className="form-field-wide">
                  Descripción contiene
                  <input
                    className="input-ui"
                    value={form.descriptionContains ?? ''}
                    placeholder="Ej: supermercado, transferencia, mercado pago"
                    onChange={(event) =>
                      updateForm({
                        descriptionContains: toNullableString(event.target.value),
                      })
                    }
                  />
                </label>

                <label>
                  Monto exacto
                  <input
                    className="input-ui"
                    type="number"
                    step="0.01"
                    value={form.exactAmount ?? ''}
                    onChange={(event) =>
                      updateForm({
                        exactAmount: toNullableNumber(event.target.value),
                      })
                    }
                  />
                </label>

                <label>
                  Monto mínimo
                  <input
                    className="input-ui"
                    type="number"
                    step="0.01"
                    value={form.minAmount ?? ''}
                    onChange={(event) =>
                      updateForm({
                        minAmount: toNullableNumber(event.target.value),
                      })
                    }
                  />
                </label>

                <label>
                  Monto máximo
                  <input
                    className="input-ui"
                    type="number"
                    step="0.01"
                    value={form.maxAmount ?? ''}
                    onChange={(event) =>
                      updateForm({
                        maxAmount: toNullableNumber(event.target.value),
                      })
                    }
                  />
                </label>

                <label>
                  Solo importados
                  <select
                    className="input-ui"
                    value={form.onlyImported == null ? '' : String(form.onlyImported)}
                    onChange={(event) =>
                      updateForm({
                        onlyImported:
                          event.target.value === ''
                            ? null
                            : event.target.value === 'true',
                      })
                    }
                  >
                    <option value="">Todos</option>
                    <option value="true">Sí</option>
                    <option value="false">No</option>
                  </select>
                </label>
              </div>

              {missingCategoryName && !form.toCategoryId ? (
                <div className="recategorization-missing-category mensaje-warning">
                  <div>
                    <strong>La categoría sugerida todavía no existe.</strong>
                    <span>{missingCategoryName}</span>
                  </div>

                  <div className="recategorization-missing-category-actions">
                    <input
                      className="input-ui"
                      value={missingCategoryName}
                      onChange={(event) => setMissingCategoryName(event.target.value)}
                    />

                    <button
                      type="button"
                      className="boton-secundario"
                      onClick={() => createCategoryMutation.mutate()}
                      disabled={!canCreateMissingCategory}
                    >
                      {createCategoryMutation.isPending ? 'Creando...' : 'Crear categoría'}
                    </button>
                  </div>
                </div>
              ) : null}

              {!hasAnySearchCriteria ? (
                <p className="mensaje-warning">
                  Cargá al menos un criterio de búsqueda. Recategorizar sin filtros es una receta
                  prolija para romper datos.
                </p>
              ) : null}

              <div className="form-actions">
                <button
                  type="button"
                  className="boton-principal"
                  onClick={() => previewMutation.mutate()}
                  disabled={!canPreview}
                >
                  {previewMutation.isPending ? 'Previsualizando...' : 'Previsualizar cambios'}
                </button>

                {!isAutoTargetMode && !form.toCategoryId ? (
                  <span className="muted">
                    Seleccioná una categoría destino para poder previsualizar.
                  </span>
                ) : null}
              </div>

              {previewMutation.isError ? (
                <ErrorState message={getApiErrorMessage(previewMutation.error)} />
              ) : null}

              {createCategoryMutation.isError ? (
                <ErrorState message={getApiErrorMessage(createCategoryMutation.error)} />
              ) : null}
            </section>

            {previewMutation.data ? (
              <section className="panel recategorization-preview-panel">
                <div className="section-title">
                  <div>
                    <p className="eyebrow">Paso 2</p>
                    <h2>Previsualización</h2>
                    <p className="muted">
                      Revisá los candidatos antes de aplicar la recategorización.
                    </p>
                  </div>

                  {activeCandidateFilterCount > 0 ? (
                    <button
                      type="button"
                      className="boton-fantasma"
                      onClick={resetFilters}
                    >
                      Limpiar filtros ({activeCandidateFilterCount})
                    </button>
                  ) : null}
                </div>

                <section className="recategorization-summary-grid">
                  <article className="recategorization-summary-card">
                    <span>Encontrados</span>
                    <strong>{previewMutation.data.totalMatched}</strong>
                    <p>Total de movimientos que cumplen la búsqueda.</p>
                  </article>

                  <article className="recategorization-summary-card tone-ok">
                    <span>Actualizables</span>
                    <strong>{previewMutation.data.updatableCount}</strong>
                    <p>Candidatos seguros para aplicar.</p>
                  </article>

                  <article className="recategorization-summary-card tone-warning">
                    <span>Ambiguos</span>
                    <strong>{previewMutation.data.ambiguousCount}</strong>
                    <p>Requieren revisión antes de tocar datos.</p>
                  </article>

                  <article className="recategorization-summary-card tone-neutral">
                    <span>Omitidos</span>
                    <strong>{previewMutation.data.skippedCount}</strong>
                    <p>No serán actualizados.</p>
                  </article>

                  <article className="recategorization-summary-card tone-warning">
                    <span>Sin sugerencia</span>
                    <strong>{statusCounters.NEEDS_CATEGORY ?? 0}</strong>
                    <p>No se resolverá automáticamente.</p>
                  </article>
                </section>

                <div className="recategorization-toolbar">
                  <label className="recategorization-search">
                    Buscar en candidatos
                    <input
                      className="input-ui"
                      value={candidateFilters.search}
                      placeholder="Descripción, fecha, monto, tipo o warning"
                      onChange={(event) =>
                        setCandidateFilters({
                          ...candidateFilters,
                          search: event.target.value,
                        })
                      }
                    />
                  </label>

                  <label>
                    Estado
                    <select
                      className="input-ui"
                      value={candidateFilters.status}
                      onChange={(event) =>
                        setCandidateFilters({
                          ...candidateFilters,
                          status: event.target.value,
                        })
                      }
                    >
                      <option value={ALL}>Todos</option>
                      <option value="READY">Actualizables</option>
                      <option value="AMBIGUOUS">Ambiguos</option>
                      <option value="SKIPPED">Omitidos</option>
                      <option value="ERROR">Errores</option>
                    </select>
                  </label>
                </div>

                <div className="recategorization-filter-chips">
                  <button
                    type="button"
                    className={`recategorization-chip ${
                      candidateFilters.status === ALL ? 'active' : ''
                    }`}
                    onClick={() =>
                      setCandidateFilters({
                        ...candidateFilters,
                        status: ALL,
                      })
                    }
                  >
                    Todos · {candidates.length}
                  </button>

                  {Object.entries(PREVIEW_STATUS_LABELS).map(([status, label]) => (
                    <button
                      key={status}
                      type="button"
                      className={`recategorization-chip ${
                        candidateFilters.status === status ? 'active' : ''
                      }`}
                      onClick={() =>
                        setCandidateFilters({
                          ...candidateFilters,
                          status,
                        })
                      }
                    >
                      {label} · {statusCounters[status] ?? 0}
                    </button>
                  ))}
                </div>

                {visibleCandidates.length === 0 ? (
                  <EmptyState
                    title="Sin candidatos visibles"
                    message="No hay movimientos que coincidan con los filtros actuales."
                  />
                ) : (
                  <>
                    <div className="tabla-ui recategorization-table">
                      <table className="table-compact">
                        <thead>
                          <tr>
                            <th>Fecha</th>
                            <th>Descripción</th>
                            <th className="amount-cell">Monto</th>
                            <th>Tipo</th>
                            <th>Estado</th>
                            <th>Categoría destino</th>
                            <th>Warning</th>
                          </tr>
                        </thead>

                        <tbody>
                          {visibleCandidates.map((candidate) => (
                            <tr key={candidate.transactionId}>
                              <td>
                                <strong>{candidate.realDate}</strong>
                              </td>

                              <td>
                                <strong>{candidate.description || 'Sin descripción'}</strong>
                              </td>

                              <td className="amount-cell">
                                {formatMoney(Number(candidate.amount ?? 0))}
                              </td>

                              <td>
                                <StatusBadge
                                  tone={getMovementTone(candidate.movementType)}
                                  label={getMovementLabel(candidate.movementType)}
                                />
                              </td>

                              <td>
                                <StatusBadge
                                  tone={getPreviewStatusTone(candidate.previewStatus)}
                                  label={getPreviewStatusLabel(candidate.previewStatus)}
                                />
                              </td>

                              <td>
                                {candidate.targetCategoryName ?? 'Sin sugerencia'}
                              </td>

                              <td>
                                <span className="muted">
                                  {candidate.warning || '-'}
                                </span>
                              </td>
                            </tr>
                          ))}
                        </tbody>
                      </table>
                    </div>

                    <div className="recategorization-mobile-list">
                      {visibleCandidates.map((candidate) => (
                        <article
                          key={candidate.transactionId}
                          className="recategorization-mobile-card"
                        >
                          <header>
                            <div>
                              <strong>{candidate.description || 'Sin descripción'}</strong>
                              <p className="muted">{candidate.realDate}</p>
                            </div>

                            <span className="recategorization-mobile-amount">
                              {formatMoney(Number(candidate.amount ?? 0))}
                            </span>
                          </header>

                          <div className="recategorization-mobile-badges">
                            <StatusBadge
                              tone={getMovementTone(candidate.movementType)}
                              label={getMovementLabel(candidate.movementType)}
                            />

                            <StatusBadge
                              tone={getPreviewStatusTone(candidate.previewStatus)}
                              label={getPreviewStatusLabel(candidate.previewStatus)}
                            />
                          </div>

                          {candidate.warning ? (
                            <p className="recategorization-warning-note">
                              {candidate.warning}
                            </p>
                          ) : null}
                        </article>
                      ))}
                    </div>
                  </>
                )}

                <section className="recategorization-apply-panel">
                  <div>
                    <p className="eyebrow">Paso 3</p>
                    <h3>Aplicar recategorización</h3>
                    <p className="muted">
                      Se actualizarán solamente los candidatos marcados como actualizables.
                    </p>
                  </div>

                  <button
                    type="button"
                    className="boton-principal"
                    onClick={() => applyMutation.mutate()}
                    disabled={!canApply}
                  >
                    {applyMutation.isPending
                      ? 'Aplicando...'
                      : `Aplicar a ${readyIds.length} movimiento(s)`}
                  </button>
                </section>

                {applyMutation.isError ? (
                  <ErrorState message={getApiErrorMessage(applyMutation.error)} />
                ) : null}
              </section>
            ) : null}

            {applyMutation.data ? (
              <section className="panel recategorization-result-panel">
                <div className="section-title">
                  <div>
                    <p className="eyebrow">Resultado</p>
                    <h2>Recategorización aplicada</h2>
                  </div>
                </div>

                <div className="mensaje-exito">
                  <strong>Proceso finalizado.</strong>
                  <span>
                    Actualizados: {applyMutation.data.updatedCount} · Omitidos:{' '}
                    {applyMutation.data.skippedCount} · Fallidos:{' '}
                    {applyMutation.data.failedCount}
                  </span>
                </div>

                <div className="form-actions">
                  <Link
                    className="boton-principal"
                    to={`/profiles/${profileId}/transactions`}
                  >
                    Ver movimientos
                  </Link>

                  <button
                    type="button"
                    className="boton-secundario"
                    onClick={() => {
                      previewMutation.reset();
                      applyMutation.reset();
                    }}
                  >
                    Hacer otra recategorización
                  </button>
                </div>
              </section>
            ) : null}
          </div>

          <aside className="recategorization-side">
            <section className="panel-soft recategorization-side-card">
              <p className="eyebrow">Destino</p>
              <h3>Categoría final</h3>

              <div className="recategorization-destination-card">
                <span>Categoría destino</span>
                <strong>{selectedTargetCategory?.name ?? 'No seleccionada'}</strong>
                {isAutoTargetMode ? <strong>Automático según importación</strong> : null}
              </div>

              <p className="muted">
                Esta categoría se aplicará a todos los candidatos actualizables del preview.
              </p>
            </section>

            <section className="panel-soft recategorization-side-card">
              <p className="eyebrow">Reglas</p>
              <h3>Seguridad</h3>

              <ul className="recategorization-rule-list">
                <li>No se aplica nada sin previsualización.</li>
                <li>En modo manual, se requiere categoría destino.</li>
                <li>No se permite buscar sin al menos un criterio.</li>
                <li>Solo se aplican candidatos marcados como actualizables.</li>
              </ul>
            </section>
          </aside>
        </section>
      </div>
    </AppLayout>
  );
}
