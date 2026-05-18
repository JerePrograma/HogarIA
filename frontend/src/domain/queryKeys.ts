export const queryKeys = {
  profiles: ['profiles'] as const,
  dashboard: (profileId: string, year: number, month: number) =>
    ['dashboard', profileId, year, month] as const,
  planning: (profileId: string, year: number, month: number) =>
    ['planning', profileId, year, month] as const,
  monthlyPlanReconciliation: (profileId: string, year: number, month: number) =>
    ['planning-reconciliation', profileId, year, month] as const,
  transactions: (profileId: string, year: number, month: number) =>
    ['tx', profileId, year, month] as const,
  accounts: (profileId: string) => ['accounts', profileId] as const,
  categories: (profileId: string, includeGlobal?: boolean) =>
    includeGlobal == null
      ? (['categories', profileId] as const)
      : (['categories', profileId, includeGlobal] as const),
};
