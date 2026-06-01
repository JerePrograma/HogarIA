import { http } from './http';
import type {
  ExternalIntegrationDiagnosticResponse,
  ExternalLoanBackfillApplyRequest,
  ExternalLoanBackfillApplyResponse,
  ExternalLoanBackfillDryRunResponse,
  ExternalLoanIdempotencyDiagnosticsResponse,
  ExternalLoanManualSyncResponse,
  ExternalLoanSyncConfig,
  ExternalLoanSyncConfigPayload,
  ExternalLoansSummaryResponse,
} from '../domains/external-loans/types';

export const getExternalLoansSummary = async (profileId: string): Promise<ExternalLoansSummaryResponse> => {
  const response = await http.get<ExternalLoansSummaryResponse>(`/profiles/${profileId}/external-loans/summary`);
  return response.data;
};

export const getExternalLoanSyncConfig = async (profileId: string): Promise<ExternalLoanSyncConfig | null> => {
  const response = await http.get<ExternalLoanSyncConfig | null>(`/profiles/${profileId}/external-loans/sync-config`);
  return response.data;
};

export const getExternalLoanHealth = async (profileId: string): Promise<ExternalIntegrationDiagnosticResponse> => {
  const response = await http.get<ExternalIntegrationDiagnosticResponse>(`/profiles/${profileId}/external-loans/health`);
  return response.data;
};

export const saveExternalLoanSyncConfig = async (
  profileId: string,
  payload: ExternalLoanSyncConfigPayload,
): Promise<ExternalLoanSyncConfig> => {
  const response = await http.put<ExternalLoanSyncConfig>(`/profiles/${profileId}/external-loans/sync-config`, payload);
  return response.data;
};

export const syncExternalLoans = async (profileId: string): Promise<ExternalLoanManualSyncResponse> => {
  const response = await http.post<ExternalLoanManualSyncResponse>(`/profiles/${profileId}/external-loans/sync`);
  return response.data;
};

export const dryRunExternalLoans = async (profileId: string): Promise<ExternalLoanManualSyncResponse> => {
  const response = await http.post<ExternalLoanManualSyncResponse>(`/profiles/${profileId}/external-loans/sync/dry-run`);
  return response.data;
};

export const getExternalLoanIdempotencyDiagnostics = async (
  profileId: string,
): Promise<ExternalLoanIdempotencyDiagnosticsResponse> => {
  const response = await http.get<ExternalLoanIdempotencyDiagnosticsResponse>(
    `/profiles/${profileId}/external-loans/idempotency/diagnostics`,
  );
  return response.data;
};

export const dryRunExternalLoanBackfill = async (
  profileId: string,
): Promise<ExternalLoanBackfillDryRunResponse> => {
  const response = await http.post<ExternalLoanBackfillDryRunResponse>(
    `/profiles/${profileId}/external-loans/backfill/dry-run`,
  );
  return response.data;
};

export const applyExternalLoanBackfill = async (
  profileId: string,
  payload: ExternalLoanBackfillApplyRequest,
): Promise<ExternalLoanBackfillApplyResponse> => {
  const response = await http.post<ExternalLoanBackfillApplyResponse>(
    `/profiles/${profileId}/external-loans/backfill/apply`,
    payload,
  );
  return response.data;
};
