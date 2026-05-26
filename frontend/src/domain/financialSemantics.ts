import type {
  Category,
  FinancialRiskLevel,
  MoneyTransaction,
  MonthlyPlanItem,
  MonthlyPlanItemType,
} from "./types";
import {
  getDefaultClassificationStatus,
  shouldCountTransactionInOperationalBalance,
} from "./transactionRules";
import { sortOperationalAlerts } from "./sorting";

export type FinancialExecutionStatus =
  | "OK"
  | "WARNING"
  | "EXCEEDED"
  | "CRITICAL";

export interface RealConfirmedSummary {
  confirmedIncome: number;
  confirmedExpenses: number;
  confirmedSavings: number;
  operationalBalance: number;
  confirmedCount: number;
  pendingCount: number;
  ignoredCount: number;
  withoutCategoryCount: number;
  reviewCount: number;
  technicalCount: number;
  transferCount: number;
  adjustmentCount: number;
  nonOperationalCount: number;
  ignoredAmount: number;
  transfersAmount: number;
  adjustmentsAmount: number;
  technicalAmount: number;
  nonOperationalAmount: number;
}

export interface PlanExecutionCategorySummary {
  key: string;
  categoryId: string | null;
  categoryName: string;
  plannedAmount: number;
  realConfirmedAmount: number;
  pendingPlannedAmount: number;
  realUnplannedAmount: number;
  difference: number;
  executedPercent: number;
  status: FinancialExecutionStatus;
  plannedCount: number;
  realCount: number;
}

export interface RealVsPlannedSummary {
  totalPlanned: number;
  totalRealConfirmed: number;
  totalDifference: number;
  totalExecutedPercent: number;
  pendingPlannedAmount: number;
  realUnplannedAmount: number;
  status: FinancialExecutionStatus;
  categories: PlanExecutionCategorySummary[];
}

export interface ClosingProjection {
  realAccumulated: number;
  pendingPlannedNet: number;
  estimatedClosing: number;
  plannedNet: number;
  estimatedDifferenceVsPlan: number;
}

export interface FinancialAlert {
  title: string;
  message: string;
  riskLevel: FinancialRiskLevel;
}

const emptyRealConfirmedSummary: RealConfirmedSummary = {
  confirmedIncome: 0,
  confirmedExpenses: 0,
  confirmedSavings: 0,
  operationalBalance: 0,
  confirmedCount: 0,
  pendingCount: 0,
  ignoredCount: 0,
  withoutCategoryCount: 0,
  reviewCount: 0,
  technicalCount: 0,
  transferCount: 0,
  adjustmentCount: 0,
  nonOperationalCount: 0,
  ignoredAmount: 0,
  transfersAmount: 0,
  adjustmentsAmount: 0,
  technicalAmount: 0,
  nonOperationalAmount: 0,
};

export function buildRealConfirmedSummary(
  transactions: MoneyTransaction[],
): RealConfirmedSummary {
  return transactions.reduce<RealConfirmedSummary>(
    (acc, transaction) => {
      const amount = toFiniteNumber(transaction.amount);
      const classificationStatus = getDefaultClassificationStatus(transaction);

      if (transaction.status === "CONFIRMED") {
        acc.confirmedCount += 1;
      }

      if (transaction.status === "PENDING") {
        acc.pendingCount += 1;
      }

      if (transaction.status === "IGNORED") {
        acc.ignoredCount += 1;
        acc.ignoredAmount += amount;
      }

      if (!transaction.categoryId || classificationStatus === "NEEDS_CATEGORY") {
        acc.withoutCategoryCount += 1;
      }

      if (classificationStatus === "REVIEW") {
        acc.reviewCount += 1;
      }

      if (classificationStatus === "TECHNICAL") {
        acc.technicalCount += 1;
        if (transaction.status === "CONFIRMED") acc.technicalAmount += amount;
      }

      if (transaction.movementType === "TRANSFER") {
        acc.transferCount += 1;
        if (transaction.status === "CONFIRMED") acc.transfersAmount += amount;
      }

      if (transaction.movementType === "ADJUSTMENT") {
        acc.adjustmentCount += 1;
        if (transaction.status === "CONFIRMED") acc.adjustmentsAmount += amount;
      }

      if (transaction.status !== "CONFIRMED") {
        return acc;
      }

      if (!shouldCountTransactionInOperationalBalance(transaction)) {
        acc.nonOperationalCount += 1;
        acc.nonOperationalAmount += amount;
        return acc;
      }

      if (transaction.movementType === "INCOME") {
        acc.confirmedIncome += amount;
        acc.operationalBalance += amount;
      }

      if (transaction.movementType === "EXPENSE") {
        acc.confirmedExpenses += amount;
        acc.operationalBalance -= amount;
      }

      if (transaction.movementType === "SAVING") {
        acc.confirmedSavings += amount;
        acc.operationalBalance -= amount;
      }

      return acc;
    },
    { ...emptyRealConfirmedSummary },
  );
}

export function buildRealVsPlannedSummary(
  transactions: MoneyTransaction[],
  planningItems: MonthlyPlanItem[],
  categories: Category[] = [],
): RealVsPlannedSummary {
  const categoryById = new Map(categories.map((category) => [category.id, category]));

  const comparablePlanningItems = planningItems.filter((item) =>
    shouldUsePlanItemForExecutionComparison(item),
  );

  const convertedTransactionIds = new Set(
    comparablePlanningItems
      .map((item) => item.transactionId)
      .filter((id): id is string => Boolean(id)),
  );

  const rows = new Map<string, PlanExecutionCategorySummary>();

  for (const item of comparablePlanningItems) {
    const row = getOrCreateCategoryRow(
      rows,
      item.categoryId ?? null,
      categoryById,
    );

    const amount = getPlannedComparableAmount(item);

    row.plannedAmount += amount;
    row.plannedCount += 1;

    if (!item.transactionId && isPendingPlanItem(item)) {
      row.pendingPlannedAmount += amount;
    }
  }

  for (const transaction of transactions) {
    if (!shouldUseTransactionForExecutionComparison(transaction)) {
      continue;
    }

    const row = getOrCreateCategoryRow(
      rows,
      transaction.categoryId ?? null,
      categoryById,
    );

    const amount = toFiniteNumber(transaction.amount);

    row.realConfirmedAmount += amount;
    row.realCount += 1;

    if (!convertedTransactionIds.has(transaction.id)) {
      row.realUnplannedAmount += amount;
    }
  }

  const categoriesSummary = [...rows.values()]
    .map((row) => finalizeCategoryRow(row))
    .sort((a, b) => {
      const byStatus =
        getExecutionStatusPriority(a.status) -
        getExecutionStatusPriority(b.status);

      if (byStatus !== 0) return byStatus;

      const byDifference = Math.abs(b.difference) - Math.abs(a.difference);
      if (byDifference !== 0) return byDifference;

      return a.categoryName.localeCompare(b.categoryName, "es", {
        sensitivity: "base",
      });
    });

  const totalPlanned = sumValues(
    categoriesSummary.map((item) => item.plannedAmount),
  );

  const totalRealConfirmed = sumValues(
    categoriesSummary.map((item) => item.realConfirmedAmount),
  );

  const pendingPlannedAmount = sumValues(
    categoriesSummary.map((item) => item.pendingPlannedAmount),
  );

  const realUnplannedAmount = sumValues(
    categoriesSummary.map((item) => item.realUnplannedAmount),
  );

  const totalDifference = totalRealConfirmed - totalPlanned;

  const totalExecutedPercent =
    totalPlanned > 0 ? (totalRealConfirmed * 100) / totalPlanned : 0;

  return {
    totalPlanned,
    totalRealConfirmed,
    totalDifference,
    totalExecutedPercent,
    pendingPlannedAmount,
    realUnplannedAmount,
    status: resolveExecutionStatus(totalPlanned, totalRealConfirmed),
    categories: categoriesSummary,
  };
}

export function buildClosingProjection(
  transactions: MoneyTransaction[],
  planningItems: MonthlyPlanItem[],
): ClosingProjection {
  const realSummary = buildRealConfirmedSummary(transactions);
  const pendingPlannedNet = planningItems
    .filter((item) => shouldUsePlanItemForFinancialProjection(item))
    .filter((item) => !item.transactionId && isPendingPlanItem(item))
    .reduce((total, item) => total + getSignedPlannedNet(item), 0);

  const plannedNet = planningItems
    .filter((item) => shouldUsePlanItemForFinancialProjection(item))
    .reduce((total, item) => total + getSignedPlannedNet(item), 0);

  const estimatedClosing = realSummary.operationalBalance + pendingPlannedNet;

  return {
    realAccumulated: realSummary.operationalBalance,
    pendingPlannedNet,
    estimatedClosing,
    plannedNet,
    estimatedDifferenceVsPlan: estimatedClosing - plannedNet,
  };
}

export function buildFinancialAlerts(
  realSummary: RealConfirmedSummary,
  realVsPlanned: RealVsPlannedSummary,
  projection: ClosingProjection,
): FinancialAlert[] {
  const alerts: FinancialAlert[] = [];

  if (realSummary.withoutCategoryCount > 0) {
    alerts.push({
      title: "Gastos sin categoría",
      message: `${realSummary.withoutCategoryCount} movimiento${realSummary.withoutCategoryCount === 1 ? "" : "s"} necesita${realSummary.withoutCategoryCount === 1 ? "" : "n"} categoría para mejorar la lectura.`,
      riskLevel: "WATCH",
    });
  }

  if (realSummary.pendingCount > 0) {
    alerts.push({
      title: "Movimientos pendientes",
      message: `${realSummary.pendingCount} movimiento${realSummary.pendingCount === 1 ? "" : "s"} todavía no impacta${realSummary.pendingCount === 1 ? "" : "n"} en el resultado real confirmado.`,
      riskLevel: "WATCH",
    });
  }

  if (realVsPlanned.realUnplannedAmount > 0) {
    alerts.push({
      title: "Real no planificado",
      message: `Hay movimientos confirmados por fuera del plan por ${formatPlainAmount(realVsPlanned.realUnplannedAmount)}.`,
      riskLevel: "RISK",
    });
  }

  if (realVsPlanned.status === "EXCEEDED" || realVsPlanned.status === "CRITICAL") {
    alerts.push({
      title: "Desvío contra plan",
      message: `La ejecución real difiere del plan por ${formatPlainAmount(Math.abs(realVsPlanned.totalDifference))}.`,
      riskLevel: realVsPlanned.status === "CRITICAL" ? "CRITICAL" : "RISK",
    });
  }

  if (projection.estimatedClosing < 0) {
    alerts.push({
      title: "Proyección de cierre negativa",
      message: "El cierre estimado queda por debajo de cero si se cumplen los pendientes.",
      riskLevel: "CRITICAL",
    });
  }

  return sortOperationalAlerts(alerts);
}

export function getFinancialExecutionLabel(status: FinancialExecutionStatus) {
  const labels: Record<FinancialExecutionStatus, string> = {
    OK: "Correcto",
    WARNING: "Atención",
    EXCEEDED: "Excedido",
    CRITICAL: "Crítico",
  };

  return labels[status];
}

export function getFinancialExecutionRiskLevel(
  status: FinancialExecutionStatus,
): FinancialRiskLevel {
  if (status === "CRITICAL") return "CRITICAL";
  if (status === "EXCEEDED") return "RISK";
  if (status === "WARNING") return "WATCH";
  return "OK";
}

function getOrCreateCategoryRow(
  rows: Map<string, PlanExecutionCategorySummary>,
  categoryId: string | null,
  categoryById: Map<string, Category>,
): PlanExecutionCategorySummary {
  const key = categoryId ?? "SIN_CATEGORIA";
  const existing = rows.get(key);
  if (existing) return existing;

  const category = categoryId ? categoryById.get(categoryId) : null;
  const row: PlanExecutionCategorySummary = {
    key,
    categoryId,
    categoryName: category?.name ?? "Sin categoría",
    plannedAmount: 0,
    realConfirmedAmount: 0,
    pendingPlannedAmount: 0,
    realUnplannedAmount: 0,
    difference: 0,
    executedPercent: 0,
    status: "OK",
    plannedCount: 0,
    realCount: 0,
  };

  rows.set(key, row);
  return row;
}

function finalizeCategoryRow(
  row: PlanExecutionCategorySummary,
): PlanExecutionCategorySummary {
  const difference = row.realConfirmedAmount - row.plannedAmount;

  return {
    ...row,
    difference,
    executedPercent:
      row.plannedAmount > 0 ? (row.realConfirmedAmount * 100) / row.plannedAmount : 0,
    status: resolveExecutionStatus(row.plannedAmount, row.realConfirmedAmount),
  };
}

function resolveExecutionStatus(
  plannedAmount: number,
  realAmount: number,
): FinancialExecutionStatus {
  if (plannedAmount <= 0 && realAmount > 0) return "EXCEEDED";
  if (plannedAmount <= 0) return "OK";

  const percent = (realAmount * 100) / plannedAmount;

  if (percent > 120) return "CRITICAL";
  if (percent > 100) return "EXCEEDED";
  if (percent >= 85) return "WARNING";

  return "OK";
}

function getExecutionStatusPriority(status: FinancialExecutionStatus): number {
  if (status === "CRITICAL") return 1;
  if (status === "EXCEEDED") return 2;
  if (status === "WARNING") return 3;
  return 4;
}

function shouldUsePlanItemForFinancialProjection(item: MonthlyPlanItem): boolean {
  return item.status !== "CANCELLED" && item.type !== "TODO";
}

function isPendingPlanItem(item: MonthlyPlanItem): boolean {
  return ["DRAFT", "ESTIMATED", "SCHEDULED", "DUE"].includes(item.status);
}

function getSignedPlannedNet(item: MonthlyPlanItem): number {
  const amount = getPlannedComparableAmount(item);
  return isIncomingPlanType(item.type) ? amount : -amount;
}

function isIncomingPlanType(type: MonthlyPlanItemType): boolean {
  return type === "INCOME" || type === "RECOVERY";
}

function getPlannedComparableAmount(item: MonthlyPlanItem): number {
  const rawValue =
    item.netMax ??
    item.amount ??
    item.maxAmount ??
    item.minAmount ??
    item.grossMax ??
    item.grossMin ??
    0;

  return Math.abs(toFiniteNumber(rawValue));
}

function toFiniteNumber(value: number | null | undefined): number {
  const numericValue = Number(value ?? 0);
  return Number.isFinite(numericValue) ? numericValue : 0;
}

function sumValues(values: number[]): number {
  return values.reduce((total, value) => total + value, 0);
}

function formatPlainAmount(value: number): string {
  return new Intl.NumberFormat("es-AR", {
    style: "currency",
    currency: "ARS",
    maximumFractionDigits: 0,
  }).format(value);
}

function shouldUsePlanItemForExecutionComparison(
  item: MonthlyPlanItem,
): boolean {
  if (!shouldUsePlanItemForFinancialProjection(item)) return false;

  return isOutflowPlanItemType(item.type);
}

function shouldUseTransactionForExecutionComparison(
  transaction: MoneyTransaction,
): boolean {
  if (transaction.status !== "CONFIRMED") return false;
  if (!shouldCountTransactionInOperationalBalance(transaction)) return false;

  return isOutflowMovementType(transaction.movementType);
}

function isOutflowPlanItemType(type: MonthlyPlanItemType): boolean {
  return type === "EXPENSE" || type === "SAVING" || type === "DEBT";
}

function isOutflowMovementType(
  movementType: MoneyTransaction["movementType"],
): boolean {
  return movementType === "EXPENSE" || movementType === "SAVING";
}