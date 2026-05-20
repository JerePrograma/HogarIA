import { useState } from 'react';

export function getDefaultDate(year: number, month: number) {
  return `${year}-${String(month).padStart(2, '0')}-01`;
}

export function getPeriodDate(year: number, month: number) {
  return new Date(year, month - 1, 1);
}

export function shiftPeriod(year: number, month: number, delta: number) {
  const date = new Date(year, month - 1 + delta, 1);

  return {
    year: date.getFullYear(),
    month: date.getMonth() + 1,
  };
}

export function formatPeriodLabel(year: number, month: number) {
  return new Intl.DateTimeFormat('es-AR', {
    month: 'long',
    year: 'numeric',
  }).format(getPeriodDate(year, month));
}

export function useMonthlyPeriod() {
  const today = new Date();
  const initialYear = today.getFullYear();
  const initialMonth = today.getMonth() + 1;

  const [year, setYear] = useState(initialYear);
  const [month, setMonth] = useState(initialMonth);

  const periodLabel = formatPeriodLabel(year, month);

  const setPeriod = (nextYear: number, nextMonth: number) => {
    setYear(nextYear);
    setMonth(nextMonth);
  };

  const shift = (delta: number) => {
    const next = shiftPeriod(year, month, delta);
    setPeriod(next.year, next.month);
  };

  const goToCurrent = () => {
    setPeriod(initialYear, initialMonth);
  };

  return {
    year,
    month,
    periodLabel,
    defaultDate: getDefaultDate(year, month),
    setPeriod,
    shift,
    goToCurrent,
  };
}