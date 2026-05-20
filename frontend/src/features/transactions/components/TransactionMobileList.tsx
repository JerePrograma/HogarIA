import { StatusBadge } from "../../../components/ui/StatusBadge";
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
import { formatDate } from "../utils/transactionUtils";

interface Props {
  transactions: MoneyTransaction[];
  accountsById: Map<string, Account>;
  categoriesById: Map<string, Category>;
  updatePending: boolean;
  deletePending: boolean;
  onToggleStatus: (transaction: MoneyTransaction) => void;
  onDelete: (transactionId: string) => void;
}

export function TransactionMobileList({
  transactions,
  accountsById,
  categoriesById,
  updatePending,
  deletePending,
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

        return (
          <article key={transaction.id} className="transactions-mobile-card">
            <header>
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
                  window.confirm("¿Eliminar este movimiento?") &&
                  onDelete(transaction.id)
                }
              >
                Eliminar
              </button>
            </div>
          </article>
        );
      })}
    </div>
  );
}
