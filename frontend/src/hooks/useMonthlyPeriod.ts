import { useState } from 'react';

export function useMonthlyPeriod() {
  const today = new Date();
  const [year, setYear] = useState(today.getFullYear());
  const [month, setMonth] = useState(today.getMonth() + 1);

  return { year, month, setYear, setMonth };
}
