import { useQuery } from '@tanstack/react-query';
import { getExternalLoansSummary } from '../../../api/externalLoansApi';
export function useExternalLoansSummary(profileId) {
    return useQuery({
        queryKey: ['external-loans-summary', profileId],
        queryFn: () => getExternalLoansSummary(profileId),
        enabled: Boolean(profileId),
    });
}
