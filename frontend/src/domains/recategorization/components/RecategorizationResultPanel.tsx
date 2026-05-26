import { Link } from "react-router-dom";
import type { BulkRecategorizeApplyResult } from "../../../api/bulkRecategorizeApi";

interface Props {
  profileId: string;
  result: BulkRecategorizeApplyResult;
  onReset: () => void;
}

export function RecategorizationResultPanel({
  profileId,
  result,
  onReset,
}: Props) {
  return (
    <section className="panel recategorization-result-panel">
      <div className="section-title">
        <div>
          <p className="eyebrow">Resultado</p>
          <h2>Recategorización aplicada</h2>
        </div>
      </div>

      <div className="mensaje-exito">
        <strong>Proceso finalizado.</strong>
        <span>
          Actualizados: {result.updatedCount} · Omitidos: {result.skippedCount}{" "}
          · Fallidos: {result.failedCount}
        </span>
      </div>

      {result.warnings.length ? (
        <div className="mensaje-warning">
          <strong>Advertencias</strong>
          <ul>
            {result.warnings.map((warning) => (
              <li key={warning}>{warning}</li>
            ))}
          </ul>
        </div>
      ) : null}

      {result.errors.length ? (
        <div className="mensaje-error">
          <strong>Errores</strong>
          <ul>
            {result.errors.map((error) => (
              <li key={error}>{error}</li>
            ))}
          </ul>
        </div>
      ) : null}

      <div className="form-actions">
        <Link
          className="boton-principal"
          to={`/profiles/${profileId}/transactions`}
        >
          Ver movimientos
        </Link>

        <button type="button" className="boton-secundario" onClick={onReset}>
          Hacer otra recategorización
        </button>
      </div>
    </section>
  );
}
