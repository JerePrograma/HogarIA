import { http } from './http';
export const listGoals = (profileId) => http.get(`/api/profiles/${profileId}/goals`).then(r => r.data);
export const createGoal = (profileId, payload) => http.post(`/api/profiles/${profileId}/goals`, payload).then(r => r.data);
export const createEmergencyFund = (profileId, coverageMonths) => http.post(`/api/profiles/${profileId}/goals/emergency-fund`, { coverageMonths }).then(r => r.data);
export const deleteGoal = (profileId, goalId) => http.delete(`/api/profiles/${profileId}/goals/${goalId}`).then(r => r.data);
