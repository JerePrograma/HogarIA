import axios from 'axios';
import { http } from './http';

export type CategoryType =
  | 'INCOME'
  | 'FIXED_EXPENSE'
  | 'VARIABLE_EXPENSE'
  | 'SAVING'
  | 'DEBT'
  | 'INVESTMENT';

export type BudgetStatus = 'OK' | 'WARNING' | 'EXCEEDED';

export type BudgetYear = {
  id: string;
  profileId: string;
  year: number;
  targetIncome: number | null;
  targetSaving: number | null;
  notes: string | null;
  createdAt: string;
  updatedAt: string;
};

export type BudgetYearCreatePayload = {
  year: number;
  targetIncome?: number | null;
  targetSaving?: number | null;
  notes?: string | null;
};

export type BudgetYearUpdatePayload = {
  targetIncome?: number | null;
  targetSaving?: number | null;
  notes?: string | null;
};

export type BudgetMonth = {
  id: string;
  budgetYearId: string;
  month: number;
  notes: string | null;
  items: BudgetCategoryItem[];
  createdAt: string;
  updatedAt: string;
};

export type BudgetMonthCreatePayload = {
  month: number;
  notes?: string | null;
};

export type BudgetMonthUpdatePayload = {
  notes?: string | null;
};

export type BudgetCategoryItem = {
  id: string;
  budgetMonthId: string;
  categoryId: string;
  categoryName: string;
  categoryType: CategoryType;
  budgetAmount: number;
  createdAt: string;
  updatedAt: string;
};

export type BudgetCategoryItemUpsertPayload = {
  categoryId: string;
  budgetAmount: number;
};

export type BudgetComparisonItem = {
  categoryId: string;
  categoryName: string;
  categoryType: CategoryType | null;
  budgetAmount: number;
  realAmount: number;
  difference: number;
  percentage: number;
  status: BudgetStatus;
};

export type BudgetComparison = {
  profileId: string;
  year: number;
  month: number;
  totalBudget: number;
  totalReal: number;
  totalDifference: number;
  items: BudgetComparisonItem[];
};

export function createBudgetYear(
  profileId: string,
  payload: BudgetYearCreatePayload
): Promise<BudgetYear> {
  return http
    .post(`/api/profiles/${profileId}/budgets`, payload)
    .then((response) => response.data);
}

export function listBudgetYears(profileId: string): Promise<BudgetYear[]> {
  return http
    .get(`/api/profiles/${profileId}/budgets`)
    .then((response) => response.data);
}

export function getBudgetYear(
  profileId: string,
  year: number
): Promise<BudgetYear> {
  return http
    .get(`/api/profiles/${profileId}/budgets/${year}`)
    .then((response) => response.data);
}

export function updateBudgetYear(
  profileId: string,
  year: number,
  payload: BudgetYearUpdatePayload
): Promise<BudgetYear> {
  return http
    .put(`/api/profiles/${profileId}/budgets/${year}`, payload)
    .then((response) => response.data);
}

export function createBudgetMonth(
  profileId: string,
  year: number,
  payload: BudgetMonthCreatePayload
): Promise<BudgetMonth> {
  return http
    .post(`/api/profiles/${profileId}/budgets/${year}/months`, payload)
    .then((response) => response.data);
}

export function getBudgetMonth(
  profileId: string,
  year: number,
  month: number
): Promise<BudgetMonth> {
  return http
    .get(`/api/profiles/${profileId}/budgets/${year}/months/${month}`)
    .then((response) => response.data);
}

export function updateBudgetMonth(
  budgetMonthId: string,
  payload: BudgetMonthUpdatePayload
): Promise<BudgetMonth> {
  return http
    .put(`/api/budget-months/${budgetMonthId}`, payload)
    .then((response) => response.data);
}

export function upsertBudgetCategoryItem(
  budgetMonthId: string,
  payload: BudgetCategoryItemUpsertPayload
): Promise<BudgetCategoryItem> {
  return http
    .put(`/api/budget-months/${budgetMonthId}/items`, payload)
    .then((response) => response.data);
}

export function deleteBudgetCategoryItem(itemId: string): Promise<void> {
  return http
    .delete(`/api/budget-category-items/${itemId}`)
    .then((response) => response.data);
}

export function getBudgetComparison(
  profileId: string,
  year: number,
  month: number
): Promise<BudgetComparison> {
  return http
    .get(`/api/profiles/${profileId}/budgets/${year}/months/${month}/comparison`)
    .then((response) => response.data);
}

export function isBudgetYearAlreadyExistsError(error: unknown): boolean {
  if (!axios.isAxiosError(error)) {
    return false;
  }

  const status = error.response?.status;
  const message = error.response?.data?.message;

  return (
    (status === 409 || status === 400) &&
    message === 'Budget year already exists'
  );
}

/**
 * Útil cuando el frontend quiere "abrir o crear" un año sin romper.
 * Pero para UI limpia, es mejor verificar antes si existe.
 */
export async function ensureBudgetYear(
  profileId: string,
  payload: BudgetYearCreatePayload
): Promise<BudgetYear> {
  try {
    return await createBudgetYear(profileId, payload);
  } catch (error) {
    if (isBudgetYearAlreadyExistsError(error)) {
      return getBudgetYear(profileId, payload.year);
    }

    throw error;
  }
}