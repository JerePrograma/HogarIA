import { useQuery } from '@tanstack/react-query';
import { getExternalLoansSummary } from '../../../api/externalLoansApi';
import type { ExternalLoansSummaryResponse } from '../types';

export function useExternalLoansSummary(profileId: string) {
  return useQuery<ExternalLoansSummaryResponse>({
    queryKey: ['external-loans-summary', profileId],
    queryFn: () => getExternalLoansSummary(profileId),
    enabled: Boolean(profileId),
  });
}
