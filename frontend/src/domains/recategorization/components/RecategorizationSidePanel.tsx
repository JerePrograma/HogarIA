import type { BulkRecategorizePreviewPayload } from "../../../api/bulkRecategorizeApi";
import type { Category } from "../../../domain/types";
import {
  CLASSIFICATION_STATUS_LABELS,
  getMovementLabel,
} from "../recategorizationUtils";

interface Props {
  selectedTargetCategory: Category | null | undefined;
  isAutoTargetMode: boolean;
  form: BulkRecategorizePreviewPayload;
}

export function RecategorizationSidePanel({
  selectedTargetCategory,
  isAutoTargetMode,
  form,
}: Props) {
  return (
    <aside className="recategorization-side">
      <section className="panel-soft recategorization-side-card">
        <p className="eyebrow">Destino</p>
        <h3>Acción final</h3>

        <div className="recategorization-destination-card">
          <span>Categoría destino</span>
          <strong>
            {isAutoTargetMode
              ? "Automática según reglas"
              : (selectedTargetCategory?.name ?? "No seleccionada")}
          </strong>
        </div>

        {form.targetMovementType ? (
          <p className="muted">
            Tipo destino: {getMovementLabel(form.targetMovementType)}
          </p>
        ) : null}

        {form.targetStatus ? (
          <p className="muted">Estado destino: {form.targetStatus}</p>
        ) : null}

        {form.targetClassificationStatus ? (
          <p className="muted">
            Clasificación destino:{" "}
            {CLASSIFICATION_STATUS_LABELS[form.targetClassificationStatus] ??
              form.targetClassificationStatus}
          </p>
        ) : null}

        {form.targetClassificationReason ? (
          <p className="muted">Motivo: {form.targetClassificationReason}</p>
        ) : null}
      </section>

      <section className="panel-soft recategorization-side-card">
        <p className="eyebrow">Reglas</p>
        <h3>Seguridad</h3>

        <ul className="recategorization-rule-list">
          <li>No se aplica nada sin previsualización.</li>
          <li>En modo manual, se requiere categoría o acción destino.</li>
          <li>No se permite buscar sin al menos un criterio.</li>
          <li>Solo se aplican candidatos marcados como actualizables.</li>
          <li>
            Los gastos DEBIN/Cuenta DNI sin contraparte no se clasifican solos.
          </li>
        </ul>
      </section>
    </aside>
  );
}
