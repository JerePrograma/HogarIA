import { routePaths } from '../../app/router/routePaths';

export const planningRoutes = {
  root: routePaths.planning,
  monthly: (profileId: string) => `${routePaths.planning(profileId)}/monthly`,
  items: (profileId: string) => `${routePaths.planning(profileId)}/monthly/items`,
  itemNew: (profileId: string) => `${routePaths.planning(profileId)}/monthly/items/new`,
  itemEdit: (profileId: string, itemId: string) =>
    `${routePaths.planning(profileId)}/monthly/items/${itemId}/edit`,
  import: (profileId: string) => `${routePaths.planning(profileId)}/monthly/import`,
  suggestions: (profileId: string) =>
    `${routePaths.planning(profileId)}/monthly/suggestions`,
  quickText: (profileId: string) =>
    `${routePaths.planning(profileId)}/monthly/quick-text`,
  alerts: (profileId: string) => `${routePaths.planning(profileId)}/monthly/alerts`,
  convert: (profileId: string) => `${routePaths.planning(profileId)}/monthly/convert`,
  reconciliation: (profileId: string) =>
    `${routePaths.planning(profileId)}/monthly/reconciliation`,
};
