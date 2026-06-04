import { useMemo, useState } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { Link, useParams } from "react-router-dom";
import { queryKeys } from "../../domain/queryKeys";
import { getCategoryDisplayName } from "../../domain/transactionRules";
import { listAccounts } from "../../api/accountsApi";
import { listCategories } from "../../api/categoriesApi";
import {
  commitTransactionImport,
  previewTransactionImport,
} from "../../api/transactionImportsApi";
import { getApiErrorMessage } from "../../api/http";
import { AppLayout } from "../../app/shell/AppShell";
import { EmptyState } from "../../shared/ui/EmptyState";
import { ErrorState } from "../../shared/ui/ErrorState";
import { ImportCommitPanel } from "../transaction-import/ImportCommitPanel";
import { ImportPreviewSummary } from "../transaction-import/ImportPreviewSummary";
import { ImportResultPanel } from "../transaction-import/ImportResultPanel";
import { ImportRowsTable } from "../transaction-import/ImportRowsTable";
import { ImportSourceForm } from "../transaction-import/ImportSourceForm";
import {
  countBlockingMissingCategoryRows,
  countImportableRows,
  countImportRows,
  countTechnicalNeutralRows,
} from "../transaction-import/importCalculations";
import type {
  TransactionImportCommitPayload,
  TransactionImportCommitResult,
  TransactionImportPreview,
  TransactionImportRow,
  TransactionImportSource,
} from "../transaction-import/types";

type RowStatus = TransactionImportRow["status"];
type RowStatusFilter = RowStatus | "ALL";

interface RowFilters {
  search: string;
  status: RowStatusFilter;
  categoryId: string;
}

const ALL = "ALL";

const sourceLabels: Record<string, string> = {
  AUTO: "Detectar automáticamente",
  BANCO_PROVINCIA: "Banco Provincia",
  MERCADO_PAGO: "Mercado Pago",
  TARJETA_CREDITO_GENERICA: "Tarjeta de crédito",
  DEUDAS_TARJETA_GENERICA: "Resumen de cuotas/deudas",
};

const rowStatusLabels: Record<string, string> = {
  READY: "Listas",
  NEEDS_CATEGORY: "Necesitan categoría",
  DUPLICATE: "Duplicadas",
  DUPLICATE_EXACT: "Duplicados exactos",
  POSSIBLE_INTERNAL_TRANSFER: "Posibles transferencias internas",
  INTERNAL_TRANSFER_MATCHED: "Transferencias internas",
  POSSIBLE_CROSS_SOURCE_DUPLICATE: "Posibles duplicados entre cuentas",
  REVIEW: "Requieren revisión",
  ERROR: "Con error",
  SKIPPED: "Omitidas",
};

function getSourceLabel(source: TransactionImportSource) {
  return sourceLabels[source] ?? source;
}

function getRowStatusLabel(status: RowStatusFilter) {
  if (status === ALL) return "Todos los estados";

  return rowStatusLabels[status] ?? status;
}

function normalize(value: string | null | undefined) {
  return (value ?? "").trim().toLowerCase();
}

function buildCommitPayload(
  rows: TransactionImportRow[],
  accountId: string,
  createMissingFallbackCategory: boolean,
  skipDuplicates: boolean,
): TransactionImportCommitPayload {
  return {
    rows: rows.map((row) => ({
      rowNumber: row.rowNumber,
      categoryId: row.suggestedCategoryId,
      accountId,
      movementType: row.movementType,
      amount: row.amount,
      status: row.status,
      description: row.rawDescription ?? row.normalizedDescription,
    })),
    createMissingFallbackCategory,
    skipDuplicates,
  };
}

function isCategoryCompatibleWithImportedMovement(
  row: TransactionImportRow,
  categoriesById: Map<string, { type: string }>,
): boolean {
  if (!row.suggestedCategoryId) return true;

  const category = categoriesById.get(row.suggestedCategoryId);
  if (!category) return true;

  if (row.movementType === "INCOME") {
    return category.type === "INCOME";
  }

  if (row.movementType === "SAVING") {
    return category.type === "SAVING" || category.type === "INVESTMENT";
  }

  if (row.movementType === "EXPENSE") {
    return (
      category.type === "FIXED_EXPENSE" ||
      category.type === "VARIABLE_EXPENSE" ||
      category.type === "DEBT"
    );
  }

  return true;
}

export function TransactionImportPage() {
  const { profileId = "" } = useParams();
  const qc = useQueryClient();

  const [source, setSource] =
    useState<TransactionImportSource>("AUTO");
  const [accountId, setAccountId] = useState("");
  const [file, setFile] = useState<File | null>(null);
  const [preview, setPreview] = useState<TransactionImportPreview | null>(null);
  const [rows, setRows] = useState<TransactionImportRow[]>([]);
  const [createMissingFallbackCategory, setCreateMissingFallbackCategory] =
    useState(false);
  const [skipDuplicatesConfirmed, setSkipDuplicatesConfirmed] = useState(false);
  const [wizardStep, setWizardStep] = useState(1);

  const [filters, setFilters] = useState<RowFilters>({
    search: "",
    status: ALL,
    categoryId: ALL,
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

  const selectedAccount = accountId ? accountsById.get(accountId) : undefined;

  const previewMutation = useMutation({
    mutationFn: () =>
      previewTransactionImport(profileId, source, accountId, file!),
    onSuccess: (data) => {
      setPreview(data);
      setRows(data.rows ?? []);
      setWizardStep(2);
      setSkipDuplicatesConfirmed(false);
      setFilters({
        search: "",
        status: ALL,
        categoryId: ALL,
      });
    },
  });

  const commitMutation = useMutation({
    mutationFn: () => {
      if (!preview?.batchId) {
        throw new Error("No hay una previsualización activa para confirmar.");
      }

      const payload = buildCommitPayload(
        rows,
        accountId,
        createMissingFallbackCategory,
        skipDuplicatesConfirmed,
      );

      return commitTransactionImport(profileId, preview.batchId, payload);
    },
    onSuccess: async () => {
      await Promise.all([
        qc.invalidateQueries({ queryKey: queryKeys.transactions(profileId) }),
        qc.invalidateQueries({
          queryKey: queryKeys.categories(profileId, true),
        }),
        qc.invalidateQueries({ queryKey: queryKeys.dashboard(profileId) }),
        qc.invalidateQueries({
          queryKey: queryKeys.budgetComparison(profileId),
        }),
      ]);
    },
  });

  const rowCounters = useMemo(() => countImportRows(rows), [rows]);

  const unresolvedRows = useMemo(
    () => countBlockingMissingCategoryRows(rows, createMissingFallbackCategory),
    [rows, createMissingFallbackCategory],
  );

  const importableRows = useMemo(
    () => countImportableRows(rows, createMissingFallbackCategory),
    [rows, createMissingFallbackCategory],
  );

  const invalidRows = rowCounters.ERROR ?? 0;
  const ignoredRows = rowCounters.SKIPPED ?? 0;
  const duplicateRows =
    (rowCounters.DUPLICATE ?? 0) +
      (rowCounters.DUPLICATE_EXACT ?? 0) +
      (rowCounters.POSSIBLE_INTERNAL_TRANSFER ?? 0) +
      (rowCounters.INTERNAL_TRANSFER_MATCHED ?? 0) +
      (rowCounters.POSSIBLE_CROSS_SOURCE_DUPLICATE ?? 0) ||
    preview?.duplicateRows ||
    0;
  const exactDuplicateRows = rowCounters.DUPLICATE_EXACT ?? 0;
  const needsCategoryRows = rowCounters.NEEDS_CATEGORY ?? 0;
  const reviewRows = rowCounters.REVIEW ?? preview?.reviewRows ?? 0;
  const technicalNeutralRows = useMemo(() => countTechnicalNeutralRows(rows), [rows]);

  const duplicateSuggestionGroups = useMemo(() => {
    const duplicates = rows.filter(
      (row) =>
        (row.status === "DUPLICATE" || row.status === "DUPLICATE_EXACT") &&
        (row.suggestedCategoryName || row.suggestedCategoryId),
    );

    const groups = new Map<string, typeof duplicates>();

    duplicates.forEach((row) => {
      const key =
        row.suggestedCategoryName ??
        row.suggestedCategoryId ??
        "Sin sugerencia";
      groups.set(key, [...(groups.get(key) ?? []), row]);
    });

    return Array.from(groups.entries()).map(([key, list]) => ({ key, list }));
  }, [rows]);

  const visibleRows = useMemo(() => {
    const search = normalize(filters.search);

    return rows.filter((row) => {
      const suggestedCategory = categoriesById.get(row.suggestedCategoryId ?? "");
      const suggestedCategoryName =
        (suggestedCategory ? getCategoryDisplayName(suggestedCategory) : null) ??
        row.suggestedCategoryName ??
        "";

      const matchesSearch =
        !search ||
        normalize(row.rawDescription).includes(search) ||
        normalize(row.normalizedDescription).includes(search) ||
        normalize(row.extendedDescription).includes(search) ||
        normalize(row.merchantName).includes(search) ||
        normalize(row.counterparty).includes(search) ||
        normalize(row.classificationReason).includes(search) ||
        normalize(row.classificationMatchedValue).includes(search) ||
        normalize(suggestedCategoryName).includes(search) ||
        normalize(suggestedCategory?.displayPath).includes(search) ||
        normalize(suggestedCategory?.categoryKey).includes(search) ||
        normalize(row.realDate).includes(search) ||
        String(row.amount ?? "").includes(search);

      const matchesStatus =
        filters.status === ALL || row.status === filters.status;

      const matchesCategory =
        filters.categoryId === ALL ||
        row.suggestedCategoryId === filters.categoryId;

      return matchesSearch && matchesStatus && matchesCategory;
    });
  }, [categoriesById, filters, rows]);

  const incompatibleRows = useMemo(
    () =>
      rows.filter(
        (row) =>
          (row.status === "READY" ||
            row.status === "NEEDS_CATEGORY" ||
            row.status === "REVIEW") &&
          !isCategoryCompatibleWithImportedMovement(row, categoriesById),
      ).length,
    [categoriesById, rows],
  );

  const activeFilterCount = [
    filters.search,
    filters.status !== ALL,
    filters.categoryId !== ALL,
  ].filter(Boolean).length;

  const canPreview = Boolean(file && accountId && !previewMutation.isPending);

  const canCommit = Boolean(
    preview?.batchId &&
    importableRows > 0 &&
    unresolvedRows === 0 &&
    invalidRows === 0 &&
    incompatibleRows === 0 &&
    (exactDuplicateRows === 0 || skipDuplicatesConfirmed) &&
    !commitMutation.isPending,
  );

  const currentStep = commitMutation.data ? 7 : preview ? wizardStep : 1;

  const resetPreviewState = () => {
    setPreview(null);
    setRows([]);
    setWizardStep(1);
    setSkipDuplicatesConfirmed(false);
    setFilters({
      search: "",
      status: ALL,
      categoryId: ALL,
    });
    previewMutation.reset();
    commitMutation.reset();
  };

  const handleSourceChange = (nextSource: TransactionImportSource) => {
    setSource(nextSource);
    resetPreviewState();
  };

  const handleAccountChange = (nextAccountId: string) => {
    setAccountId(nextAccountId);
    resetPreviewState();
  };

  const handleFileChange = (nextFile: File | null) => {
    setFile(nextFile);
    resetPreviewState();
  };

  const handleVisibleRowsChange = (nextVisibleRows: TransactionImportRow[]) => {
    const nextByRowNumber = new Map(
      nextVisibleRows.map((row) => [row.rowNumber, row]),
    );

    setRows((currentRows) =>
      currentRows.map((row) => nextByRowNumber.get(row.rowNumber) ?? row),
    );
  };

  const resetFilters = () => {
    setFilters({
      search: "",
      status: ALL,
      categoryId: ALL,
    });
  };

  const duplicateDecisionMissing =
    exactDuplicateRows > 0 && !skipDuplicatesConfirmed;
  const hasBlockingIssues =
    unresolvedRows > 0 ||
    invalidRows > 0 ||
    incompatibleRows > 0 ||
    duplicateDecisionMissing;
  const hasPreviewRows = rows.length > 0;

  const wizardSteps = buildWizardSteps({
    currentStep,
    totalRows: preview?.totalRows ?? rows.length,
    invalidRows,
    duplicateRows,
    exactDuplicateRows,
    internalRows:
      (rowCounters.POSSIBLE_INTERNAL_TRANSFER ?? 0) +
      (rowCounters.INTERNAL_TRANSFER_MATCHED ?? 0),
    blockingCategoryRows: unresolvedRows,
    reviewRows,
    technicalNeutralRows,
    incompatibleRows,
    importableRows,
    hasPreview: Boolean(preview),
    hasResult: Boolean(commitMutation.data),
    duplicateDecisionMissing,
  });

  return (
    <AppLayout>
      <div className="page-stack transaction-import-page">
        <section className="page-header transaction-import-hero">
          <div className="transaction-import-hero-copy">
            <p className="eyebrow">Movimientos</p>
            <h1>Importación guiada</h1>
            <p className="muted">
              Subí un archivo, revisá las filas detectadas y confirmá solo lo
              que esté listo para impactar en tus movimientos.
            </p>

            <div
              className="transaction-import-steps"
              aria-label="Progreso de importación"
            >
              {wizardSteps.map((step) => (
                <button
                  key={step.id}
                  type="button"
                  className={`transaction-import-step ${currentStep >= step.id ? "active" : ""}`}
                  disabled={!preview && step.id > 1}
                  onClick={() => setWizardStep(step.id)}
                >
                  {step.id} · {step.title}
                </button>
              ))}
            </div>
          </div>

          <aside className="transaction-import-status-card">
            <span className="label-ui">Estado actual</span>

            <strong>
              {commitMutation.data
                ? "Importación finalizada"
                : preview
                  ? hasBlockingIssues
                    ? "Revisión requerida"
                    : "Lista para confirmar"
                  : "Esperando archivo"}
            </strong>

            <p className="muted">
              {selectedAccount
                ? `Cuenta destino: ${selectedAccount.name}`
                : "Seleccioná una cuenta destino para iniciar."}
            </p>

            <div className="transaction-import-status-badges">
              <span className="badge badge-info">{getSourceLabel(source)}</span>
              {file ? (
                <span className="badge badge-muted">{file.name}</span>
              ) : null}
              {preview?.detectedFormat ? (
                <span className="badge badge-ok">{preview.detectedFormat}</span>
              ) : null}
            </div>
          </aside>
        </section>

        <section className="transaction-import-wizard-panel">
          {wizardSteps.map((step) => (
            <article
              key={step.id}
              className={`transaction-import-wizard-card ${currentStep === step.id ? "active" : ""}`}
            >
              <p className="label-ui">Paso {step.id}</p>
              <h3>{step.title}</h3>
              <strong>{step.counter}</strong>
              <p className="muted">{step.description}</p>
              <p className={step.blocked ? "mensaje-warning" : "compact-muted"}>
                {step.action}
              </p>
            </article>
          ))}
        </section>

        <section className="transaction-import-layout">
          <div className="transaction-import-main">
            <section className="panel transaction-import-source-panel">
              <div className="section-title">
                <div>
                  <p className="eyebrow">Paso 1</p>
                  <h2>Fuente y archivo</h2>
                  <p className="muted">
                    Elegí el origen, la cuenta destino y el archivo a
                    previsualizar.
                  </p>
                </div>

                <Link
                  className="boton-secundario"
                  to={`/profiles/${profileId}/transactions`}
                >
                  Volver a movimientos
                </Link>
              </div>

              <ImportSourceForm
                source={source}
                accountId={accountId}
                file={file}
                accounts={accounts}
                onSourceChange={handleSourceChange}
                onAccountChange={handleAccountChange}
                onFileChange={handleFileChange}
                onPreview={() => previewMutation.mutate()}
                canPreview={canPreview}
                previewLoading={previewMutation.isPending}
              />

              {previewMutation.isError ? (
                <ErrorState
                  message={getApiErrorMessage(previewMutation.error)}
                />
              ) : null}
            </section>

            {preview ? (
              <section
                className="panel transaction-import-review-panel"
                aria-live="polite"
              >
                <div className="section-title">
                  <div>
                    <p className="eyebrow">Paso 2</p>
                    <h2>Revisar y confirmar</h2>
                    <p className="muted">
                      Revisá categorías, errores y duplicados antes de confirmar
                      la importación.
                    </p>
                  </div>

                  <div className="transaction-import-review-actions">
                    <button
                      type="button"
                      className="boton-fantasma"
                      onClick={resetPreviewState}
                      disabled={commitMutation.isPending}
                    >
                      Descartar previsualización
                    </button>
                  </div>
                </div>

                <ImportPreviewSummary
                  totalRows={preview.totalRows}
                  importableRows={importableRows}
                  duplicateRows={duplicateRows}
                  invalidRows={invalidRows}
                  ignoredRows={ignoredRows}
                  reviewRows={reviewRows}
                  needsCategoryRows={needsCategoryRows}
                  technicalNeutralRows={technicalNeutralRows}
                  suggestedCategoryRows={
                    preview.suggestedCategoryRows ??
                    rows.filter((row) => row.suggestedCategoryId).length
                  }
                />

                <section className="transaction-import-rules-card">
                  <div>
                    <p className="eyebrow">Regla de categorías</p>
                    <h3>Categoría temporal para revisar</h3>
                    <p className="muted">
                      Si una fila no tiene categoría, lo más seguro es elegirla.
                      Solo activá esta opción si aceptás crear una categoría
                      temporal llamada “Otros a revisar”.
                    </p>
                  </div>

                  <label className="transaction-import-toggle">
                    <input
                      type="checkbox"
                      checked={createMissingFallbackCategory}
                      onChange={(event) =>
                        setCreateMissingFallbackCategory(event.target.checked)
                      }
                    />
                    <span>
                      Crear categoría temporal “Otros a revisar” para filas sin categoría
                    </span>
                  </label>

                  <label className="transaction-import-toggle">
                    <input
                      type="checkbox"
                      checked={skipDuplicatesConfirmed}
                      onChange={(event) =>
                        setSkipDuplicatesConfirmed(event.target.checked)
                      }
                    />
                    <span>
                      Omitir duplicados exactos detectados en este archivo
                    </span>
                  </label>
                </section>

                {hasBlockingIssues ? (
                  <section className="transaction-import-alert-grid">
                    {invalidRows > 0 ? (
                      <div className="mensaje-error">
                        <strong>{invalidRows} fila(s) con error.</strong>
                        <span>
                          No se van a importar. Revisá el archivo o corregí los
                          datos de origen.
                        </span>
                      </div>
                    ) : null}

                    {unresolvedRows > 0 ? (
                      <div className="mensaje-warning">
                        <strong>{unresolvedRows} fila(s) necesitan categoría.</strong>
                        <span>
                          Asigná una categoría o activá “Otros a revisar” para
                          continuar. Las filas en revisión técnica o neutra no
                          bloquean por categoría.
                        </span>
                      </div>
                    ) : null}

                    {duplicateDecisionMissing ? (
                      <div className="mensaje-warning">
                        <strong>{exactDuplicateRows} duplicado(s) exacto(s).</strong>
                        <span>
                          Marcá que querés omitirlos antes de confirmar.
                        </span>
                      </div>
                    ) : null}
                  </section>
                ) : null}

                {!importableRows ? (
                  <EmptyState
                    title="No hay filas importables"
                    message="No hay filas nuevas para importar. Se detectaron duplicados, transferencias internas u operaciones omitidas por seguridad. Podés revisar recategorización de movimientos existentes."
                  />
                ) : null}

                {duplicateSuggestionGroups.length ? (
                  <section className="panel-soft transaction-import-duplicates">
                    <div className="section-title">
                      <div>
                        <p className="eyebrow">Duplicados</p>
                        <h3>Movimientos duplicados con categorías sugeridas</h3>
                        <p className="muted">
                          No se importan por defecto, pero podés revisar si
                          conviene recategorizar movimientos existentes.
                        </p>
                      </div>
                    </div>

                    <div className="transaction-import-duplicate-list">
                      {duplicateSuggestionGroups.map((group) => {
                        const first = group.list[0];
                        const examples = group.list
                          .slice(0, 3)
                          .map((item) => item.normalizedDescription)
                          .filter(Boolean)
                          .join(" · ");

                        const dates = group.list
                          .map((item) => item.realDate)
                          .filter(Boolean)
                          .sort();

                        const from = dates[0];
                        const to = dates[dates.length - 1];

                        const recategorizeUrl =
                          `/profiles/${profileId}/transactions/recategorize` +
                          `?accountId=${accountId}` +
                          `${from ? `&from=${from}` : ""}` +
                          `${to ? `&to=${to}` : ""}` +
                          `${first.movementType ? `&movementType=${first.movementType}` : ""}` +
                          `&targetMode=AUTO_BY_IMPORT_RULES` +
                          `&onlyImported=true` +
                          `${group.key.includes("Falta de categoría") ? `&onlyWithoutCategory=true` : ""}` +
                          `${
                            first.suggestedCategoryId
                              ? `&toCategoryId=${first.suggestedCategoryId}`
                              : ""
                          }` +
                          `${first.suggestedCategoryName ? `&suggestedCategoryName=${encodeURIComponent(first.suggestedCategoryName)}` : ""}` +
                          `${(() => {
                            const ids = [
                              ...new Set(
                                group.list
                                  .map((r) => r.matchedTransactionId)
                                  .filter(Boolean),
                              ),
                            ];
                            return ids.length
                              ? `&transactionIds=${ids.join(",")}`
                              : "";
                          })()}`;

                        return (
                          <article
                            key={group.key}
                            className="transaction-import-duplicate-card"
                          >
                            <div>
                              <strong>{group.key}</strong>
                              <p className="muted">
                                {group.list.length} fila
                                {group.list.length === 1 ? "" : "s"} ·{" "}
                                {examples || "Sin ejemplos disponibles"}
                              </p>
                            </div>

                            <Link
                              className="boton-secundario"
                              to={recategorizeUrl}
                            >
                              Revisar recategorización
                            </Link>
                          </article>
                        );
                      })}
                    </div>
                  </section>
                ) : null}

                {hasPreviewRows ? (
                  <section className="transaction-import-table-panel">
                    <div className="section-title">
                      <div>
                        <p className="eyebrow">Filas detectadas</p>
                        <h3>Detalle de importación</h3>
                        <p className="muted">
                          {visibleRows.length} visible
                          {visibleRows.length === 1 ? "" : "s"} de {rows.length}{" "}
                          total
                          {rows.length === 1 ? "" : "es"}.
                        </p>
                      </div>

                      {activeFilterCount > 0 ? (
                        <button
                          type="button"
                          className="boton-fantasma"
                          onClick={resetFilters}
                        >
                          Limpiar filtros ({activeFilterCount})
                        </button>
                      ) : null}
                    </div>

                    <div className="transaction-import-toolbar">
                      <label className="transaction-import-search">
                        Buscar
                        <input
                          className="input-ui"
                          value={filters.search}
                          placeholder="Descripción, fecha, monto o categoría"
                          onChange={(event) =>
                            setFilters({
                              ...filters,
                              search: event.target.value,
                            })
                          }
                        />
                      </label>

                      <div className="transaction-import-filter-grid">
                        <label>
                          Estado
                          <select
                            className="input-ui"
                            value={filters.status}
                            onChange={(event) =>
                              setFilters({
                                ...filters,
                                status: event.target.value as RowStatusFilter,
                              })
                            }
                          >
                            <option value={ALL}>Todos los estados</option>
                            {Object.entries(rowStatusLabels).map(
                              ([value, label]) => (
                                <option key={value} value={value}>
                                  {label}
                                </option>
                              ),
                            )}
                          </select>
                        </label>

                        <label>
                          Categoría sugerida
                          <select
                            className="input-ui"
                            value={filters.categoryId}
                            onChange={(event) =>
                              setFilters({
                                ...filters,
                                categoryId: event.target.value,
                              })
                            }
                          >
                            <option value={ALL}>Todas</option>
                            {categories.map((category) => (
                              <option key={category.id} value={category.id}>
                                {getCategoryDisplayName(category)}
                              </option>
                            ))}
                          </select>
                        </label>
                      </div>
                    </div>

                    <div className="transaction-import-filter-chips">
                      <button
                        type="button"
                        className={`transaction-import-chip ${
                          filters.status === ALL ? "active" : ""
                        }`}
                        onClick={() => setFilters({ ...filters, status: ALL })}
                      >
                        Todos · {rowCounters.total}
                      </button>

                      {Object.entries(rowStatusLabels).map(
                        ([status, label]) => (
                          <button
                            key={status}
                            type="button"
                            className={`transaction-import-chip ${
                              filters.status === status ? "active" : ""
                            }`}
                            onClick={() =>
                              setFilters({
                                ...filters,
                                status: status as RowStatus,
                              })
                            }
                          >
                            {label} · {rowCounters[status as RowStatus] ?? 0}
                          </button>
                        ),
                      )}
                    </div>

                    {visibleRows.length === 0 ? (
                      <EmptyState
                        title="Sin resultados"
                        message="No hay filas que coincidan con los filtros actuales."
                      />
                    ) : (
                      <ImportRowsTable
                        rows={visibleRows}
                        categories={categories}
                        onRowsChange={handleVisibleRowsChange}
                        createMissingFallbackCategory={
                          createMissingFallbackCategory
                        }
                      />
                    )}
                  </section>
                ) : null}

                <div className="transaction-import-step-actions">
                  <button
                    type="button"
                    className="boton-secundario"
                    disabled={wizardStep <= 2}
                    onClick={() => setWizardStep((step) => Math.max(2, step - 1))}
                  >
                    Paso anterior
                  </button>
                  <button
                    type="button"
                    className="boton-principal"
                    disabled={
                      wizardStep >= 7 ||
                      (wizardStep === 3 && duplicateDecisionMissing) ||
                      (wizardStep === 5 && (unresolvedRows > 0 || incompatibleRows > 0)) ||
                      (wizardStep === 7 && hasBlockingIssues)
                    }
                    onClick={() => setWizardStep((step) => Math.min(7, step + 1))}
                  >
                    Siguiente paso
                  </button>
                </div>

                <ImportCommitPanel
                  canCommit={canCommit}
                  pending={commitMutation.isPending}
                  hasMissingCategory={unresolvedRows > 0}
                  onCommit={() => commitMutation.mutate()}
                />

                {commitMutation.isError ? (
                  <ErrorState
                    message={getApiErrorMessage(commitMutation.error)}
                  />
                ) : null}
              </section>
            ) : null}

            {commitMutation.data ? (
              <section className="panel transaction-import-result-panel">
                <div className="section-title">
                  <div>
                    <p className="eyebrow">Paso 3</p>
                    <h2>Resultado</h2>
                    <p className="muted">
                      La importación ya fue procesada. Revisá el resultado antes
                      de volver al listado.
                    </p>
                  </div>
                </div>

                <ImportResultPanel
                  result={commitMutation.data as TransactionImportCommitResult}
                  profileId={profileId}
                />
              </section>
            ) : null}
          </div>

          <aside className="transaction-import-side">
            <section className="panel-soft transaction-import-side-card">
              <p className="eyebrow">Control</p>
              <h3>Resumen de revisión</h3>

              <div className="transaction-import-side-list">
                <div>
                  <span>Filas totales</span>
                  <strong>{preview?.totalRows ?? rows.length}</strong>
                </div>

                <div className="tone-ok">
                  <span>Importables</span>
                  <strong>{importableRows}</strong>
                </div>

                <div className={needsCategoryRows > 0 ? "tone-warning" : ""}>
                  <span>Sin categoría</span>
                  <strong>{needsCategoryRows}</strong>
                </div>

                <div className={reviewRows > 0 ? "tone-warning" : ""}>
                  <span>En revisión</span>
                  <strong>{reviewRows}</strong>
                </div>

                <div className={technicalNeutralRows > 0 ? "tone-neutral" : ""}>
                  <span>Técnicas/neutras</span>
                  <strong>{technicalNeutralRows}</strong>
                </div>

                <div className={duplicateRows > 0 ? "tone-neutral" : ""}>
                  <span>Duplicadas</span>
                  <strong>{duplicateRows}</strong>
                </div>

                <div className={invalidRows > 0 ? "tone-danger" : ""}>
                  <span>Con error</span>
                  <strong>{invalidRows}</strong>
                </div>
              </div>

              <p className="muted">
                Estado de filtros:{" "}
                {activeFilterCount > 0
                  ? `${activeFilterCount} activo(s)`
                  : "sin filtros"}
                .
              </p>
            </section>

            <section className="panel-soft transaction-import-side-card">
              <p className="eyebrow">Reglas</p>
              <h3>Qué se confirma</h3>

              <ul className="transaction-import-rule-list">
                <li>Las filas listas se importan.</li>
                <li>Los duplicados se omiten por defecto.</li>
                <li>Las filas con error no se importan.</li>
                <li>Las filas sin categoría dependen de la regla fallback.</li>
              </ul>
            </section>
          </aside>
        </section>
      </div>
    </AppLayout>
  );
}

function buildWizardSteps({
  currentStep,
  totalRows,
  invalidRows,
  duplicateRows,
  exactDuplicateRows,
  internalRows,
  blockingCategoryRows,
  reviewRows,
  technicalNeutralRows,
  incompatibleRows,
  importableRows,
  hasPreview,
  hasResult,
  duplicateDecisionMissing,
}: {
  currentStep: number;
  totalRows: number;
  invalidRows: number;
  duplicateRows: number;
  exactDuplicateRows: number;
  internalRows: number;
  blockingCategoryRows: number;
  reviewRows: number;
  technicalNeutralRows: number;
  incompatibleRows: number;
  importableRows: number;
  hasPreview: boolean;
  hasResult: boolean;
  duplicateDecisionMissing: boolean;
}) {
  return [
    {
      id: 1,
      title: "Subir archivo",
      counter: hasPreview ? `${totalRows} fila(s)` : "Sin archivo",
      description: "Elegí origen, cuenta y archivo. Todavía no se guarda nada.",
      action: hasPreview ? "Archivo leído." : "Subí un archivo para empezar.",
      blocked: false,
    },
    {
      id: 2,
      title: "Vista previa",
      counter: hasPreview ? `${totalRows} fila(s)` : "Pendiente",
      description: "Vemos fechas, montos y textos ya normalizados.",
      action: invalidRows > 0 ? "Hay filas con error." : "Podés revisar el detalle.",
      blocked: invalidRows > 0,
    },
    {
      id: 3,
      title: "Duplicados",
      counter: `${duplicateRows} detectado(s)`,
      description: "Evitamos contar dos veces el mismo gasto o ingreso.",
      action: duplicateDecisionMissing
        ? `Omití explícitamente ${exactDuplicateRows} duplicado(s) exacto(s).`
        : "Sin bloqueo por duplicados.",
      blocked: duplicateDecisionMissing,
    },
    {
      id: 4,
      title: "Transferencias",
      counter: `${internalRows} posible(s)`,
      description: "Detectamos plata movida entre tus cuentas para no inflar el mes.",
      action: internalRows > 0 ? "Se omiten o revisan antes de contar." : "Sin pares sospechosos.",
      blocked: false,
    },
    {
      id: 5,
      title: "Categorías",
      counter: `${blockingCategoryRows} por resolver`,
      description: "Las categorías mantienen entendibles los gráficos.",
      action:
        blockingCategoryRows > 0 || incompatibleRows > 0
          ? "Elegí categorías compatibles o usá fallback antes de confirmar."
          : reviewRows > 0
            ? "Hay filas en revisión que pueden importarse sin confirmar impacto operativo."
            : "Categorías listas.",
      blocked: blockingCategoryRows > 0 || incompatibleRows > 0,
    },
    {
      id: 6,
      title: "Impacto",
      counter: `${importableRows} importable(s)`,
      description: "Revisá qué entra como gasto, ingreso o movimiento neutral.",
      action: technicalNeutralRows > 0
        ? `${technicalNeutralRows} técnica(s)/neutra(s) no impactan ingresos o gastos.`
        : importableRows > 0 ? "Listo para confirmar." : "No hay filas nuevas para importar.",
      blocked: importableRows === 0,
    },
    {
      id: 7,
      title: "Confirmar",
      counter: hasResult ? "Finalizado" : currentStep >= 7 ? "Último paso" : "Pendiente",
      description: "Recién acá se crean movimientos reales.",
      action: hasResult ? "Importación procesada." : "Confirmá solo si no quedan bloqueos.",
      blocked: false,
    },
  ];
}
