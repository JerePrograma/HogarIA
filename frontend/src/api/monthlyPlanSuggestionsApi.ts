import { http } from './http';
import type { PlanningSuggestionRequest, PlanningSuggestionResponse } from '../domain/types';

export const getMonthlyPlanSuggestions = (profileId: string, payload: PlanningSuggestionRequest) =>
  http.post<PlanningSuggestionResponse>(`/profiles/${profileId}/planning/suggestions`, payload).then((r) => r.data);
