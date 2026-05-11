import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import {
  getExternalLoanSyncConfig,
  saveExternalLoanSyncConfig,
  syncExternalLoans,
  dryRunExternalLoans,
} from '../../../api/externalLoansApi';
import type {
  ExternalLoanManualSyncResponse,
  ExternalLoanSyncConfig,
  ExternalLoanSyncConfigPayload,
} from '../types';

export function useExternalLoanSyncConfig(profileId: string) {
  return useQuery<ExternalLoanSyncConfig | null>({
    queryKey: ['external-loans-sync-config', profileId],
    queryFn: () => getExternalLoanSyncConfig(profileId),
    enabled: Boolean(profileId),
  });
}

export function useSaveExternalLoanSyncConfig(profileId: string) {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (payload: ExternalLoanSyncConfigPayload) => saveExternalLoanSyncConfig(profileId, payload),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['external-loans-sync-config', profileId] });
    },
  });
}

export function useSyncExternalLoans(profileId: string) {
  const queryClient = useQueryClient();

  return useMutation<ExternalLoanManualSyncResponse>({
    mutationFn: () => syncExternalLoans(profileId),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['external-loans-summary', profileId] });
    },
  });
}


export function useDryRunExternalLoans(profileId: string) {
  return useMutation<ExternalLoanManualSyncResponse>({
    mutationFn: () => dryRunExternalLoans(profileId),
  });
}
