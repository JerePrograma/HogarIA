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

export type GoalType = 'OTHER' | 'EMERGENCY_FUND' | 'TRAVEL' | 'HOME' | 'INVESTMENT';
export type GoalStatus = 'ACTIVE' | 'COMPLETED' | 'PAUSED' | 'CANCELLED' | 'ARCHIVED';
export type HabitFrequency = 'DAILY' | 'WEEKLY' | 'MONTHLY';
export type InflationSource = 'MANUAL' | 'IMPORT' | 'SYSTEM';

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
  WARNING: 'Atención',
  EXCEEDED: 'Excedido',
};

export const financialHealthLabels: Record<FinancialHealth, string> = {
  EXCELLENT: 'Excelente',
  HEALTHY: 'Saludable',
  WARNING: 'En observación',
  CRITICAL: 'Crítico',
};

export const goalTypeLabels: Record<GoalType, string> = {
  OTHER: 'Otro',
  EMERGENCY_FUND: 'Fondo de emergencia',
  TRAVEL: 'Viaje',
  HOME: 'Vivienda',
  INVESTMENT: 'Inversión',
};

export const goalStatusLabels: Record<GoalStatus, string> = {
  ACTIVE: 'Activo',
  COMPLETED: 'Completado',
  PAUSED: 'Pausado',
  CANCELLED: 'Cancelado',
  ARCHIVED: 'Archivado',
};

export const habitFrequencyLabels: Record<HabitFrequency, string> = {
  DAILY: 'Diario',
  WEEKLY: 'Semanal',
  MONTHLY: 'Mensual',
};

export const inflationSourceLabels: Record<InflationSource, string> = {
  MANUAL: 'Manual',
  IMPORT: 'Importado',
  SYSTEM: 'Sistema',
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

export function labelOrMissing<T extends string | number>(
  labels: Partial<Record<T, string>>,
  value: T | string | number | null | undefined,
  fallback = 'Sin definir',
): string {
  if (value === null || value === undefined || value === '') return fallback;

  const mapped = labels[value as T];

  if (mapped) return mapped;

  return import.meta.env.DEV ? `Sin traducir: ${String(value)}` : fallback;
}

/**
 * Alias mantenido para compatibilidad con pantallas existentes.
 * Usar labelOrMissing en código nuevo.
 */
export const labelOrValue = labelOrMissing;