export type ExternalLoanDashboard = {
  investedAmount: number;
  earnedAmount: number;
  amountToEarn: number;
  totalDebt: number;
  activeLoans: number;
};

export type ExternalLoanCashControl = {
  availableCash: number;
  activeInvestment: number;
  recoveredCapital: number;
  pendingCapital: number;
  realizedProfit: number;
  projectedProfit: number;
  currentMonthIncome: number;
  currentMonthExpense: number;
  currentMonthBalance: number;
  projectedCollection30Days: number;
  projectedCollection60Days: number;
  projectedCollection90Days: number;
  overduePortfolio: number;
  pendingInstallments: number;
  dueNext7DaysInstallments: number;
  capitalRecoveryPercentage: number;
  expectedYieldPercentage: number;
};

export type ExternalLoan = {
  externalLoanId: number;
  externalBorrowerId: number;
  borrowerName: string;
  principalAmount: number;
  totalCollected: number;
  totalPending: number;
  realizedProfit: number;
  projectedProfit: number;
  status: string;
};

export type ExternalLoansSummaryResponse = {
  status: 'ENABLED' | 'DISABLED' | string;
  message: string;
  readOnly: boolean;
  dashboard: ExternalLoanDashboard;
  cashControl: ExternalLoanCashControl;
  activeLoans: ExternalLoan[];
};

export type ExternalLoanSyncConfig = {
  id?: string;
  profileId?: string;
  accountId: string | null;
  loanDisbursementCategoryId: string | null;
  principalRecoveryCategoryId: string | null;
  interestIncomeCategoryId: string | null;
  enabled: boolean;
  createdAt?: string;
  updatedAt?: string;
};

export type ExternalLoanSyncConfigPayload = {
  accountId: string | null;
  loanDisbursementCategoryId: string | null;
  principalRecoveryCategoryId: string | null;
  interestIncomeCategoryId: string | null;
  enabled: boolean;
};

export type ExternalIntegrationDiagnosticResponse = {
  status: string;
  message: string;
  integrationEnabled: boolean;
  syncEnabled: boolean;
  baseUrl: string;
  apiPrefix: string;
  hasUsername: boolean;
  hasPassword: boolean;
  connectTimeoutMs: number;
  readTimeoutMs: number;
  remoteCheckExecuted: boolean;
  missingFields: string[];
};

export type ExternalLoanManualSyncResponse = {
  dryRun: boolean;
  loansSynced: number;
  paymentsSynced: number;
  movementsCreated: number;
  skippedDuplicates: number;
  detectedExistingWithoutMapping: number;
  backfillRecommended: boolean;
  errors: string[];
  detectedLoans: string[];
  detectedPayments: string[];
  plannedMovements: string[];
  summaryByType: Record<string, number>;
};

export type ExternalLoanBackfillCandidate = {
  transactionId: string;
  description: string;
  amount: number;
  realDate: string;
  inferredEntityType: string;
  inferredEntityId: string;
  inferredEventType: string;
  confidence: 'HIGH' | 'MEDIUM' | 'LOW' | string;
  warning: string | null;
  wouldCreateMapping: boolean;
};

export type ExternalLoanBackfillDryRunResponse = {
  candidates: ExternalLoanBackfillCandidate[];
};

export type ExternalLoanBackfillApplyRequest = {
  includeLowConfidence: boolean;
};

export type ExternalLoanBackfillApplyResponse = {
  createdMappings: number;
  skipped: string[];
  errors: string[];
};

export type DuplicateTransactionSample = {
  transactionId: string;
  realDate: string;
  description: string | null;
  amount: number;
};

export type DuplicateSourceOperationGroup = {
  profileId: string;
  source: string;
  sourceOperationId: string;
  count: number;
  transactions: DuplicateTransactionSample[];
};

export type DuplicateSourceHashGroup = {
  profileId: string;
  sourceHash: string;
  count: number;
  transactions: DuplicateTransactionSample[];
};

export type ExternalLoanIdempotencyDiagnosticsResponse = {
  cjTransactions: number;
  mappedTransactions: number;
  unmappedCandidates: number;
  backfillRecommended: boolean;
  canRunSync: boolean;
  hasIndexBlockingDuplicates: boolean;
  requiresManualReview: boolean;
  candidateCountsByConfidence: Record<string, number>;
  wouldCreateMappings: number;
  alreadyMappedEvents: number;
  alreadyMappedTransactions: number;
  duplicateSourceOperationGroups: DuplicateSourceOperationGroup[];
  duplicateSourceHashGroups: DuplicateSourceHashGroup[];
};
