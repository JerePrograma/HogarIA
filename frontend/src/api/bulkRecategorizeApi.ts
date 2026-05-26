import { http } from "./http";
import type {
  CategoryType,
  MovementType,
  PaymentChannel,
  TransactionClassificationStatus,
  TransactionOrigin,
  TransactionStatus,
} from "../domain/types";

export type BulkRecategorizeTargetMode = "MANUAL" | "AUTO_BY_IMPORT_RULES";

export type BulkRecategorizePreviewStatus =
  | "READY"
  | "SKIPPED"
  | "AMBIGUOUS"
  | "NEEDS_CATEGORY"
  | "ERROR";

export type BulkRecategorizeReviewFilter =
  | "POSSIBLE_INTERNAL_TRANSFER"
  | "POSSIBLE_CROSS_SOURCE_DUPLICATE"
  | "CJ_DISBURSEMENT_EXPENSE"
  | "DEBIN_CDNI_PENDING"
  | "NEEDS_CATEGORY"
  | "TECHNICAL"
  | "REVIEW";

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
  reviewFilter?: BulkRecategorizeReviewFilter | null;
  targetMovementType?: MovementType | null;
  targetStatus?: TransactionStatus | null;
  targetClassificationStatus?: TransactionClassificationStatus | null;
  targetClassificationReason?: string | null;
  transactionIds?: string[] | null;

  classificationStatus?: TransactionClassificationStatus | null;
  paymentChannel?: PaymentChannel | null;
  source?: string | null;
  counterpartyContains?: string | null;
}

export interface BulkRecategorizeCandidate {
  transactionId: string;
  accountId: string;
  currentCategoryId: string | null;
  currentCategoryName?: string | null;
  targetCategoryId: string | null;
  targetCategoryName?: string | null;
  targetCategoryType?: CategoryType | null;
  targetMovementType?: MovementType | null;

  // Si el backend lo empieza a devolver, el frontend ya queda preparado.
  targetClassificationStatus?: TransactionClassificationStatus | null;
  targetClassificationReason?: string | null;

  suggestionReason?: string | null;
  confidence?: string | null;
  movementType: MovementType;
  realDate: string;
  budgetDate: string;
  amount: number;
  description: string | null;
  origin: TransactionOrigin;
  status: TransactionStatus;
  source?: string | null;
  paymentChannel?: PaymentChannel | null;
  classificationStatus?: TransactionClassificationStatus | null;
  classificationReason?: string | null;
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

export interface BulkRecategorizeApplyUpdate {
  transactionId: string;
  targetCategoryId?: string | null;
  targetMovementType?: MovementType | null;
  targetStatus?: TransactionStatus | null;
  targetClassificationStatus?: TransactionClassificationStatus | null;
  targetClassificationReason?: string | null;
}

export interface BulkRecategorizeApplyPayload {
  targetMode?: BulkRecategorizeTargetMode;
  toCategoryId?: string | null;
  targetMovementType?: MovementType | null;
  targetStatus?: TransactionStatus | null;
  targetClassificationStatus?: TransactionClassificationStatus | null;
  targetClassificationReason?: string | null;
  transactionIds?: string[];
  updates?: BulkRecategorizeApplyUpdate[];
  forceAmbiguous?: boolean | null;
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
