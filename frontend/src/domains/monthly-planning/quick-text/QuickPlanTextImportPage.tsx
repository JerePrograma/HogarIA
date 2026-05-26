import { useMutation, useQueryClient } from "@tanstack/react-query";
import { useMemo, useState } from "react";
import { useParams } from "react-router-dom";
import {
  commitQuickPlanText,
  previewQuickPlanText,
  type QuickPlanCandidate,
  type QuickPlanCommitResponse,
} from "../../../api/quickPlanTextImportApi";
import { getApiErrorMessage } from "../../../api/http";
import { ErrorState } from "../../../shared/ui/ErrorState";
import { EmptyState } from "../../../shared/ui/EmptyState";
import { queryKeys } from "../../../domain/queryKeys";
import { monthLabels } from "../../../domain/financeLabels";
import { useMonthlyPeriod } from "../../../shared/hooks/useMonthlyPeriod";
import { QuickPlanPreviewTable } from "./QuickPlanPreviewTable";
import { QuickPlanSummaryPanel } from "./QuickPlanSummaryPanel";
import { QuickPlanTextArea } from "./QuickPlanTextArea";

const exampleText = `Hostal 430
Megu 180
Viaje 300
Inmobiliaria 200
Psicóloga 140
Psiquiatra 70
Viajes a mdp aprox 60
tarjeta si o si 120
Cancelar Mercadopago 300`;

function countImportableRows(rows: QuickPlanCandidate[]) {
  return rows.filter((row) => !row.duplicate).length;
}

function countRowsWithBlockingWarnings(rows: QuickPlanCandidate[]) {
  return rows.filter((row) =>
    row.warnings.some((warning) =>
      warning.toLowerCase().includes("no se encontró categoría"),
    ),
  ).length;
}

type PeriodKey = `${number}-${number}`;

function toPeriodKey(year: number, month: number): PeriodKey {
  return `${year}-${month}`;
}

function parsePeriodKey(key: PeriodKey) {
  const [year, month] = key.split("-").map(Number);
  return { year, month };
}

function formatPeriod(year: number, month: number) {
  return `${monthLabels[month] ?? `Mes ${month}`} ${year}`;
}

function collectAffectedPeriods(rows: QuickPlanCandidate[]) {
  return new Set(
    rows.map((row) =>
      toPeriodKey(row.candidate.periodYear, row.candidate.periodMonth),
    ),
  );
}

function summarizeCreatedByPeriod(response: QuickPlanCommitResponse | null) {
  if (!response) return [];

  const result = new Map<PeriodKey, number>();

  for (const item of response.created) {
    const key = toPeriodKey(item.periodYear, item.periodMonth);
    result.set(key, (result.get(key) ?? 0) + 1);
  }

  return [...result.entries()].map(([key, count]) => ({
    ...parsePeriodKey(key),
    count,
  }));
}

export function QuickPlanTextImportPage() {
  const { profileId = "" } = useParams();
  const { year, month } = useMonthlyPeriod();
  const qc = useQueryClient();

  const [text, setText] = useState("");
  const [rows, setRows] = useState<QuickPlanCandidate[]>([]);
  const [error, setError] = useState<string | null>(null);
  const [commitResult, setCommitResult] = useState<QuickPlanCommitResponse | null>(null);

  const importableRows = useMemo(() => countImportableRows(rows), [rows]);

  const duplicateRows = useMemo(
    () => rows.filter((row) => row.duplicate).length,
    [rows],
  );

  const rowsWithCategoryWarnings = useMemo(
    () => countRowsWithBlockingWarnings(rows),
    [rows],
  );

  const affectedPeriods = useMemo(
    () => collectAffectedPeriods(rows.filter((row) => !row.duplicate)),
    [rows],
  );

  const otherPeriodLabels = useMemo(
    () =>
      [...affectedPeriods]
        .map(parsePeriodKey)
        .filter((period) => period.year !== year || period.month !== month)
        .map((period) => formatPeriod(period.year, period.month)),
    [affectedPeriods, year, month],
  );

  const createdByPeriod = useMemo(
    () => summarizeCreatedByPeriod(commitResult),
    [commitResult],
  );

  const preview = useMutation({
    mutationFn: () =>
      previewQuickPlanText(profileId, {
        rawText: text,
        periodYear: year,
        periodMonth: month,
        defaultAmountScale: "THOUSANDS",
      }),
    onSuccess: (data) => {
      setRows(data.candidates ?? []);
      setError(null);
      setCommitResult(null);
    },
    onError: (error) => {
      setRows([]);
      setError(getApiErrorMessage(error));
    },
  });

  const commit = useMutation({
    mutationFn: () =>
      commitQuickPlanText(profileId, {
        periodYear: year,
        periodMonth: month,
        candidates: rows
          .filter((row) => !row.duplicate)
          .map((row) => row.candidate),
        skipDuplicates: true,
      }),
    onSuccess: async (data) => {
      const periods = collectAffectedPeriods(rows.filter((row) => !row.duplicate));
      for (const item of data.created) {
        periods.add(toPeriodKey(item.periodYear, item.periodMonth));
      }

      setText("");
      setRows([]);
      setError(null);
      setCommitResult(data);

      await Promise.all(
        [...periods].flatMap((key) => {
          const period = parsePeriodKey(key);
          return [
            qc.invalidateQueries({
              queryKey: queryKeys.planning(profileId, period.year, period.month),
            }),
            qc.invalidateQueries({
              queryKey: queryKeys.dashboard(profileId, period.year, period.month),
            }),
            qc.invalidateQueries({
              queryKey: queryKeys.monthlyPlanReconciliation(profileId, period.year, period.month),
            }),
          ];
        }),
      );
    },
    onError: (error) => setError(getApiErrorMessage(error)),
  });

  const canPreview = Boolean(text.trim()) && !preview.isPending;

  const canCommit = rows.length > 0 && importableRows > 0 && !commit.isPending;

  const clear = () => {
    setText("");
    setRows([]);
    setError(null);
    setCommitResult(null);
    preview.reset();
    commit.reset();
  };

  return (
    <section className="panel quick-plan-text-page">
      <div className="section-title">
        <div>
          <p className="eyebrow">Texto rápido</p>
          <h2>Cargar por texto</h2>
          <p className="muted">
            Pegá una lista tipo WhatsApp. Cada línea se interpreta como un
            compromiso mensual independiente.
          </p>
        </div>

        <button
          type="button"
          className="boton-secundario"
          onClick={() => setText(exampleText)}
          disabled={preview.isPending || commit.isPending}
        >
          Usar ejemplo
        </button>
      </div>

      <QuickPlanTextArea value={text} onChange={setText} />

      <div className="form-actions">
        <button
          type="button"
          className="boton-principal"
          onClick={() => preview.mutate()}
          disabled={!canPreview}
        >
          {preview.isPending ? "Previsualizando..." : "Previsualizar"}
        </button>

        <button
          type="button"
          className="boton-secundario"
          onClick={clear}
          disabled={preview.isPending || commit.isPending}
        >
          Limpiar
        </button>
      </div>

      {error ? <ErrorState message={error} /> : null}

      {commitResult ? (
        <section className="surface-inset mt-4">
          <p className="label-ui">Resultado de creación</p>
          {createdByPeriod.length > 0 ? (
            <ul className="mb-0 mt-2">
              {createdByPeriod.map((period) => (
                <li key={`${period.year}-${period.month}`}>
                  {period.count} compromiso{period.count === 1 ? "" : "s"} creado
                  {period.count === 1 ? "" : "s"} en{" "}
                  {formatPeriod(period.year, period.month)}
                </li>
              ))}
            </ul>
          ) : null}

          {commitResult.skippedDuplicates > 0 ? (
            <p className="mensaje-warning">
              {commitResult.skippedDuplicates} candidato
              {commitResult.skippedDuplicates === 1 ? "" : "s"} omitido
              {commitResult.skippedDuplicates === 1 ? "" : "s"} por duplicado.
            </p>
          ) : null}

          {commitResult.warnings.length > 0 ? (
            <ul className="mb-0 mt-2">
              {commitResult.warnings.map((warning, index) => (
                <li key={`${warning}-${index}`}>{warning}</li>
              ))}
            </ul>
          ) : null}
        </section>
      ) : null}

      {rows.length > 0 ? (
        <section className="stack-ui mt-4">
          <QuickPlanSummaryPanel rows={rows} />

          <div className="surface-inset">
            <p>
              <strong>{rows.length}</strong> candidato
              {rows.length === 1 ? "" : "s"} detectado
              {rows.length === 1 ? "" : "s"}.
            </p>

            <p>
              <strong>{importableRows}</strong> importable
              {importableRows === 1 ? "" : "s"}.
            </p>

            <p>
              <strong>{duplicateRows}</strong> duplicado
              {duplicateRows === 1 ? "" : "s"}.
            </p>

            {rowsWithCategoryWarnings > 0 ? (
              <p className="mensaje-warning">
                {rowsWithCategoryWarnings} fila
                {rowsWithCategoryWarnings === 1 ? "" : "s"} no tienen categoría
                sugerida. Podés crearlas igual como planificación, pero después
                vas a tener que clasificarlas para convertirlas en movimientos.
              </p>
            ) : null}
          </div>

          {otherPeriodLabels.length > 0 ? (
            <p className="mensaje-info">
              Algunos compromisos se crearán en {otherPeriodLabels.join(", ")} porque su fecha esperada cae en ese período.
            </p>
          ) : null}

          <QuickPlanPreviewTable rows={rows} setRows={setRows} />

          {importableRows === 0 ? (
            <EmptyState
              title="No hay compromisos nuevos para crear"
              message="Todos los candidatos detectados parecen duplicados. Revisá el período o modificá el texto."
            />
          ) : null}

          <div className="form-actions">
            <button
              type="button"
              className="boton-principal"
              onClick={() => commit.mutate()}
              disabled={!canCommit}
            >
              {commit.isPending
                ? "Creando compromisos..."
                : `Crear ${importableRows} compromiso${importableRows === 1 ? "" : "s"}`}
            </button>
          </div>
        </section>
      ) : null}
    </section>
  );
}
