import { http } from './http';

export type DevUser = {
  id: string;
  email: string;
  displayName?: string | null;
};

export type DevUserCreateRequest = {
  email: string;
  displayName?: string | null;
};

export const createDevUser = async (
  payload: DevUserCreateRequest,
): Promise<DevUser> => {
  const { data } = await http.post<DevUser>('/api/dev/users', payload);
  return data;
};

export const listDevUsers = async (): Promise<DevUser[]> => {
  const { data } = await http.get<DevUser[]>('/api/dev/users');
  return data;
};

export const getDevUser = async (id: string): Promise<DevUser> => {
  const { data } = await http.get<DevUser>(`/api/dev/users/${id}`);
  return data;
};