export const queryKeys = {
  profiles: ["profiles"] as const,

  dashboard: (profileId: string, year?: number, month?: number) =>
    year && month
      ? (["dashboard", profileId, year, month] as const)
      : (["dashboard", profileId] as const),

  budgetComparison: (profileId: string, year?: number, month?: number) =>
    year && month
      ? (["budget-comp", profileId, year, month] as const)
      : (["budget-comp", profileId] as const),

  transactions: (profileId: string, year?: number, month?: number) =>
    year && month
      ? (["tx", profileId, year, month] as const)
      : (["tx", profileId] as const),

  transactionImports: (profileId: string) =>
    ["transaction-imports", profileId] as const,

  bulkRecategorize: (profileId: string) =>
    ["bulk-recategorize", profileId] as const,

  accounts: (profileId: string) => ["accounts", profileId] as const,

  categories: (profileId: string, includeGlobal?: boolean) =>
    includeGlobal == null
      ? (["categories", profileId] as const)
      : (["categories", profileId, includeGlobal] as const),

  planning: (profileId: string, year: number, month: number) =>
    ["planning", profileId, year, month] as const,

  monthlyPlanReconciliation: (profileId: string, year: number, month: number) =>
    ["planning-reconciliation", profileId, year, month] as const,
};
