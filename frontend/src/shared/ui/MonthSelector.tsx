type Props = {
  year: number;
  month: number;
  onYearChange: (value: number) => void;
  onMonthChange: (value: number) => void;
};

const months = [
  { value: 1, label: 'Enero' },
  { value: 2, label: 'Febrero' },
  { value: 3, label: 'Marzo' },
  { value: 4, label: 'Abril' },
  { value: 5, label: 'Mayo' },
  { value: 6, label: 'Junio' },
  { value: 7, label: 'Julio' },
  { value: 8, label: 'Agosto' },
  { value: 9, label: 'Septiembre' },
  { value: 10, label: 'Octubre' },
  { value: 11, label: 'Noviembre' },
  { value: 12, label: 'Diciembre' },
];

export function MonthSelector({ year, month, onYearChange, onMonthChange }: Props) {
  return (
    <div className="panel-soft">
      <p className="label-ui">Período de análisis</p>

      <div className="form-row mt-3">
        <label>
          Año
          <input
            className="input-ui"
            type="number"
            min={2000}
            max={2100}
            value={year}
            onChange={(event) => onYearChange(Number(event.target.value))}
          />
        </label>

        <label>
          Mes
          <select
            className="input-ui"
            value={month}
            onChange={(event) => onMonthChange(Number(event.target.value))}
          >
            {months.map((monthOption) => (
              <option key={monthOption.value} value={monthOption.value}>
                {monthOption.label}
              </option>
            ))}
          </select>
        </label>
      </div>
    </div>
  );
}