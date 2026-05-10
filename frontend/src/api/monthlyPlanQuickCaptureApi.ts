import { http } from './http';
import type { QuickCaptureCommitRequest, QuickCaptureCommitResponse, QuickCapturePreviewRequest, QuickCapturePreviewResponse } from '../domain/types';

export const previewMonthlyPlanQuickCapture = (profileId: string, payload: QuickCapturePreviewRequest) =>
  http.post<QuickCapturePreviewResponse>(`/api/profiles/${profileId}/planning/quick-capture/preview`, payload).then(r => r.data);

export const commitMonthlyPlanQuickCapture = (profileId: string, payload: QuickCaptureCommitRequest) =>
  http.post<QuickCaptureCommitResponse>(`/api/profiles/${profileId}/planning/quick-capture/commit`, payload).then(r => r.data);
