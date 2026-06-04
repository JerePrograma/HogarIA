// src/api/categoriesApi.ts

import { http } from './http';
import type { Category } from '../domain/types';

export interface CategoryCreateRequest {
  name: string;
  type: Category['type'];
  scope: Category['scope'];
  parentId?: string | null;
}

export interface CategoryUpdateRequest {
  name?: string;
  type?: Category['type'];
  scope?: Category['scope'];
  parentId?: string | null;
  active?: boolean;
}

export const listCategories = (
  profileId: string,
  includeGlobal: boolean,
): Promise<Category[]> =>
  http
    .get(`/profiles/${profileId}/categories`, {
      params: { includeGlobal },
    })
    .then((response) => response.data);

export const createCategory = (
  profileId: string,
  payload: CategoryCreateRequest,
): Promise<Category> =>
  http
    .post(`/profiles/${profileId}/categories`, payload)
    .then((response) => response.data);

export const getCategory = (id: string): Promise<Category> =>
  http.get(`/categories/${id}`).then((response) => response.data);

export const updateCategory = (
  id: string,
  payload: CategoryUpdateRequest,
): Promise<Category> =>
  http.put(`/categories/${id}`, payload).then((response) => response.data);

export const deleteCategory = (id: string): Promise<void> =>
  http.delete(`/categories/${id}`).then((response) => response.data);
