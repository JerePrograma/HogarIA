import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import {
  dryRunExternalLoans,
  getExternalLoanSyncConfig,
  saveExternalLoanSyncConfig,
  syncExternalLoans,
} from '../../../api/externalLoansApi';
import { queryKeys } from '../../../domain/queryKeys';
import type {
  ExternalLoanManualSyncResponse,
  ExternalLoanSyncConfig,
  ExternalLoanSyncConfigPayload,
} from '../types';

export function useExternalLoanSyncConfig(profileId: string) {
  return useQuery<ExternalLoanSyncConfig | null>({
    queryKey: queryKeys.externalLoanSyncConfig(profileId),
    queryFn: () => getExternalLoanSyncConfig(profileId),
    enabled: Boolean(profileId),
  });
}

export function useSaveExternalLoanSyncConfig(profileId: string) {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (payload: ExternalLoanSyncConfigPayload) =>
      saveExternalLoanSyncConfig(profileId, payload),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: queryKeys.externalLoanSyncConfig(profileId) });
      queryClient.invalidateQueries({ queryKey: queryKeys.externalLoans(profileId) });
    },
  });
}

export function useSyncExternalLoans(profileId: string) {
  const queryClient = useQueryClient();

  return useMutation<ExternalLoanManualSyncResponse>({
    mutationFn: () => syncExternalLoans(profileId),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: queryKeys.externalLoans(profileId) });
      queryClient.invalidateQueries({ queryKey: queryKeys.dashboard(profileId) });
      queryClient.invalidateQueries({ queryKey: queryKeys.transactions(profileId) });
    },
  });
}

export function useDryRunExternalLoans(profileId: string) {
  return useMutation<ExternalLoanManualSyncResponse>({
    mutationFn: () => dryRunExternalLoans(profileId),
  });
}
