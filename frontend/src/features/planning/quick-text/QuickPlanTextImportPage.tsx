import { useMutation, useQueryClient } from "@tanstack/react-query";
import { useMemo, useState } from "react";
import { useParams } from "react-router-dom";
import {
  commitQuickPlanText,
  previewQuickPlanText,
  type QuickPlanCandidate,
} from "../../../api/quickPlanTextImportApi";
import { getApiErrorMessage } from "../../../api/http";
import { ErrorState } from "../../../components/ui/ErrorState";
import { EmptyState } from "../../../components/ui/EmptyState";
import { queryKeys } from "../../../domain/queryKeys";
import { useMonthlyPeriod } from "../../../hooks/useMonthlyPeriod";
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

export function QuickPlanTextImportPage() {
  const { profileId = "" } = useParams();
  const { year, month } = useMonthlyPeriod();
  const qc = useQueryClient();

  const [text, setText] = useState("");
  const [rows, setRows] = useState<QuickPlanCandidate[]>([]);
  const [error, setError] = useState<string | null>(null);

  const importableRows = useMemo(() => countImportableRows(rows), [rows]);

  const duplicateRows = useMemo(
    () => rows.filter((row) => row.duplicate).length,
    [rows],
  );

  const rowsWithCategoryWarnings = useMemo(
    () => countRowsWithBlockingWarnings(rows),
    [rows],
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
    onSuccess: async () => {
      setText("");
      setRows([]);
      setError(null);

      await Promise.all([
        qc.invalidateQueries({
          queryKey: queryKeys.planning(profileId, year, month),
        }),
        qc.invalidateQueries({
          queryKey: queryKeys.dashboard(profileId, year, month),
        }),
        qc.invalidateQueries({
          queryKey: queryKeys.monthlyPlanReconciliation(profileId, year, month),
        }),
      ]);
    },
    onError: (error) => setError(getApiErrorMessage(error)),
  });

  const canPreview = Boolean(text.trim()) && !preview.isPending;

  const canCommit = rows.length > 0 && importableRows > 0 && !commit.isPending;

  const clear = () => {
    setText("");
    setRows([]);
    setError(null);
    preview.reset();
    commit.reset();
  };

  return (
    <section className="panel quick-plan-text-page">
      <div className="section-title">
        <div>
          <p className="eyebrow">Texto rápido</p>
          <h2>Carga rápida por texto</h2>
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
