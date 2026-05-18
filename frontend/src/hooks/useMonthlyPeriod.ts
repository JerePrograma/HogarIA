import { useMemo } from 'react';
import { useSearchParams } from 'react-router-dom';

export function useMonthlyPeriod() {
  const [searchParams, setSearchParams] = useSearchParams();
  const today = new Date();
  const parsedYear = Number(searchParams.get('year'));
  const parsedMonth = Number(searchParams.get('month'));
  const year = Number.isInteger(parsedYear) && parsedYear > 2000 ? parsedYear : today.getFullYear();
  const month = Number.isInteger(parsedMonth) && parsedMonth >= 1 && parsedMonth <= 12 ? parsedMonth : today.getMonth() + 1;

  const setYear = (nextYear: number) => {
    const next = new URLSearchParams(searchParams);
    next.set('year', String(nextYear));
    next.set('month', String(month));
    setSearchParams(next, { replace: true });
  };
  const setMonth = (nextMonth: number) => {
    const next = new URLSearchParams(searchParams);
    next.set('year', String(year));
    next.set('month', String(nextMonth));
    setSearchParams(next, { replace: true });
  };

  return useMemo(() => ({ year, month, setYear, setMonth, searchParams, setSearchParams }), [year, month, searchParams]);
}
