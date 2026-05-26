import { useCallback, useEffect, useState } from 'react';
import { MonthlyPlanItemCreatePayload } from '../domain/types';

export function createEmptyPlanItemForm(
  year: number,
  month: number,
): MonthlyPlanItemCreatePayload {
  return {
    type: 'EXPENSE',
    title: '',
    periodYear: year,
    periodMonth: month,
    priority: 'IMPORTANT',
    status: 'ESTIMATED',
    currency: 'ARS',
  };
}

export function useStructuredPlanItemDraft(year: number, month: number) {
  const [form, setForm] = useState<MonthlyPlanItemCreatePayload>(() =>
    createEmptyPlanItemForm(year, month),
  );

  useEffect(() => {
    setForm((current) => ({
      ...current,
      periodYear: current.expectedDate ? current.periodYear : year,
      periodMonth: current.expectedDate ? current.periodMonth : month,
    }));
  }, [year, month]);

  const resetAfterCreate = useCallback(
    (previous: MonthlyPlanItemCreatePayload) => {
      setForm({
        ...createEmptyPlanItemForm(year, month),
        type: previous.type,
        priority: previous.priority,
        status: previous.status,
        accountId: previous.accountId,
        categoryId: previous.categoryId,
      });
    },
    [year, month],
  );

  return {
    form,
    setForm,
    resetAfterCreate,
  };
}
