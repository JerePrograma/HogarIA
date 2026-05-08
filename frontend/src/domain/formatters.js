// src/domain/formatters.ts
export function formatMoney(value, currency = 'ARS') {
    return new Intl.NumberFormat('es-AR', {
        style: 'currency',
        currency,
        maximumFractionDigits: 2,
    }).format(value ?? 0);
}
export function formatPercent(value) {
    return `${Number(value ?? 0).toFixed(2)}%`;
}
