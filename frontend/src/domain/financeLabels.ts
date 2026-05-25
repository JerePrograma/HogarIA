// src/domain/financeLabels.ts

import type {
  AccountType,
  BudgetComparisonStatus,
  CategoryScope,
  CategoryType,
  FinancialHealth,
  FinancialRiskLevel,
  GoalStatus,
  GoalType,
  HabitFrequency,
  MonthlyPlanItemType,
  MonthlyPlanPriority,
  MonthlyPlanSource,
  MonthlyPlanStatus,
  MovementType,
  PaymentChannel,
  ProfileType,
  TransactionClassificationStatus,
  TransactionOrigin,
  TransactionStatus,
} from "./types";

// Backward-compatible re-export.
// Permite que componentes viejos sigan importando tones desde financeLabels
// mientras migrás gradualmente a financeTones.
export {
  budgetComparisonStatusTones,
  classificationStatusTones,
  financialHealthTones,
  financialRiskLevelTones,
  goalStatusTones,
  importStatusTones,
  movementTypeTones,
  monthlyPlanPriorityTones,
  monthlyPlanStatusTones,
  transactionStatusTones,
  type Tone,
} from "./financeTones";

export function labelOrValue<T extends string>(
  labels: Partial<Record<T, string>>,
  value: T | null | undefined,
) {
  if (!value) return "-";
  return labels[value] ?? value;
}

// ============================================================
// Core labels
// ============================================================

export const profileTypeLabels: Record<ProfileType, string> = {
  PERSONAL: "Personal",
  FAMILY: "Familiar",
  BUSINESS: "Negocio",
};

export const accountTypeLabels: Record<AccountType, string> = {
  CASH: "Efectivo",
  BANK: "Banco",
  CREDIT_CARD: "Tarjeta de crédito",
  DEBIT_CARD: "Tarjeta de débito",
  VIRTUAL_WALLET: "Billetera virtual",
  BUSINESS: "Cuenta de negocio",
};

export const categoryTypeLabels: Record<CategoryType, string> = {
  INCOME: "Ingreso",
  FIXED_EXPENSE: "Gasto fijo",
  VARIABLE_EXPENSE: "Gasto variable",
  SAVING: "Ahorro",
  DEBT: "Deuda",
  INVESTMENT: "Inversión",
};

export const categoryScopeLabels: Record<CategoryScope, string> = {
  PERSONAL: "Personal",
  FAMILY: "Familiar",
  BUSINESS: "Negocio",
  GLOBAL: "Global",
};

// ============================================================
// Transaction labels
// ============================================================

export const movementTypeLabels: Record<MovementType, string> = {
  INCOME: "Ingreso",
  EXPENSE: "Gasto",
  SAVING: "Ahorro",
  TRANSFER: "Transferencia",
  ADJUSTMENT: "Ajuste",
};

export const transactionStatusLabels: Record<TransactionStatus, string> = {
  CONFIRMED: "Confirmado",
  PENDING: "Pendiente",
  IGNORED: "Ignorado",
};

export const transactionOriginLabels: Record<TransactionOrigin, string> = {
  MANUAL: "Manual",
  IMPORT: "Importado",
  RECURRENT: "Recurrente",
  SYSTEM: "Sistema",
};

export const paymentChannelLabels: Record<PaymentChannel, string> = {
  UNKNOWN: "Canal no identificado",
  CASH: "Efectivo",
  BANK_TRANSFER: "Transferencia bancaria",
  DEBIN: "DEBIN",
  CUENTA_DNI: "Cuenta DNI",
  DEBIT_CARD: "Tarjeta débito",
  CREDIT_CARD: "Tarjeta crédito",
  MERCADO_PAGO: "Mercado Pago",
  MERCADO_CREDITO: "MercadoCrédito",
  INTERNAL_TRANSFER: "Transferencia interna",
  OTHER: "Otro",
};

export const classificationStatusLabels: Record<
  TransactionClassificationStatus,
  string
> = {
  CLASSIFIED: "Clasificado",
  NEEDS_CATEGORY: "Sin categoría",
  REVIEW: "Revisar",
  TECHNICAL: "Técnico",
  IGNORED_BY_RULE: "Ignorado por regla",
};

// ============================================================
// Budget / dashboard labels
// ============================================================

export const budgetComparisonStatusLabels: Record<
  BudgetComparisonStatus,
  string
> = {
  OK: "Correcto",
  WARNING: "Atención",
  EXCEEDED: "Excedido",
};

export const financialHealthLabels: Record<FinancialHealth, string> = {
  EXCELLENT: "Excelente",
  HEALTHY: "Saludable",
  WARNING: "Atención",
  CRITICAL: "Crítico",
};

export const financialRiskLevelLabels: Record<FinancialRiskLevel, string> = {
  OK: "Correcto",
  WATCH: "Atención",
  RISK: "Riesgo",
  CRITICAL: "Crítico",
};

// ============================================================
// Goals / habits labels
// ============================================================

export const goalTypeLabels: Record<GoalType, string> = {
  EMERGENCY_FUND: "Fondo de emergencia",
  DEBT_PAYOFF: "Cancelación de deuda",
  SAVING_TARGET: "Meta de ahorro",
  INVESTMENT: "Inversión",
  BUSINESS: "Negocio",
  TRAVEL: "Viaje",
  EDUCATION: "Educación",
  OTHER: "Otro",
};

export const goalStatusLabels: Record<GoalStatus, string> = {
  ACTIVE: "Activo",
  PAUSED: "Pausado",
  COMPLETED: "Completado",
  CANCELLED: "Cancelado",
};

export const habitFrequencyLabels: Record<HabitFrequency, string> = {
  DAILY: "Diario",
  WEEKLY: "Semanal",
  MONTHLY: "Mensual",
};

// ============================================================
// Import labels
// ============================================================

export const importBatchStatusLabels: Record<string, string> = {
  READY: "Listo",
  IMPORTED: "Importado",
  SKIPPED: "Omitido",
  WARNING: "Atención",
  ERROR: "Error",
};

export const importRowStatusLabels: Record<string, string> = {
  READY: "Listo",
  IMPORTED: "Importado",
  SKIPPED: "Omitido",
  WARNING: "Atención",
  ERROR: "Error",
  DUPLICATE: "Duplicado",
  DUPLICATE_EXACT: "Duplicado exacto",
  POSSIBLE_INTERNAL_TRANSFER: "Posible transferencia interna",
  INTERNAL_TRANSFER_MATCHED: "Transferencia interna",
  POSSIBLE_CROSS_SOURCE_DUPLICATE: "Posible duplicado cross-source",
  NEEDS_CATEGORY: "Requiere categoría",
};

export const importTargetEntityLabels: Record<string, string> = {
  CATEGORY: "Categoría",
  ACCOUNT: "Cuenta",
  INCOME: "Ingreso",
  EXPENSE: "Gasto",
  SAVING: "Ahorro",
  BUDGET: "Presupuesto",
  CARD: "Tarjeta",
  GOAL: "Objetivo",
  HABIT: "Hábito",
  HABIT_CHECKIN: "Control de hábito",
  INFLATION: "Inflación",
  UNKNOWN: "Sin clasificar",
};

// ============================================================
// Inflation labels
// ============================================================

export const inflationProjectionLabels: Record<"real" | "projected", string> = {
  real: "Real",
  projected: "Proyectado",
};

// ============================================================
// Monthly planning labels
// ============================================================

export const monthlyPlanTypeLabels: Record<MonthlyPlanItemType, string> = {
  INCOME: "Ingreso",
  EXPENSE: "Egreso",
  SAVING: "Ahorro",
  DEBT: "Deuda",
  TRANSFER: "Transferencia",
  RECOVERY: "Recupero",
  TODO: "Pendiente",
};

export const monthlyPlanPriorityLabels: Record<MonthlyPlanPriority, string> = {
  ESSENTIAL: "Esencial",
  IMPORTANT: "Importante",
  OPTIONAL: "Opcional",
};

export const monthlyPlanStatusLabels: Record<MonthlyPlanStatus, string> = {
  DRAFT: "Borrador",
  ESTIMATED: "Estimado",
  SCHEDULED: "Programado",
  DUE: "Por vencer",
  PAID: "Pagado",
  COLLECTED: "Cobrado",
  CANCELLED: "Cancelado",
};

export const monthlyPlanSourceLabels: Record<MonthlyPlanSource, string> = {
  MANUAL: "Manual",
  IMPORT: "Importado",
  QUICK_CAPTURE: "Captura rápida",
  SYSTEM: "Sistema",
};

// ============================================================
// Calendar labels
// ============================================================

export const monthLabels: Record<number, string> = {
  1: "Enero",
  2: "Febrero",
  3: "Marzo",
  4: "Abril",
  5: "Mayo",
  6: "Junio",
  7: "Julio",
  8: "Agosto",
  9: "Septiembre",
  10: "Octubre",
  11: "Noviembre",
  12: "Diciembre",
};
