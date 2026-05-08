import { http } from './http';
import type { BudgetExcelImportCommitRequest, BudgetExcelImportCommitResponse, BudgetExcelImportPreviewResponse, ExcelImportBatch } from '../domain/types';

export async function previewBudgetExcelImport(profileId: string, file: File): Promise<BudgetExcelImportPreviewResponse> {
  const form = new FormData();
  form.append('file', file);
  const { data } = await http.post(`/api/profiles/${profileId}/imports/budget-excel/preview`, form, {
    headers: { 'Content-Type': 'multipart/form-data' },
  });
  return data;
}

export async function commitBudgetExcelImport(profileId: string, batchId: string, body: BudgetExcelImportCommitRequest): Promise<BudgetExcelImportCommitResponse> {
  const { data } = await http.post(`/api/profiles/${profileId}/imports/budget-excel/${batchId}/commit`, body);
  return data;
}

export async function getImportBatch(profileId: string, batchId: string): Promise<ExcelImportBatch> {
  const { data } = await http.get(`/api/profiles/${profileId}/imports/${batchId}`);
  return data;
}

export async function listImportBatches(profileId: string): Promise<ExcelImportBatch[]> {
  const { data } = await http.get(`/api/profiles/${profileId}/imports`);
  return data;
}
