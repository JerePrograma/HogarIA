// src/api/categoriesApi.ts

import { http } from './http';
import type { Category } from '../domain/types';

export interface CategoryCreateRequest {
  name: string;
  type: Category['type'];
  scope: Category['scope'];
  parentId?: string;
}

export interface CategoryUpdateRequest extends CategoryCreateRequest {
  active?: boolean;
}

export const listCategories = (
  profileId: string,
  includeGlobal: boolean,
): Promise<Category[]> =>
  http
    .get(`/api/profiles/${profileId}/categories`, {
      params: { includeGlobal },
    })
    .then((response) => response.data);

export const createCategory = (
  profileId: string,
  payload: CategoryCreateRequest,
): Promise<Category> =>
  http
    .post(`/api/profiles/${profileId}/categories`, payload)
    .then((response) => response.data);

export const getCategory = (id: string): Promise<Category> =>
  http.get(`/api/categories/${id}`).then((response) => response.data);

export const updateCategory = (
  id: string,
  payload: CategoryUpdateRequest,
): Promise<Category> =>
  http.put(`/api/categories/${id}`, payload).then((response) => response.data);

export const deleteCategory = (id: string): Promise<void> =>
  http.delete(`/api/categories/${id}`).then((response) => response.data);