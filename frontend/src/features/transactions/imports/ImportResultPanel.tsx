import { Link } from 'react-router-dom';
import type { TransactionImportCommitResult } from './types';

interface Props {
  result: TransactionImportCommitResult;
  profileId: string;
}

function ResultMetric({
  title,
  value,
  tone,
  helper,
}: {
  title: string;
  value: number;
  tone: 'success' | 'warning' | 'danger' | 'info';
  helper: string;
}) {
  return (
    <article className="metric-card" data-tone={tone}>
      <span>{title}</span>
      <strong>{value}</strong>
      <p className="metric-description">{helper}</p>
    </article>
  );
}

export function ImportResultPanel({ result, profileId }: Props) {
  const created = result.createdCount ?? 0;
  const skipped = result.skippedCount ?? 0;
  const duplicated = result.duplicateCount ?? 0;
  const failed = result.failedCount ?? 0;

  const hasWarnings = Boolean(result.warnings?.length);
  const hasErrors = Boolean(result.errors?.length);
  const hasProblems = hasWarnings || hasErrors || failed > 0;

  return (
    <div className="import-result" aria-live="polite">
      <div className={hasProblems ? 'mensaje-warning' : 'mensaje-exito'}>
        <strong>
          {hasProblems ? 'Importación procesada con observaciones.' : 'Importación completada.'}
        </strong>

        <span>
          Creadas {created} · omitidas {skipped} · duplicadas {duplicated} · fallidas {failed}
        </span>
      </div>

      <section className="metric-grid">
        <ResultMetric
          title="Creadas"
          value={created}
          tone="success"
          helper="Movimientos nuevos generados."
        />

        <ResultMetric
          title="Duplicadas"
          value={duplicated}
          tone="info"
          helper="Movimientos detectados como existentes."
        />

        <ResultMetric
          title="Omitidas"
          value={skipped}
          tone="warning"
          helper="Filas que no se importaron por regla."
        />

        <ResultMetric
          title="Fallidas"
          value={failed}
          tone="danger"
          helper="Filas que no pudieron procesarse."
        />
      </section>

      {hasWarnings ? (
        <section className="import-result-list import-result-warnings">
          <div className="section-title">
            <div>
              <p className="eyebrow">Warnings</p>
              <h3>Advertencias ({result.warnings?.length})</h3>
            </div>
          </div>

          <ul>
            {result.warnings?.map((warning, index) => (
              <li key={`warning-${index}`}>{warning}</li>
            ))}
          </ul>
        </section>
      ) : null}

      {hasErrors ? (
        <section className="import-result-list import-result-errors">
          <div className="section-title">
            <div>
              <p className="eyebrow">Errores</p>
              <h3>Errores ({result.errors?.length})</h3>
            </div>
          </div>

          <ul>
            {result.errors?.map((error, index) => (
              <li key={`error-${index}`}>{error}</li>
            ))}
          </ul>
        </section>
      ) : null}

      <div className="form-actions">
        <Link
          to={`/profiles/${profileId}/transactions`}
          className="boton-principal"
        >
          Ver movimientos importados
        </Link>

        <Link
          to={`/profiles/${profileId}/transactions/import`}
          className="boton-secundario"
        >
          Hacer otra importación
        </Link>
      </div>
    </div>
  );
}