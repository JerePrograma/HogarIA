import { http } from './http';
export const listAccounts = (pid) => http.get(`/api/profiles/${pid}/accounts`).then(r => r.data);
export const createAccount = (pid, p) => http.post(`/api/profiles/${pid}/accounts`, p).then(r => r.data);
export const getAccount = (id) => http.get(`/api/accounts/${id}`).then(r => r.data);
export const updateAccount = (id, p) => http.put(`/api/accounts/${id}`, p).then(r => r.data);
export const deleteAccount = (id) => http.delete(`/api/accounts/${id}`).then(r => r.data);
