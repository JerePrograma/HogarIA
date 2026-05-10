import { http } from './http';
export const getMonthlyPlan = (profileId, year, month) => http.get(`/api/profiles/${profileId}/planning/monthly?year=${year}&month=${month}`).then(r => r.data);
export const createMonthlyPlanItem = (profileId, payload) => http.post(`/api/profiles/${profileId}/planning/items`, payload).then(r => r.data);
export const updateMonthlyPlanItem = (profileId, itemId, payload) => http.put(`/api/profiles/${profileId}/planning/items/${itemId}`, payload).then(r => r.data);
export const deleteMonthlyPlanItem = (profileId, itemId) => http.delete(`/api/profiles/${profileId}/planning/items/${itemId}`).then(r => r.data);
export const convertMonthlyPlanItemToTransaction = (profileId, itemId) => http.post(`/api/profiles/${profileId}/planning/items/${itemId}/convert-to-transaction`).then(r => r.data);
