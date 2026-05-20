// src/domain/financeTones.ts

import type {
  BudgetComparisonStatus,
  FinancialHealth,
  FinancialRiskLevel,
  GoalStatus,
  MovementType,
  MonthlyPlanPriority,
  MonthlyPlanStatus,
  TransactionClassificationStatus,
  TransactionStatus,
} from "./types";

export type Tone = "ok" | "watch" | "risk" | "critical" | "neutral";

export const movementTypeTones: Record<MovementType, Tone> = {
  INCOME: "ok",
  EXPENSE: "critical",
  SAVING: "ok",
  TRANSFER: "neutral",
  ADJUSTMENT: "watch",
};

export const transactionStatusTones: Record<TransactionStatus, Tone> = {
  CONFIRMED: "ok",
  PENDING: "watch",
  IGNORED: "neutral",
};

export const classificationStatusTones: Record<
  TransactionClassificationStatus,
  Tone
> = {
  CLASSIFIED: "ok",
  NEEDS_CATEGORY: "watch",
  REVIEW: "risk",
  TECHNICAL: "neutral",
  IGNORED_BY_RULE: "neutral",
};

export const budgetComparisonStatusTones: Record<BudgetComparisonStatus, Tone> =
  {
    OK: "ok",
    WARNING: "watch",
    EXCEEDED: "critical",
  };

export const financialHealthTones: Record<FinancialHealth, Tone> = {
  EXCELLENT: "ok",
  HEALTHY: "ok",
  WARNING: "watch",
  CRITICAL: "critical",
};

export const financialRiskLevelTones: Record<FinancialRiskLevel, Tone> = {
  OK: "ok",
  WATCH: "watch",
  RISK: "risk",
  CRITICAL: "critical",
};

export const goalStatusTones: Record<GoalStatus, Tone> = {
  ACTIVE: "ok",
  PAUSED: "watch",
  COMPLETED: "ok",
  CANCELLED: "neutral",
};

export const monthlyPlanPriorityTones: Record<MonthlyPlanPriority, Tone> = {
  ESSENTIAL: "critical",
  IMPORTANT: "watch",
  OPTIONAL: "neutral",
};

export const monthlyPlanStatusTones: Record<MonthlyPlanStatus, Tone> = {
  DRAFT: "neutral",
  ESTIMATED: "watch",
  SCHEDULED: "watch",
  DUE: "critical",
  PAID: "ok",
  COLLECTED: "ok",
  CANCELLED: "neutral",
};

export const importStatusTones: Record<string, Tone> = {
  READY: "ok",
  IMPORTED: "ok",
  SKIPPED: "neutral",
  WARNING: "watch",
  ERROR: "critical",
  DUPLICATE: "neutral",
  DUPLICATE_EXACT: "neutral",
  POSSIBLE_INTERNAL_TRANSFER: "neutral",
  INTERNAL_TRANSFER_MATCHED: "neutral",
  NEEDS_CATEGORY: "watch",
};
