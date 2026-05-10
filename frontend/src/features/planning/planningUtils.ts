import { formatMoney } from '../../domain/formatters';
import type { MonthlyPlanItem, QuickCapturePreviewResponse } from '../../domain/types';

export function formatPlanAmount(item: Pick<MonthlyPlanItem, 'amount' | 'minAmount' | 'maxAmount'>): string {
  if (item.amount != null) return formatMoney(item.amount);
  if (item.minAmount != null || item.maxAmount != null) {
    const min = item.minAmount ?? item.maxAmount;
    const max = item.maxAmount ?? item.minAmount;
    if (min != null && max != null) return `${formatMoney(min)} – ${formatMoney(max)}`;
  }
  return 'Sin monto';
}

export function formatPlanRecovery(item: Pick<MonthlyPlanItem, 'expectedRecoveryPercent' | 'expectedRecoveryAmount'>): string {
  if (item.expectedRecoveryPercent != null) return `${item.expectedRecoveryPercent}%`;
  if (item.expectedRecoveryAmount != null) return formatMoney(item.expectedRecoveryAmount);
  return 'Sin monto';
}

export function formatPlanNet(item: Pick<MonthlyPlanItem, 'netMin' | 'netMax'>): string {
  if (item.netMin == null && item.netMax == null) return 'Sin monto';
  const min = item.netMin ?? item.netMax;
  const max = item.netMax ?? item.netMin;
  if (min != null && max != null) return `${formatMoney(min)} – ${formatMoney(max)}`;
  return 'Sin monto';
}

export function canConvertPlanItem(item: MonthlyPlanItem): boolean {
  const hasExactAmount = item.amount != null || (item.minAmount != null && item.maxAmount != null && item.minAmount === item.maxAmount);
  return hasExactAmount && Boolean(item.accountId) && Boolean(item.categoryId) && item.type !== 'TODO' && item.status !== 'CANCELLED' && !item.transactionId;
}

export function confidenceMeta(confidence: QuickCapturePreviewResponse['confidence']): { label: string; className: string } {
  if (confidence === 'HIGH') return { label: 'Alta', className: 'badge-confidence-high' };
  if (confidence === 'MEDIUM') return { label: 'Media', className: 'badge-confidence-medium' };
  return { label: 'Baja', className: 'badge-confidence-low' };
}
