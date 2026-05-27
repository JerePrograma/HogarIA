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
  shouldCountTransactionInOperationalBalance,
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

                  <div className="row-actions mt-2">
                    <StatusBadge
                      tone={transaction.origin === "IMPORT" ? "watch" : "neutral"}
                      label={transaction.origin === "IMPORT" ? "Importado" : "Manual"}
                    />
                    {transaction.internalTransferGroupId ? (
                      <StatusBadge tone="watch" label="Transferencia interna" />
                    ) : null}
                    {!transaction.categoryId ? (
                      <StatusBadge tone="watch" label="Sin categoría" />
                    ) : null}
                    {!shouldCountTransactionInOperationalBalance(transaction) ? (
                      <StatusBadge tone="neutral" label="No impacta balance" />
                    ) : null}
                  </div>
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

                  {transaction.source ? (
                    <p className="compact-muted">Source: {transaction.source}</p>
                  ) : null}

                  {transaction.duplicateFingerprint ? (
                    <p className="compact-muted">
                      FP: {transaction.duplicateFingerprint.slice(0, 10)}
                    </p>
                  ) : null}

                  {transaction.normalizedDescription ? (
                    <p className="compact-muted">
                      Normalizada: {transaction.normalizedDescription}
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
                        window.confirm(
                          getTransactionRemovalConfirmMessage(transaction),
                        ) && onDelete(transaction)
                      }
                    >
                      {getTransactionRemovalLabel(transaction, isDeleting)}
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
