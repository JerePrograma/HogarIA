import { DataTable } from '../../../shared/data-display/DataTable';
import { formatMoney } from '../../../domain/formatters';
import { getApiErrorMessage } from '../../../api/http';
import { StatusBadge } from '../../../shared/ui/StatusBadge';
import type {
  ExternalIntegrationDiagnosticResponse,
  ExternalLoanBackfillApplyResponse,
  ExternalLoanBackfillDryRunResponse,
  ExternalLoanIdempotencyDiagnosticsResponse,
  ExternalLoanManualSyncResponse,
} from '../types';
import { ExternalLoanSyncResultPanel } from './ExternalLoanSyncResultPanel';

type ExternalLoanOperationsPanelProps = {
  health?: ExternalIntegrationDiagnosticResponse;
  healthLoading?: boolean;
  healthError?: unknown;
  hasSyncConfig: boolean;
  canSync: boolean;
  readOnlyMode: boolean;
  configEnabled: boolean;
  missingConfigItems: string[];
  dryRunResult?: ExternalLoanManualSyncResponse;
  dryRunPending?: boolean;
  dryRunError?: unknown;
  syncResult?: ExternalLoanManualSyncResponse;
  syncPending?: boolean;
  syncError?: unknown;
  diagnostics?: ExternalLoanIdempotencyDiagnosticsResponse;
  diagnosticsPending?: boolean;
  diagnosticsError?: unknown;
  backfillDryRun?: ExternalLoanBackfillDryRunResponse;
  backfillDryRunPending?: boolean;
  backfillDryRunError?: unknown;
  backfillApply?: ExternalLoanBackfillApplyResponse;
  backfillApplyPending?: boolean;
  backfillApplyError?: unknown;
  onAnalyzeSync: () => void;
  onSync: () => void;
  onDiagnose: () => void;
  onAnalyzeBackfill: () => void;
  onApplyBackfill: (includeLowConfidence: boolean) => void;
};

const statusTone = (status?: string): 'ok' | 'watch' | 'critical' | 'neutral' => {
  if (status === 'OK' || status === 'ENABLED') return 'ok';
  if (status === 'MISCONFIGURED' || status === 'UNAUTHORIZED' || status === 'UNAVAILABLE') {
    return 'critical';
  }
  return status ? 'watch' : 'neutral';
};

const booleanLabel = (value: boolean) => (value ? 'Sí' : 'No');

export function ExternalLoanOperationsPanel({
  health,
  healthLoading = false,
  healthError,
  hasSyncConfig,
  canSync,
  readOnlyMode,
  configEnabled,
  missingConfigItems,
  dryRunResult,
  dryRunPending = false,
  dryRunError,
  syncResult,
  syncPending = false,
  syncError,
  diagnostics,
  diagnosticsPending = false,
  diagnosticsError,
  backfillDryRun,
  backfillDryRunPending = false,
  backfillDryRunError,
  backfillApply,
  backfillApplyPending = false,
  backfillApplyError,
  onAnalyzeSync,
  onSync,
  onDiagnose,
  onAnalyzeBackfill,
  onApplyBackfill,
}: ExternalLoanOperationsPanelProps) {
  const hasIndexBlockingDuplicates = Boolean(diagnostics?.hasIndexBlockingDuplicates);
  const syncBlockedByDiagnostics = hasIndexBlockingDuplicates || diagnostics?.canRunSync === false;
  const syncDisabled =
    syncPending || !hasSyncConfig || !canSync || readOnlyMode || syncBlockedByDiagnostics;
  const candidates = backfillDryRun?.candidates ?? [];
  const safeBackfillCandidates = candidates.filter(
    (candidate) => candidate.wouldCreateMapping && candidate.confidence !== 'LOW',
  );

  return (
    <section className="panel">
      <div className="section-title">
        <div>
          <p className="eyebrow">Operación</p>
          <h2>CJPrestamos - Sincronización contable</h2>
        </div>
        <StatusBadge
          tone={hasIndexBlockingDuplicates ? 'critical' : configEnabled ? 'ok' : 'watch'}
          label={hasIndexBlockingDuplicates ? 'Duplicados bloqueantes' : configEnabled ? 'Lista' : 'Incompleta'}
        />
      </div>

      <div className="stack-ui">
        <section className="surface-inset">
          <div className="section-title">
            <div>
              <p className="eyebrow">A</p>
              <h3>Health / configuración</h3>
            </div>
            <StatusBadge tone={statusTone(health?.status)} label={health?.status ?? 'Sin health'} />
          </div>

          <div className="metric-grid">
            <article>
              <p className="label-ui">Health</p>
              <strong>{healthLoading ? 'Consultando...' : health?.message ?? 'Sin diagnóstico'}</strong>
            </article>
            <article>
              <p className="label-ui">Sync habilitada</p>
              <strong>{booleanLabel(configEnabled && !readOnlyMode)}</strong>
            </article>
            <article>
              <p className="label-ui">Campos pendientes</p>
              <strong>{missingConfigItems.length}</strong>
            </article>
          </div>

          {healthError ? <p className="mensaje-error mt-3">{getApiErrorMessage(healthError)}</p> : null}
          {readOnlyMode ? <p className="mensaje-warning mt-3">Modo solo lectura activo.</p> : null}
          {missingConfigItems.length > 0 ? (
            <p className="mensaje-warning mt-3">
              Configuración incompleta: falta {missingConfigItems.join(', ')}.
            </p>
          ) : null}
        </section>

        <section className="surface-inset">
          <div className="section-title">
            <div>
              <p className="eyebrow">B</p>
              <h3>Dry-run result</h3>
            </div>
          </div>
          {dryRunResult ? (
            <ExternalLoanSyncResultPanel title="Resultado de análisis sync" result={dryRunResult} />
          ) : (
            <p className="muted">Sin análisis de sync ejecutado.</p>
          )}
          {dryRunError ? <p className="mensaje-error mt-3">{getApiErrorMessage(dryRunError)}</p> : null}
        </section>

        <section className="surface-inset">
          <div className="section-title">
            <div>
              <p className="eyebrow">C</p>
              <h3>Idempotency diagnostics</h3>
            </div>
            {diagnostics ? (
              <StatusBadge
                tone={diagnostics.hasIndexBlockingDuplicates ? 'critical' : 'ok'}
                label={diagnostics.hasIndexBlockingDuplicates ? 'Bloqueante' : 'Sin bloqueos'}
              />
            ) : null}
          </div>

          {diagnostics ? (
            <>
              <div className="metric-grid">
                <article>
                  <p className="label-ui">CJ transactions</p>
                  <strong>{diagnostics.cjTransactions}</strong>
                </article>
                <article>
                  <p className="label-ui">Mapped</p>
                  <strong>{diagnostics.mappedTransactions}</strong>
                </article>
                <article>
                  <p className="label-ui">Unmapped</p>
                  <strong>{diagnostics.unmappedCandidates}</strong>
                </article>
                <article>
                  <p className="label-ui">wouldCreateMapping</p>
                  <strong>{diagnostics.wouldCreateMappings}</strong>
                </article>
              </div>

              <p className="secondary-text mt-3">
                HIGH {diagnostics.candidateCountsByConfidence.HIGH ?? 0} · MEDIUM{' '}
                {diagnostics.candidateCountsByConfidence.MEDIUM ?? 0} · LOW{' '}
                {diagnostics.candidateCountsByConfidence.LOW ?? 0} · alreadyMappedEvents{' '}
                {diagnostics.alreadyMappedEvents} · alreadyMappedTransactions{' '}
                {diagnostics.alreadyMappedTransactions}
              </p>

              {diagnostics.backfillRecommended ? (
                <p className="mensaje-warning mt-3">
                  Backfill recomendado: ejecutar dry-run de backfill antes de sincronizar.
                </p>
              ) : null}

              {diagnostics.hasIndexBlockingDuplicates ? (
                <p className="mensaje-error mt-3">Resolver duplicados antes de sincronizar.</p>
              ) : null}

              <DuplicateGroupsSummary diagnostics={diagnostics} />
            </>
          ) : (
            <p className="muted">Sin diagnóstico de idempotencia ejecutado.</p>
          )}
          {diagnosticsError ? (
            <p className="mensaje-error mt-3">{getApiErrorMessage(diagnosticsError)}</p>
          ) : null}
        </section>

        <section className="surface-inset">
          <div className="section-title">
            <div>
              <p className="eyebrow">D</p>
              <h3>Backfill candidates</h3>
            </div>
            {backfillDryRun ? (
              <StatusBadge tone={safeBackfillCandidates.length > 0 ? 'watch' : 'ok'} label={`${candidates.length} candidatos`} />
            ) : null}
          </div>

          {candidates.length > 0 ? (
            <DataTable minWidth={980}>
              <thead>
                <tr>
                  <th>transactionId</th>
                  <th>realDate</th>
                  <th>description</th>
                  <th>amount</th>
                  <th>inferredEntityType</th>
                  <th>inferredEntityId</th>
                  <th>inferredEventType</th>
                  <th>confidence</th>
                  <th>warning</th>
                  <th>wouldCreateMapping</th>
                </tr>
              </thead>
              <tbody>
                {candidates.map((candidate) => (
                  <tr key={candidate.transactionId}>
                    <td>{candidate.transactionId}</td>
                    <td>{candidate.realDate}</td>
                    <td>{candidate.description}</td>
                    <td>{formatMoney(candidate.amount)}</td>
                    <td>{candidate.inferredEntityType}</td>
                    <td>{candidate.inferredEntityId}</td>
                    <td>{candidate.inferredEventType}</td>
                    <td>{candidate.confidence}</td>
                    <td>{candidate.warning ?? '-'}</td>
                    <td>{booleanLabel(candidate.wouldCreateMapping)}</td>
                  </tr>
                ))}
              </tbody>
            </DataTable>
          ) : (
            <p className="muted">No hay candidatos analizados.</p>
          )}

          {backfillDryRunError ? (
            <p className="mensaje-error mt-3">{getApiErrorMessage(backfillDryRunError)}</p>
          ) : null}

          {backfillApply ? (
            <p className={backfillApply.errors.length > 0 ? 'mensaje-warning mt-3' : 'mensaje-exito mt-3'}>
              Mappings creados: {backfillApply.createdMappings}. Omitidos:{' '}
              {backfillApply.skipped.length}. Errores: {backfillApply.errors.length}.
            </p>
          ) : null}

          {backfillApplyError ? (
            <p className="mensaje-error mt-3">{getApiErrorMessage(backfillApplyError)}</p>
          ) : null}
        </section>

        <section className="surface-inset">
          <div className="section-title">
            <div>
              <p className="eyebrow">E</p>
              <h3>Actions</h3>
            </div>
          </div>

          <div className="page-actions">
            <button
              type="button"
              className="boton-secundario"
              onClick={onAnalyzeSync}
              disabled={dryRunPending || !hasSyncConfig}
            >
              {dryRunPending ? 'Analizando...' : 'Analizar sync'}
            </button>

            <button
              type="button"
              className="boton-principal"
              onClick={onSync}
              disabled={syncDisabled}
            >
              {syncPending ? 'Sincronizando...' : 'Ejecutar sync'}
            </button>

            <button
              type="button"
              className="boton-secundario"
              onClick={onDiagnose}
              disabled={diagnosticsPending || !hasSyncConfig}
            >
              {diagnosticsPending ? 'Diagnosticando...' : 'Diagnosticar idempotencia'}
            </button>

            <button
              type="button"
              className="boton-secundario"
              onClick={onAnalyzeBackfill}
              disabled={backfillDryRunPending || !hasSyncConfig}
            >
              {backfillDryRunPending ? 'Analizando...' : 'Analizar backfill'}
            </button>

            <button
              type="button"
              className="boton-secundario"
              onClick={() => onApplyBackfill(false)}
              disabled={backfillApplyPending || safeBackfillCandidates.length === 0}
            >
              {backfillApplyPending ? 'Aplicando...' : 'Aplicar backfill seguro'}
            </button>
          </div>

          {syncBlockedByDiagnostics ? (
            <p className="mensaje-error mt-3">Resolver duplicados antes de sincronizar.</p>
          ) : null}
          {syncError ? <p className="mensaje-error mt-3">{getApiErrorMessage(syncError)}</p> : null}
          {syncResult ? (
            <ExternalLoanSyncResultPanel title="Resultado de sync" result={syncResult} />
          ) : null}
        </section>
      </div>
    </section>
  );
}

function DuplicateGroupsSummary({
  diagnostics,
}: {
  diagnostics: ExternalLoanIdempotencyDiagnosticsResponse;
}) {
  const sourceOperationGroups = diagnostics.duplicateSourceOperationGroups;
  const sourceHashGroups = diagnostics.duplicateSourceHashGroups;

  if (sourceOperationGroups.length === 0 && sourceHashGroups.length === 0) {
    return <p className="mensaje-exito mt-3">Sin duplicados bloqueantes para los índices estrictos.</p>;
  }

  return (
    <div className="grid mt-4">
      <div>
        <p className="label-ui">Duplicate sourceOperationId groups</p>
        {sourceOperationGroups.length === 0 ? (
          <p className="muted">Ninguno.</p>
        ) : (
          sourceOperationGroups.map((group) => (
            <div className="surface-inset mt-3" key={`${group.source}-${group.sourceOperationId}`}>
              <strong>
                {group.source} · {group.sourceOperationId} · {group.count}
              </strong>
              <p className="secondary-text mb-0">
                {group.transactions
                  .map((transaction) => `${transaction.transactionId}: ${transaction.description ?? '-'}`)
                  .join(' | ')}
              </p>
            </div>
          ))
        )}
      </div>

      <div>
        <p className="label-ui">Duplicate sourceHash groups</p>
        {sourceHashGroups.length === 0 ? (
          <p className="muted">Ninguno.</p>
        ) : (
          sourceHashGroups.map((group) => (
            <div className="surface-inset mt-3" key={group.sourceHash}>
              <strong>
                {group.sourceHash} · {group.count}
              </strong>
              <p className="secondary-text mb-0">
                {group.transactions
                  .map((transaction) => `${transaction.transactionId}: ${transaction.description ?? '-'}`)
                  .join(' | ')}
              </p>
            </div>
          ))
        )}
      </div>
    </div>
  );
}
