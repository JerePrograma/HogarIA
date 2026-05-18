import type { PlanItemSortKey, StatusFilterKey, TableFilterKey } from './planningUtils';

const tableFilterKeys: TableFilterKey[] = ['ALL', 'UNPRICED', 'MISSING_CLASSIFICATION', 'READY_TO_CONVERT', 'DUE_NEXT_7_DAYS'];
const statusFilterKeys: StatusFilterKey[] = ['ALL', 'PENDING', 'DONE', 'CANCELLED'];
const sortKeys: PlanItemSortKey[] = ['DATE', 'PRIORITY', 'AMOUNT'];
const typeKeys = ['ALL', 'INCOME', 'EXPENSE', 'DEBT', 'SAVING', 'TRANSFER', 'RECOVERY', 'TODO'] as const;

export type TypeFilterKey = (typeof typeKeys)[number];

export function parseTableFilterKey(value: string | null): TableFilterKey {
  return tableFilterKeys.find((key) => key === value) ?? 'ALL';
}
export function parseStatusFilterKey(value: string | null): StatusFilterKey {
  return statusFilterKeys.find((key) => key === value) ?? 'ALL';
}
export function parseSortKey(value: string | null): PlanItemSortKey {
  return sortKeys.find((key) => key === value) ?? 'DATE';
}
export function parseTypeFilterKey(value: string | null): TypeFilterKey {
  return typeKeys.find((key) => key === value) ?? 'ALL';
}

export function setPlanningSearchParam(params: URLSearchParams, key: string, value: string, defaultValue: string): URLSearchParams {
  const next = new URLSearchParams(params);
  if (!value || value === defaultValue) {
    next.delete(key);
  } else {
    next.set(key, value);
  }
  return next;
}

export function preservePlanningPeriodParams(params: URLSearchParams): URLSearchParams {
  const next = new URLSearchParams();
  const year = params.get('year');
  const month = params.get('month');
  if (year) next.set('year', year);
  if (month) next.set('month', month);
  return next;
}
