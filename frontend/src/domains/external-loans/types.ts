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

export type ExternalLoanManualSyncResponse = {
  dryRun: boolean;
  loansSynced: number;
  paymentsSynced: number;
  movementsCreated: number;
  skippedDuplicates: number;
  errors: string[];
  detectedLoans: string[];
  detectedPayments: string[];
  plannedMovements: string[];
  summaryByType: Record<string, number>;
};