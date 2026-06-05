import type { TransactionImportRow } from "./types";

type RowStatus = TransactionImportRow["status"];

export interface ImportDerivedState {
  totalRows: number;
  readyRows: number;
  needsCategoryRows: number;
  missingCategoryRows: number;
  blockingMissingCategoryRows: number;
  fallbackCoveredRows: number;
  categoryOptionalRows: number;
  duplicateRows: number;
  exactDuplicateRows: number;
  crossSourceRiskRows: number;
  internalTransferRows: number;
  reviewRows: number;
  technicalNeutralRows: number;
  skippedRows: number;
  errorRows: number;
  importableRows: number;
  blockedRows: number;
  hasBlockingIssues: boolean;
}

export function buildImportDerivedState(
  rows: TransactionImportRow[],
  createMissingFallbackCategory: boolean,
  incompatibleRowNumbers: ReadonlySet<number>,
  skipDuplicatesConfirmed: boolean,
): ImportDerivedState {
  const totalRows = rows.length;

  const readyRows = countByStatus(rows, "READY");
  const needsCategoryRows = countByStatus(rows, "NEEDS_CATEGORY");
  const skippedRows = countByStatus(rows, "SKIPPED");
  const errorRows = countByStatus(rows, "ERROR");

  const duplicateRows =
    countByStatus(rows, "DUPLICATE") +
    countByStatus(rows, "DUPLICATE_EXACT");

  const exactDuplicateRows = countByStatus(rows, "DUPLICATE_EXACT");

  const crossSourceRiskRows = countByStatus(
    rows,
    "POSSIBLE_CROSS_SOURCE_DUPLICATE",
  );

  const internalTransferRows = rows.filter(isInternalTransferRow).length;

  const technicalNeutralRows = rows.filter(isTechnicalOrNeutralRow).length;

  const reviewRows = rows.filter(isReviewRow).length;

  const missingCategoryRows = rows.filter(
    (row) => !row.suggestedCategoryId && !isNonImportableStatus(row),
  ).length;

  const blockingMissingCategoryRows = rows.filter(
    (row) =>
      !row.suggestedCategoryId &&
      !isNonImportableStatus(row) &&
      !canImportWithoutCategory(row) &&
      !(createMissingFallbackCategory && needsFallbackCategory(row)),
  ).length;

  const fallbackCoveredRows = rows.filter(
    (row) =>
      !row.suggestedCategoryId &&
      !canImportWithoutCategory(row) &&
      createMissingFallbackCategory &&
      needsFallbackCategory(row) &&
      !isNonImportableStatus(row),
  ).length;

  const categoryOptionalRows = rows.filter(
    (row) =>
      !row.suggestedCategoryId &&
      canImportWithoutCategory(row) &&
      !isNonImportableStatus(row),
  ).length;

  const importableRows = rows.filter(
    (row) =>
      !incompatibleRowNumbers.has(row.rowNumber) &&
      isImportableRow(row, createMissingFallbackCategory),
  ).length;

  const duplicateDecisionMissing =
    duplicateRows > 0 && !skipDuplicatesConfirmed;

  const blockedRows = rows.filter(
    (row) =>
      row.status === "ERROR" ||
      row.status === "SKIPPED" ||
      incompatibleRowNumbers.has(row.rowNumber) ||
      (!isNonImportableStatus(row) &&
        !row.suggestedCategoryId &&
        !canImportWithoutCategory(row) &&
        !(createMissingFallbackCategory && needsFallbackCategory(row))) ||
      isBlockingDuplicateRow(row),
  ).length;

  const hasBlockingIssues =
    errorRows > 0 ||
    blockingMissingCategoryRows > 0 ||
    incompatibleRowNumbers.size > 0 ||
    duplicateDecisionMissing;

  return {
    totalRows,
    readyRows,
    needsCategoryRows,
    missingCategoryRows,
    blockingMissingCategoryRows,
    fallbackCoveredRows,
    categoryOptionalRows,
    duplicateRows,
    exactDuplicateRows,
    crossSourceRiskRows,
    internalTransferRows,
    reviewRows,
    technicalNeutralRows,
    skippedRows,
    errorRows,
    importableRows,
    blockedRows,
    hasBlockingIssues,
  };
}

export function countByStatus(
  rows: TransactionImportRow[],
  status: RowStatus,
): number {
  return rows.filter((row) => row.status === status).length;
}

export function isBlockingDuplicateRow(row: TransactionImportRow): boolean {
  return row.status === "DUPLICATE" || row.status === "DUPLICATE_EXACT";
}

export function isCrossSourceRiskRow(row: TransactionImportRow): boolean {
  return row.status === "POSSIBLE_CROSS_SOURCE_DUPLICATE";
}

export function isInternalTransferRow(row: TransactionImportRow): boolean {
  return (
    row.status === "POSSIBLE_INTERNAL_TRANSFER" ||
    row.status === "INTERNAL_TRANSFER_MATCHED" ||
    row.balanceImpact === "INTERNAL_TRANSFER" ||
    row.paymentChannel === "INTERNAL_TRANSFER" ||
    row.classificationStatus === "TECHNICAL" && row.movementType === "TRANSFER"
  );
}

export function isTechnicalOrNeutralRow(row: TransactionImportRow): boolean {
  return (
    row.classificationStatus === "TECHNICAL" ||
    row.balanceImpact === "INTERNAL_TRANSFER" ||
    row.balanceImpact === "NEUTRAL_ADJUSTMENT" ||
    row.balanceImpact === "TECHNICAL"
  );
}

export function isReviewRow(row: TransactionImportRow): boolean {
  return (
    row.status === "REVIEW" ||
    row.classificationStatus === "REVIEW" ||
    row.status === "POSSIBLE_INTERNAL_TRANSFER" ||
    row.status === "INTERNAL_TRANSFER_MATCHED" ||
    row.status === "POSSIBLE_CROSS_SOURCE_DUPLICATE"
  );
}

export function canImportWithoutCategory(row: TransactionImportRow): boolean {
  return (
    row.classificationStatus === "TECHNICAL" ||
    row.balanceImpact === "INTERNAL_TRANSFER" ||
    row.balanceImpact === "NEUTRAL_ADJUSTMENT" ||
    row.balanceImpact === "TECHNICAL"
  );
}

export function isImportableRow(
  row: TransactionImportRow,
  createMissingFallbackCategory: boolean,
): boolean {
  if (row.status === "ERROR" || row.status === "SKIPPED") {
    return false;
  }

  if (isBlockingDuplicateRow(row)) {
    return false;
  }

  if (row.suggestedCategoryId) {
    return true;
  }

  if (canImportWithoutCategory(row)) {
    return true;
  }

  return createMissingFallbackCategory && needsFallbackCategory(row);
}

export function needsFallbackCategory(row: TransactionImportRow): boolean {
  return (
    row.status === "NEEDS_CATEGORY" ||
    row.classificationStatus === "NEEDS_CATEGORY"
  );
}

function isNonImportableStatus(row: TransactionImportRow): boolean {
  return (
    row.status === "ERROR" ||
    row.status === "SKIPPED" ||
    isBlockingDuplicateRow(row)
  );
}
