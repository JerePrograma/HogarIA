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
    WARNING: 'Atención',
    CRITICAL: 'Crítico',
};
export const goalTypeLabels = {
    EMERGENCY_FUND: 'Fondo de emergencia',
    DEBT_PAYOFF: 'Cancelación de deuda',
    SAVING_TARGET: 'Meta de ahorro',
    INVESTMENT: 'Inversión',
    BUSINESS: 'Negocio',
    TRAVEL: 'Viaje',
    EDUCATION: 'Educación',
    OTHER: 'Otro',
};
export const goalStatusLabels = {
    ACTIVE: 'Activo',
    PAUSED: 'Pausado',
    COMPLETED: 'Completado',
    CANCELLED: 'Cancelado',
};
export const habitFrequencyLabels = {
    DAILY: 'Diario',
    WEEKLY: 'Semanal',
    MONTHLY: 'Mensual',
};
export const importBatchStatusLabels = {
    READY: 'Listo',
    IMPORTED: 'Importado',
    SKIPPED: 'Omitido',
    WARNING: 'Atención',
    ERROR: 'Error',
};
export const importRowStatusLabels = {
    READY: 'Listo',
    IMPORTED: 'Importado',
    SKIPPED: 'Omitido',
    WARNING: 'Atención',
    ERROR: 'Error',
};
export const importTargetEntityLabels = {
    CATEGORY: 'Categoría',
    ACCOUNT: 'Cuenta',
    INCOME: 'Ingreso',
    EXPENSE: 'Gasto',
    SAVING: 'Ahorro',
    BUDGET: 'Presupuesto',
    CARD: 'Tarjeta',
    GOAL: 'Objetivo',
    HABIT: 'Hábito',
    HABIT_CHECKIN: 'Control de hábito',
    INFLATION: 'Inflación',
    UNKNOWN: 'Sin clasificar',
};
export const inflationProjectionLabels = {
    real: 'Real',
    projected: 'Proyectado',
};
export const monthlyPlanTypeLabels = { INCOME: 'Ingreso', EXPENSE: 'Egreso', SAVING: 'Ahorro', DEBT: 'Deuda', TRANSFER: 'Transferencia', RECOVERY: 'Recupero', TODO: 'Pendiente' };
export const monthlyPlanPriorityLabels = { ESSENTIAL: 'Esencial', IMPORTANT: 'Importante', OPTIONAL: 'Opcional' };
export const monthlyPlanStatusLabels = { DRAFT: 'Borrador', ESTIMATED: 'Estimado', SCHEDULED: 'Programado', DUE: 'Por vencer', PAID: 'Pagado', COLLECTED: 'Cobrado', CANCELLED: 'Cancelado' };
export const monthlyPlanSourceLabels = { MANUAL: 'Manual', IMPORT: 'Importado', QUICK_CAPTURE: 'Captura rápida', SYSTEM: 'Sistema' };
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
export function labelOrValue(labels, value) {
    if (!value)
        return '-';
    return labels[value] ?? value;
}
