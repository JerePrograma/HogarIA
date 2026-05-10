import { http } from './http';
export const listDevUsers = () => http.get('/api/dev/users').then((response) => response.data);
export const createDevUser = (payload) => http.post('/api/dev/users', payload).then((response) => response.data);
export const getDevUser = (id) => http.get(`/api/dev/users/${id}`).then((response) => response.data);
