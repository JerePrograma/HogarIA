import { Link } from 'react-router-dom';
import type { TransactionImportCommitResult } from './types';

export function ImportResultPanel({ result, profileId }: { result: TransactionImportCommitResult; profileId: string }) {
  const created = result.createdCount ?? 0;
  const skipped = result.skippedCount ?? 0;
  const duplicated = result.duplicateCount ?? 0;
  const failed = result.failedCount ?? 0;

  return (
    <section className="panel stack-ui" aria-live="polite">
      <h2>Paso 3: Resultado</h2>
      <p className="mensaje-exito">Resumen: creadas {created} · omitidas {skipped} · duplicadas {duplicated} · fallidas {failed}</p>
      <p>Creadas: {created} · Duplicadas: {duplicated} · Ignoradas: {skipped} · Fallidas: {failed}</p>

      {result.warnings?.length ? (
        <div>
          <h3>Warnings ({result.warnings.length})</h3>
          <ul>
            {result.warnings.map((warning, index) => <li key={`warning-${index}`}>{warning}</li>)}
          </ul>
        </div>
      ) : null}

      {result.errors?.length ? (
        <div>
          <h3>Errors ({result.errors.length})</h3>
          <ul>
            {result.errors.map((error, index) => <li key={`error-${index}`}>{error}</li>)}
          </ul>
        </div>
      ) : null}

      <div className="stack-ui">
        <Link to={`/profiles/${profileId}/transactions`} className="boton-secundario">Ver movimientos importados</Link>
      </div>
    </section>
  );
}
