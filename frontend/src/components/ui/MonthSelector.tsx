type Props = { year: number; month: number; onYearChange: (value: number) => void; onMonthChange: (value: number) => void };

export function MonthSelector({ year, month, onYearChange, onMonthChange }: Props) {
  return <div className='form-row'><label>Año<input className='input' type='number' value={year} onChange={e => onYearChange(Number(e.target.value))} /></label><label>Mes<input className='input' type='number' min={1} max={12} value={month} onChange={e => onMonthChange(Number(e.target.value))} /></label></div>;
}
