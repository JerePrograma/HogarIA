import { formatMoney } from "../../domain/formatters";
import type {
  MonthlyPlanItem,
  MonthlyPlanItemType,
  MonthlyPlanPriority,
  MonthlyPlanReconciliationSummary,
  MonthlyPlanStatus,
  PlanExecutionStatus,
  PlanItemReconciliation,
  QuickCapturePreviewResponse,
} from "../../domain/types";

export type PlanItemAmountLike = Pick<
  MonthlyPlanItem,
  "amount" | "minAmount" | "maxAmount"
>;
export type TableFilterKey =
  | "ALL"
  | "UNPRICED"
  | "MISSING_CLASSIFICATION"
  | "READY_TO_CONVERT"
  | "DUE_NEXT_7_DAYS"
  | "PARTIALLY_EXECUTED"
  | "OVER_EXECUTED"
  | "NOT_EXECUTED"
  | "UNPLANNED_MOVEMENTS"
  | "SUGGESTED_MATCHES";

export type StatusFilterKey = "ALL" | "PENDING" | "DONE" | "CANCELLED";
export type PlanItemSortKey = "DATE" | "PRIORITY" | "AMOUNT";

export type PlanItemNextAction =
  | "COMPLETE_AMOUNT"
  | "PREPARE_CONVERSION"
  | "CONVERT"
  | "MARK_COLLECTED"
  | "MARK_PAID"
  | "EDIT";

const PENDING_STATUSES: MonthlyPlanStatus[] = [
  "DRAFT",
  "ESTIMATED",
  "SCHEDULED",
  "DUE",
];
const DONE_STATUSES: MonthlyPlanStatus[] = ["PAID", "COLLECTED"];
const MONEY_OUT_TYPES: MonthlyPlanItemType[] = [
  "EXPENSE",
  "DEBT",
  "SAVING",
  "TRANSFER",
];
const MONEY_IN_TYPES: MonthlyPlanItemType[] = ["INCOME", "RECOVERY"];

const priorityOrder: Record<MonthlyPlanPriority, number> = {
  ESSENTIAL: 1,
  IMPORTANT: 2,
  OPTIONAL: 3,
};

export function formatPlanAmount(item: PlanItemAmountLike): string {
  if (item.amount != null) {
    return formatMoney(item.amount);
  }

  const range = normalizeAmountRange(item);

  if (range) {
    return range.min === range.max
      ? formatMoney(range.min)
      : `${formatMoney(range.min)} – ${formatMoney(range.max)}`;
  }

  return "Sin monto";
}

export function formatPlanRecovery(
  item: Pick<
    MonthlyPlanItem,
    "expectedRecoveryPercent" | "expectedRecoveryAmount"
  >,
): string {
  if (item.expectedRecoveryPercent != null) {
    return `${item.expectedRecoveryPercent}%`;
  }

  if (item.expectedRecoveryAmount != null) {
    return formatMoney(item.expectedRecoveryAmount);
  }

  return "Sin recupero";
}

export function formatPlanNet(
  item: Pick<MonthlyPlanItem, "netMin" | "netMax">,
): string {
  if (item.netMin == null && item.netMax == null) {
    return "Sin monto";
  }

  const min = item.netMin ?? item.netMax;
  const max = item.netMax ?? item.netMin;

  if (min == null || max == null) {
    return "Sin monto";
  }

  return min === max
    ? formatMoney(min)
    : `${formatMoney(min)} – ${formatMoney(max)}`;
}

export function normalizeAmountRange(
  item: PlanItemAmountLike,
): { min: number; max: number } | null {
  if (item.minAmount == null && item.maxAmount == null) {
    return null;
  }

  const min = item.minAmount ?? item.maxAmount;
  const max = item.maxAmount ?? item.minAmount;

  if (min == null || max == null) {
    return null;
  }

  return { min, max };
}

export function hasExactAmount(item: PlanItemAmountLike): boolean {
  return (
    item.amount != null ||
    (item.minAmount != null &&
      item.maxAmount != null &&
      item.minAmount === item.maxAmount)
  );
}

export function isPendingPlanItem(
  item: Pick<MonthlyPlanItem, "status">,
): boolean {
  return PENDING_STATUSES.includes(item.status);
}

export function isDonePlanItem(item: Pick<MonthlyPlanItem, "status">): boolean {
  return DONE_STATUSES.includes(item.status);
}

export function isCancelledPlanItem(
  item: Pick<MonthlyPlanItem, "status">,
): boolean {
  return item.status === "CANCELLED";
}

export function isConvertedPlanItem(
  item: Pick<MonthlyPlanItem, "transactionId">,
): boolean {
  return Boolean(item.transactionId);
}

export function isUnpricedPlanItem(
  item: Pick<
    MonthlyPlanItem,
    "amount" | "minAmount" | "maxAmount" | "status" | "transactionId"
  >,
): boolean {
  return (
    !isCancelledPlanItem(item) &&
    !isConvertedPlanItem(item) &&
    item.amount == null &&
    item.minAmount == null &&
    item.maxAmount == null
  );
}

export function isMissingClassificationPlanItem(
  item: Pick<
    MonthlyPlanItem,
    "accountId" | "categoryId" | "status" | "transactionId"
  >,
): boolean {
  return (
    !isCancelledPlanItem(item) &&
    !isConvertedPlanItem(item) &&
    (!item.accountId || !item.categoryId)
  );
}

export function canConvertPlanItem(item: MonthlyPlanItem): boolean {
  return (
    hasExactAmount(item) &&
    Boolean(item.accountId) &&
    Boolean(item.categoryId) &&
    item.type !== "TODO" &&
    !isCancelledPlanItem(item) &&
    !isConvertedPlanItem(item)
  );
}

export function getPlanItemNextAction(
  item: MonthlyPlanItem,
): PlanItemNextAction {
  if (isUnpricedPlanItem(item)) {
    return "COMPLETE_AMOUNT";
  }

  if (canConvertPlanItem(item)) {
    return "CONVERT";
  }

  if (
    MONEY_IN_TYPES.includes(item.type) &&
    !["COLLECTED", "CANCELLED"].includes(item.status)
  ) {
    return "MARK_COLLECTED";
  }

  if (
    MONEY_OUT_TYPES.includes(item.type) &&
    !["PAID", "CANCELLED"].includes(item.status)
  ) {
    return "MARK_PAID";
  }

  if (isMissingClassificationPlanItem(item) || item.amount == null) {
    return "PREPARE_CONVERSION";
  }

  return "EDIT";
}

export function getPlanItemMissingLabels(item: MonthlyPlanItem): string[] {
  if (isCancelledPlanItem(item)) {
    return ["Cancelado"];
  }

  if (isConvertedPlanItem(item)) {
    return ["Convertido"];
  }

  const labels: string[] = [];

  if (isUnpricedPlanItem(item)) {
    labels.push("Sin monto");
  }

  if (!item.accountId) {
    labels.push("Sin cuenta");
  }

  if (!item.categoryId) {
    labels.push("Sin categoría");
  }

  if (labels.length === 0 && canConvertPlanItem(item)) {
    labels.push("Listo para convertir");
  }

  return labels.length > 0 ? labels : ["Sin acciones"];
}

export function getPlanItemCompletionScore(item: MonthlyPlanItem): number {
  if (isCancelledPlanItem(item) || isConvertedPlanItem(item)) {
    return 100;
  }

  const checks = [
    Boolean(item.title.trim()),
    Boolean(item.expectedDate),
    !isUnpricedPlanItem(item),
    Boolean(item.accountId),
    Boolean(item.categoryId),
  ];

  return Math.round((checks.filter(Boolean).length / checks.length) * 100);
}

export function matchesExternalFilter(
  item: MonthlyPlanItem,
  filter: TableFilterKey = "ALL",
  reconciliationByItemId?: Map<string, PlanItemReconciliation>,
  suggestedItemIds?: Set<string>,
): boolean {
  if (filter === "ALL") {
    return true;
  }

  const itemReconciliation = reconciliationByItemId?.get(item.id);

  if (filter === "PARTIALLY_EXECUTED") {
    return itemReconciliation?.executionStatus === "PARTIALLY_EXECUTED";
  }

  if (filter === "OVER_EXECUTED") {
    return itemReconciliation?.executionStatus === "OVER_EXECUTED";
  }

  if (filter === "NOT_EXECUTED") {
    return itemReconciliation?.executionStatus === "NOT_EXECUTED";
  }

  if (filter === "SUGGESTED_MATCHES") {
    return suggestedItemIds?.has(item.id) ?? false;
  }

  if (filter === "UNPLANNED_MOVEMENTS") {
    return false;
  }

  if (filter === "UNPRICED") {
    return isUnpricedPlanItem(item);
  }

  if (filter === "MISSING_CLASSIFICATION") {
    return isMissingClassificationPlanItem(item);
  }

  if (filter === "READY_TO_CONVERT") {
    return canConvertPlanItem(item);
  }

  if (filter === "DUE_NEXT_7_DAYS") {
    return (
      !isCancelledPlanItem(item) &&
      !isConvertedPlanItem(item) &&
      isDueNextDays(item.expectedDate, 7)
    );
  }

  return true;
}

export function matchesStatusFilter(
  item: MonthlyPlanItem,
  filter: StatusFilterKey,
): boolean {
  if (filter === "ALL") {
    return true;
  }

  if (filter === "PENDING") {
    return isPendingPlanItem(item);
  }

  if (filter === "DONE") {
    return isDonePlanItem(item);
  }

  if (filter === "CANCELLED") {
    return isCancelledPlanItem(item);
  }

  return true;
}

export function sortPlanItems(
  a: MonthlyPlanItem,
  b: MonthlyPlanItem,
  sortBy: PlanItemSortKey,
): number {
  if (sortBy === "DATE") {
    const dateA = a.expectedDate ?? "9999-12-31";
    const dateB = b.expectedDate ?? "9999-12-31";
    const byDate = dateA.localeCompare(dateB);

    return byDate !== 0
      ? byDate
      : priorityWeight(a.priority) - priorityWeight(b.priority);
  }

  if (sortBy === "PRIORITY") {
    const byPriority = priorityWeight(a.priority) - priorityWeight(b.priority);

    return byPriority !== 0
      ? byPriority
      : (a.expectedDate ?? "9999-12-31").localeCompare(
          b.expectedDate ?? "9999-12-31",
        );
  }

  return getComparableAmount(b) - getComparableAmount(a);
}

export function priorityWeight(priority: MonthlyPlanPriority): number {
  return priorityOrder[priority] ?? 99;
}

export function isDueNextDays(
  expectedDate: string | null | undefined,
  days: number,
): boolean {
  if (!expectedDate || days < 0) {
    return false;
  }

  const expected = parseLocalDate(expectedDate);

  if (!expected) {
    return false;
  }

  const today = startOfDay(new Date());
  const limit = addDays(today, days);

  return expected >= today && expected <= limit;
}

export function parseLocalDate(value: string): Date | null {
  const match = /^(\d{4})-(\d{2})-(\d{2})$/.exec(value);

  if (!match) {
    return null;
  }

  const [, rawYear, rawMonth, rawDay] = match;
  const year = Number(rawYear);
  const month = Number(rawMonth);
  const day = Number(rawDay);
  const parsed = startOfDay(new Date(year, month - 1, day));

  if (
    parsed.getFullYear() !== year ||
    parsed.getMonth() !== month - 1 ||
    parsed.getDate() !== day
  ) {
    return null;
  }

  return parsed;
}

export function startOfDay(date: Date): Date {
  return new Date(date.getFullYear(), date.getMonth(), date.getDate());
}

export function addDays(date: Date, days: number): Date {
  const result = new Date(date);
  result.setDate(result.getDate() + days);
  return result;
}

export function confidenceMeta(
  confidence: QuickCapturePreviewResponse["confidence"],
): { label: string; tone: "neutral" | "ok" | "watch" | "critical" } {
  if (confidence === "HIGH") {
    return { label: "Alta", tone: "ok" };
  }

  if (confidence === "MEDIUM") {
    return { label: "Media", tone: "watch" };
  }

  if (confidence === "LOW") {
    return { label: "Baja", tone: "critical" };
  }

  return { label: "Sin confianza", tone: "neutral" };
}

function getComparableAmount(item: MonthlyPlanItem): number {
  return Math.abs(
    item.netMax ?? item.amount ?? item.maxAmount ?? item.minAmount ?? 0,
  );
}

export function createReconciliationByItemId(
  reconciliation?: MonthlyPlanReconciliationSummary,
): Map<string, PlanItemReconciliation> {
  return new Map(
    (reconciliation?.items ?? []).map((item) => [item.itemId, item]),
  );
}

export function getExecutionStatusLabel(status?: PlanExecutionStatus): string {
  if (!status) return "Sin conciliación";

  const labels: Record<PlanExecutionStatus, string> = {
    UNPRICED: "Sin monto",
    NOT_EXECUTED: "Sin ejecutar",
    PARTIALLY_EXECUTED: "Parcial",
    EXECUTED: "Ejecutado",
    OVER_EXECUTED: "Excedido",
    CANCELLED: "Cancelado",
    NOT_APPLICABLE: "No aplica",
  };

  return labels[status];
}

export function getExecutionStatusTone(
  status?: PlanExecutionStatus,
): "neutral" | "ok" | "watch" | "critical" {
  if (status === "EXECUTED") return "ok";
  if (
    status === "PARTIALLY_EXECUTED" ||
    status === "UNPRICED" ||
    status === "NOT_EXECUTED"
  ) {
    return "watch";
  }
  if (status === "OVER_EXECUTED") return "critical";
  return "neutral";
}
