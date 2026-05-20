import { http } from './http';
import type { MovementType, TransactionOrigin, TransactionStatus } from '../domain/types';

export type BulkRecategorizeTargetMode = 'MANUAL' | 'AUTO_BY_IMPORT_RULES';

export type BulkRecategorizePreviewStatus =
  | 'READY'
  | 'SKIPPED'
  | 'AMBIGUOUS'
  | 'NEEDS_CATEGORY'
  | 'ERROR';

export interface BulkRecategorizePreviewPayload {
  accountId?: string | null;
  from?: string | null;
  to?: string | null;
  fromCategoryId?: string | null;
  onlyWithoutCategory?: boolean | null;
  targetMode?: BulkRecategorizeTargetMode;
  toCategoryId?: string | null;
  movementType?: MovementType | null;
  descriptionContains?: string | null;
  exactAmount?: number | null;
  minAmount?: number | null;
  maxAmount?: number | null;
  onlyImported?: boolean | null;
}

export interface BulkRecategorizeCandidate {
  transactionId: string;
  accountId: string;
  currentCategoryId: string | null;
  currentCategoryName?: string | null;
  targetCategoryId: string | null;
  targetCategoryName?: string | null;
  movementType: MovementType;
  realDate: string;
  budgetDate: string;
  amount: number;
  description: string | null;
  origin: TransactionOrigin;
  status: TransactionStatus;
  previewStatus: BulkRecategorizePreviewStatus;
  warning: string | null;
}

export interface BulkRecategorizePreviewResult {
  profileId: string;
  targetCategoryId: string | null;
  totalMatched: number;
  updatableCount: number;
  ambiguousCount: number;
  skippedCount: number;
  candidates: BulkRecategorizeCandidate[];
  warnings: string[];
  errors: string[];
}

export interface BulkRecategorizeApplyPayload {
  targetMode?: BulkRecategorizeTargetMode;
  toCategoryId?: string | null;
  transactionIds?: string[];
  updates?: Array<{
    transactionId: string;
    targetCategoryId: string;
  }>;
}

export interface BulkRecategorizeApplyResult {
  updatedCount: number;
  skippedCount: number;
  failedCount: number;
  updatedTransactionIds: string[];
  warnings: string[];
  errors: string[];
}

export async function previewBulkRecategorize(
  profileId: string,
  payload: BulkRecategorizePreviewPayload,
): Promise<BulkRecategorizePreviewResult> {
  const { data } = await http.post(
    `/profiles/${profileId}/transactions/bulk-recategorize/preview`,
    payload,
  );

  return data;
}

export async function applyBulkRecategorize(
  profileId: string,
  payload: BulkRecategorizeApplyPayload,
): Promise<BulkRecategorizeApplyResult> {
  const { data } = await http.post(
    `/profiles/${profileId}/transactions/bulk-recategorize/apply`,
    payload,
  );

  return data;
}