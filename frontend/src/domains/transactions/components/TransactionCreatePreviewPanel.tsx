import { Link } from "react-router-dom";
import { routePaths } from "../../../app/router/routePaths";
import { formatMoney } from "../../../domain/formatters";
import type {
  Category,
  InternalTransferCandidate,
  TransactionCreatePreview,
} from "../../../domain/types";
import { StatusBadge } from "../../../shared/ui/StatusBadge";
import type { TransactionForm } from "../types";

interface Props {
  profileId: string;
  preview: TransactionCreatePreview;
  form: TransactionForm;
  categoriesById: Map<string, Category>;
  saving: boolean;
  onCreateAnyway: () => void;
  onMarkPending: () => void;
  onLinkTransfer: (candidate: InternalTransferCandidate) => void;
  onChangeCategory: (categoryId: string) => void;
  onCancel: () => void;
}

export function TransactionCreatePreviewPanel({
  profileId,
  preview,
  form,
  categoriesById,
  saving,
  onCreateAnyway,
  onMarkPending,
  onLinkTransfer,
  onChangeCategory,
  onCancel,
}: Props) {
  const firstDuplicate = preview.duplicateCandidates[0];
  const firstTransfer = preview.internalTransferCandidates[0];
  const suggestedCategoryId = preview.categorySuggestion?.categoryId ?? "";
  const suggestedCategory = suggestedCategoryId
    ? categoriesById.get(suggestedCategoryId)
    : null;

  return (
    <section className="panel transaction-preview-panel" aria-live="polite">
      <div className="section-title">
        <div>
          <p className="eyebrow">Antes de guardar</p>
          <h2>Revisemos este movimiento</h2>
          <p className="muted">{preview.humanSummary}</p>
        </div>

        <StatusBadge
          tone={preview.riskLevel === "BLOCKING" ? "critical" : preview.riskLevel === "WARNING" ? "watch" : "ok"}
          label={preview.riskLevel === "BLOCKING" ? "Necesita revisión" : preview.riskLevel === "WARNING" ? "Atención" : "Listo"}
        />
      </div>

      <div className="transactions-preview-grid">
        <article className="surface-inset">
          <p className="label-ui">Movimiento</p>
          <strong>{form.description || "Sin descripción"}</strong>
          <p className="muted">
            {formatMoney(form.amount, form.currency)} · {preview.financialImpact}
          </p>
          {preview.normalizedDescription ? (
            <p className="compact-muted">Lo leímos como: {preview.normalizedDescription}</p>
          ) : null}
        </article>

        <article className="surface-inset">
          <p className="label-ui">Categoría</p>
          {suggestedCategory ? (
            <>
              <strong>{suggestedCategory.name}</strong>
              <p className="muted">
                {preview.categorySuggestion?.humanReason ?? "Sugerencia disponible."}
              </p>
              {!form.categoryId ? (
                <button
                  type="button"
                  className="boton-secundario mt-3"
                  onClick={() => onChangeCategory(suggestedCategory.id)}
                >
                  Usar esta categoría
                </button>
              ) : null}
            </>
          ) : (
            <>
              <strong>Necesita categoría</strong>
              <p className="muted">
                No estamos seguros. Elegí una o guardalo como pendiente.
              </p>
            </>
          )}
        </article>
      </div>

      {preview.duplicateCandidates.length > 0 ? (
        <div className="mensaje-warning">
          <strong>Ya existe un movimiento muy parecido.</strong>
          <span>
            {firstDuplicate?.description || "Movimiento existente"} ·{" "}
            {formatMoney(Number(firstDuplicate?.amount ?? 0), firstDuplicate?.currency ?? form.currency)}
          </span>
          {firstDuplicate ? (
            <Link
              className="boton-secundario mt-3"
              to={`${routePaths.transactions(profileId)}?search=${encodeURIComponent(firstDuplicate.description ?? "")}`}
            >
              Ver movimiento existente
            </Link>
          ) : null}
        </div>
      ) : null}

      {firstTransfer ? (
        <div className="mensaje-warning">
          <strong>Podría ser transferencia interna.</strong>
          <span>
            Mismo monto en otra cuenta y fecha cercana. Esto se ve en las cuentas, pero no cambia tu resultado del mes.
          </span>
        </div>
      ) : null}

      <div className="form-actions">
        {preview.canCreateDirectly ? (
          <button
            type="button"
            className="boton-principal"
            disabled={saving}
            onClick={onCreateAnyway}
          >
            {saving ? "Guardando..." : "Guardar como movimiento nuevo"}
          </button>
        ) : null}

        {firstTransfer ? (
          <button
            type="button"
            className="boton-secundario"
            disabled={saving}
            onClick={() => onLinkTransfer(firstTransfer)}
          >
            Guardar y vincular transferencia
          </button>
        ) : null}

        {!form.categoryId ? (
          <button
            type="button"
            className="boton-secundario"
            disabled={saving}
            onClick={onMarkPending}
          >
            Marcar como pendiente
          </button>
        ) : null}

        <button
          type="button"
          className="boton-fantasma"
          disabled={saving}
          onClick={onCancel}
        >
          Cancelar
        </button>
      </div>
    </section>
  );
}
