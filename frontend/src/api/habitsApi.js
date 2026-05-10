import { http } from './http';
export const listHabits = (profileId) => http.get(`/api/profiles/${profileId}/habits`).then((r) => r.data);
export const createHabit = (profileId, payload) => http.post(`/api/profiles/${profileId}/habits`, payload).then((r) => r.data);
export const upsertHabitCheckin = (profileId, habitId, date, payload) => http.put(`/api/profiles/${profileId}/habits/${habitId}/checkins/${date}`, payload).then((r) => r.data);
