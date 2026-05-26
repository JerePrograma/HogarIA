import { monthLabels } from './financeLabels';

export function formatMoney(value: number | null | undefined, currency = 'ARS'): string {
  const numericValue = Number(value ?? 0);
  if (!Number.isFinite(numericValue)) return '$ 0,00';
  return new Intl.NumberFormat('es-AR', { style: 'currency', currency, maximumFractionDigits: 2 }).format(numericValue);
}

export function formatSignedMoney(value: number | null | undefined, currency = 'ARS'): string {
  const numericValue = Number(value ?? 0);
  const formattedValue = formatMoney(Math.abs(numericValue), currency);

  if (numericValue > 0) return `+${formattedValue}`;
  if (numericValue < 0) return `-${formattedValue}`;
  return formattedValue;
}

export function formatPercent(value: number | null | undefined): string {
  const numericValue = Number(value ?? 0);
  if (!Number.isFinite(numericValue)) return '0,00%';
  return `${new Intl.NumberFormat('es-AR', { maximumFractionDigits: 2 }).format(numericValue)}%`;
}

export function formatDate(value: string | Date | null | undefined): string {
  if (!value) return '-';
  const date = value instanceof Date ? value : new Date(value);
  if (Number.isNaN(date.getTime())) return '-';
  return new Intl.DateTimeFormat('es-AR').format(date);
}

export function formatMonth(month: number | null | undefined): string {
  if (!month) return '-';
  return monthLabels[month] ?? `Mes ${month}`;
}

export function formatNumber(value: number | null | undefined): string {
  const numericValue = Number(value ?? 0);
  if (!Number.isFinite(numericValue)) return '0';
  return new Intl.NumberFormat('es-AR').format(numericValue);
}

export function formatCompactNumber(value: number | null | undefined): string {
  const numericValue = Number(value ?? 0);
  if (!Number.isFinite(numericValue)) return '0';
  return new Intl.NumberFormat('es-AR', {
    notation: 'compact',
    maximumFractionDigits: 1,
  }).format(numericValue);
}

export function normalizeOptionalText(value: string | null | undefined): string {
  const clean = value?.trim();
  return clean ? clean : '-';
}

export const formatOptionalText = normalizeOptionalText;
