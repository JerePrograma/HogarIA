import { http } from './http';
import type { ExternalLoansSummaryResponse } from '../features/external-loans/types';

export const getExternalLoansSummary = async (profileId: string): Promise<ExternalLoansSummaryResponse> => {
  const response = await http.get<ExternalLoansSummaryResponse>(`/api/profiles/${profileId}/external-loans/summary`);
  return response.data;
};
