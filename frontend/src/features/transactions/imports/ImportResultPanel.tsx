import { Link } from 'react-router-dom';
import type { TransactionImportCommitResult } from './types';

export function ImportResultPanel({ result, profileId }: { result: TransactionImportCommitResult; profileId: string }) {
  return (
    <section className="panel stack-ui" aria-live="polite">
      <h2>Paso 3: Resultado</h2>
      <p className="mensaje-exito">Creadas: {result.createdCount} · Duplicadas: {result.duplicateCount} · Ignoradas: {result.skippedCount ?? 0}</p>
      <div className="stack-ui">
        <Link to={`/profiles/${profileId}/transactions`} className="boton-secundario">Ver movimientos importados</Link>
      </div>
    </section>
  );
}
