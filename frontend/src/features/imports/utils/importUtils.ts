import { getCompatibleCategories } from "../../../domain/financeLabels";
import { Category } from "../../../domain/types";
import { TransactionImportMovementType, TransactionImportRowStatus, TransactionImportRow } from "../types";

export const importMovementLabels: Record<TransactionImportMovementType, string> = {
  INCOME: 'Ingreso',
  EXPENSE: 'Gasto',
  SAVING: 'Ahorro / inversión',
  TRANSFER: 'Transferencia',
  ADJUSTMENT: 'Ajuste',
};

export const importRowStatusLabels: Record<TransactionImportRowStatus, string> = {
  READY: 'Lista',
  NEEDS_CATEGORY: 'Requiere categoría',
  DUPLICATE: 'Duplicada',
  SKIPPED: 'Omitida',
  ERROR: 'Error',
};

export function getImportRowStatusTone(
  status: TransactionImportRowStatus,
): 'ok' | 'watch' | 'critical' | 'neutral' {
  if (status === 'READY') return 'ok';
  if (status === 'NEEDS_CATEGORY') return 'watch';
  if (status === 'ERROR') return 'critical';

  return 'neutral';
}

export function getImportRowIssueMessage(
  row: TransactionImportRow,
  createMissingFallbackCategory: boolean,
) {
  if (row.status === 'DUPLICATE') {
    return row.suggestedCategoryName
      ? `Movimiento ya existente. Categoría sugerida para revisar: ${row.suggestedCategoryName}.`
      : 'Movimiento ya existente. Se omitirá al confirmar.';
  }

  if (
    row.status === 'NEEDS_CATEGORY'
    && !row.suggestedCategoryId
    && row.suggestedCategoryName
    && createMissingFallbackCategory
  ) {
    return `Sugerida: ${row.suggestedCategoryName}. Se creará al confirmar.`;
  }

  if (
    row.status === 'NEEDS_CATEGORY'
    && !row.suggestedCategoryId
    && !createMissingFallbackCategory
  ) {
    return 'Necesita una categoría para poder importarse.';
  }

  if (row.status === 'SKIPPED' && row.skipReason) {
    return `Motivo de omisión: ${row.skipReason}`;
  }

  if (row.status === 'ERROR') {
    return row.warning ?? 'La fila tiene un error y no se importará.';
  }

  return '';
}

export function getSuggestedCategoryName(
  row: TransactionImportRow,
  categoriesById: Map<string, Category>,
) {
  return row.suggestedCategoryName
    ?? categoriesById.get(row.suggestedCategoryId ?? '')?.name
    ?? null;
}

export function getSelectableCategoriesForImportRow(
  row: TransactionImportRow,
  categories: Category[],
) {
  return getCompatibleCategories(categories, row.movementType, {
    includeTechnical: true,
  });
}