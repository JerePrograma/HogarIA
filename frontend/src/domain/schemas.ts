import { z } from 'zod';

export const devUserCreateSchema = z.object({
  email: z.string().email('Email inválido'),
  password: z.string().min(8, 'Mínimo 8 caracteres'),
  fullName: z.string().min(1, 'Nombre requerido'),
});

export const profileCreateSchema = z.object({
  name: z.string().min(1),
  type: z.enum(['PERSONAL', 'FAMILY', 'BUSINESS']),
  baseCurrency: z.string().default('ARS'),
  activeYear: z.coerce.number().min(2000).max(2100),
});
export const profileUpdateSchema = profileCreateSchema.extend({ active: z.boolean().optional() });

export const accountCreateSchema = z.object({
  name: z.string().min(1), accountType: z.enum(['CASH', 'BANK', 'CREDIT_CARD', 'DEBIT_CARD', 'VIRTUAL_WALLET', 'BUSINESS']),
  currency: z.string().default('ARS'), creditLimit: z.coerce.number().min(0).optional(),
  statementCloseDay: z.coerce.number().min(1).max(31).optional(), dueDay: z.coerce.number().min(1).max(31).optional(),
});
export const accountUpdateSchema = accountCreateSchema.extend({ active: z.boolean().optional() });

export const categoryCreateSchema = z.object({
  name: z.string().min(1), type: z.enum(['INCOME', 'FIXED_EXPENSE', 'VARIABLE_EXPENSE', 'SAVING', 'DEBT', 'INVESTMENT']),
  scope: z.enum(['PERSONAL', 'FAMILY', 'BUSINESS', 'GLOBAL']), parentId: z.string().optional(),
});
export const categoryUpdateSchema = categoryCreateSchema.extend({ active: z.boolean().optional() });

export const transactionCreateSchema = z.object({
  accountId: z.string().min(1), categoryId: z.string().min(1), movementType: z.enum(['INCOME', 'EXPENSE', 'SAVING', 'TRANSFER', 'ADJUSTMENT']),
  realDate: z.string().min(1), budgetDate: z.string().min(1), amount: z.coerce.number().gt(0), currency: z.string().default('ARS'),
  description: z.string().optional(), status: z.enum(['CONFIRMED', 'PENDING', 'IGNORED']).optional(),
});
export const transactionUpdateSchema = transactionCreateSchema;

export const budgetYearSchema = z.object({ year: z.coerce.number().min(2000).max(2100), targetIncome: z.coerce.number().min(0).optional(), targetSaving: z.coerce.number().min(0).optional(), notes: z.string().optional() });
export const budgetMonthSchema = z.object({ month: z.coerce.number().min(1).max(12), notes: z.string().optional() });
export const budgetItemSchema = z.object({ categoryId: z.string().min(1), budgetAmount: z.coerce.number().min(0) });
