export type ProfileType = 'PERSONAL' | 'FAMILY' | 'BUSINESS';
export type AccountType = 'CASH' | 'BANK' | 'CREDIT_CARD' | 'DEBIT_CARD' | 'VIRTUAL_WALLET' | 'BUSINESS';
export type CategoryType = 'INCOME' | 'FIXED_EXPENSE' | 'VARIABLE_EXPENSE' | 'SAVING' | 'DEBT' | 'INVESTMENT';
export type CategoryScope = 'PERSONAL' | 'FAMILY' | 'BUSINESS' | 'GLOBAL';
export type MovementType = 'INCOME' | 'EXPENSE' | 'SAVING' | 'TRANSFER' | 'ADJUSTMENT';
export type TransactionOrigin = 'MANUAL' | 'IMPORT' | 'RECURRENT' | 'SYSTEM';
export type TransactionStatus = 'CONFIRMED' | 'PENDING' | 'IGNORED';
export type BudgetComparisonStatus = 'OK' | 'WARNING' | 'EXCEEDED';
export type FinancialHealth = 'EXCELLENT' | 'HEALTHY' | 'WARNING' | 'CRITICAL';

export interface DevUser { id: string; email: string; fullName: string; createdAt: string }
export interface Profile { id: string; name: string; type: ProfileType; baseCurrency: string; activeYear: number; active: boolean; createdAt: string; updatedAt: string }
export interface Account { id: string; profileId: string; name: string; accountType: AccountType; currency: string; creditLimit: number | null; statementCloseDay: number | null; dueDay: number | null; active: boolean; createdAt: string; updatedAt: string }
export interface Category { id: string; profileId: string | null; parentId: string | null; name: string; type: CategoryType; scope: CategoryScope; active: boolean; createdAt: string; updatedAt: string }
export interface MoneyTransaction { id: string; profileId: string; accountId: string; categoryId: string; movementType: MovementType; realDate: string; budgetDate: string; amount: number; currency: string; description?: string; origin: TransactionOrigin; status: TransactionStatus; createdAt: string; updatedAt: string }
export interface BudgetYear { id: string; profileId: string; year: number; targetIncome: number | null; targetSaving: number | null; notes: string | null; createdAt: string; updatedAt: string }
export interface BudgetCategoryItem { id: string; budgetMonthId: string; categoryId: string; categoryName: string; categoryType: CategoryType; budgetAmount: number; createdAt: string; updatedAt: string }
export interface BudgetMonth { id: string; budgetYearId: string; month: number; notes: string | null; items: BudgetCategoryItem[]; createdAt: string; updatedAt: string }
export interface BudgetComparisonItem { categoryId: string; categoryName: string; categoryType: CategoryType; budgetAmount: number; realAmount: number; difference: number; percentUsed: number; status: BudgetComparisonStatus }
export interface BudgetComparison { profileId: string; year: number; month: number; totalBudget: number; totalReal: number; totalDifference: number; items: BudgetComparisonItem[] }
export interface MonthlyBalance { totalIncome: number; totalExpenses: number; savings: number; balance: number }
export interface FiftyThirtyTwenty { fixedPercent: number; variablePercent: number; savingPercent: number }
export interface BudgetSummary { totalBudget: number; totalReal: number; totalDifference: number; exceededCount: number; warningCount: number }
export interface CategoryBreakdown { categoryId: string; categoryName: string; categoryType: CategoryType; totalAmount: number; percentOfIncome: number; movementCount: number }
export interface DashboardSummary { monthlyBalance: MonthlyBalance; fiftyThirtyTwenty: FiftyThirtyTwenty; fixedExpenses: number; variableExpenses: number; financialHealth: FinancialHealth; categoryBreakdown: CategoryBreakdown[]; budgetSummary: BudgetSummary | null }
