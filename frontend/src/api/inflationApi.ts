import { http } from './http';
import type { InflationAccumulated, InflationIndex } from '../domain/types';

export interface CreateInflationRequest {
  year: number;
  month: number;
  categoryCode?: string;
  categoryName?: string;
  monthlyRate: number;
  source?: string;
  projection: boolean;
}

export const listInflation = (year: number) =>
  http.get<InflationIndex[]>(`/inflation?year=${year}`).then((r) => r.data);

export const createInflation = (payload: CreateInflationRequest) =>
  http.post<InflationIndex>('/inflation', payload).then((r) => r.data);

export const getAccumulatedInflation = (fromYear: number, fromMonth: number, toYear: number, toMonth: number) =>
  http.get<InflationAccumulated>(`/inflation/accumulated?fromYear=${fromYear}&fromMonth=${fromMonth}&toYear=${toYear}&toMonth=${toMonth}`).then((r) => r.data);
