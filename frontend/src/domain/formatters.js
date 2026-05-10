import { monthLabels } from './financeLabels';
export function formatMoney(value, currency = 'ARS') {
    const numericValue = Number(value ?? 0);
    if (!Number.isFinite(numericValue))
        return '$ 0,00';
    return new Intl.NumberFormat('es-AR', { style: 'currency', currency, maximumFractionDigits: 2 }).format(numericValue);
}
export function formatPercent(value) {
    const numericValue = Number(value ?? 0);
    if (!Number.isFinite(numericValue))
        return '0,00%';
    return `${new Intl.NumberFormat('es-AR', { maximumFractionDigits: 2 }).format(numericValue)}%`;
}
export function formatDate(value) {
    if (!value)
        return '-';
    const date = value instanceof Date ? value : new Date(value);
    if (Number.isNaN(date.getTime()))
        return '-';
    return new Intl.DateTimeFormat('es-AR').format(date);
}
export function formatMonth(month) {
    if (!month)
        return '-';
    return monthLabels[month] ?? `Mes ${month}`;
}
export function formatNumber(value) {
    const numericValue = Number(value ?? 0);
    if (!Number.isFinite(numericValue))
        return '0';
    return new Intl.NumberFormat('es-AR').format(numericValue);
}
export function normalizeOptionalText(value) {
    const clean = value?.trim();
    return clean ? clean : '-';
}
