// src/api/categoriesApi.ts
import { http } from './http';
export const listCategories = (profileId, includeGlobal) => http
    .get(`/api/profiles/${profileId}/categories`, {
    params: { includeGlobal },
})
    .then((response) => response.data);
export const createCategory = (profileId, payload) => http
    .post(`/api/profiles/${profileId}/categories`, payload)
    .then((response) => response.data);
export const getCategory = (id) => http.get(`/api/categories/${id}`).then((response) => response.data);
export const updateCategory = (id, payload) => http.put(`/api/categories/${id}`, payload).then((response) => response.data);
export const deleteCategory = (id) => http.delete(`/api/categories/${id}`).then((response) => response.data);
