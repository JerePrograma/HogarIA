import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import {
  applyExternalLoanBackfill,
  dryRunExternalLoans,
  dryRunExternalLoanBackfill,
  getExternalLoanHealth,
  getExternalLoanIdempotencyDiagnostics,
  getExternalLoanSyncConfig,
  saveExternalLoanSyncConfig,
  syncExternalLoans,
} from '../../../api/externalLoansApi';
import { queryKeys } from '../../../domain/queryKeys';
import type {
  ExternalIntegrationDiagnosticResponse,
  ExternalLoanBackfillApplyRequest,
  ExternalLoanBackfillApplyResponse,
  ExternalLoanBackfillDryRunResponse,
  ExternalLoanIdempotencyDiagnosticsResponse,
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

export function useExternalLoanHealth(profileId: string) {
  return useQuery<ExternalIntegrationDiagnosticResponse>({
    queryKey: queryKeys.externalLoanHealth(profileId),
    queryFn: () => getExternalLoanHealth(profileId),
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

export function useExternalLoanIdempotencyDiagnostics(profileId: string) {
  return useMutation<ExternalLoanIdempotencyDiagnosticsResponse>({
    mutationFn: () => getExternalLoanIdempotencyDiagnostics(profileId),
  });
}

export function useBackfillDryRunExternalLoans(profileId: string) {
  return useMutation<ExternalLoanBackfillDryRunResponse>({
    mutationFn: () => dryRunExternalLoanBackfill(profileId),
  });
}

export function useApplyExternalLoanBackfill(profileId: string) {
  const queryClient = useQueryClient();

  return useMutation<ExternalLoanBackfillApplyResponse, Error, ExternalLoanBackfillApplyRequest>({
    mutationFn: (payload) => applyExternalLoanBackfill(profileId, payload),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: queryKeys.externalLoans(profileId) });
      queryClient.invalidateQueries({ queryKey: queryKeys.dashboard(profileId) });
      queryClient.invalidateQueries({ queryKey: queryKeys.transactions(profileId) });
    },
  });
}
