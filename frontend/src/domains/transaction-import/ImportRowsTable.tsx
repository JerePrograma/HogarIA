import { useMemo } from "react";
import { StatusBadge } from "../../shared/ui/StatusBadge";
import type { Category } from "../../domain/types";
import { formatMoney } from "../../domain/formatters";
import type {
  TransactionImportMovementType,
  TransactionImportRow,
} from "./types";
import { ImportRowCategorySelect } from "./ImportRowCategorySelect";
import {
  getClassificationExplanationSummary,
  getImportRowIssueMessage,
  getSuggestedCategoryName,
  importBalanceImpactLabels,
  importClassificationStatusLabels,
  importConfidenceLabels,
  importMovementLabels,
  importPaymentChannelLabels,
  getImportRowStatusTone,
  importRowStatusLabels,
} from "./utils/importUtils";
import { labelOrFallback, movementTypeTones } from "../../domain/financeLabels";
import { ImportRowsMobileList } from "./ImportRowsMobileList";

interface Props {
  rows: TransactionImportRow[];
  categories: Category[];
  onRowsChange: (rows: TransactionImportRow[]) => void;
  createMissingFallbackCategory: boolean;
}

function isLockedImportRowStatus(status: TransactionImportRow["status"]) {
  return ["DUPLICATE", "DUPLICATE_EXACT", "SKIPPED", "ERROR"].includes(status);
}

function getExistingMovementNote(row: TransactionImportRow) {
  if (row.status === "POSSIBLE_INTERNAL_TRANSFER") {
    return "Posible transferencia interna. Se puede importar como movimiento técnico/neutro para no inflar ingresos o gastos.";
  }

  if (row.status === "INTERNAL_TRANSFER_MATCHED") {
    return "Transferencia interna detectada. Se importará como movimiento técnico/neutro, sin impacto operativo.";
  }

  if (row.status === "POSSIBLE_CROSS_SOURCE_DUPLICATE") {
    return "Posible duplicado entre fuentes. Revisalo antes de confirmar; no se trata como duplicado exacto.";
  }

  if (row.status === "DUPLICATE" || row.status === "DUPLICATE_EXACT") {
    return "No se creará al confirmar: ya existe un movimiento compatible. Revisá recategorización si corresponde.";
  }

  return null;
}

function compactMeta(row: TransactionImportRow) {
  return [
    row.paymentChannel
      ? `Canal: ${importPaymentChannelLabels[row.paymentChannel] ?? row.paymentChannel}`
      : null,
    row.balanceImpact
      ? `Impacto: ${importBalanceImpactLabels[row.balanceImpact] ?? row.balanceImpact}`
      : null,
    row.classificationStatus
      ? `Clasificación: ${importClassificationStatusLabels[row.classificationStatus] ?? row.classificationStatus}`
      : null,
    row.classificationLayer ? `Capa: ${row.classificationLayer}` : null,
  ]
    .filter(Boolean)
    .join(" · ");
}

export function ImportRowsTable({
  rows,
  categories,
  onRowsChange,
  createMissingFallbackCategory,
}: Props) {
  const categoriesById = useMemo(
    () => new Map(categories.map((category) => [category.id, category])),
    [categories],
  );

  const updateRow = (
    rowNumber: number,
    patch: Partial<TransactionImportRow>,
  ) => {
    onRowsChange(
      rows.map((row) =>
        row.rowNumber === rowNumber ? { ...row, ...patch } : row,
      ),
    );
  };

  return (
    <div className="import-rows">
      <div className="tabla-ui import-rows-table">
        <table className="table-compact">
          <thead>
            <tr>
              <th>Fila</th>
              <th>Fecha</th>
              <th>Descripción</th>
              <th className="amount-cell">Monto</th>
              <th>Tipo</th>
              <th>Categoría</th>
              <th>Estado</th>
            </tr>
          </thead>

          <tbody>
            {rows.map((row) => {
              const issueMessage = getImportRowIssueMessage(
                row,
                createMissingFallbackCategory,
              );

              const existingMovementNote = getExistingMovementNote(row);
              const categoryName = getSuggestedCategoryName(
                row,
                categoriesById,
              );

              function isExistingMovementStatus(status: TransactionImportRow["status"]) {
                return [
                  "DUPLICATE",
                  "DUPLICATE_EXACT",
                  "POSSIBLE_INTERNAL_TRANSFER",
                  "INTERNAL_TRANSFER_MATCHED",
                  "POSSIBLE_CROSS_SOURCE_DUPLICATE",
                  "SKIPPED",
                ].includes(status);
              }

              return (
                <tr key={row.rowNumber}>
                  <td>
                    <strong>#{row.rowNumber}</strong>
                  </td>

                  <td>
                    <strong>{row.realDate || "-"}</strong>
                  </td>

                  <td>
                    <div className="import-row-description">
                      <strong>
                        {row.normalizedDescription ||
                          row.rawDescription ||
                          "Sin descripción"}
                      </strong>

                      {row.rawDescription &&
                      row.rawDescription !== row.normalizedDescription ? (
                        <span className="muted">
                          Original: {row.rawDescription}
                        </span>
                      ) : null}

                      {row.matchReason ? (
                        <span className="muted">Match: {row.matchReason}</span>
                      ) : null}

                      {row.merchantName ? (
                        <span className="muted">
                          Comercio: {row.merchantName}
                        </span>
                      ) : null}

                      {row.counterparty &&
                      row.counterparty !== row.merchantName ? (
                        <span className="muted">
                          Contraparte: {row.counterparty}
                        </span>
                      ) : null}

                      {compactMeta(row) ? (
                        <span className="muted">{compactMeta(row)}</span>
                      ) : null}

                      {row.classificationReason ? (
                        <span className="muted">
                          Regla: {row.classificationReason}
                        </span>
                      ) : null}

                      {getClassificationExplanationSummary(row) ? (
                        <span className="muted">
                          Match: {getClassificationExplanationSummary(row)}
                        </span>
                      ) : null}

                      {existingMovementNote ? (
                        <small className="import-row-note">
                          {existingMovementNote}
                        </small>
                      ) : null}
                    </div>
                  </td>

                  <td className="amount-cell">
                    {formatMoney(Number(row.amount ?? 0))}
                  </td>

                  <td>
                    <div className="import-row-type-cell">
                      <StatusBadge
                        tone={movementTypeTones[row.movementType]}
                        label={
                          importMovementLabels[row.movementType] ??
                          row.movementType
                        }
                      />

                      <select
                        className="input-ui"
                        value={row.movementType}
                        disabled={isExistingMovementStatus(row.status)}
                        onChange={(event) =>
                          updateRow(row.rowNumber, {
                            movementType: event.target
                              .value as TransactionImportMovementType,
                            suggestedCategoryId: null,
                          })
                        }
                      >
                        {Object.entries(importMovementLabels).map(
                          ([value, label]) => (
                            <option key={value} value={value}>
                              {label}
                            </option>
                          ),
                        )}
                      </select>
                    </div>
                  </td>

                  <td>
                    <div className="import-row-category-cell">
                      <ImportRowCategorySelect
                        row={row}
                        categories={categories}
                        onChange={(categoryId) =>
                          updateRow(row.rowNumber, {
                            suggestedCategoryId: categoryId,
                          })
                        }
                      />

                      {categoryName ? (
                        <span className="muted">Sugerida: {categoryName}</span>
                      ) : null}

                      {row.confidence ? (
                        <span className="muted">
                          Confianza:{" "}
                          {importConfidenceLabels[row.confidence] ??
                            row.confidence}
                        </span>
                      ) : null}

                      {issueMessage ? (
                        <small className="import-row-note">
                          {issueMessage}
                        </small>
                      ) : null}
                    </div>
                  </td>

                  <td>
                    <StatusBadge
                      tone={getImportRowStatusTone(row.status)}
                      label={labelOrFallback(
                        importRowStatusLabels,
                        row.status,
                        "Estado no reconocido",
                      )}
                    />
                  </td>
                </tr>
              );
            })}
          </tbody>
        </table>
      </div>

      <ImportRowsMobileList
        rows={rows}
        categories={categories}
        categoriesById={categoriesById}
        createMissingFallbackCategory={createMissingFallbackCategory}
        onUpdateRow={updateRow}
      />
    </div>
  );
}
