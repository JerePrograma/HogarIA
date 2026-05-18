import { useMemo } from 'react';
import { useMutation } from '@tanstack/react-query';
import {
  confirmPlanTransactionMatch,
  deletePlanTransactionMatch,
} from '../api/monthlyPlanReconciliationApi';
import { getApiErrorMessage } from '../api/http';
import type { ConfirmPlanTransactionMatchPayload } from '../domain/types';

type Params = {
  profileId: string;
  invalidatePlanningViews: () => void;
};

export function useMonthlyPlanReconciliationActions({
  profileId,
  invalidatePlanningViews,
}: Params) {
  const confirmMutation = useMutation({
    mutationFn: (payload: ConfirmPlanTransactionMatchPayload) =>
      confirmPlanTransactionMatch(profileId, payload),
    onSuccess: invalidatePlanningViews,
  });

  const deleteMutation = useMutation({
    mutationFn: (matchId: string) => deletePlanTransactionMatch(profileId, matchId),
    onSuccess: invalidatePlanningViews,
  });

  const pendingMatchId = useMemo(
    () =>
      (confirmMutation.isPending ? confirmMutation.variables?.transactionId : null)
      ?? (deleteMutation.isPending ? deleteMutation.variables : null)
      ?? null,
    [
      confirmMutation.isPending,
      confirmMutation.variables,
      deleteMutation.isPending,
      deleteMutation.variables,
    ],
  );

  const error = confirmMutation.error ?? deleteMutation.error;

  return {
    confirmMatch: (payload: ConfirmPlanTransactionMatchPayload) =>
      confirmMutation.mutate(payload),
    deleteMatch: (matchId: string) => deleteMutation.mutate(matchId),
    pendingMatchId,
    reconciliationErrorMessage: error ? getApiErrorMessage(error) : null,
  };
}