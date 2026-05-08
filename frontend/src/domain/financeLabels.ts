// src/domain/financeLabels.ts

import type {
  AccountType,
  BudgetComparisonStatus,
  CategoryScope,
  CategoryType,
  FinancialHealth,
  MovementType,
  ProfileType,
  TransactionOrigin,
  TransactionStatus,
} from './types';

export const profileTypeLabels: Record<ProfileType, string> = {
  PERSONAL: 'Personal',
  FAMILY: 'Familiar',
  BUSINESS: 'Negocio',
};

export const accountTypeLabels: Record<AccountType, string> = {
  CASH: 'Efectivo',
  BANK: 'Banco',
  CREDIT_CARD: 'Tarjeta de crédito',
  DEBIT_CARD: 'Tarjeta de débito',
  VIRTUAL_WALLET: 'Billetera virtual',
  BUSINESS: 'Cuenta de negocio',
};

export const categoryTypeLabels: Record<CategoryType, string> = {
  INCOME: 'Ingreso',
  FIXED_EXPENSE: 'Gasto fijo',
  VARIABLE_EXPENSE: 'Gasto variable',
  SAVING: 'Ahorro',
  DEBT: 'Deuda',
  INVESTMENT: 'Inversión',
};

export const categoryScopeLabels: Record<CategoryScope, string> = {
  PERSONAL: 'Personal',
  FAMILY: 'Familiar',
  BUSINESS: 'Negocio',
  GLOBAL: 'Global',
};

export const movementTypeLabels: Record<MovementType, string> = {
  INCOME: 'Ingreso',
  EXPENSE: 'Gasto',
  SAVING: 'Ahorro',
  TRANSFER: 'Transferencia',
  ADJUSTMENT: 'Ajuste',
};

export const transactionOriginLabels: Record<TransactionOrigin, string> = {
  MANUAL: 'Manual',
  IMPORT: 'Importado',
  RECURRENT: 'Recurrente',
  SYSTEM: 'Sistema',
};

export const transactionStatusLabels: Record<TransactionStatus, string> = {
  CONFIRMED: 'Confirmado',
  PENDING: 'Pendiente',
  IGNORED: 'Ignorado',
};

export const budgetComparisonStatusLabels: Record<BudgetComparisonStatus, string> = {
  OK: 'Correcto',
  WARNING: 'Advertencia',
  EXCEEDED: 'Excedido',
};

export const financialHealthLabels: Record<FinancialHealth, string> = {
  EXCELLENT: 'Excelente',
  HEALTHY: 'Saludable',
  WARNING: 'En observación',
  CRITICAL: 'Crítico',
};

export const monthLabels: Record<number, string> = {
  1: 'Enero',
  2: 'Febrero',
  3: 'Marzo',
  4: 'Abril',
  5: 'Mayo',
  6: 'Junio',
  7: 'Julio',
  8: 'Agosto',
  9: 'Septiembre',
  10: 'Octubre',
  11: 'Noviembre',
  12: 'Diciembre',
};

export function labelOrValue<T extends string>(
  labels: Record<T, string>,
  value: T | string | null | undefined,
): string {
  if (!value) return '-';

  return labels[value as T] ?? value;
}