import { useState } from "react";
import { getDefaultDate, shiftPeriod } from "../utils/transactionUtils";

export function useTransactionPeriod() {
  const today = new Date();
  const initialYear = today.getFullYear();
  const initialMonth = today.getMonth() + 1;

  const [year, setYear] = useState(initialYear);
  const [month, setMonth] = useState(initialMonth);

  const handlePeriodChange = (
    nextYear: number,
    nextMonth: number,
    onDefaultDateChange?: (nextDefaultDate: string) => void,
  ) => {
    setYear(nextYear);
    setMonth(nextMonth);

    onDefaultDateChange?.(getDefaultDate(nextYear, nextMonth));
  };

  const handleShiftPeriod = (
    delta: number,
    onDefaultDateChange?: (nextDefaultDate: string) => void,
  ) => {
    const next = shiftPeriod(year, month, delta);
    handlePeriodChange(next.year, next.month, onDefaultDateChange);
  };

  const handleCurrentPeriod = (
    onDefaultDateChange?: (nextDefaultDate: string) => void,
  ) => {
    handlePeriodChange(initialYear, initialMonth, onDefaultDateChange);
  };

  return {
    year,
    month,
    initialYear,
    initialMonth,
    handlePeriodChange,
    handleShiftPeriod,
    handleCurrentPeriod,
  };
}
