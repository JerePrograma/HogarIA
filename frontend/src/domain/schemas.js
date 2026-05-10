import { z } from 'zod';
export const devUserCreateSchema = z.object({
    email: z.string().email('Email inválido'),
    password: z.string().min(8, 'La contraseña debe tener al menos 8 caracteres'),
    fullName: z.string().min(1, 'Nombre requerido'),
});
export const profileCreateSchema = z.object({
    name: z.string().min(1, 'Nombre del perfil requerido'),
    type: z.enum(['PERSONAL', 'FAMILY', 'BUSINESS'], {
        message: 'Tipo de perfil inválido',
    }),
    baseCurrency: z.string().min(3, 'Moneda requerida').default('ARS'),
    activeYear: z.coerce
        .number()
        .min(2000, 'El año debe ser mayor o igual a 2000')
        .max(2100, 'El año debe ser menor o igual a 2100'),
});
export const profileUpdateSchema = profileCreateSchema.extend({
    active: z.boolean().optional(),
});
export const accountCreateSchema = z.object({
    name: z.string().min(1, 'Nombre de cuenta requerido'),
    accountType: z.enum(['CASH', 'BANK', 'CREDIT_CARD', 'DEBIT_CARD', 'VIRTUAL_WALLET', 'BUSINESS'], { message: 'Tipo de cuenta inválido' }),
    currency: z.string().min(3, 'Moneda requerida').default('ARS'),
    creditLimit: z.coerce.number().min(0, 'El límite no puede ser negativo').optional(),
    statementCloseDay: z.coerce
        .number()
        .min(1, 'El día de cierre debe estar entre 1 y 31')
        .max(31, 'El día de cierre debe estar entre 1 y 31')
        .optional(),
    dueDay: z.coerce
        .number()
        .min(1, 'El día de vencimiento debe estar entre 1 y 31')
        .max(31, 'El día de vencimiento debe estar entre 1 y 31')
        .optional(),
});
export const accountUpdateSchema = accountCreateSchema.extend({
    active: z.boolean().optional(),
});
export const categoryCreateSchema = z.object({
    name: z.string().min(1, 'Nombre de categoría requerido'),
    type: z.enum(['INCOME', 'FIXED_EXPENSE', 'VARIABLE_EXPENSE', 'SAVING', 'DEBT', 'INVESTMENT'], { message: 'Tipo de categoría inválido' }),
    scope: z.enum(['PERSONAL', 'FAMILY', 'BUSINESS', 'GLOBAL'], {
        message: 'Alcance de categoría inválido',
    }),
    parentId: z.string().optional(),
});
export const categoryUpdateSchema = categoryCreateSchema.extend({
    active: z.boolean().optional(),
});
export const transactionCreateSchema = z.object({
    accountId: z.string().min(1, 'Cuenta requerida'),
    categoryId: z.string().min(1, 'Categoría requerida'),
    movementType: z.enum(['INCOME', 'EXPENSE', 'SAVING', 'TRANSFER', 'ADJUSTMENT'], {
        message: 'Tipo de movimiento inválido',
    }),
    realDate: z.string().min(1, 'Fecha real requerida'),
    budgetDate: z.string().min(1, 'Fecha presupuestaria requerida'),
    amount: z.coerce.number().gt(0, 'El importe debe ser mayor a cero'),
    currency: z.string().min(3, 'Moneda requerida').default('ARS'),
    description: z.string().optional(),
    status: z.enum(['CONFIRMED', 'PENDING', 'IGNORED'], {
        message: 'Estado de movimiento inválido',
    }).optional(),
});
export const transactionUpdateSchema = transactionCreateSchema;
export const budgetYearSchema = z.object({
    year: z.coerce
        .number()
        .min(2000, 'El año debe ser mayor o igual a 2000')
        .max(2100, 'El año debe ser menor o igual a 2100'),
    targetIncome: z.coerce.number().min(0, 'El ingreso objetivo no puede ser negativo').optional(),
    targetSaving: z.coerce.number().min(0, 'El ahorro objetivo no puede ser negativo').optional(),
    notes: z.string().optional(),
});
export const budgetMonthSchema = z.object({
    month: z.coerce
        .number()
        .min(1, 'El mes debe estar entre 1 y 12')
        .max(12, 'El mes debe estar entre 1 y 12'),
    notes: z.string().optional(),
});
export const budgetItemSchema = z.object({
    categoryId: z.string().min(1, 'Categoría requerida'),
    budgetAmount: z.coerce.number().min(0, 'El presupuesto no puede ser negativo'),
});
