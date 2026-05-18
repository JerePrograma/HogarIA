import { http } from './http';
import type { Habit, HabitCheckin, HabitFrequency } from '../domain/types';

export interface HabitCreateRequest {
  description: string;
  area?: string;
  frequency: HabitFrequency;
}

export interface HabitCheckinRequest {
  completed: boolean;
  note?: string;
}

export const listHabits = (profileId: string) =>
  http.get<Habit[]>(`/profiles/${profileId}/habits`).then((r) => r.data);

export const createHabit = (profileId: string, payload: HabitCreateRequest) =>
  http.post<Habit>(`/profiles/${profileId}/habits`, payload).then((r) => r.data);

export const upsertHabitCheckin = (
  profileId: string,
  habitId: string,
  date: string,
  payload: HabitCheckinRequest,
) =>
  http.put<HabitCheckin>(`/profiles/${profileId}/habits/${habitId}/checkins/${date}`, payload).then((r) => r.data);
