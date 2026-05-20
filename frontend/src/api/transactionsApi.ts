import { http } from "./http";
import type {
  MoneyTransaction,
  MovementType,
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
}

export type TransactionUpdatePayload = Partial<
  Omit<TransactionCreatePayload, "profileId">
>;

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

export async function deleteTransaction(id: string): Promise<void> {
  await http.delete(`/transactions/${id}`);
}
