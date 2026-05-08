export const profileTypeLabels = {
    PERSONAL: 'Personal',
    FAMILY: 'Familiar',
    BUSINESS: 'Negocio',
};
export const accountTypeLabels = {
    CASH: 'Efectivo',
    BANK: 'Banco',
    CREDIT_CARD: 'Tarjeta de crédito',
    DEBIT_CARD: 'Tarjeta de débito',
    VIRTUAL_WALLET: 'Billetera virtual',
    BUSINESS: 'Cuenta de negocio',
};
export const categoryTypeLabels = {
    INCOME: 'Ingreso',
    FIXED_EXPENSE: 'Gasto fijo',
    VARIABLE_EXPENSE: 'Gasto variable',
    SAVING: 'Ahorro',
    DEBT: 'Deuda',
    INVESTMENT: 'Inversión',
};
export const categoryScopeLabels = {
    PERSONAL: 'Personal',
    FAMILY: 'Familiar',
    BUSINESS: 'Negocio',
    GLOBAL: 'Global',
};
export const movementTypeLabels = {
    INCOME: 'Ingreso',
    EXPENSE: 'Gasto',
    SAVING: 'Ahorro',
    TRANSFER: 'Transferencia',
    ADJUSTMENT: 'Ajuste',
};
export const transactionOriginLabels = {
    MANUAL: 'Manual',
    IMPORT: 'Importado',
    RECURRENT: 'Recurrente',
    SYSTEM: 'Sistema',
};
export const transactionStatusLabels = {
    CONFIRMED: 'Confirmado',
    PENDING: 'Pendiente',
    IGNORED: 'Ignorado',
};
export const budgetComparisonStatusLabels = {
    OK: 'Correcto',
    WARNING: 'Atención',
    EXCEEDED: 'Excedido',
};
export const financialHealthLabels = {
    EXCELLENT: 'Excelente',
    HEALTHY: 'Saludable',
    WARNING: 'En observación',
    CRITICAL: 'Crítico',
};
export const goalTypeLabels = {
    OTHER: 'Otro',
    EMERGENCY_FUND: 'Fondo de emergencia',
    TRAVEL: 'Viaje',
    HOME: 'Vivienda',
    INVESTMENT: 'Inversión',
};
export const goalStatusLabels = {
    ACTIVE: 'Activo',
    COMPLETED: 'Completado',
    PAUSED: 'Pausado',
    CANCELLED: 'Cancelado',
    ARCHIVED: 'Archivado',
};
export const habitFrequencyLabels = {
    DAILY: 'Diario',
    WEEKLY: 'Semanal',
    MONTHLY: 'Mensual',
};
export const inflationSourceLabels = {
    MANUAL: 'Manual',
    IMPORT: 'Importado',
    SYSTEM: 'Sistema',
};
export const monthLabels = {
    1: 'Enero',
    2: 'Febrero',
    3: 'Marzo',
    4: 'Abril',
    5: 'Mayo',
    6: 'Junio',
    7: 'Julio',
    8: 'Agosto',
    9: 'Septiembre',
    10: 'Octubre',
    11: 'Noviembre',
    12: 'Diciembre',
};
export function labelOrMissing(labels, value, fallback = 'Sin definir') {
    if (value === null || value === undefined || value === '')
        return fallback;
    const mapped = labels[value];
    if (mapped)
        return mapped;
    return import.meta.env.DEV ? `Sin traducir: ${String(value)}` : fallback;
}
/**
 * Alias mantenido para compatibilidad con pantallas existentes.
 * Usar labelOrMissing en código nuevo.
 */
export const labelOrValue = labelOrMissing;
