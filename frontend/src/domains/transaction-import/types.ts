export type TransactionImportSource =
  | 'AUTO'
  | 'BANCO_PROVINCIA'
  | 'MERCADO_PAGO'
  | 'TARJETA_CREDITO_GENERICA'
  | 'DEUDAS_TARJETA_GENERICA';
export type TransactionImportMovementType = 'INCOME' | 'EXPENSE' | 'SAVING' | 'TRANSFER' | 'ADJUSTMENT';
export type TransactionImportRowStatus = 'READY' | 'NEEDS_CATEGORY' | 'DUPLICATE' | 'DUPLICATE_EXACT' | 'POSSIBLE_INTERNAL_TRANSFER' | 'INTERNAL_TRANSFER_MATCHED' | 'POSSIBLE_CROSS_SOURCE_DUPLICATE' | 'REVIEW' | 'SKIPPED' | 'ERROR';
export type TransactionImportConfidence = 'HIGH' | 'MEDIUM' | 'LOW' | 'NONE';
export type TransactionImportPaymentChannel =
  | 'UNKNOWN'
  | 'CASH'
  | 'BANK_TRANSFER'
  | 'DEBIN'
  | 'CUENTA_DNI'
  | 'DEBIT_CARD'
  | 'CREDIT_CARD'
  | 'DIRECT_DEBIT'
  | 'POS_TRANSFER'
  | 'ATM'
  | 'MONEY_MARKET_YIELD'
  | 'TRANSPORT_CARD'
  | 'QR_PAYMENT'
  | 'CARD_FOREIGN_CURRENCY'
  | 'MERCADO_PAGO'
  | 'MERCADO_CREDITO'
  | 'INTERNAL_TRANSFER'
  | 'OTHER';
export type TransactionImportBalanceImpact = 'OPERATING_INCOME' | 'CONSUMPTION_EXPENSE' | 'SAVING_OUTFLOW' | 'INVESTMENT_OUTFLOW' | 'DEBT_OUTFLOW' | 'RECOVERABLE_OUTFLOW' | 'PRINCIPAL_RECOVERY' | 'INTEREST_INCOME' | 'REFUND_OR_REIMBURSEMENT' | 'INTERNAL_TRANSFER' | 'EXTERNAL_TRANSFER' | 'NEUTRAL_ADJUSTMENT' | 'IGNORED' | 'TECHNICAL' | 'UNKNOWN';
export type TransactionImportClassificationStatus = 'CLASSIFIED' | 'NEEDS_CATEGORY' | 'REVIEW' | 'TECHNICAL' | 'IGNORED_BY_RULE';

export interface TransactionImportRow {
  rowNumber: number;
  source?: TransactionImportSource;
  detectedFormat?: string | null;
  sourceOperationId?: string | null;
  sourceHash?: string | null;
  externalSequence?: string | null;
  sheetName?: string | null;
  realDate: string;
  budgetDate?: string | null;
  operationDateTime?: string | null;
  operationDateTimePrecision?: 'DATE_ONLY' | 'DATE_TIME' | null;
  normalizedDescription: string;
  rawDescription?: string;
  extendedDescription?: string | null;
  merchantName?: string | null;
  counterparty?: string | null;
  counterpartyDocumentHash?: string | null;
  rawSignedAmount?: number;
  amount: number;
  currency?: string;
  movementType: TransactionImportMovementType;
  paymentChannel?: TransactionImportPaymentChannel | null;
  balanceImpact?: TransactionImportBalanceImpact | null;
  classificationStatus?: TransactionImportClassificationStatus | null;
  classificationReason?: string | null;
  classificationLayer?: string | null;
  classificationMatchedField?: string | null;
  classificationMatchedValue?: string | null;
  classificationExplanationJson?: string | null;
  categorySuggestionKey?: string | null;
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
  source?: TransactionImportSource;
  accountId?: string | null;
  detectedFormat?: string | null;
  totalRows: number;
  importableRows: number;
  duplicateRows: number;
  skippedRows?: number;
  unresolvedRows?: number;
  suggestedCategoryRows?: number;
  needsCategoryRows?: number;
  reviewRows?: number;
  errorRows?: number;
  internalTransferRows?: number;
  technicalNeutralRows?: number;
  blockedRows?: number;
  blockingCategoryRows?: number;
  crossSourceRiskRows?: number;
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
