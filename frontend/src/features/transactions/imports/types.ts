export type TransactionImportSource = 'BANCO_PROVINCIA' | 'MERCADO_PAGO';
export type TransactionImportMovementType = 'INCOME' | 'EXPENSE';
export type TransactionImportRowStatus = 'READY' | 'DUPLICATE' | 'INVALID' | 'IGNORED';

export interface TransactionImportRow {
  rowNumber: number;
  realDate: string;
  normalizedDescription: string;
  amount: number;
  movementType: TransactionImportMovementType;
  suggestedCategoryId: string | null;
  status: TransactionImportRowStatus;
}

export interface TransactionImportPreview {
  batchId: string;
  totalRows: number;
  importableRows: number;
  duplicateRows: number;
  rows: TransactionImportRow[];
}

export interface TransactionImportCommitPayload {
  rows: Array<{
    rowNumber: number;
    categoryId: string | null;
    accountId: string;
    movementType: TransactionImportMovementType;
    amount: number;
    status: TransactionImportRowStatus;
    description: string;
  }>;
  createMissingFallbackCategory: boolean;
  skipDuplicates: boolean;
}

export interface TransactionImportCommitResult {
  createdCount: number;
  duplicateCount: number;
  skippedCount?: number;
  errors?: string[];
}
