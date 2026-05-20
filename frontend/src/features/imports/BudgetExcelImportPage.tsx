import { useState } from "react";
import { useParams, Link } from "react-router-dom";
import {
  previewBudgetExcelImport,
  commitBudgetExcelImport,
} from "../../api/budgetImportsApi";
import { AppLayout } from "../../components/layout/AppLayout";
import { EmptyState } from "../../components/ui/EmptyState";
import { MetricCard } from "../../components/ui/MetricCard";
import { StatusBadge } from "../../components/ui/StatusBadge";
import { importTargetEntityLabels, labelOrValue, profileTypeLabels } from "../../domain/financeLabels";
import {
  formatNumber,
  normalizeOptionalText,
  formatMonth,
  formatMoney,
} from "../../domain/formatters";
import {
  BudgetExcelImportCommitRequest,
  BudgetExcelImportPreviewResponse,
} from "../../domain/types";
import { getApiErrorMessage } from "../budgets/budgetErrors";
import { importRowStatusLabels } from "./utils/importUtils";

const defaultOptions: BudgetExcelImportCommitRequest = {
  createCategories: true,
  createAccounts: true,
  createBudgets: true,
  createTransactions: true,
  createGoals: true,
  createHabits: true,
  createInflation: true,
  updateExisting: true,
  ignoreInvalidRows: true,
  year: new Date().getFullYear(),
  currency: "ARS",
  profileType: "PERSONAL",
};

type ImportOptionKey =
  | "createCategories"
  | "createAccounts"
  | "createBudgets"
  | "createTransactions"
  | "createGoals"
  | "createHabits"
  | "createInflation"
  | "updateExisting"
  | "ignoreInvalidRows";

const importOptions: Array<{
  key: ImportOptionKey;
  label: string;
  description: string;
}> = [
  {
    key: "createCategories",
    label: "Crear categorías",
    description: "Genera categorías detectadas en el Excel.",
  },
  {
    key: "createAccounts",
    label: "Crear cuentas",
    description: "Genera cuentas faltantes para movimientos.",
  },
  {
    key: "createBudgets",
    label: "Crear presupuestos",
    description: "Carga estructura presupuestaria mensual.",
  },
  {
    key: "createTransactions",
    label: "Crear movimientos",
    description: "Importa ingresos, gastos y ajustes.",
  },
  {
    key: "createGoals",
    label: "Crear objetivos",
    description: "Carga metas financieras detectadas.",
  },
  {
    key: "createHabits",
    label: "Crear hábitos",
    description: "Carga rutinas financieras sugeridas.",
  },
  {
    key: "createInflation",
    label: "Crear inflación",
    description: "Importa índices o proyecciones.",
  },
  {
    key: "updateExisting",
    label: "Actualizar existentes",
    description: "Actualiza registros si ya existen.",
  },
  {
    key: "ignoreInvalidRows",
    label: "Ignorar filas inválidas",
    description: "Permite continuar aunque haya filas rechazadas.",
  },
];

const rowStatusTone = (
  status: string,
): "ok" | "watch" | "critical" | "neutral" => {
  const normalized = status.toUpperCase();

  if (["VALID", "OK", "READY"].includes(normalized)) return "ok";
  if (["WARNING", "DUPLICATED", "SKIPPED"].includes(normalized)) return "watch";
  if (["INVALID", "ERROR", "FAILED"].includes(normalized)) return "critical";

  return "neutral";
};

export function BudgetExcelImportPage() {
  const { profileId = "" } = useParams();

  const [file, setFile] = useState<File | null>(null);
  const [preview, setPreview] =
    useState<BudgetExcelImportPreviewResponse | null>(null);
  const [options, setOptions] = useState(defaultOptions);
  const [loading, setLoading] = useState(false);
  const [message, setMessage] = useState("");

  const previewRows = preview?.rows ?? [];
  const validRows = previewRows.filter(
    (row) => row.status?.toUpperCase() === "VALID",
  ).length;
  const invalidRows = previewRows.filter(
    (row) => row.status?.toUpperCase() === "INVALID",
  ).length;
  const shownRows = previewRows.slice(0, 200);

  const updateBooleanOption = (key: ImportOptionKey, value: boolean) => {
    setOptions((current) => ({ ...current, [key]: value }));
  };

  const analyze = async () => {
    if (!file) return;

    setLoading(true);
    setMessage("");

    try {
      setPreview(await previewBudgetExcelImport(profileId, file));
    } catch (error) {
      setPreview(null);
      setMessage(getApiErrorMessage(error));
    } finally {
      setLoading(false);
    }
  };

  const commit = async () => {
    if (!preview) return;

    setLoading(true);
    setMessage("");

    try {
      const result = await commitBudgetExcelImport(
        profileId,
        preview.batchId,
        options,
      );
      setMessage(`Importación finalizada: ${result.status}.`);
    } catch (error) {
      setMessage(getApiErrorMessage(error));
    } finally {
      setLoading(false);
    }
  };

  return (
    <AppLayout>
      <div className="page-stack">
        <section className="page-header">
          <div>
            <p className="eyebrow">Carga masiva</p>
            <h1>Carga guiada de Excel</h1>
            <p className="muted">
              Analizá el archivo, revisá filas detectadas y confirmá solo cuando
              el preview sea razonable.
            </p>
          </div>

          <Link
            className="boton-secundario"
            to={`/profiles/${profileId}/dashboard`}
          >
            Ir al panel mensual
          </Link>
        </section>

        <section className="grid">
          {[
            "Seleccionar archivo",
            "Analizar Excel",
            "Revisar hojas detectadas",
            "Elegir opciones",
            "Validar filas",
            "Confirmar importación",
            "Revisar resultado",
          ].map((step, index) => (
            <article key={step} className="guide-step">
              <span className="badge-count">{index + 1}</span>
              <h3 className="mb-0 mt-3 text-base font-semibold">{step}</h3>
            </article>
          ))}
        </section>

        <section className="panel-accent">
          <div className="section-title">
            <div>
              <p className="eyebrow">Archivo</p>
              <h2>Seleccionar y analizar</h2>
              <p className="secondary-text">
                Solo se aceptan archivos `.xlsx`. El análisis no escribe datos
                todavía.
              </p>
            </div>
          </div>

          <div className="form-row">
            <label>
              Archivo Excel
              <input
                className="input-ui"
                type="file"
                accept=".xlsx"
                onChange={(event) => setFile(event.target.files?.[0] ?? null)}
              />
            </label>

            <button
              type="button"
              className="boton-principal"
              onClick={analyze}
              disabled={!file || loading}
            >
              {loading ? "Procesando..." : "Analizar Excel"}
            </button>
          </div>

          {file ? (
            <p className="mensaje-info">
              Archivo seleccionado: <strong>{file.name}</strong>
            </p>
          ) : null}
        </section>

        {preview ? (
          <>
            <section className="metric-grid">
              <MetricCard
                title="Filas detectadas"
                value={formatNumber(previewRows.length)}
                helper="Total de filas interpretadas."
                tone="info"
              />

              <MetricCard
                title="Filas válidas"
                value={formatNumber(validRows)}
                helper="Listas para importar."
                tone="success"
              />

              <MetricCard
                title="Filas inválidas"
                value={formatNumber(invalidRows)}
                helper="Requieren revisión."
                tone={invalidRows > 0 ? "danger" : "success"}
              />

              <MetricCard
                title="Hojas faltantes"
                value={formatNumber(preview.missingSheets.length)}
                helper={preview.missingSheets.join(", ") || "Ninguna"}
                tone={preview.missingSheets.length > 0 ? "warning" : "success"}
              />
            </section>

            <section className="panel">
              <div className="section-title">
                <div>
                  <p className="eyebrow">Detección</p>
                  <h2>Hojas detectadas</h2>
                </div>
              </div>

              <div className="grid">
                <div className="surface-inset">
                  <p className="label-ui">Detectadas</p>
                  <p className="mb-0">
                    {preview.detectedSheets.join(", ") || "-"}
                  </p>
                </div>

                <div className="surface-inset">
                  <p className="label-ui">Faltantes</p>
                  <p className="mb-0">
                    {preview.missingSheets.join(", ") || "Ninguna"}
                  </p>
                </div>
              </div>
            </section>

            <section className="panel">
              <div className="section-title">
                <div>
                  <p className="eyebrow">Opciones</p>
                  <h2>Configuración de importación</h2>
                  <p className="secondary-text">
                    Activá solo los dominios que querés crear o actualizar.
                  </p>
                </div>
              </div>

              <div className="checklist-grid">
                {importOptions.map((option) => (
                  <label key={option.key} className="checklist-item">
                    <span className="cluster-ui">
                      <input
                        type="checkbox"
                        checked={Boolean(options[option.key])}
                        onChange={(event) =>
                          updateBooleanOption(option.key, event.target.checked)
                        }
                      />
                      <strong>{option.label}</strong>
                    </span>

                    <span className="compact-muted">{option.description}</span>
                  </label>
                ))}
              </div>

              <div className="form-row mt-4">
                <label>
                  Año
                  <input
                    className="input-ui"
                    type="number"
                    min={2000}
                    max={2100}
                    value={options.year}
                    onChange={(event) =>
                      setOptions({
                        ...options,
                        year: Number(event.target.value),
                      })
                    }
                  />
                </label>

                <label>
                  Moneda
                  <input
                    className="input-ui"
                    value={options.currency}
                    onChange={(event) =>
                      setOptions({
                        ...options,
                        currency: event.target.value.toUpperCase(),
                      })
                    }
                  />
                </label>

                <label>
                  Tipo de perfil
                  <select
                    className="input-ui"
                    value={options.profileType}
                    onChange={(event) =>
                      setOptions({
                        ...options,
                        profileType: event.target
                          .value as BudgetExcelImportCommitRequest["profileType"],
                      })
                    }
                  >
                    <option value="PERSONAL">
                      {profileTypeLabels.PERSONAL}
                    </option>
                    <option value="FAMILY">{profileTypeLabels.FAMILY}</option>
                    <option value="BUSINESS">
                      {profileTypeLabels.BUSINESS}
                    </option>
                  </select>
                </label>
              </div>

              <div className="form-actions">
                <button
                  type="button"
                  className="boton-principal"
                  onClick={commit}
                  disabled={loading}
                >
                  {loading ? "Importando..." : "Confirmar importación"}
                </button>
              </div>
            </section>

            <section className="panel">
              <div className="section-title">
                <div>
                  <p className="eyebrow">Preview</p>
                  <h2>Filas detectadas</h2>
                  <p className="secondary-text">
                    Se muestran hasta 200 filas para revisión rápida.
                  </p>
                </div>

                <span className="badge-count">{shownRows.length}</span>
              </div>

              <div className="tabla-ui">
                <table className="table-compact">
                  <thead>
                    <tr>
                      <th>Hoja</th>
                      <th>Fila</th>
                      <th>Concepto</th>
                      <th>Mes</th>
                      <th className="amount-cell">Monto</th>
                      <th>Entidad</th>
                      <th>Estado</th>
                    </tr>
                  </thead>

                  <tbody>
                    {shownRows.map((row) => (
                      <tr key={row.id}>
                        <td>{normalizeOptionalText(row.sheetName)}</td>
                        <td>
                          {row.rowNumber ? formatNumber(row.rowNumber) : "-"}
                        </td>
                        <td>
                          <strong>{normalizeOptionalText(row.concept)}</strong>
                        </td>
                        <td>{formatMonth(row.month ?? undefined)}</td>
                        <td className="amount-cell">
                          {formatMoney(row.amount)}
                        </td>
                        <td>
                          {labelOrValue(
                            importTargetEntityLabels,
                            row.targetEntity ?? "UNKNOWN",
                          )}
                        </td>
                        <td>
                          <StatusBadge
                            tone={rowStatusTone(row.status)}
                            label={labelOrValue(
                              importRowStatusLabels,
                              row.status,
                            )}
                          />
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            </section>
          </>
        ) : (
          <EmptyState
            title="Sin preview"
            message="Seleccioná un archivo y ejecutá el análisis para revisar las filas antes de importar."
          />
        )}

        {message ? (
          <p
            className={
              message.toLowerCase().includes("error")
                ? "mensaje-error"
                : "mensaje-exito"
            }
          >
            {message}
          </p>
        ) : null}
      </div>
    </AppLayout>
  );
}
