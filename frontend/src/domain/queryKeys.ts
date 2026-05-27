// src/domain/queryKeys.ts

export const queryKeys = {
  profiles: ["profiles"] as const,

  devUsers: ["dev-users"] as const,

  profile: (profileId: string) => ["profiles", profileId] as const,

  dashboard: (profileId: string, year?: number, month?: number) =>
    year != null && month != null
      ? (["dashboard", profileId, year, month] as const)
      : (["dashboard", profileId] as const),

  accounts: (profileId: string) => ["accounts", profileId] as const,

  account: (profileId: string, accountId: string) =>
    ["accounts", profileId, accountId] as const,

  categories: (profileId: string, includeGlobal?: boolean) =>
    includeGlobal == null
      ? (["categories", profileId] as const)
      : (["categories", profileId, includeGlobal] as const),

  category: (profileId: string, categoryId: string) =>
    ["categories", profileId, categoryId] as const,

  transactions: (profileId: string, year?: number, month?: number) =>
    year != null && month != null
      ? (["tx", profileId, year, month] as const)
      : (["tx", profileId] as const),

  transaction: (profileId: string, transactionId: string) =>
    ["tx", profileId, transactionId] as const,

  transactionDataQuality: (profileId: string, year: number, month: number) =>
    ["tx-quality", profileId, year, month] as const,

  budgetYears: (profileId: string) => ["budgets", profileId, "years"] as const,

  budgetYear: (profileId: string, year: number) =>
    ["budgets", profileId, "years", year] as const,

  budgetMonth: (profileId: string, year: number, month: number) =>
    ["budgets", profileId, "months", year, month] as const,

  budgetComparison: (profileId: string, year?: number, month?: number) =>
    year != null && month != null
      ? (["budget-comp", profileId, year, month] as const)
      : (["budget-comp", profileId] as const),

  transactionImports: (profileId: string) =>
    ["transaction-imports", profileId] as const,

  transactionImportBatch: (profileId: string, batchId: string) =>
    ["transaction-imports", profileId, batchId] as const,

  budgetExcelImports: (profileId: string) =>
    ["budget-excel-imports", profileId] as const,

  budgetExcelImportBatch: (profileId: string, batchId: string) =>
    ["budget-excel-imports", profileId, batchId] as const,

  bulkRecategorize: (profileId: string) =>
    ["bulk-recategorize", profileId] as const,

  goals: (profileId: string) => ["goals", profileId] as const,

  goalSuggestions: (profileId: string) =>
    ["goals", profileId, "suggestions"] as const,

  habits: (profileId: string) => ["habits", profileId] as const,

  habitSummary: (profileId: string, year?: number, month?: number) =>
    year != null && month != null
      ? (["habits", profileId, "summary", year, month] as const)
      : (["habits", profileId, "summary"] as const),

  inflation: (profileId: string) => ["inflation", profileId] as const,

  inflationYear: (year: number) => ["inflation", "year", year] as const,

  inflationAccumulatedYear: (year: number) =>
    ["inflation", "year", year, "accumulated"] as const,

  inflationAccumulated: (profileId: string, from?: string, to?: string) =>
    from && to
      ? (["inflation", profileId, "accumulated", from, to] as const)
      : (["inflation", profileId, "accumulated"] as const),

  planning: (profileId: string, year: number, month: number) =>
    ["planning", profileId, year, month] as const,

  monthlyPlanItems: (profileId: string, year: number, month: number) =>
    ["planning", profileId, year, month, "items"] as const,

  monthlyPlanItem: (profileId: string, itemId: string) =>
    ["planning", profileId, "items", itemId] as const,

  monthlyPlanSuggestions: (profileId: string) =>
    ["planning", profileId, "suggestions"] as const,

  budgetPlanningSuggestions: (profileId: string, year?: number, month?: number) =>
    year != null && month != null
      ? (["budget-planning-suggestions", profileId, year, month] as const)
      : (["budget-planning-suggestions", profileId] as const),

  monthlyPlanQuickCapture: (profileId: string) =>
    ["planning", profileId, "quick-capture"] as const,

  monthlyPlanQuickText: (profileId: string) =>
    ["planning", profileId, "quick-text"] as const,

  monthlyPlanReconciliation: (profileId: string, year: number, month: number) =>
    ["planning-reconciliation", profileId, year, month] as const,

  externalLoans: (profileId: string) => ["external-loans", profileId] as const,

  externalLoanSyncConfig: (profileId: string) =>
    ["external-loans", profileId, "sync-config"] as const,
};
