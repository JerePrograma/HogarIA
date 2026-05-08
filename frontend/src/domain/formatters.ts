// src/domain/formatters.ts

export function formatMoney(
  value: number | null | undefined,
  currency = 'ARS',
): string {
  return new Intl.NumberFormat('es-AR', {
    style: 'currency',
    currency,
    maximumFractionDigits: 2,
  }).format(value ?? 0);
}

export function formatPercent(value: number | null | undefined): string {
  return `${Number(value ?? 0).toFixed(2)}%`;
}