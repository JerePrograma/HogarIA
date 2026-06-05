import type { TransactionImportRow } from './types';
import {
  canImportWithoutCategory,
  isImportableRow,
  isTechnicalOrNeutralRow,
  needsFallbackCategory,
} from './importDerivedState';

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

export function countBlockingMissingCategoryRows(
  rows: TransactionImportRow[],
  createMissingFallbackCategory: boolean,
) {
  return rows.filter(
    (row) =>
      !row.suggestedCategoryId
      && row.status !== 'ERROR'
      && row.status !== 'SKIPPED'
      && row.status !== 'DUPLICATE'
      && row.status !== 'DUPLICATE_EXACT'
      && !canImportWithoutCategory(row)
      && !(createMissingFallbackCategory && needsFallbackCategory(row)),
  ).length;
}

export const countUnresolvedRows = countBlockingMissingCategoryRows;

export function countImportableRows(
  rows: TransactionImportRow[],
  createMissingFallbackCategory: boolean,
) {
  return rows.filter(
    (row) => isImportableRow(row, createMissingFallbackCategory),
  ).length;
}

export function countTechnicalNeutralRows(rows: TransactionImportRow[]) {
  return rows.filter(isTechnicalOrNeutralRow).length;
}
