import { http } from './http';
import type {
  ConfirmPlanTransactionMatchPayload,
  MonthlyPlanReconciliationSummary,
  TransactionMatch,
} from '../domain/types';

export const getMonthlyPlanReconciliation = (
  profileId: string,
  year: number,
  month: number,
): Promise<MonthlyPlanReconciliationSummary> =>
  http
    .get(`/api/profiles/${profileId}/planning/reconciliation`, {
      params: { year, month },
    })
    .then((response) => response.data);

export const confirmPlanTransactionMatch = (
  profileId: string,
  payload: ConfirmPlanTransactionMatchPayload,
): Promise<TransactionMatch> =>
  http
    .post(`/api/profiles/${profileId}/planning/reconciliation/matches`, payload)
    .then((response) => response.data);

export const deletePlanTransactionMatch = (
  profileId: string,
  matchId: string,
): Promise<void> =>
  http
    .delete(`/api/profiles/${profileId}/planning/reconciliation/matches/${matchId}`)
    .then((response) => response.data);