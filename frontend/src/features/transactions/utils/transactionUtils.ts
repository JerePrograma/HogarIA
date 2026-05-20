import type { MoneyTransaction } from "../../../domain/types";
import type { TransactionUpdatePayload } from "../../../api/transactionsApi";

export function getDefaultDate(year: number, month: number) {
  return `${year}-${String(month).padStart(2, "0")}-01`;
}

export function getPeriodDate(year: number, month: number) {
  return new Date(year, month - 1, 1);
}

export function shiftPeriod(year: number, month: number, delta: number) {
  const date = new Date(year, month - 1 + delta, 1);

  return {
    year: date.getFullYear(),
    month: date.getMonth() + 1,
  };
}

export function formatPeriodLabel(year: number, month: number) {
  return new Intl.DateTimeFormat("es-AR", {
    month: "long",
    year: "numeric",
  }).format(getPeriodDate(year, month));
}

export function formatDate(value: string | null | undefined) {
  if (!value) return "-";

  return new Intl.DateTimeFormat("es-AR", {
    day: "2-digit",
    month: "2-digit",
    year: "numeric",
  }).format(new Date(`${value}T00:00:00`));
}

export function normalizeSearch(value: string | number | null | undefined) {
  return String(value ?? "")
    .trim()
    .toLowerCase();
}

export function toTransactionUpdatePayload(
  transaction: MoneyTransaction,
): TransactionUpdatePayload {
  return {
    accountId: transaction.accountId,
    categoryId: transaction.categoryId,
    movementType: transaction.movementType,
    realDate: transaction.realDate,
    budgetDate: transaction.budgetDate,
    amount: transaction.amount,
    currency: transaction.currency,
    description: transaction.description ?? "",
    status: transaction.status === "CONFIRMED" ? "PENDING" : "CONFIRMED",
  };
}
