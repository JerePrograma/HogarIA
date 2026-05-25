import type { BudgetPlanningSuggestionMode } from '../../../domain/types';

type Props = {
  mode: BudgetPlanningSuggestionMode;
  onChange: (mode: BudgetPlanningSuggestionMode) => void;
};

const options: Array<{ value: BudgetPlanningSuggestionMode; label: string }> = [
  { value: 'CURRENT_MONTH_ONLY', label: 'Mes actual' },
  { value: 'LAST_3_MONTHS_AVERAGE', label: 'Promedio últimos 3 meses' },
  { value: 'LAST_6_MONTHS_AVERAGE', label: 'Promedio últimos 6 meses' },
];

export function SuggestionModeSelector({ mode, onChange }: Props) {
  return (
    <div className="suggestion-mode-selector" role="group" aria-label="Modo de sugerencia">
      {options.map((option) => (
        <button
          key={option.value}
          type="button"
          className={option.value === mode ? 'planning-filter-chip active' : 'planning-filter-chip'}
          onClick={() => onChange(option.value)}
        >
          {option.label}
        </button>
      ))}
    </div>
  );
}
