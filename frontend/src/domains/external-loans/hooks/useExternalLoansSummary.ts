import { useQuery } from '@tanstack/react-query';
import { getExternalLoansSummary } from '../../../api/externalLoansApi';
import { queryKeys } from '../../../domain/queryKeys';
import type { ExternalLoansSummaryResponse } from '../types';

export function useExternalLoansSummary(profileId: string) {
  return useQuery<ExternalLoansSummaryResponse>({
    queryKey: queryKeys.externalLoans(profileId),
    queryFn: () => getExternalLoansSummary(profileId),
    enabled: Boolean(profileId),
  });
}
