import { http } from './http';
export const listProfiles = () => http.get('/api/profiles').then((response) => response.data);
export const createProfile = (payload) => http.post('/api/profiles', payload).then((response) => response.data);
export const getProfile = (id) => http.get(`/api/profiles/${id}`).then((response) => response.data);
export const updateProfile = (id, payload) => http.put(`/api/profiles/${id}`, payload).then((response) => response.data);
export const deleteProfile = (id) => http.delete(`/api/profiles/${id}`).then((response) => response.data);
