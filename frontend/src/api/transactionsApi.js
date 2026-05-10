import { http } from './http';
export const listTransactions = (pid, year, month) => http.get(`/api/profiles/${pid}/transactions?year=${year}&month=${month}`).then(r => r.data);
export const createTransaction = (p) => http.post('/api/transactions', p).then(r => r.data);
export const getTransaction = (id) => http.get(`/api/transactions/${id}`).then(r => r.data);
export const updateTransaction = (id, p) => http.put(`/api/transactions/${id}`, p).then(r => r.data);
export const deleteTransaction = (id) => http.delete(`/api/transactions/${id}`).then(r => r.data);
