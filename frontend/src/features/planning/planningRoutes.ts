export const planningRoutes = {
  root: (profileId: string) => `/profiles/${profileId}/planning`,
  monthly: (profileId: string) => `/profiles/${profileId}/planning/monthly`,
  items: (profileId: string) => `/profiles/${profileId}/planning/monthly/items`,
  itemNew: (profileId: string) => `/profiles/${profileId}/planning/monthly/items/new`,
  itemEdit: (profileId: string, itemId: string) => `/profiles/${profileId}/planning/monthly/items/${itemId}/edit`,
  import: (profileId: string) => `/profiles/${profileId}/planning/monthly/import`,
  quickText: (profileId: string) => `/profiles/${profileId}/planning/monthly/quick-text`,
  alerts: (profileId: string) => `/profiles/${profileId}/planning/monthly/alerts`,
  convert: (profileId: string) => `/profiles/${profileId}/planning/monthly/convert`,
  reconciliation: (profileId: string) => `/profiles/${profileId}/planning/monthly/reconciliation`,
};
