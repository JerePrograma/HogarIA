import { useMutation } from '@tanstack/react-query';
import { useMemo } from 'react';
import { getApiErrorMessage } from '../api/http';
import { createMonthlyPlanItem, deleteMonthlyPlanItem, convertMonthlyPlanItemToTransaction, updateMonthlyPlanItem } from '../api/monthlyPlanningApi';
import { MonthlyPlanItemCreatePayload, MonthlyPlanItemUpdatePayload } from '../domain/types';

type Params = {
  profileId: string;
  year: number;
  month: number;
  form: MonthlyPlanItemCreatePayload;
  resetFormAfterCreate: (previous: MonthlyPlanItemCreatePayload) => void;
  invalidatePlanningViews: () => void;
};

export function useMonthlyPlanItemActions({
  profileId,
  year,
  month,
  form,
  resetFormAfterCreate,
  invalidatePlanningViews,
}: Params) {
  const createMutation = useMutation({
    mutationFn: () =>
      createMonthlyPlanItem(profileId, {
        ...form,
        periodYear: year,
        periodMonth: month,
      }),
    onSuccess: () => {
      resetFormAfterCreate(form);
      invalidatePlanningViews();
    },
  });

  const deleteMutation = useMutation({
    mutationFn: (id: string) => deleteMonthlyPlanItem(profileId, id),
    onSuccess: invalidatePlanningViews,
  });

  const convertMutation = useMutation({
    mutationFn: (id: string) => convertMonthlyPlanItemToTransaction(profileId, id),
    onSuccess: invalidatePlanningViews,
  });

  const updateMutation = useMutation({
    mutationFn: ({ id, payload }: { id: string; payload: MonthlyPlanItemUpdatePayload }) =>
      updateMonthlyPlanItem(profileId, id, payload),
    onSuccess: invalidatePlanningViews,
  });

  const cancelMutation = useMutation({
    mutationFn: (id: string) =>
      updateMonthlyPlanItem(profileId, id, { status: 'CANCELLED' }),
    onSuccess: invalidatePlanningViews,
  });

  const markPaidMutation = useMutation({
    mutationFn: (id: string) => updateMonthlyPlanItem(profileId, id, { status: 'PAID' }),
    onSuccess: invalidatePlanningViews,
  });

  const markCollectedMutation = useMutation({
    mutationFn: (id: string) =>
      updateMonthlyPlanItem(profileId, id, { status: 'COLLECTED' }),
    onSuccess: invalidatePlanningViews,
  });

  const lastActionError =
    convertMutation.error ??
    updateMutation.error ??
    cancelMutation.error ??
    markPaidMutation.error ??
    markCollectedMutation.error ??
    deleteMutation.error;

  const actionErrorMessage = lastActionError ? getApiErrorMessage(lastActionError) : null;

  const pendingActionId = useMemo(
    () =>
      (convertMutation.isPending ? convertMutation.variables : null) ??
      (updateMutation.isPending ? updateMutation.variables?.id : null) ??
      (cancelMutation.isPending ? cancelMutation.variables : null) ??
      (markPaidMutation.isPending ? markPaidMutation.variables : null) ??
      (markCollectedMutation.isPending ? markCollectedMutation.variables : null) ??
      (deleteMutation.isPending ? deleteMutation.variables : null) ??
      null,
    [
      convertMutation.isPending,
      convertMutation.variables,
      updateMutation.isPending,
      updateMutation.variables,
      cancelMutation.isPending,
      cancelMutation.variables,
      markPaidMutation.isPending,
      markPaidMutation.variables,
      markCollectedMutation.isPending,
      markCollectedMutation.variables,
      deleteMutation.isPending,
      deleteMutation.variables,
    ],
  );

  return {
    create: () => createMutation.mutate(),
    createAsync: () => createMutation.mutateAsync().then(() => undefined),
    convert: (id: string) => convertMutation.mutate(id),
    cancel: (id: string) => cancelMutation.mutate(id),
    remove: (id: string) => deleteMutation.mutate(id),
    markPaid: (id: string) => markPaidMutation.mutate(id),
    markCollected: (id: string) => markCollectedMutation.mutate(id),
    update: (id: string, payload: MonthlyPlanItemUpdatePayload) =>
      updateMutation.mutateAsync({ id, payload }).then(() => undefined),

    isCreating: createMutation.isPending,
    createErrorMessage: createMutation.error ? getApiErrorMessage(createMutation.error) : null,

    pendingActionId,
    actionErrorMessage,
  };
}