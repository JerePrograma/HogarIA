export const fmtMoney = (v, c = 'ARS') => new Intl.NumberFormat('es-AR', { style: 'currency', currency: c, maximumFractionDigits: 2 }).format(v ?? 0);
