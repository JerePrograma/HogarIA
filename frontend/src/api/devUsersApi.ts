import { http } from './http';
import type { DevUser } from '../domain/types';

export interface DevUserCreateRequest {
  fullName: string;
  email: string;
  password: string;
}

export const listDevUsers = (): Promise<DevUser[]> =>
  http.get<DevUser[]>('/api/dev/users').then((response) => response.data);

export const createDevUser = (payload: DevUserCreateRequest): Promise<DevUser> =>
  http.post<DevUser>('/api/dev/users', payload).then((response) => response.data);

export const getDevUser = (id: string): Promise<DevUser> =>
  http.get<DevUser>(`/api/dev/users/${id}`).then((response) => response.data);
