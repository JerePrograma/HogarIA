import { Link } from "react-router-dom";
import type { Account, Category } from "../../../domain/types";

interface Props {
  profileId: string;
  selectedAccount: Account | null | undefined;
  selectedTargetCategory: Category | null | undefined;
  isAutoTargetMode: boolean;
  descriptionContains?: string | null;
  statusTitle: string;
  statusDescription: string;
}

export function RecategorizationHero({
  profileId,
  selectedAccount,
  selectedTargetCategory,
  isAutoTargetMode,
  descriptionContains,
  statusTitle,
  statusDescription,
}: Props) {
  return (
    <section className="page-header recategorization-hero">
      <div className="recategorization-hero-copy">
        <p className="eyebrow">Movimientos</p>
        <h1>Recategorizar movimientos</h1>
        <p className="muted">
          Buscá movimientos existentes, previsualizá el impacto y aplicá cambios
          solo sobre candidatos seguros.
        </p>

        <div className="recategorization-hero-badges">
          <span className="badge badge-info">
            {selectedAccount?.name ?? "Todas las cuentas"}
          </span>

          <span
            className={
              selectedTargetCategory || isAutoTargetMode
                ? "badge badge-ok"
                : "badge badge-warning"
            }
          >
            {isAutoTargetMode
              ? "Destino automático"
              : (selectedTargetCategory?.name ?? "Sin categoría destino")}
          </span>

          {descriptionContains ? (
            <span className="badge badge-muted">
              Texto: {descriptionContains}
            </span>
          ) : null}
        </div>
      </div>

      <aside className="recategorization-status-card">
        <span className="label-ui">Estado</span>
        <strong>{statusTitle}</strong>
        <p className="muted">{statusDescription}</p>

        <div className="recategorization-status-actions">
          <Link
            className="boton-secundario"
            to={`/profiles/${profileId}/transactions`}
          >
            Volver a movimientos
          </Link>
        </div>
      </aside>
    </section>
  );
}
