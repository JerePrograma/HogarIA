import { http } from './http';

export type BulkRecategorizeMovementType = 'INCOME' | 'EXPENSE' | 'SAVING' | 'TRANSFER' | 'ADJUSTMENT';

export interface BulkRecategorizePreviewPayload {
  accountId?: string | null;
  from?: string | null;
  to?: string | null;
  fromCategoryId?: string | null;
  onlyWithoutCategory?: boolean | null;
  targetMode?: 'MANUAL' | 'AUTO_BY_IMPORT_RULES';
  toCategoryId?: string | null;
  movementType?: BulkRecategorizeMovementType | null;
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
  movementType: BulkRecategorizeMovementType;
  realDate: string;
  budgetDate: string;
  amount: number;
  description: string;
  origin: string;
  status: string;
  previewStatus: 'READY' | 'SKIPPED' | 'AMBIGUOUS' | 'NEEDS_CATEGORY';
  warning: string;
}

export interface BulkRecategorizePreviewResult {
  profileId: string;
  targetCategoryId: string;
  totalMatched: number;
  updatableCount: number;
  ambiguousCount: number;
  skippedCount: number;
  candidates: BulkRecategorizeCandidate[];
  warnings: string[];
  errors: string[];
}

export interface BulkRecategorizeApplyPayload {
  targetMode?: 'MANUAL' | 'AUTO_BY_IMPORT_RULES';
  toCategoryId?: string | null;
  transactionIds?: string[];
  updates?: Array<{ transactionId: string; targetCategoryId: string }>;
}

export interface BulkRecategorizeApplyResult {
  updatedCount: number;
  skippedCount: number;
  failedCount: number;
  updatedTransactionIds: string[];
  warnings: string[];
  errors: string[];
}

export async function previewBulkRecategorize(profileId: string, payload: BulkRecategorizePreviewPayload): Promise<BulkRecategorizePreviewResult> {
  const { data } = await http.post(`/profiles/${profileId}/transactions/bulk-recategorize/preview`, payload);
  return data;
}

export async function applyBulkRecategorize(profileId: string, payload: BulkRecategorizeApplyPayload): Promise<BulkRecategorizeApplyResult> {
  const { data } = await http.post(`/profiles/${profileId}/transactions/bulk-recategorize/apply`, payload);
  return data;
}
