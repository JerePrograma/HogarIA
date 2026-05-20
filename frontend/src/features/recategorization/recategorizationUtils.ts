import type {
  BulkRecategorizeCandidate,
  BulkRecategorizePreviewPayload,
  BulkRecategorizePreviewStatus,
} from "../../api/bulkRecategorizeApi";
import { MovementType } from "../../domain/types";

export const WITHOUT_CATEGORY_VALUE = "__WITHOUT_CATEGORY__";
export const ALL_VALUE = "ALL";

export const previewStatusLabels: Record<
  BulkRecategorizePreviewStatus,
  string
> = {
  READY: "Actualizable",
  AMBIGUOUS: "Ambiguo",
  SKIPPED: "Omitido",
  NEEDS_CATEGORY: "Sin sugerencia",
  ERROR: "Error",
};

export function getPreviewStatusTone(
  status: BulkRecategorizePreviewStatus,
): "ok" | "watch" | "critical" | "neutral" {
  if (status === "READY") return "ok";
  if (status === "AMBIGUOUS" || status === "NEEDS_CATEGORY") return "watch";
  if (status === "ERROR") return "critical";

  return "neutral";
}

export function getMovementLabel(type: MovementType | null | undefined) {
  if (!type) return "-";
  return movementTypeLabels[type] ?? type;
}

export function getMovementTone(type: MovementType | null | undefined) {
  if (!type) return "neutral";
  return movementTypeTones[type] ?? "neutral";
}

export function toNullableString(value: string) {
  return value.trim() ? value : null;
}

export function toNullableNumber(value: string) {
  if (!value.trim()) return null;

  const parsed = Number(value);
  return Number.isFinite(parsed) ? parsed : null;
}

export function countCandidatesByStatus(
  candidates: BulkRecategorizeCandidate[],
) {
  return candidates.reduce<Record<string, number>>((acc, candidate) => {
    acc[candidate.previewStatus] = (acc[candidate.previewStatus] ?? 0) + 1;
    return acc;
  }, {});
}

export function hasAnyRecategorizationCriteria(
  form: BulkRecategorizePreviewPayload,
) {
  return Boolean(
    form.accountId ||
    form.from ||
    form.to ||
    form.fromCategoryId ||
    form.onlyWithoutCategory != null ||
    form.movementType ||
    form.descriptionContains ||
    form.exactAmount != null ||
    form.minAmount != null ||
    form.maxAmount != null ||
    form.onlyImported != null,
  );
}
