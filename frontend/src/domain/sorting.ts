import type {
  BudgetComparisonItem,
  FinancialRiskLevel,
  MoneyTransaction,
  MonthlyPlanItem,
  MonthlyPlanPriority,
} from "./types";

type NamedItem = {
  name?: string | null;
  title?: string | null;
  categoryName?: string | null;
};

type OperationalAlertLike =
  | string
  | {
      message?: string | null;
      title?: string | null;
      riskLevel?: FinancialRiskLevel | null;
      severity?: FinancialRiskLevel | null;
    };

const riskPriority: Record<FinancialRiskLevel, number> = {
  CRITICAL: 1,
  RISK: 2,
  WATCH: 3,
  OK: 4,
};

const planPriority: Record<MonthlyPlanPriority, number> = {
  ESSENTIAL: 1,
  IMPORTANT: 2,
  OPTIONAL: 3,
};

const budgetStatusPriority: Record<string, number> = {
  EXCEEDED: 1,
  WARNING: 2,
  OK: 3,
};

export function sortByNameDesc<T extends NamedItem>(items: T[]): T[] {
  return [...items].sort(compareByNameDesc);
}

export function compareByNameDesc<T extends NamedItem>(a: T, b: T): number {
  return getDisplayName(b).localeCompare(getDisplayName(a), "es", {
    sensitivity: "base",
  });
}

export function sortTransactionsForLedger(
  transactions: MoneyTransaction[],
): MoneyTransaction[] {
  return [...transactions].sort(compareTransactionsForLedger);
}

export function compareTransactionsForLedger(
  a: MoneyTransaction,
  b: MoneyTransaction,
): number {
  const byRealDate = compareDateDesc(a.realDate, b.realDate);
  if (byRealDate !== 0) return byRealDate;

  const byBudgetDate = compareDateDesc(a.budgetDate, b.budgetDate);
  if (byBudgetDate !== 0) return byBudgetDate;

  const byAmount = Number(b.amount ?? 0) - Number(a.amount ?? 0);
  if (byAmount !== 0) return byAmount;

  return (a.description ?? "").localeCompare(b.description ?? "", "es", {
    sensitivity: "base",
  });
}

export function sortOperationalAlerts<T extends OperationalAlertLike>(
  alerts: T[],
): T[] {
  return [...alerts].sort((a, b) => {
    const byRisk = getAlertPriority(a) - getAlertPriority(b);
    if (byRisk !== 0) return byRisk;

    return getAlertText(a).localeCompare(getAlertText(b), "es", {
      sensitivity: "base",
    });
  });
}

export function sortPlanningItemsByOperationalPriority(
  items: MonthlyPlanItem[],
): MonthlyPlanItem[] {
  return [...items].sort((a, b) => {
    const byBlocker = getPlanningBlockerPriority(a) - getPlanningBlockerPriority(b);
    if (byBlocker !== 0) return byBlocker;

    const byDueDate = (a.expectedDate ?? "9999-12-31").localeCompare(
      b.expectedDate ?? "9999-12-31",
    );
    if (byDueDate !== 0) return byDueDate;

    const byPriority =
      (planPriority[a.priority] ?? 99) - (planPriority[b.priority] ?? 99);
    if (byPriority !== 0) return byPriority;

    return a.title.localeCompare(b.title, "es", { sensitivity: "base" });
  });
}

export function sortBudgetComparisonsByRisk(
  items: BudgetComparisonItem[],
): BudgetComparisonItem[] {
  return [...items].sort((a, b) => {
    const byStatus =
      (budgetStatusPriority[a.status] ?? 99) -
      (budgetStatusPriority[b.status] ?? 99);
    if (byStatus !== 0) return byStatus;

    const byDifference = Math.abs(b.difference ?? 0) - Math.abs(a.difference ?? 0);
    if (byDifference !== 0) return byDifference;

    return a.categoryName.localeCompare(b.categoryName, "es", {
      sensitivity: "base",
    });
  });
}

function compareDateDesc(a?: string | null, b?: string | null): number {
  return (b ?? "").localeCompare(a ?? "");
}

function getDisplayName(item: NamedItem): string {
  return item.name ?? item.title ?? item.categoryName ?? "";
}

function getAlertPriority(alert: OperationalAlertLike): number {
  if (typeof alert === "string") {
    return inferRiskFromText(alert);
  }

  const level = alert.riskLevel ?? alert.severity;
  return level ? riskPriority[level] : inferRiskFromText(getAlertText(alert));
}

function getAlertText(alert: OperationalAlertLike): string {
  if (typeof alert === "string") return alert;
  return alert.message ?? alert.title ?? "";
}

function inferRiskFromText(text: string): number {
  const normalized = text.toLocaleLowerCase("es-AR");

  if (
    normalized.includes("crítico") ||
    normalized.includes("negativo") ||
    normalized.includes("superan")
  ) {
    return riskPriority.CRITICAL;
  }

  if (
    normalized.includes("riesgo") ||
    normalized.includes("excedido") ||
    normalized.includes("desvío")
  ) {
    return riskPriority.RISK;
  }

  if (
    normalized.includes("atención") ||
    normalized.includes("pendiente") ||
    normalized.includes("revis")
  ) {
    return riskPriority.WATCH;
  }

  return riskPriority.OK;
}

function getPlanningBlockerPriority(item: MonthlyPlanItem): number {
  if (item.status === "CANCELLED") return 90;
  if (item.transactionId) return 80;
  if (item.amount == null && item.minAmount == null && item.maxAmount == null) {
    return 1;
  }
  if (!item.accountId || !item.categoryId) return 2;
  if (isDueSoon(item.expectedDate)) return 3;
  if (item.status === "DUE") return 4;
  return 10;
}

function isDueSoon(value: string | null | undefined): boolean {
  if (!value) return false;

  const [year, month, day] = value.split("-").map(Number);
  if (!year || !month || !day) return false;

  const date = new Date(year, month - 1, day);
  const today = new Date();
  const start = new Date(today.getFullYear(), today.getMonth(), today.getDate());
  const limit = new Date(start);
  limit.setDate(limit.getDate() + 7);

  return date >= start && date <= limit;
}
