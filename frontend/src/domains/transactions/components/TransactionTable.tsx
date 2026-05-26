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
import { formatDate } from "../utils/transactionUtils";

interface Props {
  transactions: MoneyTransaction[];
  accountsById: Map<string, Account>;
  categoriesById: Map<string, Category>;
  updatePending: boolean;
  deletePending: boolean;
  updatingTransactionId?: string;
  deletingTransactionId?: string;
  onToggleStatus: (transaction: MoneyTransaction) => void;
  onDelete: (transactionId: string) => void;
}

export function TransactionTable({
  transactions,
  accountsById,
  categoriesById,
  updatePending,
  deletePending,
  updatingTransactionId,
  deletingTransactionId,
  onToggleStatus,
  onDelete,
}: Props) {
  return (
    <div className="tabla-ui transactions-table">
      <table className="table-compact">
        <thead>
          <tr>
            <th>Fecha</th>
            <th>Movimiento</th>
            <th>Cuenta</th>
            <th>Categoría</th>
            <th>Clasificación</th>
            <th className="amount-cell">Monto</th>
            <th>Estado</th>
            <th>Acciones</th>
          </tr>
        </thead>

        <tbody>
          {transactions.map((transaction) => {
            const accountName =
              accountsById.get(transaction.accountId)?.name ??
              "Cuenta no encontrada";

            const category = transaction.categoryId
              ? categoriesById.get(transaction.categoryId)
              : null;

            const classificationStatus =
              getDefaultClassificationStatus(transaction);

            const isUpdating =
              updatePending && updatingTransactionId === transaction.id;
            const isDeleting =
              deletePending && deletingTransactionId === transaction.id;

            return (
              <tr key={transaction.id}>
                <td>
                  <strong>{formatDate(transaction.realDate)}</strong>
                  <br />
                  <span className="muted">
                    Presupuesto: {formatDate(transaction.budgetDate)}
                  </span>
                </td>

                <td>
                  <StatusBadge
                    tone={movementTypeTones[transaction.movementType]}
                    label={labelOrValue(
                      movementTypeLabels,
                      transaction.movementType,
                    )}
                  />

                  <p className="compact-muted">
                    {transaction.description || "Sin descripción"}
                  </p>

                  {transaction.counterparty ? (
                    <p className="compact-muted">
                      Contraparte: {transaction.counterparty}
                    </p>
                  ) : null}
                </td>

                <td>{accountName}</td>

                <td>{getCategoryDisplayName(category)}</td>

                <td>
                  <StatusBadge
                    tone={classificationStatusTones[classificationStatus]}
                    label={labelOrValue(
                      classificationStatusLabels,
                      classificationStatus,
                    )}
                  />

                  {transaction.paymentChannel ? (
                    <p className="compact-muted">
                      {labelOrValue(
                        paymentChannelLabels,
                        transaction.paymentChannel,
                      )}
                    </p>
                  ) : null}

                  {transaction.classificationReason ? (
                    <p className="compact-muted">
                      {transaction.classificationReason}
                    </p>
                  ) : null}
                </td>

                <td className="amount-cell">
                  {formatMoney(transaction.amount, transaction.currency)}
                </td>

                <td>
                  <StatusBadge
                    tone={transactionStatusTones[transaction.status]}
                    label={labelOrValue(
                      transactionStatusLabels,
                      transaction.status,
                    )}
                  />
                </td>

                <td>
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
                        window.confirm("¿Eliminar este movimiento?") &&
                        onDelete(transaction.id)
                      }
                    >
                      {isDeleting ? "Eliminando..." : "Eliminar"}
                    </button>
                  </div>
                </td>
              </tr>
            );
          })}
        </tbody>
      </table>
    </div>
  );
}
