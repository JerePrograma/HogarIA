import { http } from './http';
import type { ConfirmPlanTransactionMatchPayload } from '../domain/types';

export const getMonthlyPlanReconciliation = (profileId: string, year: number, month: number) =>
  http.get(`/api/profiles/${profileId}/planning/reconciliation?year=${year}&month=${month}`).then((r) => r.data);

export const confirmPlanTransactionMatch = (profileId: string, payload: ConfirmPlanTransactionMatchPayload) =>
  http.post(`/api/profiles/${profileId}/planning/reconciliation/matches`, payload).then((r) => r.data);

export const deletePlanTransactionMatch = (profileId: string, matchId: string) =>
  http.delete(`/api/profiles/${profileId}/planning/reconciliation/matches/${matchId}`).then((r) => r.data);
