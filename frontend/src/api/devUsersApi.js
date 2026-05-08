import { http } from './http';
export const listDevUsers = () => http.get('/api/dev/users').then(r => r.data);
export const createDevUser = (p) => http.post('/api/dev/users', p).then(r => r.data);
export const getDevUser = (id) => http.get(`/api/dev/users/${id}`).then(r => r.data);
