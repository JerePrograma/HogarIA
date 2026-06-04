import type {
  BulkRecategorizeCandidate,
  BulkRecategorizePreviewPayload,
  BulkRecategorizePreviewStatus,
  BulkRecategorizeReviewFilter,
} from '../../api/bulkRecategorizeApi';
import type {
  MovementType,
  PaymentChannel,
  TransactionClassificationStatus,
} from '../../domain/types';

export const ALL = 'ALL';

export type CandidateStatusFilter = BulkRecategorizePreviewStatus | typeof ALL;

export interface CandidateFilters {
  search: string;
  status: CandidateStatusFilter;
}

export type ReviewAction =
  | 'RECATEGORIZE'
  | 'CONVERT_TRANSFER'
  | 'MARK_IGNORED'
  | 'ADJUST_RECOVERABLE'
  | 'CONFIRM_EXPENSE'
  | 'CONFIRM_RECOVERY_INCOME';

export const MOVEMENT_LABELS: Record<MovementType, string> = {
  INCOME: 'Ingreso',
  EXPENSE: 'Gasto',
  SAVING: 'Ahorro',
  TRANSFER: 'Transferencia',
  ADJUSTMENT: 'Ajuste',
};

export const PREVIEW_STATUS_LABELS: Record<BulkRecategorizePreviewStatus, string> = {
  READY: 'Actualizable',
  AMBIGUOUS: 'Ambiguo',
  SKIPPED: 'Omitido',
  NEEDS_CATEGORY: 'Sin sugerencia',
  ERROR: 'Error',
};

export const CLASSIFICATION_STATUS_LABELS: Record<TransactionClassificationStatus, string> = {
  CLASSIFIED: 'Clasificado',
  NEEDS_CATEGORY: 'Necesita categoría',
  REVIEW: 'En revisión',
  TECHNICAL: 'Técnico',
  IGNORED_BY_RULE: 'Ignorado por regla',
};

export const PAYMENT_CHANNEL_LABELS: Record<PaymentChannel, string> = {
  UNKNOWN: 'Desconocido',
  CASH: 'Efectivo',
  BANK_TRANSFER: 'Transferencia bancaria',
  DEBIN: 'DEBIN',
  CUENTA_DNI: 'Cuenta DNI',
  DEBIT_CARD: 'Tarjeta de débito',
  CREDIT_CARD: 'Tarjeta de crédito',
  DIRECT_DEBIT: 'Débito directo',
  POS_TRANSFER: 'Transferencia POS',
  ATM: 'Cajero / punto efectivo',
  MONEY_MARKET_YIELD: 'Rendimiento diario',
  TRANSPORT_CARD: 'Tarjeta transporte',
  QR_PAYMENT: 'Pago QR',
  CARD_FOREIGN_CURRENCY: 'Tarjeta moneda extranjera',
  MERCADO_PAGO: 'Mercado Pago',
  MERCADO_CREDITO: 'Mercado Crédito',
  INTERNAL_TRANSFER: 'Transferencia interna',
  OTHER: 'Otro',
};

export const REVIEW_FILTER_LABELS: Record<BulkRecategorizeReviewFilter, string> = {
  POSSIBLE_INTERNAL_TRANSFER: 'Fondeos Banco ↔ MP',
  POSSIBLE_CROSS_SOURCE_DUPLICATE: 'Duplicados Banco ↔ MP',
  CJ_DISBURSEMENT_EXPENSE: 'CJ capital como gasto',
  DEBIN_CDNI_PENDING: 'DEBIN / Cuenta DNI pendientes',
  NEEDS_CATEGORY: 'Necesitan categoría',
  TECHNICAL: 'Técnicos',
  REVIEW: 'En revisión',
};

export function normalize(value: string | number | null | undefined) {
  return String(value ?? '').trim().toLowerCase();
}

export function toNullableString(value: string) {
  return value.trim() ? value : null;
}

export function toNullableNumber(value: string) {
  return value.trim() ? Number(value) : null;
}

export function parseQueryBoolean(value: string | null) {
  if (value === 'true') return true;
  if (value === 'false') return false;
  return null;
}

export function parseQueryList(value: string | null) {
  const items = value?.split(',').map((item) => item.trim()).filter(Boolean) ?? [];
  return items.length ? items : null;
}

export function getPreviewStatusLabel(status: string) {
  return PREVIEW_STATUS_LABELS[status as BulkRecategorizePreviewStatus] ?? status;
}

export function getPreviewStatusTone(
  status: string,
): 'ok' | 'watch' | 'critical' | 'neutral' {
  if (status === 'READY') return 'ok';
  if (status === 'AMBIGUOUS' || status === 'NEEDS_CATEGORY') return 'watch';
  if (status === 'ERROR') return 'critical';
  return 'neutral';
}

export function getMovementLabel(type: string | null | undefined) {
  if (!type) return '-';
  return MOVEMENT_LABELS[type as MovementType] ?? type;
}

export function getMovementTone(
  type: string | null | undefined,
): 'ok' | 'watch' | 'risk' | 'critical' | 'neutral' {
  if (type === 'INCOME') return 'ok';
  if (type === 'SAVING') return 'ok';
  if (type === 'TRANSFER') return 'neutral';
  if (type === 'ADJUSTMENT') return 'watch';
  if (type === 'EXPENSE') return 'critical';
  return 'neutral';
}

export function countByStatus(candidates: Array<{ previewStatus: string }>) {
  return candidates.reduce<Record<string, number>>((acc, candidate) => {
    acc[candidate.previewStatus] = (acc[candidate.previewStatus] ?? 0) + 1;
    return acc;
  }, {});
}

export function matchesCandidateFilter(
  candidate: BulkRecategorizeCandidate,
  filters: CandidateFilters,
) {
  const search = normalize(filters.search);

  const matchesSearch =
    !search ||
    normalize(candidate.description).includes(search) ||
    normalize(candidate.realDate).includes(search) ||
    normalize(candidate.amount).includes(search) ||
    normalize(candidate.movementType).includes(search) ||
    normalize(candidate.warning).includes(search) ||
    normalize(candidate.source).includes(search) ||
    normalize(candidate.paymentChannel).includes(search) ||
    normalize(candidate.classificationStatus).includes(search) ||
    normalize(candidate.classificationReason).includes(search) ||
    normalize(candidate.targetCategoryName).includes(search);

  const matchesStatus =
    filters.status === ALL || candidate.previewStatus === filters.status;

  return matchesSearch && matchesStatus;
}

export function getRecategorizationStatusTitle(args: {
  hasApplyResult: boolean;
  hasPreview: boolean;
  readyCount: number;
  ambiguousCount: number;
  needsCategoryCount: number;
}) {
  if (args.hasApplyResult) return 'Recategorización aplicada';
  if (!args.hasPreview) return 'Esperando previsualización';
  if (args.readyCount === 0) return 'Sin candidatos aplicables';

  if (args.ambiguousCount > 0 || args.needsCategoryCount > 0) {
    return 'Aplicación parcial disponible';
  }

  return 'Lista para aplicar';
}

export function getRecategorizationStatusDescription(args: {
  hasAnySearchCriteria: boolean;
  readyCount: number;
  ambiguousCount: number;
  needsCategoryCount: number;
}) {
  if (!args.hasAnySearchCriteria) {
    return 'Cargá al menos un criterio para evitar cambios masivos accidentales.';
  }

  if (args.readyCount > 0 && (args.ambiguousCount > 0 || args.needsCategoryCount > 0)) {
    return `${args.readyCount} candidato(s) listos. ${args.ambiguousCount + args.needsCategoryCount} requieren revisión.`;
  }

  return 'Hay criterios cargados para buscar candidatos.';
}

export function inferAutoTargetClassificationStatus(
  candidate: BulkRecategorizeCandidate,
): TransactionClassificationStatus | null {
  if (candidate.targetClassificationStatus) {
    return candidate.targetClassificationStatus;
  }

  const reason = normalize(candidate.suggestionReason);
  const targetName = normalize(candidate.targetCategoryName);

  if (
    reason.includes('technical') ||
    reason.includes('tecnico') ||
    reason.includes('técnico') ||
    (candidate.targetMovementType === 'TRANSFER' && targetName.includes('cuenta dni'))
  ) {
    return 'TECHNICAL';
  }

  if (candidate.classificationStatus === 'NEEDS_CATEGORY' && candidate.targetCategoryId) {
    return 'CLASSIFIED';
  }

  return null;
}

export function inferAutoTargetClassificationReason(
  candidate: BulkRecategorizeCandidate,
) {
  if (candidate.targetClassificationReason) {
    return candidate.targetClassificationReason;
  }

  if (inferAutoTargetClassificationStatus(candidate) === 'TECHNICAL') {
    return candidate.suggestionReason ?? 'AUTO_TECHNICAL_RECATEGORY';
  }

  return candidate.suggestionReason ?? null;
}

export function buildReviewActionPatch(
  action: ReviewAction,
): Partial<BulkRecategorizePreviewPayload> {
  const base: Partial<BulkRecategorizePreviewPayload> = {
    targetMovementType: null,
    targetStatus: null,
    targetClassificationStatus: null,
    targetClassificationReason: null,
  };

  if (action === 'CONVERT_TRANSFER') {
    return {
      ...base,
      targetMovementType: 'TRANSFER',
      targetClassificationStatus: 'TECHNICAL',
      targetClassificationReason: 'USER_MARKED_INTERNAL_TRANSFER',
    };
  }

  if (action === 'MARK_IGNORED') {
    return {
      ...base,
      targetStatus: 'IGNORED',
      targetClassificationStatus: 'IGNORED_BY_RULE',
      targetClassificationReason: 'USER_IGNORED_CROSS_SOURCE',
    };
  }

  if (action === 'ADJUST_RECOVERABLE') {
    return {
      ...base,
      targetMovementType: 'ADJUSTMENT',
      targetClassificationStatus: 'CLASSIFIED',
      targetClassificationReason: 'CJPRESTAMOS_DISBURSEMENT',
    };
  }

  if (action === 'CONFIRM_EXPENSE') {
    return {
      ...base,
      targetMovementType: 'EXPENSE',
      targetStatus: 'CONFIRMED',
      targetClassificationStatus: 'CLASSIFIED',
      targetClassificationReason: 'USER_CONFIRMED_EXPENSE',
    };
  }

  return base;
}
