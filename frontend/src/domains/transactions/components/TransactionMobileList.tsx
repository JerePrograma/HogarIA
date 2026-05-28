import { StatusBadge } from "../../../shared/ui/StatusBadge";
import {
  classificationStatusLabels,
  labelOrValue,
  movementTypeLabels,
  paymentChannelLabels,
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
} from "../../../domain/transactionRules";
import type {
  Account,
  Category,
  MoneyTransaction,
} from "../../../domain/types";
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
  updatePending: boolean;
  deletePending: boolean;
  deletingTransactionId?: string;
  onToggleStatus: (transaction: MoneyTransaction) => void;
  onDelete: (transaction: MoneyTransaction) => void;
}

export function TransactionMobileList({
  transactions,
  accountsById,
  categoriesById,
  viewMode,
  selectedTransactionIds,
  onToggleSelection,
  updatePending,
  deletePending,
  deletingTransactionId,
  onToggleStatus,
  onDelete,
}: Props) {
  return (
    <div className="transactions-mobile-list">
      {transactions.map((transaction) => {
        const accountName =
          accountsById.get(transaction.accountId)?.name ??
          "Cuenta no encontrada";

        const category = transaction.categoryId
          ? categoriesById.get(transaction.categoryId)
          : null;

        const classificationStatus =
          getDefaultClassificationStatus(transaction);
        const isDeleting =
          deletePending && deletingTransactionId === transaction.id;

        return (
          <article key={transaction.id} className="transactions-mobile-card">
            <header>
              <input
                type="checkbox"
                checked={selectedTransactionIds.includes(transaction.id)}
                onChange={() => onToggleSelection(transaction.id)}
                aria-label="Seleccionar movimiento"
              />
              <div>
                <strong>{transaction.description || "Sin descripción"}</strong>
                <p className="muted">
                  {formatDate(transaction.realDate)} · Presupuesto:{" "}
                  {formatDate(transaction.budgetDate)}
                </p>
              </div>

              <span className="transactions-mobile-amount">
                {formatMoney(transaction.amount, transaction.currency)}
              </span>
            </header>

            <div className="transactions-mobile-badges">
              <StatusBadge
                tone={movementTypeTones[transaction.movementType]}
                label={labelOrValue(
                  movementTypeLabels,
                  transaction.movementType,
                )}
              />

              <StatusBadge
                tone={transactionStatusTones[transaction.status]}
                label={labelOrValue(
                  transactionStatusLabels,
                  transaction.status,
                )}
              />

              <StatusBadge
                tone={classificationStatusTones[classificationStatus]}
                label={labelOrValue(
                  classificationStatusLabels,
                  classificationStatus,
                )}
              />
            </div>

            <dl className="transactions-mobile-details">
              <div>
                <dt>Cuenta</dt>
                <dd>{accountName}</dd>
              </div>

              <div>
                <dt>Categoría</dt>
                <dd>{getCategoryDisplayName(category)}</dd>
              </div>

              {transaction.paymentChannel ? (
                <div>
                  <dt>Canal</dt>
                  <dd>
                    {labelOrValue(
                      paymentChannelLabels,
                      transaction.paymentChannel,
                    )}
                  </dd>
                </div>
              ) : null}

              {transaction.classificationReason ? (
                <div>
                  <dt>Motivo</dt>
                  <dd>{transaction.classificationReason}</dd>
                </div>
              ) : null}

              {viewMode === "AUDIT" ? (
                <>
                  <div>
                    <dt>Descripción leída</dt>
                    <dd>{transaction.normalizedDescription ?? "-"}</dd>
                  </div>
                  <div>
                    <dt>Clave repetido</dt>
                    <dd>{transaction.duplicateFingerprint?.slice(0, 10) ?? "-"}</dd>
                  </div>
                  <div>
                    <dt>Grupo interno</dt>
                    <dd>{transaction.internalTransferGroupId?.slice(0, 10) ?? "-"}</dd>
                  </div>
                </>
              ) : null}
            </dl>

            <div className="row-actions">
              <button
                type="button"
                className="boton-secundario"
                disabled={updatePending}
                onClick={() => onToggleStatus(transaction)}
              >
                {transaction.status === "CONFIRMED"
                  ? "Pasar a pendiente"
                  : "Confirmar"}
              </button>

              <button
                type="button"
                className="boton-danger"
                disabled={deletePending}
                onClick={() =>
                  window.confirm(
                    getTransactionRemovalConfirmMessage(transaction),
                  ) && onDelete(transaction)
                }
              >
                {getTransactionRemovalLabel(transaction, isDeleting)}
              </button>
            </div>
          </article>
        );
      })}
    </div>
  );
}
