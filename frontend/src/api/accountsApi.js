import { http } from './http';
export const listAccounts = (profileId) => http.get(`/api/profiles/${profileId}/accounts`).then((response) => response.data);
export const createAccount = (profileId, payload) => http.post(`/api/profiles/${profileId}/accounts`, payload).then((response) => response.data);
export const getAccount = (id) => http.get(`/api/accounts/${id}`).then((response) => response.data);
export const updateAccount = (id, payload) => http.put(`/api/accounts/${id}`, payload).then((response) => response.data);
export const deleteAccount = (id) => http.delete(`/api/accounts/${id}`).then((response) => response.data);
