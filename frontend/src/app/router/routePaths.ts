export const routePaths = {
  devUser: "/dev-user",
  profiles: "/profiles",
  profileRoot: (profileId: string) => `/profiles/${profileId}`,
  dashboard: (profileId: string) => `/profiles/${profileId}/dashboard`,
  accounts: (profileId: string) => `/profiles/${profileId}/accounts`,
  categories: (profileId: string) => `/profiles/${profileId}/categories`,
  transactions: (profileId: string) => `/profiles/${profileId}/transactions`,
  transactionImport: (profileId: string) => `/profiles/${profileId}/transactions/import`,
  transactionRecategorize: (profileId: string) =>
    `/profiles/${profileId}/transactions/recategorize`,
  budgets: (profileId: string) => `/profiles/${profileId}/budgets`,
  goals: (profileId: string) => `/profiles/${profileId}/goals`,
  habits: (profileId: string) => `/profiles/${profileId}/habits`,
  inflation: (profileId: string) => `/profiles/${profileId}/inflation`,
  planning: (profileId: string) => `/profiles/${profileId}/planning`,
  monthlyPlanningLegacy: (profileId: string) =>
    `/profiles/${profileId}/monthly-planning`,
  externalLoans: (profileId: string) =>
    `/profiles/${profileId}/prestamos-externos`,
} as const;

export const planningPathSegments = {
  monthly: "monthly",
  items: "monthly/items",
  suggestions: "monthly/suggestions",
  itemNew: "monthly/items/new",
  itemEdit: "monthly/items/:itemId/edit",
  import: "monthly/import",
  quickText: "monthly/quick-text",
  bancoProvincia: "monthly/external-debts/banco-provincia",
  alerts: "monthly/alerts",
  convert: "monthly/convert",
  reconciliation: "monthly/reconciliation",
} as const;
