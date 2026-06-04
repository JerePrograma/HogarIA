import type { TransactionImportRow } from './types';

type RowStatus = TransactionImportRow['status'];

export function countImportRows(rows: TransactionImportRow[]) {
  return rows.reduce(
    (acc, row) => {
      acc.total += 1;
      acc[row.status] = (acc[row.status] ?? 0) + 1;
      return acc;
    },
    {
      total: 0,
      READY: 0,
      NEEDS_CATEGORY: 0,
      DUPLICATE: 0,
      DUPLICATE_EXACT: 0,
      POSSIBLE_INTERNAL_TRANSFER: 0,
      INTERNAL_TRANSFER_MATCHED: 0,
      POSSIBLE_CROSS_SOURCE_DUPLICATE: 0,
      REVIEW: 0,
      ERROR: 0,
      SKIPPED: 0,
    } as Record<RowStatus | 'total', number>,
  );
}

export function canImportWithoutCategory(row: TransactionImportRow) {
  if (row.status === 'NEEDS_CATEGORY') return false;

  return (
    row.status === 'REVIEW'
    || row.classificationStatus === 'REVIEW'
    || row.classificationStatus === 'NEEDS_CATEGORY'
    || row.classificationStatus === 'TECHNICAL'
    || row.balanceImpact === 'INTERNAL_TRANSFER'
    || row.balanceImpact === 'NEUTRAL_ADJUSTMENT'
    || row.balanceImpact === 'TECHNICAL'
  );
}

export function isTechnicalOrNeutralRow(row: TransactionImportRow) {
  return (
    row.classificationStatus === 'TECHNICAL'
    || row.balanceImpact === 'INTERNAL_TRANSFER'
    || row.balanceImpact === 'NEUTRAL_ADJUSTMENT'
    || row.balanceImpact === 'TECHNICAL'
  );
}

export function countBlockingMissingCategoryRows(
  rows: TransactionImportRow[],
  createMissingFallbackCategory: boolean,
) {
  return rows.filter(
    (row) =>
      row.status === 'NEEDS_CATEGORY'
      && !row.suggestedCategoryId
      && !createMissingFallbackCategory,
  ).length;
}

export const countUnresolvedRows = countBlockingMissingCategoryRows;

export function countImportableRows(
  rows: TransactionImportRow[],
  createMissingFallbackCategory: boolean,
) {
  return rows.filter(
    (row) =>
      (row.status === 'READY' || row.status === 'NEEDS_CATEGORY' || row.status === 'REVIEW')
      && (
        Boolean(row.suggestedCategoryId)
        || canImportWithoutCategory(row)
        || (row.status === 'NEEDS_CATEGORY' && createMissingFallbackCategory)
      ),
  ).length;
}

export function countTechnicalNeutralRows(rows: TransactionImportRow[]) {
  return rows.filter(isTechnicalOrNeutralRow).length;
}
