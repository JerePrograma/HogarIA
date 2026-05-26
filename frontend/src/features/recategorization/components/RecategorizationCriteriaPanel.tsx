import type {
  BulkRecategorizePreviewPayload,
  BulkRecategorizeReviewFilter,
} from "../../../api/bulkRecategorizeApi";
import { ErrorState } from "../../../components/ui/ErrorState";
import type {
  Account,
  Category,
  MovementType,
  PaymentChannel,
  TransactionClassificationStatus,
} from "../../../domain/types";
import {
  CLASSIFICATION_STATUS_LABELS,
  PAYMENT_CHANNEL_LABELS,
  REVIEW_FILTER_LABELS,
  type ReviewAction,
  toNullableNumber,
  toNullableString,
} from "../recategorizationUtils";

interface Props {
  form: BulkRecategorizePreviewPayload;
  accounts: Account[];
  categories: Category[];
  isAutoTargetMode: boolean;
  reviewAction: ReviewAction;
  missingCategoryName: string;
  canCreateMissingCategory: boolean;
  canPreview: boolean;
  hasAnySearchCriteria: boolean;
  previewPending: boolean;
  applyPending: boolean;
  createCategoryPending: boolean;
  previewErrorMessage?: string | null;
  createCategoryErrorMessage?: string | null;
  onFormChange: (patch: Partial<BulkRecategorizePreviewPayload>) => void;
  onReviewActionChange: (action: ReviewAction) => void;
  onMissingCategoryNameChange: (value: string) => void;
  onCreateCategory: () => void;
  onClearCriteria: () => void;
  onPreview: () => void;
}

const movementOptions: Array<{ value: MovementType; label: string }> = [
  { value: "INCOME", label: "Ingreso" },
  { value: "EXPENSE", label: "Gasto" },
  { value: "SAVING", label: "Ahorro" },
  { value: "TRANSFER", label: "Transferencia" },
  { value: "ADJUSTMENT", label: "Ajuste" },
];

const statusOptions = [
  { value: "CONFIRMED", label: "Confirmado" },
  { value: "PENDING", label: "Pendiente" },
  { value: "IGNORED", label: "Ignorado" },
] as const;

export function RecategorizationCriteriaPanel({
  form,
  accounts,
  categories,
  isAutoTargetMode,
  reviewAction,
  missingCategoryName,
  canCreateMissingCategory,
  canPreview,
  hasAnySearchCriteria,
  previewPending,
  applyPending,
  createCategoryPending,
  previewErrorMessage,
  createCategoryErrorMessage,
  onFormChange,
  onReviewActionChange,
  onMissingCategoryNameChange,
  onCreateCategory,
  onClearCriteria,
  onPreview,
}: Props) {
  const activeTransactionIdsCount = form.transactionIds?.length ?? 0;

  return (
    <section className="panel recategorization-filter-panel">
      <div className="section-title">
        <div>
          <p className="eyebrow">Paso 1</p>
          <h2>Definir búsqueda y destino</h2>
          <p className="muted">
            Elegí qué movimientos querés encontrar y qué acción aplicar.
          </p>
        </div>

        <button
          type="button"
          className="boton-fantasma"
          onClick={onClearCriteria}
          disabled={previewPending || applyPending}
        >
          Limpiar criterios
        </button>
      </div>

      {activeTransactionIdsCount > 0 ? (
        <div className="mensaje-warning recategorization-id-filter-warning">
          <div>
            <strong>Filtro por movimientos específicos activo.</strong>
            <p>
              Esta previsualización está limitada a {activeTransactionIdsCount}{" "}
              movimiento(s) recibidos desde otra pantalla. Si querés buscar por
              criterios generales, quitá este filtro.
            </p>
          </div>

          <button
            type="button"
            className="boton-secundario"
            onClick={() => onFormChange({ transactionIds: null })}
            disabled={previewPending || applyPending}
          >
            Quitar filtro por IDs
          </button>
        </div>
      ) : null}

      <div className="form-grid">
        <label>
          Cuenta
          <select
            className="input-ui"
            value={form.accountId ?? ""}
            onChange={(event) =>
              onFormChange({ accountId: toNullableString(event.target.value) })
            }
          >
            <option value="">Todas</option>
            {accounts.map((account) => (
              <option key={account.id} value={account.id}>
                {account.name}
              </option>
            ))}
          </select>
        </label>

        <label>
          Desde
          <input
            className="input-ui"
            type="date"
            value={form.from ?? ""}
            onChange={(event) =>
              onFormChange({ from: toNullableString(event.target.value) })
            }
          />
        </label>

        <label>
          Hasta
          <input
            className="input-ui"
            type="date"
            value={form.to ?? ""}
            onChange={(event) =>
              onFormChange({ to: toNullableString(event.target.value) })
            }
          />
        </label>

        <label>
          Categoría actual
          <select
            className="input-ui"
            value={
              form.onlyWithoutCategory
                ? "__WITHOUT_CATEGORY__"
                : (form.fromCategoryId ?? "")
            }
            onChange={(event) =>
              onFormChange(
                event.target.value === "__WITHOUT_CATEGORY__"
                  ? { fromCategoryId: null, onlyWithoutCategory: true }
                  : {
                      fromCategoryId: toNullableString(event.target.value),
                      onlyWithoutCategory: null,
                    },
              )
            }
          >
            <option value="">Todas</option>
            <option value="__WITHOUT_CATEGORY__">Sin categoría</option>
            {categories.map((category) => (
              <option key={category.id} value={category.id}>
                {category.name}
              </option>
            ))}
          </select>
        </label>

        <label>
          Modo destino
          <select
            className="input-ui"
            value={form.targetMode ?? "MANUAL"}
            onChange={(event) =>
              onFormChange({
                targetMode: event.target
                  .value as BulkRecategorizePreviewPayload["targetMode"],
                toCategoryId:
                  event.target.value === "AUTO_BY_IMPORT_RULES"
                    ? null
                    : form.toCategoryId,
              })
            }
          >
            <option value="MANUAL">Categoría fija</option>
            <option value="AUTO_BY_IMPORT_RULES">
              Automático según importación
            </option>
          </select>
        </label>

        <label>
          Categoría destino
          <select
            className="input-ui"
            value={form.toCategoryId ?? ""}
            disabled={isAutoTargetMode}
            onChange={(event) =>
              onFormChange({
                toCategoryId: toNullableString(event.target.value),
              })
            }
          >
            <option value="">Seleccionar</option>
            {categories.map((category) => (
              <option key={category.id} value={category.id}>
                {category.name}
              </option>
            ))}
          </select>
        </label>

        {isAutoTargetMode ? (
          <p className="muted form-field-wide">
            Se intentará resolver la categoría usando las mismas reglas del
            importador. Los gastos DEBIN/Cuenta DNI sin contraparte quedarán
            pendientes, no se inventará una categoría.
          </p>
        ) : null}

        <label>
          Tipo
          <select
            className="input-ui"
            value={form.movementType ?? ""}
            onChange={(event) =>
              onFormChange({
                movementType: (event.target.value ||
                  null) as MovementType | null,
              })
            }
          >
            <option value="">Todos</option>
            {movementOptions.map((option) => (
              <option key={option.value} value={option.value}>
                {option.label}
              </option>
            ))}
          </select>
        </label>

        <label>
          Clasificación actual
          <select
            className="input-ui"
            value={form.classificationStatus ?? ""}
            onChange={(event) =>
              onFormChange({
                classificationStatus: (event.target.value ||
                  null) as TransactionClassificationStatus | null,
              })
            }
          >
            <option value="">Todas</option>
            {Object.entries(CLASSIFICATION_STATUS_LABELS).map(
              ([value, label]) => (
                <option key={value} value={value}>
                  {label}
                </option>
              ),
            )}
          </select>
        </label>

        <label>
          Canal de pago
          <select
            className="input-ui"
            value={form.paymentChannel ?? ""}
            onChange={(event) =>
              onFormChange({
                paymentChannel: (event.target.value ||
                  null) as PaymentChannel | null,
              })
            }
          >
            <option value="">Todos</option>
            {Object.entries(PAYMENT_CHANNEL_LABELS).map(([value, label]) => (
              <option key={value} value={value}>
                {label}
              </option>
            ))}
          </select>
        </label>

        <label>
          Solo importados
          <select
            className="input-ui"
            value={form.onlyImported == null ? "" : String(form.onlyImported)}
            onChange={(event) =>
              onFormChange({
                onlyImported:
                  event.target.value === ""
                    ? null
                    : event.target.value === "true",
              })
            }
          >
            <option value="">Todos</option>
            <option value="true">Sí</option>
            <option value="false">No</option>
          </select>
        </label>

        <label>
          Caso a revisar
          <select
            className="input-ui"
            value={form.reviewFilter ?? ""}
            onChange={(event) =>
              onFormChange({
                reviewFilter: (event.target.value ||
                  null) as BulkRecategorizeReviewFilter | null,
              })
            }
          >
            <option value="">Sin filtro</option>
            {Object.entries(REVIEW_FILTER_LABELS).map(([value, label]) => (
              <option key={value} value={value}>
                {label}
              </option>
            ))}
          </select>
        </label>

        <label>
          Source
          <input
            className="input-ui"
            value={form.source ?? ""}
            placeholder="Ej: BANCO_PROVINCIA, MERCADO_PAGO"
            onChange={(event) =>
              onFormChange({ source: toNullableString(event.target.value) })
            }
          />
        </label>

        <label>
          Contraparte contiene
          <input
            className="input-ui"
            value={form.counterpartyContains ?? ""}
            placeholder="Ej: comercio, persona, banco"
            onChange={(event) =>
              onFormChange({
                counterpartyContains: toNullableString(event.target.value),
              })
            }
          />
        </label>

        <label className="form-field-wide">
          Descripción contiene
          <input
            className="input-ui"
            value={form.descriptionContains ?? ""}
            placeholder="Ej: supermercado, transferencia, mercado pago"
            onChange={(event) =>
              onFormChange({
                descriptionContains: toNullableString(event.target.value),
              })
            }
          />
        </label>

        <label>
          Monto exacto
          <input
            className="input-ui"
            type="number"
            step="0.01"
            value={form.exactAmount ?? ""}
            onChange={(event) =>
              onFormChange({
                exactAmount: toNullableNumber(event.target.value),
              })
            }
          />
        </label>

        <label>
          Monto mínimo
          <input
            className="input-ui"
            type="number"
            step="0.01"
            value={form.minAmount ?? ""}
            onChange={(event) =>
              onFormChange({ minAmount: toNullableNumber(event.target.value) })
            }
          />
        </label>

        <label>
          Monto máximo
          <input
            className="input-ui"
            type="number"
            step="0.01"
            value={form.maxAmount ?? ""}
            onChange={(event) =>
              onFormChange({ maxAmount: toNullableNumber(event.target.value) })
            }
          />
        </label>

        <label>
          Acción
          <select
            className="input-ui"
            value={reviewAction}
            onChange={(event) =>
              onReviewActionChange(event.target.value as ReviewAction)
            }
          >
            <option value="RECATEGORIZE">Solo recategorizar</option>
            <option value="CONVERT_TRANSFER">Convertir a transferencia</option>
            <option value="MARK_IGNORED">Marcar ignorado</option>
            <option value="ADJUST_RECOVERABLE">Ajuste recuperable</option>
            <option value="CONFIRM_EXPENSE">Mantener como gasto</option>
            <option value="CONFIRM_RECOVERY_INCOME">
              Confirmar recupero como ingreso
            </option>
          </select>
        </label>

        <label>
          Tipo destino
          <select
            className="input-ui"
            value={form.targetMovementType ?? ""}
            onChange={(event) =>
              onFormChange({
                targetMovementType: (event.target.value ||
                  null) as MovementType | null,
              })
            }
          >
            <option value="">Sin cambio</option>
            {movementOptions.map((option) => (
              <option key={option.value} value={option.value}>
                {option.label}
              </option>
            ))}
          </select>
        </label>

        <label>
          Estado destino
          <select
            className="input-ui"
            value={form.targetStatus ?? ""}
            onChange={(event) =>
              onFormChange({
                targetStatus: (event.target.value ||
                  null) as BulkRecategorizePreviewPayload["targetStatus"],
              })
            }
          >
            <option value="">Sin cambio</option>
            {statusOptions.map((option) => (
              <option key={option.value} value={option.value}>
                {option.label}
              </option>
            ))}
          </select>
        </label>

        <label>
          Clasificación destino
          <select
            className="input-ui"
            value={form.targetClassificationStatus ?? ""}
            onChange={(event) =>
              onFormChange({
                targetClassificationStatus: (event.target.value ||
                  null) as TransactionClassificationStatus | null,
              })
            }
          >
            <option value="">Sin cambio</option>
            {Object.entries(CLASSIFICATION_STATUS_LABELS).map(
              ([value, label]) => (
                <option key={value} value={value}>
                  {label}
                </option>
              ),
            )}
          </select>
        </label>

        <label className="form-field-wide">
          Motivo de clasificación destino
          <input
            className="input-ui"
            value={form.targetClassificationReason ?? ""}
            placeholder="Ej: USER_MARKED_INTERNAL_TRANSFER"
            onChange={(event) =>
              onFormChange({
                targetClassificationReason: toNullableString(
                  event.target.value,
                ),
              })
            }
          />
        </label>
      </div>

      {missingCategoryName && !form.toCategoryId ? (
        <div className="recategorization-missing-category mensaje-warning">
          <div>
            <strong>La categoría sugerida todavía no existe.</strong>
            <span>{missingCategoryName}</span>
          </div>

          <div className="recategorization-missing-category-actions">
            <input
              className="input-ui"
              value={missingCategoryName}
              onChange={(event) =>
                onMissingCategoryNameChange(event.target.value)
              }
            />

            <button
              type="button"
              className="boton-secundario"
              onClick={onCreateCategory}
              disabled={!canCreateMissingCategory}
            >
              {createCategoryPending ? "Creando..." : "Crear categoría"}
            </button>
          </div>
        </div>
      ) : null}

      {!hasAnySearchCriteria ? (
        <p className="mensaje-warning">
          Cargá al menos un criterio de búsqueda. Recategorizar sin filtros es
          una receta prolija para romper datos.
        </p>
      ) : null}

      <div className="form-actions">
        <button
          type="button"
          className="boton-principal"
          onClick={onPreview}
          disabled={!canPreview}
        >
          {previewPending ? "Previsualizando..." : "Previsualizar cambios"}
        </button>

        {!isAutoTargetMode &&
        !form.toCategoryId &&
        !form.targetMovementType &&
        !form.targetStatus &&
        !form.targetClassificationStatus ? (
          <span className="muted">
            Seleccioná una categoría destino o una acción destino para poder
            previsualizar.
          </span>
        ) : null}
      </div>

      {previewErrorMessage ? (
        <ErrorState message={previewErrorMessage} />
      ) : null}
      {createCategoryErrorMessage ? (
        <ErrorState message={createCategoryErrorMessage} />
      ) : null}
    </section>
  );
}
