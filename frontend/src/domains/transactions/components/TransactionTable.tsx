import { StatusBadge } from "../../../shared/ui/StatusBadge";
import {
  classificationStatusLabels,
  labelOrValue,
  movementTypeLabels,
  transactionOriginLabels,
  transactionStatusLabels,
} from "../../../domain/financeLabels";
import {
  classificationStatusTones,
  movementTypeTones,
  transactionStatusTones,
} from "../../../domain/financeTones";
import { formatMoney } from "../../../domain/formatters";
import {
  getCategoryDisplayName,
  getDefaultClassificationStatus,
  shouldCountTransactionInOperationalBalance,
} from "../../../domain/transactionRules";
import type { Account, Category, MoneyTransaction } from "../../../domain/types";
import {
  formatDate,
  getTransactionRemovalConfirmMessage,
  getTransactionRemovalLabel,
} from "../utils/transactionUtils";

interface Props {
  transactions: MoneyTransaction[];
  accountsById: Map<string, Account>;
  categoriesById: Map<string, Category>;
  viewMode: "SIMPLE" | "AUDIT";
  selectedTransactionIds: string[];
  onToggleSelection: (id: string) => void;
  onToggleVisibleSelection: () => void;
  updatePending: boolean;
  deletePending: boolean;
  updatingTransactionId?: string;
  deletingTransactionId?: string;
  onToggleStatus: (transaction: MoneyTransaction) => void;
  onDelete: (transaction: MoneyTransaction) => void;
}

export function TransactionTable({
  transactions,
  accountsById,
  categoriesById,
  viewMode,
  selectedTransactionIds,
  onToggleSelection,
  onToggleVisibleSelection,
  updatePending,
  deletePending,
  updatingTransactionId,
  deletingTransactionId,
  onToggleStatus,
  onDelete,
}: Props) {
  const allVisibleSelected =
    transactions.length > 0 &&
    transactions.every((transaction) =>
      selectedTransactionIds.includes(transaction.id),
    );

  return (
    <div className="tabla-ui transactions-table">
      <table className="table-compact">
        <thead>
          {viewMode === "SIMPLE" ? (
            <tr>
              <th>
                <input
                  type="checkbox"
                  checked={allVisibleSelected}
                  onChange={onToggleVisibleSelection}
                  aria-label="Seleccionar movimientos visibles"
                />
              </th>
              <th>Fecha</th>
              <th>Descripción</th>
              <th>Cuenta</th>
              <th>Categoría</th>
              <th>Tipo</th>
              <th className="amount-cell">Monto</th>
              <th>Estado</th>
              <th>Acción rápida</th>
            </tr>
          ) : (
            <tr>
              <th>
                <input
                  type="checkbox"
                  checked={allVisibleSelected}
                  onChange={onToggleVisibleSelection}
                  aria-label="Seleccionar movimientos visibles"
                />
              </th>
              <th>Descripción original</th>
              <th>Descripción leída</th>
              <th>Origen</th>
              <th>Claves</th>
              <th>Impacto</th>
              <th>Clasificación</th>
              <th>Importación</th>
              <th>Acciones</th>
            </tr>
          )}
        </thead>

        <tbody>
          {transactions.map((transaction) =>
            viewMode === "SIMPLE" ? (
              <SimpleRow
                key={transaction.id}
                transaction={transaction}
                accountName={
                  accountsById.get(transaction.accountId)?.name ??
                  "Cuenta no encontrada"
                }
                category={transaction.categoryId ? categoriesById.get(transaction.categoryId) : null}
                selected={selectedTransactionIds.includes(transaction.id)}
                onToggleSelection={onToggleSelection}
                updatePending={updatePending}
                deletePending={deletePending}
                updatingTransactionId={updatingTransactionId}
                deletingTransactionId={deletingTransactionId}
                onToggleStatus={onToggleStatus}
                onDelete={onDelete}
              />
            ) : (
              <AuditRow
                key={transaction.id}
                transaction={transaction}
                selected={selectedTransactionIds.includes(transaction.id)}
                onToggleSelection={onToggleSelection}
                updatePending={updatePending}
                deletePending={deletePending}
                updatingTransactionId={updatingTransactionId}
                deletingTransactionId={deletingTransactionId}
                onToggleStatus={onToggleStatus}
                onDelete={onDelete}
              />
            ),
          )}
        </tbody>
      </table>
    </div>
  );
}

function SimpleRow({
  transaction,
  accountName,
  category,
  selected,
  onToggleSelection,
  updatePending,
  deletePending,
  updatingTransactionId,
  deletingTransactionId,
  onToggleStatus,
  onDelete,
}: {
  transaction: MoneyTransaction;
  accountName: string;
  category?: Category | null;
  selected: boolean;
  onToggleSelection: (id: string) => void;
  updatePending: boolean;
  deletePending: boolean;
  updatingTransactionId?: string;
  deletingTransactionId?: string;
  onToggleStatus: (transaction: MoneyTransaction) => void;
  onDelete: (transaction: MoneyTransaction) => void;
}) {
  const classificationStatus = getDefaultClassificationStatus(transaction);
  const isUpdating = updatePending && updatingTransactionId === transaction.id;
  const isDeleting = deletePending && deletingTransactionId === transaction.id;

  return (
    <tr>
      <td>
        <input
          type="checkbox"
          checked={selected}
          onChange={() => onToggleSelection(transaction.id)}
          aria-label="Seleccionar movimiento"
        />
      </td>
      <td>
        <strong>{formatDate(transaction.realDate)}</strong>
        <br />
        <span className="muted">Presupuesto: {formatDate(transaction.budgetDate)}</span>
      </td>
      <td>
        <strong>{transaction.description || "Sin descripción"}</strong>
        <div className="row-actions mt-2">
          <HumanBadges transaction={transaction} />
        </div>
      </td>
      <td>{accountName}</td>
      <td>{getCategoryDisplayName(category)}</td>
      <td>
        <StatusBadge
          tone={movementTypeTones[transaction.movementType]}
          label={labelOrValue(movementTypeLabels, transaction.movementType)}
        />
      </td>
      <td className="amount-cell">
        {formatMoney(transaction.amount, transaction.currency)}
      </td>
      <td>
        <StatusBadge
          tone={transactionStatusTones[transaction.status]}
          label={labelOrValue(transactionStatusLabels, transaction.status)}
        />
        {classificationStatus === "REVIEW" || classificationStatus === "NEEDS_CATEGORY" ? (
          <p className="compact-muted">
            {labelOrValue(classificationStatusLabels, classificationStatus)}
          </p>
        ) : null}
      </td>
      <td>
        <RowActions
          transaction={transaction}
          isUpdating={isUpdating}
          isDeleting={isDeleting}
          updatePending={updatePending}
          deletePending={deletePending}
          onToggleStatus={onToggleStatus}
          onDelete={onDelete}
        />
      </td>
    </tr>
  );
}

function AuditRow({
  transaction,
  selected,
  onToggleSelection,
  updatePending,
  deletePending,
  updatingTransactionId,
  deletingTransactionId,
  onToggleStatus,
  onDelete,
}: {
  transaction: MoneyTransaction;
  selected: boolean;
  onToggleSelection: (id: string) => void;
  updatePending: boolean;
  deletePending: boolean;
  updatingTransactionId?: string;
  deletingTransactionId?: string;
  onToggleStatus: (transaction: MoneyTransaction) => void;
  onDelete: (transaction: MoneyTransaction) => void;
}) {
  const classificationStatus = getDefaultClassificationStatus(transaction);
  const isUpdating = updatePending && updatingTransactionId === transaction.id;
  const isDeleting = deletePending && deletingTransactionId === transaction.id;

  return (
    <tr>
      <td>
        <input
          type="checkbox"
          checked={selected}
          onChange={() => onToggleSelection(transaction.id)}
          aria-label="Seleccionar movimiento"
        />
      </td>
      <td>{transaction.description || "Sin descripción"}</td>
      <td>{transaction.normalizedDescription || "-"}</td>
      <td>
        {labelOrValue(transactionOriginLabels, transaction.origin)}
        {transaction.source ? <p className="compact-muted">{transaction.source}</p> : null}
      </td>
      <td>
        <ShortValue label="Operación" value={transaction.sourceOperationId} />
        <ShortValue label="Hash" value={transaction.sourceHash} />
        <ShortValue label="Repetido" value={transaction.duplicateFingerprint} />
        <ShortValue label="Grupo" value={transaction.internalTransferGroupId} />
      </td>
      <td>{humanImpact(transaction)}</td>
      <td>
        <StatusBadge
          tone={classificationStatusTones[classificationStatus]}
          label={labelOrValue(classificationStatusLabels, classificationStatus)}
        />
        {transaction.classificationReason ? (
          <p className="compact-muted">{transaction.classificationReason}</p>
        ) : null}
      </td>
      <td>{transaction.importBatchId ? shortId(transaction.importBatchId) : "-"}</td>
      <td>
        <RowActions
          transaction={transaction}
          isUpdating={isUpdating}
          isDeleting={isDeleting}
          updatePending={updatePending}
          deletePending={deletePending}
          onToggleStatus={onToggleStatus}
          onDelete={onDelete}
        />
      </td>
    </tr>
  );
}

function HumanBadges({ transaction }: { transaction: MoneyTransaction }) {
  const classificationStatus = getDefaultClassificationStatus(transaction);
  const badges = [
    humanImpact(transaction),
    transaction.internalTransferGroupId ? "Transferencia entre cuentas" : null,
    classificationStatus === "REVIEW" ? "Revisar" : null,
    !transaction.categoryId ? "Sin categoría" : null,
    transaction.origin === "IMPORT" ? "Importado" : "Manual",
    transaction.status === "IGNORED" ? "Ignorado" : null,
    transaction.classificationReason?.toLowerCase().includes("duplicate")
      ? "Duplicado posible"
      : null,
  ].filter((badge): badge is string => Boolean(badge));

  return (
    <>
      {badges.map((badge) => (
        <StatusBadge key={badge} tone={badgeTone(String(badge))} label={String(badge)} />
      ))}
    </>
  );
}

function RowActions({
  transaction,
  isUpdating,
  isDeleting,
  updatePending,
  deletePending,
  onToggleStatus,
  onDelete,
}: {
  transaction: MoneyTransaction;
  isUpdating: boolean;
  isDeleting: boolean;
  updatePending: boolean;
  deletePending: boolean;
  onToggleStatus: (transaction: MoneyTransaction) => void;
  onDelete: (transaction: MoneyTransaction) => void;
}) {
  return (
    <div className="row-actions transactions-row-actions">
      <button
        type="button"
        className="boton-secundario"
        disabled={updatePending}
        onClick={() => onToggleStatus(transaction)}
      >
        {isUpdating
          ? "Actualizando..."
          : transaction.status === "CONFIRMED"
            ? "Pasar a pendiente"
            : "Confirmar"}
      </button>

      <button
        type="button"
        className="boton-danger"
        disabled={deletePending}
        onClick={() =>
          window.confirm(getTransactionRemovalConfirmMessage(transaction)) &&
          onDelete(transaction)
        }
      >
        {getTransactionRemovalLabel(transaction, isDeleting)}
      </button>
    </div>
  );
}

function ShortValue({ label, value }: { label: string; value?: string | null }) {
  if (!value) return null;

  return (
    <p className="compact-muted">
      {label}: {shortId(value)}
    </p>
  );
}

function humanImpact(transaction: MoneyTransaction) {
  if (
    transaction.movementType === "TRANSFER" ||
    transaction.balanceImpact === "INTERNAL_TRANSFER"
  ) {
    return "No afecta el mes";
  }

  if (transaction.status === "IGNORED" || transaction.balanceImpact === "IGNORED") {
    return "Ignorado";
  }

  if (!shouldCountTransactionInOperationalBalance(transaction)) {
    return "No afecta el mes";
  }

  if (transaction.movementType === "INCOME") return "Ingreso real";
  if (transaction.movementType === "EXPENSE") return "Gasto real";
  if (transaction.movementType === "SAVING") return "Ahorro";

  return "Revisar";
}

function badgeTone(label: string): "neutral" | "ok" | "watch" {
  if (label === "Gasto real" || label === "Revisar" || label === "Sin categoría") {
    return "watch";
  }
  if (label === "Ingreso real" || label === "Ahorro") return "ok";
  return "neutral";
}

function shortId(value: string) {
  return value.length <= 12 ? value : value.slice(0, 10);
}
