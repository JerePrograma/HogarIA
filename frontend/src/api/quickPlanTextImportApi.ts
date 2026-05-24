import { http } from './http';

export interface QuickPlanPreviewRequest { rawText: string; periodYear: number; periodMonth: number; defaultAmountScale?: 'UNITS'|'THOUSANDS'; approximateMargin?: number; currency?: string; }
export interface QuickPlanCandidate { lineNumber: number; rawLine: string; item: any; suggestedCategoryId?: string|null; suggestedCategoryName?: string|null; warnings: string[]; duplicate: boolean; }
export interface QuickPlanPreviewResponse { candidates: QuickPlanCandidate[]; warnings: string[]; }
export interface QuickPlanCommitResponse { created: any[]; warnings: string[]; }

export const previewQuickPlanText = (profileId: string, payload: QuickPlanPreviewRequest) =>
  http.post<QuickPlanPreviewResponse>(`/profiles/${profileId}/planning/quick-text/preview`, payload).then(r => r.data);

export const commitQuickPlanText = (profileId: string, payload: { items: any[] }) =>
  http.post<QuickPlanCommitResponse>(`/profiles/${profileId}/planning/quick-text/commit`, payload).then(r => r.data);
