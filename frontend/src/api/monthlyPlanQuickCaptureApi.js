import { http } from './http';
export const previewMonthlyPlanQuickCapture = (profileId, payload) => http.post(`/api/profiles/${profileId}/planning/quick-capture/preview`, payload).then(r => r.data);
export const commitMonthlyPlanQuickCapture = (profileId, payload) => http.post(`/api/profiles/${profileId}/planning/quick-capture/commit`, payload).then(r => r.data);
