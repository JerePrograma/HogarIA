import { http } from './http';

export type BancoProvinciaPreviewCandidate = {
  lineNumber:number; accountNumber:string; currentDebtAmount:number; originalAmount:number; dueDate:string; monthsRemaining:number; estimatedMonthlyAmount:number; duplicate:boolean; warnings:string[];
};

export type BancoProvinciaPreviewResponse = {
  candidates: BancoProvinciaPreviewCandidate[];
  warnings?: string[];
};

export type BancoProvinciaCommitPayload = {
  periodYear: number;
  periodMonth: number;
  candidates: BancoProvinciaPreviewCandidate[];
  skipDuplicates: boolean;
  createMonthlyPlanItems: boolean;
};

export type BancoProvinciaCommitResponse = {
  createdMonthlyPlanItems?: number;
  skippedDuplicates?: number;
  warnings?: string[];
  errors?: string[];
};

export const previewBancoProvinciaLoans = async (profileId:string, file:File, periodYear:number, periodMonth:number) => {
  const fd = new FormData();
  fd.append('file', file);
  fd.append('periodYear', String(periodYear));
  fd.append('periodMonth', String(periodMonth));
  fd.append('currency', 'ARS');
  const { data } = await http.post<BancoProvinciaPreviewResponse>(`/profiles/${profileId}/external-debts/banco-provincia/preview`, fd);
  return data;
};

export const commitBancoProvinciaLoans = async (profileId:string, payload:BancoProvinciaCommitPayload) => {
  const { data } = await http.post<BancoProvinciaCommitResponse>(`/profiles/${profileId}/external-debts/banco-provincia/commit`, payload);
  return data;
};
