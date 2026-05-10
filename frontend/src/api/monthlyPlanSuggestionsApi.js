import { http } from './http';
export const getMonthlyPlanSuggestions = (profileId, payload) => http.post(`/api/profiles/${profileId}/planning/suggestions`, payload).then((r) => r.data);
