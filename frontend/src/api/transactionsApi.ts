import { http } from "./http";
import type {
  MoneyTransaction,
  MovementType,
  PaymentChannel,
  TransactionDeletionResponse,
  TransactionClassificationStatus,
  TransactionOrigin,
  TransactionStatus,
} from "../domain/types";

export interface TransactionCreatePayload {
  profileId: string;
  accountId: string;
  categoryId: string | null;
  movementType: MovementType;
  realDate: string;
  budgetDate: string;
  amount: number;
  currency: string;
  description?: string | null;
  origin?: TransactionOrigin;
  status?: TransactionStatus;

  source?: string | null;
  sourceOperationId?: string | null;
  sourceHash?: string | null;
  paymentChannel?: PaymentChannel | null;
  counterparty?: string | null;
  classificationStatus?: TransactionClassificationStatus | null;
  classificationReason?: string | null;
  importBatchId?: string | null;
  internalTransferGroupId?: string | null;
}

export type TransactionUpdatePayload = Partial<
  Omit<TransactionCreatePayload, "profileId">
> & {
  clearCategory?: boolean;
};

export async function listTransactions(
  profileId: string,
  year: number,
  month: number,
): Promise<MoneyTransaction[]> {
  const { data } = await http.get(`/profiles/${profileId}/transactions`, {
    params: { year, month },
  });

  return data;
}

export async function createTransaction(
  payload: TransactionCreatePayload,
): Promise<MoneyTransaction> {
  const { data } = await http.post("/transactions", payload);
  return data;
}

export async function getTransaction(id: string): Promise<MoneyTransaction> {
  const { data } = await http.get(`/transactions/${id}`);
  return data;
}

export async function updateTransaction(
  id: string,
  payload: TransactionUpdatePayload,
): Promise<MoneyTransaction> {
  const { data } = await http.put(`/transactions/${id}`, payload);
  return data;
}

export async function deleteTransaction(
  id: string,
): Promise<TransactionDeletionResponse> {
  const response = await http.delete<TransactionDeletionResponse | "">(
    `/transactions/${id}`,
  );

  if (response.status === 204 || !response.data) {
    return {
      transactionId: id,
      mode: "PHYSICAL_DELETE",
      code: "TRANSACTION_PHYSICALLY_DELETED",
      message: "Movimiento eliminado correctamente.",
      linkedItemsUpdated: 0,
      matchesDeleted: 0,
      systemConversionMatchesDeleted: 0,
      resultingStatus: null,
      resultingClassificationStatus: null,
    };
  }

  return response.data;
}
