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
      ERROR: 0,
      SKIPPED: 0,
    } as Record<RowStatus | 'total', number>,
  );
}

export function countUnresolvedRows(
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

export function countImportableRows(
  rows: TransactionImportRow[],
  createMissingFallbackCategory: boolean,
) {
  return rows.filter(
    (row) =>
      (row.status === 'READY' || row.status === 'NEEDS_CATEGORY')
      && (createMissingFallbackCategory || Boolean(row.suggestedCategoryId)),
  ).length;
}