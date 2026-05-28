export type TransactionImportSource =
  | 'BANCO_PROVINCIA'
  | 'MERCADO_PAGO'
  | 'TARJETA_CREDITO_GENERICA'
  | 'DEUDAS_TARJETA_GENERICA';
export type TransactionImportMovementType = 'INCOME' | 'EXPENSE' | 'SAVING' | 'TRANSFER' | 'ADJUSTMENT';
export type TransactionImportRowStatus = 'READY' | 'NEEDS_CATEGORY' | 'DUPLICATE' | 'DUPLICATE_EXACT' | 'POSSIBLE_INTERNAL_TRANSFER' | 'INTERNAL_TRANSFER_MATCHED' | 'POSSIBLE_CROSS_SOURCE_DUPLICATE' | 'SKIPPED' | 'ERROR';
export type TransactionImportConfidence = 'HIGH' | 'MEDIUM' | 'LOW' | 'NONE';

export interface TransactionImportRow {
  rowNumber: number;
  realDate: string;
  normalizedDescription: string;
  rawDescription?: string;
  rawSignedAmount?: number;
  amount: number;
  currency?: string;
  movementType: TransactionImportMovementType;
  suggestedCategoryId: string | null;
  suggestedCategoryName?: string | null;
  confidence?: TransactionImportConfidence;
  skipReason?: string | null;
  status: TransactionImportRowStatus;
  warning?: string;
  matchedTransactionId?: string | null;
  matchedAccountId?: string | null;
  matchedCurrentCategoryId?: string | null;
  matchedCurrentCategoryName?: string | null;
  matchType?: string | null;
  matchReason?: string | null;
}

export interface TransactionImportPreview {
  batchId: string;
  totalRows: number;
  importableRows: number;
  duplicateRows: number;
  skippedRows?: number;
  unresolvedRows?: number;
  rows: TransactionImportRow[];
  warnings?: string[];
  errors?: string[];
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
  skippedCount: number;
  failedCount: number;
  createdTransactionIds?: string[];
  warnings?: string[];
  errors?: string[];
}
