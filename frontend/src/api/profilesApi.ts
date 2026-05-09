import { http } from './http';
import type { Profile, ProfileType } from '../domain/types';

export interface ProfileCreateRequest {
  name: string;
  type: ProfileType;
  baseCurrency: string;
  activeYear: number;
}

export interface ProfileUpdateRequest extends ProfileCreateRequest {
  active?: boolean;
}

export const listProfiles = (): Promise<Profile[]> =>
  http.get<Profile[]>('/api/profiles').then((response) => response.data);

export const createProfile = (payload: ProfileCreateRequest): Promise<Profile> =>
  http.post<Profile>('/api/profiles', payload).then((response) => response.data);

export const getProfile = (id: string): Promise<Profile> =>
  http.get<Profile>(`/api/profiles/${id}`).then((response) => response.data);

export const updateProfile = (id: string, payload: ProfileUpdateRequest): Promise<Profile> =>
  http.put<Profile>(`/api/profiles/${id}`, payload).then((response) => response.data);

export const deleteProfile = (id: string): Promise<void> =>
  http.delete<void>(`/api/profiles/${id}`).then((response) => response.data);
