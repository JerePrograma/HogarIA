import { http } from './http';

export type DevUser = {
  id: string;
  email: string;
  fullName: string;
  createdAt?: string | null;
};

export type DevUserCreateRequest = {
  email: string;
  password: string;
  fullName: string;
};

export const createDevUser = async (
  payload: DevUserCreateRequest,
): Promise<DevUser> => {
  const { data } = await http.post<DevUser>('/dev/users', payload);
  return data;
};

export const listDevUsers = async (): Promise<DevUser[]> => {
  const { data } = await http.get<DevUser[]>('/dev/users');
  return data;
};

export const getDevUser = async (id: string): Promise<DevUser> => {
  const { data } = await http.get<DevUser>(`/dev/users/${id}`);
  return data;
};