import { http } from './http';
export const listProfiles = () => http.get('/api/profiles').then(r => r.data);
export const createProfile = (p) => http.post('/api/profiles', p).then(r => r.data);
export const getProfile = (id) => http.get(`/api/profiles/${id}`).then(r => r.data);
export const updateProfile = (id, p) => http.put(`/api/profiles/${id}`, p).then(r => r.data);
export const deleteProfile = (id) => http.delete(`/api/profiles/${id}`).then(r => r.data);
