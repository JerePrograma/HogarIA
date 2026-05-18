import { MonthSelector } from "../../components/ui/MonthSelector";

type Props = {
  year: number;
  month: number;
  onYearChange: (year: number) => void;
  onMonthChange: (month: number) => void;
};

export function BudgetPageHeader({
  year,
  month,
  onYearChange,
  onMonthChange,
}: Props) {
  return (
    <section className="page-header">
      <div>
        <p className="eyebrow">Plan financiero</p>
        <h1>Presupuesto mensual</h1>
        <p className="muted">
          Definí límites por categoría y comparalos contra los movimientos reales
          del período.
        </p>
      </div>

      <div className="stack-ui md:min-w-[360px]">
        <MonthSelector
          year={year}
          month={month}
          onYearChange={onYearChange}
          onMonthChange={onMonthChange}
        />
      </div>
    </section>
  );
}