import { http } from './http';
import type { MonthlyPlanItem, MonthlyPlanItemType, MonthlyPlanPriority } from '../domain/types';

export interface QuickPlanPreviewRequest { rawText: string; periodYear: number; periodMonth: number; defaultAmountScale?: 'UNITS'|'THOUSANDS'; approximateMargin?: number; currency?: string; }
export interface NormalizedCandidate { lineNumber:number; title:string; type:MonthlyPlanItemType; priority:MonthlyPlanPriority; expectedDate?:string|null; periodYear:number; periodMonth:number; amount?:number|null; minAmount?:number|null; maxAmount?:number|null; categoryId?:string|null; accountId?:string|null; }
export interface QuickPlanCandidate { lineNumber: number; rawLine: string; candidate: NormalizedCandidate; suggestedCategoryId?: string|null; suggestedCategoryName?: string|null; warnings: string[]; duplicate: boolean; }
export interface QuickPlanPreviewResponse { candidates: QuickPlanCandidate[]; warnings: string[]; }
export interface QuickPlanCommitResponse { created: MonthlyPlanItem[]; warnings: string[]; skippedDuplicates:number; }

export const previewQuickPlanText = (profileId: string, payload: QuickPlanPreviewRequest) =>
  http.post<QuickPlanPreviewResponse>(`/profiles/${profileId}/planning/quick-text/preview`, payload).then(r => r.data);

export const commitQuickPlanText = (profileId: string, payload: { periodYear?:number; periodMonth?:number; candidates: NormalizedCandidate[]; skipDuplicates:boolean }) =>
  http.post<QuickPlanCommitResponse>(`/profiles/${profileId}/planning/quick-text/commit`, payload).then(r => r.data);
