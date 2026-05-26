import { EmptyState } from "../../../components/ui/EmptyState";
import { StatusBadge } from "../../../components/ui/StatusBadge";
import { formatMoney } from "../../../domain/formatters";
import type {
  BulkRecategorizeCandidate,
  BulkRecategorizePreviewResult,
} from "../../../api/bulkRecategorizeApi";
import {
  ALL,
  PREVIEW_STATUS_LABELS,
  getMovementLabel,
  getMovementTone,
  getPreviewStatusLabel,
  getPreviewStatusTone,
  type CandidateFilters,
  type CandidateStatusFilter,
} from "../recategorizationUtils";

interface Props {
  preview: BulkRecategorizePreviewResult;
  candidates: BulkRecategorizeCandidate[];
  visibleCandidates: BulkRecategorizeCandidate[];
  candidateFilters: CandidateFilters;
  statusCounters: Record<string, number>;
  activeCandidateFilterCount: number;
  readyCount: number;
  canApply: boolean;
  applyPending: boolean;
  onCandidateFiltersChange: (patch: Partial<CandidateFilters>) => void;
  onResetFilters: () => void;
  onApply: () => void;
}

export function RecategorizationPreviewPanel({
  preview,
  candidates,
  visibleCandidates,
  candidateFilters,
  statusCounters,
  activeCandidateFilterCount,
  readyCount,
  canApply,
  applyPending,
  onCandidateFiltersChange,
  onResetFilters,
  onApply,
}: Props) {
  return (
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
            onClick={onResetFilters}
          >
            Limpiar filtros ({activeCandidateFilterCount})
          </button>
        ) : null}
      </div>

      {preview.warnings.length ? (
        <div className="mensaje-warning">
          <strong>Advertencias del preview</strong>
          <ul>
            {preview.warnings.map((warning) => (
              <li key={warning}>{warning}</li>
            ))}
          </ul>
        </div>
      ) : null}

      {preview.errors.length ? (
        <div className="mensaje-error">
          <strong>Errores del preview</strong>
          <ul>
            {preview.errors.map((error) => (
              <li key={error}>{error}</li>
            ))}
          </ul>
        </div>
      ) : null}

      <section className="recategorization-summary-grid">
        <article className="recategorization-summary-card">
          <span>Encontrados</span>
          <strong>{preview.totalMatched}</strong>
          <p>Total de movimientos que cumplen la búsqueda.</p>
        </article>

        <article className="recategorization-summary-card tone-ok">
          <span>Actualizables</span>
          <strong>{preview.updatableCount}</strong>
          <p>Candidatos seguros para aplicar.</p>
        </article>

        <article className="recategorization-summary-card tone-warning">
          <span>Ambiguos</span>
          <strong>{preview.ambiguousCount}</strong>
          <p>Requieren revisión antes de tocar datos.</p>
        </article>

        <article className="recategorization-summary-card tone-neutral">
          <span>Omitidos</span>
          <strong>{preview.skippedCount}</strong>
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
            placeholder="Descripción, fecha, monto, tipo, canal, estado o warning"
            onChange={(event) =>
              onCandidateFiltersChange({ search: event.target.value })
            }
          />
        </label>

        <label>
          Estado
          <select
            className="input-ui"
            value={candidateFilters.status}
            onChange={(event) =>
              onCandidateFiltersChange({
                status: event.target.value as CandidateStatusFilter,
              })
            }
          >
            <option value={ALL}>Todos</option>
            {Object.entries(PREVIEW_STATUS_LABELS).map(([value, label]) => (
              <option key={value} value={value}>
                {label}
              </option>
            ))}
          </select>
        </label>
      </div>

      <div className="recategorization-filter-chips">
        <button
          type="button"
          className={`recategorization-chip ${candidateFilters.status === ALL ? "active" : ""}`}
          onClick={() => onCandidateFiltersChange({ status: ALL })}
        >
          Todos · {candidates.length}
        </button>

        {Object.entries(PREVIEW_STATUS_LABELS).map(([status, label]) => (
          <button
            key={status}
            type="button"
            className={`recategorization-chip ${candidateFilters.status === status ? "active" : ""}`}
            onClick={() =>
              onCandidateFiltersChange({
                status: status as CandidateStatusFilter,
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
                  <th>Canal</th>
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
                      <strong>
                        {candidate.description || "Sin descripción"}
                      </strong>
                      {candidate.source ? (
                        <p className="muted">Source: {candidate.source}</p>
                      ) : null}
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
                      {candidate.targetCategoryName ?? "Sin sugerencia"}
                      {candidate.targetCategoryId &&
                      candidate.targetMovementType ? (
                        <p className="muted">
                          Tipo destino:{" "}
                          {getMovementLabel(candidate.targetMovementType)}
                        </p>
                      ) : null}{" "}
                    </td>

                    <td>
                      <span className="muted">
                        {candidate.paymentChannel ?? "-"}
                      </span>
                    </td>

                    <td>
                      <span className="muted">{candidate.warning || "-"}</span>
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
                    <strong>
                      {candidate.description || "Sin descripción"}
                    </strong>
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

                <p className="muted">
                  Destino: {candidate.targetCategoryName ?? "Sin sugerencia"}
                </p>

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
            Se actualizarán solamente los candidatos marcados como
            actualizables.
          </p>
        </div>

        <button
          type="button"
          className="boton-principal"
          onClick={onApply}
          disabled={!canApply}
        >
          {applyPending
            ? "Aplicando..."
            : `Aplicar a ${readyCount} movimiento(s)`}
        </button>
      </section>
    </section>
  );
}
