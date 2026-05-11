import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { getExternalLoanSyncConfig, saveExternalLoanSyncConfig, syncExternalLoans, dryRunExternalLoans, } from '../../../api/externalLoansApi';
export function useExternalLoanSyncConfig(profileId) {
    return useQuery({
        queryKey: ['external-loans-sync-config', profileId],
        queryFn: () => getExternalLoanSyncConfig(profileId),
        enabled: Boolean(profileId),
    });
}
export function useSaveExternalLoanSyncConfig(profileId) {
    const queryClient = useQueryClient();
    return useMutation({
        mutationFn: (payload) => saveExternalLoanSyncConfig(profileId, payload),
        onSuccess: () => {
            queryClient.invalidateQueries({ queryKey: ['external-loans-sync-config', profileId] });
        },
    });
}
export function useSyncExternalLoans(profileId) {
    const queryClient = useQueryClient();
    return useMutation({
        mutationFn: () => syncExternalLoans(profileId),
        onSuccess: () => {
            queryClient.invalidateQueries({ queryKey: ['external-loans-summary', profileId] });
        },
    });
}
export function useDryRunExternalLoans(profileId) {
    return useMutation({
        mutationFn: () => dryRunExternalLoans(profileId),
    });
}
