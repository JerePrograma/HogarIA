import type { TransactionImportRow } from "./types";

type RowStatus = TransactionImportRow["status"];

export interface ImportDerivedState {
  totalRows: number;
  readyRows: number;
  needsCategoryRows: number;
  blockingMissingCategoryRows: number;
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
  incompatibleRows: number,
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

  const blockingMissingCategoryRows = rows.filter(
    (row) =>
      row.status === "NEEDS_CATEGORY" &&
      !row.suggestedCategoryId &&
      !createMissingFallbackCategory,
  ).length;

  const importableRows = rows.filter((row) =>
    isImportableRow(row, createMissingFallbackCategory),
  ).length;

  const duplicateDecisionMissing =
    exactDuplicateRows > 0 && !skipDuplicatesConfirmed;

  const blockedRows =
    errorRows +
    skippedRows +
    blockingMissingCategoryRows +
    incompatibleRows +
    (duplicateDecisionMissing ? exactDuplicateRows : 0);

  return {
    totalRows,
    readyRows,
    needsCategoryRows,
    blockingMissingCategoryRows,
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
    hasBlockingIssues: blockedRows > 0,
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
  if (row.status === "NEEDS_CATEGORY") {
    return false;
  }

  return (
    row.status === "REVIEW" ||
    row.status === "POSSIBLE_INTERNAL_TRANSFER" ||
    row.status === "INTERNAL_TRANSFER_MATCHED" ||
    row.status === "POSSIBLE_CROSS_SOURCE_DUPLICATE" ||
    row.classificationStatus === "REVIEW" ||
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

  return row.status === "NEEDS_CATEGORY" && createMissingFallbackCategory;
}