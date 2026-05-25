import { http } from './http';
import type {
  BudgetPlanningSuggestionCommitRequest,
  BudgetPlanningSuggestionCommitResponse,
  BudgetPlanningSuggestionPreviewRequest,
  BudgetPlanningSuggestionPreviewResponse,
} from '../domain/types';

export const previewBudgetPlanningSuggestions = (
  profileId: string,
  payload: BudgetPlanningSuggestionPreviewRequest,
): Promise<BudgetPlanningSuggestionPreviewResponse> =>
  http
    .post(`/profiles/${profileId}/budget-planning-suggestions/preview`, payload)
    .then((response) => response.data);

export const commitBudgetPlanningSuggestions = (
  profileId: string,
  payload: BudgetPlanningSuggestionCommitRequest,
): Promise<BudgetPlanningSuggestionCommitResponse> =>
  http
    .post(`/profiles/${profileId}/budget-planning-suggestions/commit`, payload)
    .then((response) => response.data);
