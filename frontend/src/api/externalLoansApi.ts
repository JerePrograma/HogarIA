import { http } from './http';
import type {
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
