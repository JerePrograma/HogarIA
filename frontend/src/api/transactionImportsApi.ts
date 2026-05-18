import { http } from './http';
import type { TransactionImportCommitPayload, TransactionImportCommitResult, TransactionImportPreview, TransactionImportSource } from '../features/transactions/imports/types';

export const previewTransactionImport = (
  profileId: string,
  source: TransactionImportSource,
  accountId: string,
  file: File,
): Promise<TransactionImportPreview> => {
  const formData = new FormData();
  formData.append('file', file);

  return http
    .post<TransactionImportPreview>(
      `/profiles/${profileId}/transaction-imports/preview?source=${source}&accountId=${accountId}`,
      formData,
      { headers: { 'Content-Type': 'multipart/form-data' } },
    )
    .then((response) => response.data);
};

export const commitTransactionImport = (
  profileId: string,
  batchId: string,
  payload: TransactionImportCommitPayload,
): Promise<TransactionImportCommitResult> =>
  http.post<TransactionImportCommitResult>(`/profiles/${profileId}/transaction-imports/${batchId}/commit`, payload).then((response) => response.data);
