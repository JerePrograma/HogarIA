// src/domain/types.ts

export type ProfileType = 'PERSONAL' | 'FAMILY' | 'BUSINESS';

export type AccountType =
  | 'CASH'
  | 'BANK'
  | 'CREDIT_CARD'
  | 'DEBIT_CARD'
  | 'VIRTUAL_WALLET'
  | 'BUSINESS';

export type CategoryType =
  | 'INCOME'
  | 'FIXED_EXPENSE'
  | 'VARIABLE_EXPENSE'
  | 'SAVING'
  | 'DEBT'
  | 'INVESTMENT';

export type CategoryScope = 'PERSONAL' | 'FAMILY' | 'BUSINESS' | 'GLOBAL';

export type MovementType =
  | 'INCOME'
  | 'EXPENSE'
  | 'SAVING'
  | 'TRANSFER'
  | 'ADJUSTMENT';

export type TransactionOrigin = 'MANUAL' | 'IMPORT' | 'RECURRENT' | 'SYSTEM';
export type TransactionStatus = 'CONFIRMED' | 'PENDING' | 'IGNORED';
export type BudgetComparisonStatus = 'OK' | 'WARNING' | 'EXCEEDED';
export type FinancialHealth = 'EXCELLENT' | 'HEALTHY' | 'WARNING' | 'CRITICAL';
export type FinancialRiskLevel = 'OK' | 'WATCH' | 'RISK' | 'CRITICAL';

export interface DevUser {
  id: string;
  email: string;
  fullName: string;
  createdAt: string;
}

export interface Profile {
  id: string;
  name: string;
  type: ProfileType;
  baseCurrency: string;
  activeYear: number;
  active: boolean;
  createdAt: string;
  updatedAt: string;
}

export interface Account {
  id: string;
  profileId: string;
  name: string;
  accountType: AccountType;
  currency: string;
  creditLimit: number | null;
  statementCloseDay: number | null;
  dueDay: number | null;
  active: boolean;
  createdAt: string;
  updatedAt: string;
}

export interface Category {
  id: string;
  profileId: string | null;
  parentId: string | null;
  name: string;
  type: CategoryType;
  scope: CategoryScope;
  active: boolean;
  createdAt: string;
  updatedAt: string;
}

export interface MoneyTransaction {
  id: string;
  profileId: string;
  accountId: string;
  categoryId: string;
  movementType: MovementType;
  realDate: string;
  budgetDate: string;
  amount: number;
  currency: string;
  description?: string;
  origin: TransactionOrigin;
  status: TransactionStatus;
  createdAt: string;
  updatedAt: string;
}

export interface BudgetYear {
  id: string;
  profileId: string;
  year: number;
  targetIncome: number | null;
  targetSaving: number | null;
  notes: string | null;
  createdAt: string;
  updatedAt: string;
}

export interface BudgetCategoryItem {
  id: string;
  budgetMonthId: string;
  categoryId: string;
  categoryName: string;
  categoryType: CategoryType;
  budgetAmount: number;
  createdAt: string;
  updatedAt: string;
}

export interface BudgetMonth {
  id: string;
  budgetYearId: string;
  month: number;
  notes: string | null;
  items: BudgetCategoryItem[];
  createdAt: string;
  updatedAt: string;
}

export interface BudgetComparisonItem {
  categoryId: string;
  categoryName: string;
  categoryType: CategoryType;
  budgetAmount: number;
  realAmount: number;
  difference: number;
  percentUsed: number;
  status: BudgetComparisonStatus;
}

export interface BudgetComparison {
  profileId: string;
  year: number;
  month: number;
  totalBudget: number;
  totalReal: number;
  totalDifference: number;
  items: BudgetComparisonItem[];
}

export interface MonthlyBalance {
  totalIncome: number;
  totalExpenses: number;
  savings: number;
  balance: number;
}

export interface FiftyThirtyTwenty {
  fixedPercent: number;
  variablePercent: number;
  savingPercent: number;
}

export interface BudgetSummary {
  totalBudget: number;
  totalReal: number;
  totalDifference: number;
  exceededCount: number;
  warningCount: number;
}

export interface CategoryBreakdown {
  categoryId: string;
  categoryName: string;
  categoryType: CategoryType;
  totalAmount: number;
  percentOfIncome: number;
  movementCount: number;
}

export interface PlanningDashboardSummary {
  totalIncomeMin: number;
  totalIncomeMax: number;
  totalExpenseMin: number;
  totalExpenseMax: number;
  totalRecoveryMin: number;
  totalRecoveryMax: number;
  projectedNetMin: number;
  projectedNetMax: number;
  pendingIncome: number;
  pendingExpense: number;
  unpricedCount: number;
  dueNext7DaysCount: number;
  plannedItemsCount: number;
  cancelledItemsCount: number;
  convertedItemsCount: number;
}

export interface DashboardOperationalSummary {
  confirmedIncome: number;
  confirmedExpenses: number;
  confirmedSavings: number;
  confirmedBalance: number;
  projectedNetMin: number;
  projectedNetMax: number;
  deltaProjectedMinVsConfirmed: number;
  deltaProjectedMaxVsConfirmed: number;
  pendingIncome: number;
  pendingExpense: number;
  expectedRecoveriesMin: number;
  expectedRecoveriesMax: number;
  unpricedCount: number;
  dueNext7DaysCount: number;
  financialRiskLevel: FinancialRiskLevel;
  alerts: string[];
}

export interface MonthlyCashFlowSummary {
  grossCashOutflow: number;
  consumptionExpense: number;
  fixedExpense: number;
  variableExpense: number;
  debtOutflow: number;
  savingOutflow: number;
  investmentOutflow: number;
  recoverableOutflow: number;
  principalRecovered: number;
  refundsOrReimbursements: number;
  earnedIncome: number;
  interestIncome: number;
  totalIncome: number;
  netCashFlow: number;
  economicNetExpense: number;
  internalTransfers: number;
  externalTransfers: number;
  neutralAdjustments: number;
  unknownOutflow: number;
  alerts: string[];
}

export interface DashboardSummary {
  monthlyBalance: MonthlyBalance;
  fiftyThirtyTwenty: FiftyThirtyTwenty;
  fixedExpenses: number;
  variableExpenses: number;
  financialHealth: FinancialHealth;
  categoryBreakdown: CategoryBreakdown[];
  budgetSummary: BudgetSummary | null;
  planningSummary: PlanningDashboardSummary | null;
  operationalSummary: DashboardOperationalSummary | null;
  monthlyCashFlowSummary: MonthlyCashFlowSummary | null;
}

export type GoalType =
  | 'EMERGENCY_FUND'
  | 'DEBT_PAYOFF'
  | 'SAVING_TARGET'
  | 'INVESTMENT'
  | 'BUSINESS'
  | 'TRAVEL'
  | 'EDUCATION'
  | 'OTHER';

export type GoalStatus = 'ACTIVE' | 'PAUSED' | 'COMPLETED' | 'CANCELLED';
export type HabitFrequency = 'DAILY' | 'WEEKLY' | 'MONTHLY';

export interface FinancialGoal {
  id: string;
  profileId: string;
  name: string;
  goalType: GoalType;
  targetAmount: number;
  currentAmount: number;
  monthlyContribution?: number | null;
  targetDate?: string | null;
  status: GoalStatus;
  progressPercent?: number;
  monthsRemaining?: number | null;
  notes?: string | null;
}

export interface GoalSuggestion {
  averageMonthlyExpenses: number;
  targetMin: number;
  targetRecommended: number;
  suggestedType: GoalType;
}

export interface Habit {
  id: string;
  profileId: string;
  description: string;
  area?: string | null;
  frequency: HabitFrequency;
  active: boolean;
}

export interface HabitCheckin {
  id: string;
  habitId: string;
  checkinDate: string;
  completed: boolean;
  note?: string | null;
}

export interface HabitSummary {
  totalHabits: number;
  completedCheckins: number;
  expectedCheckins: number;
  completionRate: number;
  completionRateByArea: Record<string, number>;
}

export interface InflationIndex {
  id: string;
  year: number;
  month: number;
  categoryCode?: string | null;
  categoryName?: string | null;
  monthlyRate: number;
  source?: string | null;
  projection: boolean;
}

export interface InflationAccumulated {
  accumulatedRate: number;
}

export interface ExcelImportRow {
  id: string;
  sheetName: string;
  rowNumber?: number | null;
  concept?: string | null;
  month?: number | null;
  amount?: number | null;
  targetEntity?: string | null;
  status: string;
  errorMessage?: string | null;
}

export interface ExcelImportBatch {
  id: string;
  profileId: string;
  originalFileName?: string | null;
  status: string;
  detectedProfileType?: ProfileType | null;
  year?: number | null;
  currency: string;
  rows?: ExcelImportRow[];
}

export interface BudgetExcelImportPreviewResponse {
  batchId: string;
  detectedSheets: string[];
  missingSheets: string[];
  rows: ExcelImportRow[];
  warnings: string[];
  errors: string[];
  summary: Record<string, number>;
}

export interface BudgetExcelImportCommitRequest {
  createCategories: boolean;
  createAccounts: boolean;
  createBudgets: boolean;
  createTransactions: boolean;
  createGoals: boolean;
  createHabits: boolean;
  createInflation: boolean;
  updateExisting: boolean;
  ignoreInvalidRows: boolean;
  year: number;
  currency: string;
  profileType: ProfileType;
}

export interface BudgetExcelImportCommitResponse {
  batchId: string;
  status: string;
  importedByType: Record<string, number>;
  warnings: string[];
  errors: string[];
}

export type MonthlyPlanItemType =
  | 'INCOME'
  | 'EXPENSE'
  | 'SAVING'
  | 'DEBT'
  | 'TRANSFER'
  | 'RECOVERY'
  | 'TODO';

export type MonthlyPlanPriority = 'ESSENTIAL' | 'IMPORTANT' | 'OPTIONAL';

export type MonthlyPlanStatus =
  | 'DRAFT'
  | 'ESTIMATED'
  | 'SCHEDULED'
  | 'DUE'
  | 'PAID'
  | 'COLLECTED'
  | 'CANCELLED';

export type MonthlyPlanSource = 'MANUAL' | 'IMPORT' | 'QUICK_CAPTURE' | 'SYSTEM';

export interface MonthlyPlanItem {
  id: string;
  profileId: string;
  categoryId: string | null;
  accountId: string | null;
  type: MonthlyPlanItemType;
  title: string;
  description?: string | null;
  expectedDate?: string | null;
  periodYear: number;
  periodMonth: number;
  amount?: number | null;
  minAmount?: number | null;
  maxAmount?: number | null;
  currency: string;
  expectedRecoveryAmount?: number | null;
  expectedRecoveryPercent?: number | null;
  grossMin: number;
  grossMax: number;
  recoveryMin: number;
  recoveryMax: number;
  netMin: number;
  netMax: number;
  priority: MonthlyPlanPriority;
  status: MonthlyPlanStatus;
  source: MonthlyPlanSource;
  transactionId?: string | null;
  counterparty?: string | null;
  installmentNumber?: number | null;
  installmentTotal?: number | null;
  createdAt: string;
  updatedAt: string;
}

export interface MonthlyPlanSummary {
  totalIncomeMin: number;
  totalIncomeMax: number;
  totalExpenseMin: number;
  totalExpenseMax: number;
  totalRecoveryMin: number;
  totalRecoveryMax: number;
  netMin: number;
  netMax: number;
  pendingIncome: number;
  pendingExpense: number;
  unpricedCount: number;
  dueNext7DaysCount: number;
  items: MonthlyPlanItem[];
}

export interface MonthlyPlanItemCreatePayload {
  type: MonthlyPlanItemType;
  title: string;
  description?: string;
  expectedDate?: string | null;
  periodYear: number;
  periodMonth: number;
  amount?: number | null;
  minAmount?: number | null;
  maxAmount?: number | null;
  currency?: string;
  expectedRecoveryAmount?: number | null;
  expectedRecoveryPercent?: number | null;
  counterparty?: string | null;
  installmentNumber?: number | null;
  installmentTotal?: number | null;
  priority?: MonthlyPlanPriority;
  status?: MonthlyPlanStatus;
  source?: MonthlyPlanSource;
  categoryId?: string | null;
  accountId?: string | null;
}

export interface MonthlyPlanItemUpdatePayload extends Partial<MonthlyPlanItemCreatePayload> {
  clearExpectedDate?: boolean;
  clearAmount?: boolean;
  clearRange?: boolean;
  clearRecovery?: boolean;
  clearCounterparty?: boolean;
  clearInstallment?: boolean;
  clearCategory?: boolean;
  clearAccount?: boolean;
}

export type SuggestionConfidence = 'HIGH' | 'MEDIUM' | 'LOW' | 'NONE';
export type QuickCaptureConfidence = Exclude<SuggestionConfidence, 'NONE'>;

export interface SuggestedAccount {
  id: string;
  name: string;
  confidence: SuggestionConfidence;
  reason: string;
}

export interface SuggestedCategory {
  id: string;
  name: string;
  confidence: SuggestionConfidence;
  reason: string;
}

export interface PlanningSuggestionRequest {
  type: MonthlyPlanItemType;
  title: string;
  counterparty?: string | null;
  amount?: number | null;
  minAmount?: number | null;
  maxAmount?: number | null;
  expectedRecoveryAmount?: number | null;
  expectedRecoveryPercent?: number | null;
}

export interface PlanningSuggestionResponse {
  accountSuggestion?: SuggestedAccount | null;
  categorySuggestion?: SuggestedCategory | null;
  confidence: SuggestionConfidence;
  reasons: string[];
}

export interface QuickCapturePreviewRequest {
  rawText: string;
  defaultYear?: number | null;
  defaultMonth?: number | null;
  defaultCurrency?: string | null;
}

export interface QuickCapturePreviewResponse {
  rawText: string;
  confidence: QuickCaptureConfidence;
  warnings: string[];
  parsed: MonthlyPlanItemCreatePayload;
  detectedDateText?: string | null;
  detectedAmountText?: string | null;
  detectedRangeText?: string | null;
  detectedRecoveryText?: string | null;
  detectedInstallmentText?: string | null;
  detectedCounterpartyText?: string | null;
  detectedTypeText?: string | null;
}

export interface QuickCaptureCommitRequest {
  rawText: string;
  payload: MonthlyPlanItemCreatePayload;
}

export interface QuickCaptureCommitResponse {
  item: MonthlyPlanItem;
  warnings: string[];
}