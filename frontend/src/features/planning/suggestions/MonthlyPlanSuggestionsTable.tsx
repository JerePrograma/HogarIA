import { StatusBadge } from '../../../components/ui/StatusBadge';
import { formatMoney } from '../../../domain/formatters';
import {
  labelOrValue,
  monthlyPlanPriorityLabels,
  monthlyPlanSourceLabels,
  monthlyPlanTypeLabels,
  suggestionConfidenceLabels,
} from '../../../domain/financeLabels';
import type {
  Account,
  ApplyMonthlyPlanSuggestion,
  Category,
  MonthlyPlanItemType,
  MonthlyPlanPriority,
  MonthlyPlanSuggestion,
  SuggestionConfidence,
} from '../../../domain/types';

export type MonthlyPlanSuggestionDraft = ApplyMonthlyPlanSuggestion & {
  categoryName?: string | null;
  accountName?: string | null;
  confidence: SuggestionConfidence;
  reason: string;
};

type Props = {
  rows: MonthlyPlanSuggestionDraft[];
  categories: Category[];
  accounts: Account[];
  onRowsChange: (rows: MonthlyPlanSuggestionDraft[]) => void;
};

const typeOptions: MonthlyPlanItemType[] = ['INCOME', 'EXPENSE', 'DEBT', 'SAVING', 'RECOVERY'];
const priorityOptions: MonthlyPlanPriority[] = ['ESSENTIAL', 'IMPORTANT', 'OPTIONAL'];

export function getMonthlyPlanSuggestionValidationMessage(row: MonthlyPlanSuggestionDraft): string | null {
  if (!row.title.trim()) {
    return 'El título es obligatorio.';
  }

  if (row.amount == null && row.minAmount == null && row.maxAmount == null) {
    return 'Debe tener monto exacto o rango.';
  }

  if (row.minAmount != null && row.maxAmount != null && row.minAmount > row.maxAmount) {
    return 'El mínimo no puede superar al máximo.';
  }

  if (row.expectedDate && !isDateInPeriod(row.expectedDate, row.periodYear, row.periodMonth)) {
    return 'La fecha esperada debe pertenecer al período destino.';
  }

  return null;
}

export const toMonthlyPlanDrafts = (
  suggestions: MonthlyPlanSuggestion[],
): MonthlyPlanSuggestionDraft[] =>
  suggestions.map((suggestion) => ({
    title: suggestion.title,
    description: suggestion.description,
    expectedDate: suggestion.expectedDate,
    periodYear: suggestion.periodYear,
    periodMonth: suggestion.periodMonth,
    amount: suggestion.amount,
    minAmount: suggestion.minAmount,
    maxAmount: suggestion.maxAmount,
    categoryId: suggestion.categoryId,
    categoryName: suggestion.categoryName,
    accountId: suggestion.accountId,
    accountName: suggestion.accountName,
    type: suggestion.type,
    priority: suggestion.priority,
    source: suggestion.source,
    apply: suggestion.applyByDefault,
    duplicate: suggestion.duplicate,
    sourceTransactionIds: suggestion.sourceTransactionIds,
    confidence: suggestion.confidence,
    reason: suggestion.reason,
  }));

export function MonthlyPlanSuggestionsTable({
  rows,
  categories,
  accounts,
  onRowsChange,
}: Props) {
  const updateRow = (index: number, patch: Partial<MonthlyPlanSuggestionDraft>) => {
    onRowsChange(rows.map((row, rowIndex) => (rowIndex === index ? { ...row, ...patch } : row)));
  };

  if (rows.length === 0) {
    return <p className="mensaje-info">No hay sugerencias de planificación para los filtros actuales.</p>;
  }

  return (
    <section className="panel suggestion-table-panel">
      <div className="section-title">
        <div>
          <p className="eyebrow">Planificación</p>
          <h2>Compromisos sugeridos</h2>
        </div>
      </div>

      <div className="tabla-ui suggestion-table-desktop">
        <table>
          <thead>
            <tr>
              <th>Aplicar</th>
              <th>Título</th>
              <th>Tipo</th>
              <th>Prioridad</th>
              <th>Fecha</th>
              <th>Monto exacto</th>
              <th>Rango</th>
              <th>Categoría</th>
              <th>Cuenta</th>
              <th>Confianza</th>
              <th>Motivo</th>
            </tr>
          </thead>
          <tbody>
            {rows.map((row, index) => {
              const validationMessage = row.apply ? getMonthlyPlanSuggestionValidationMessage(row) : null;

              return (
              <tr key={`${row.title}-${row.periodYear}-${row.periodMonth}-${index}`}>
                <td>
                  <input
                    type="checkbox"
                    checked={row.apply}
                    onChange={(event) => updateRow(index, { apply: event.target.checked })}
                    aria-label={`Aplicar ${row.title}`}
                  />
                </td>
                <td>
                  <input
                    className="input-ui suggestion-title-input"
                    value={row.title}
                    onChange={(event) => updateRow(index, { title: event.target.value })}
                  />
                  <span className="compact-muted">
                    {row.periodMonth}/{row.periodYear} · {labelOrValue(monthlyPlanSourceLabels, row.source)}
                  </span>
                </td>
                <td>
                  <select
                    className="input-ui"
                    value={row.type}
                    onChange={(event) => updateRow(index, { type: event.target.value as MonthlyPlanItemType })}
                  >
                    {typeOptions.map((type) => (
                      <option key={type} value={type}>
                        {monthlyPlanTypeLabels[type]}
                      </option>
                    ))}
                  </select>
                </td>
                <td>
                  <select
                    className="input-ui"
                    value={row.priority ?? 'IMPORTANT'}
                    onChange={(event) =>
                      updateRow(index, { priority: event.target.value as MonthlyPlanPriority })
                    }
                  >
                    {priorityOptions.map((priority) => (
                      <option key={priority} value={priority}>
                        {monthlyPlanPriorityLabels[priority]}
                      </option>
                    ))}
                  </select>
                </td>
                <td>
                  <input
                    className="input-ui"
                    type="date"
                    value={row.expectedDate ?? ''}
                    onChange={(event) => updateRow(index, { expectedDate: event.target.value || null })}
                  />
                </td>
                <td>
                  <input
                    className="input-ui suggestion-amount-input"
                    type="number"
                    min="0"
                    inputMode="decimal"
                    value={row.amount ?? ''}
                    onChange={(event) => updateRow(index, { amount: parseNullableAmount(event.target.value) })}
                  />
                </td>
                <td>
                  <div className="suggestion-range-inputs">
                    <input
                      className="input-ui suggestion-amount-input"
                      type="number"
                      min="0"
                      inputMode="decimal"
                      value={row.minAmount ?? ''}
                      aria-label={`Monto mínimo para ${row.title}`}
                      onChange={(event) => updateRow(index, { minAmount: parseNullableAmount(event.target.value) })}
                    />
                    <input
                      className="input-ui suggestion-amount-input"
                      type="number"
                      min="0"
                      inputMode="decimal"
                      value={row.maxAmount ?? ''}
                      aria-label={`Monto máximo para ${row.title}`}
                      onChange={(event) => updateRow(index, { maxAmount: parseNullableAmount(event.target.value) })}
                    />
                  </div>
                  {validationMessage ? (
                    <span className="compact-error">{validationMessage}</span>
                  ) : null}
                </td>
                <td>
                  <select
                    className="input-ui"
                    value={row.categoryId ?? ''}
                    onChange={(event) => updateRow(index, { categoryId: event.target.value || null })}
                  >
                    <option value="">Sin categoría</option>
                    {categories.map((category) => (
                      <option key={category.id} value={category.id}>
                        {category.name}
                      </option>
                    ))}
                  </select>
                </td>
                <td>
                  <select
                    className="input-ui"
                    value={row.accountId ?? ''}
                    onChange={(event) => updateRow(index, { accountId: event.target.value || null })}
                  >
                    <option value="">Sin cuenta</option>
                    {accounts.map((account) => (
                      <option key={account.id} value={account.id}>
                        {account.name}
                      </option>
                    ))}
                  </select>
                </td>
                <td>
                  <ConfidenceBadge confidence={row.confidence} />
                </td>
                <td>
                  <span>{row.reason}</span>
                  {row.duplicate ? <StatusBadge label="Duplicado" tone="watch" /> : null}
                </td>
              </tr>
              );
            })}
          </tbody>
        </table>
      </div>

      <div className="suggestion-mobile-list">
        {rows.map((row, index) => {
          const validationMessage = row.apply ? getMonthlyPlanSuggestionValidationMessage(row) : null;

          return (
          <article key={`${row.title}-${index}`} className="suggestion-mobile-card">
            <header>
              <div>
                <strong>{row.title}</strong>
                <span className="compact-muted">
                  {labelOrValue(monthlyPlanTypeLabels, row.type)} · {formatMoney(row.amount ?? row.minAmount ?? row.maxAmount)}
                </span>
              </div>
              <input
                type="checkbox"
                checked={row.apply}
                onChange={(event) => updateRow(index, { apply: event.target.checked })}
                aria-label={`Aplicar ${row.title}`}
              />
            </header>
            <div className="form-grid">
              <label>
                Título
                <input value={row.title} onChange={(event) => updateRow(index, { title: event.target.value })} />
              </label>
              <label>
                Tipo
                <select
                  value={row.type}
                  onChange={(event) => updateRow(index, { type: event.target.value as MonthlyPlanItemType })}
                >
                  {typeOptions.map((type) => (
                    <option key={type} value={type}>
                      {monthlyPlanTypeLabels[type]}
                    </option>
                  ))}
                </select>
              </label>
              <label>
                Fecha esperada
                <input
                  type="date"
                  value={row.expectedDate ?? ''}
                  onChange={(event) => updateRow(index, { expectedDate: event.target.value || null })}
                />
              </label>
              <label>
                Monto exacto
                <input
                  type="number"
                  min="0"
                  inputMode="decimal"
                  value={row.amount ?? ''}
                  onChange={(event) => updateRow(index, { amount: parseNullableAmount(event.target.value) })}
                />
              </label>
              <label>
                Mínimo
                <input
                  type="number"
                  min="0"
                  inputMode="decimal"
                  value={row.minAmount ?? ''}
                  onChange={(event) => updateRow(index, { minAmount: parseNullableAmount(event.target.value) })}
                />
              </label>
              <label>
                Máximo
                <input
                  type="number"
                  min="0"
                  inputMode="decimal"
                  value={row.maxAmount ?? ''}
                  onChange={(event) => updateRow(index, { maxAmount: parseNullableAmount(event.target.value) })}
                />
              </label>
              <label>
                Categoría
                <select
                  value={row.categoryId ?? ''}
                  onChange={(event) => updateRow(index, { categoryId: event.target.value || null })}
                >
                  <option value="">Sin categoría</option>
                  {categories.map((category) => (
                    <option key={category.id} value={category.id}>
                      {category.name}
                    </option>
                  ))}
                </select>
              </label>
              <label>
                Cuenta
                <select
                  value={row.accountId ?? ''}
                  onChange={(event) => updateRow(index, { accountId: event.target.value || null })}
                >
                  <option value="">Sin cuenta</option>
                  {accounts.map((account) => (
                    <option key={account.id} value={account.id}>
                      {account.name}
                    </option>
                  ))}
                </select>
              </label>
              <label>
                Prioridad
                <select
                  value={row.priority ?? 'IMPORTANT'}
                  onChange={(event) =>
                    updateRow(index, { priority: event.target.value as MonthlyPlanPriority })
                  }
                >
                  {priorityOptions.map((priority) => (
                    <option key={priority} value={priority}>
                      {monthlyPlanPriorityLabels[priority]}
                    </option>
                  ))}
                </select>
              </label>
            </div>
            <ConfidenceBadge confidence={row.confidence} />
            {validationMessage ? <p className="mensaje-error compact-message">{validationMessage}</p> : null}
            <p className="secondary-text">{row.reason}</p>
            {row.duplicate ? <StatusBadge label="Duplicado" tone="watch" /> : null}
          </article>
          );
        })}
      </div>
    </section>
  );
}

function ConfidenceBadge({ confidence }: { confidence: SuggestionConfidence }) {
  const tone = confidence === 'HIGH' ? 'ok' : confidence === 'MEDIUM' ? 'watch' : 'risk';
  return <StatusBadge label={suggestionConfidenceLabels[confidence]} tone={tone} />;
}

function parseNullableAmount(value: string): number | null {
  if (!value.trim()) {
    return null;
  }

  const parsed = Number(value.replace(',', '.'));
  return Number.isFinite(parsed) && parsed >= 0 ? parsed : null;
}

function isDateInPeriod(value: string, year: number, month: number): boolean {
  const [dateYear, dateMonth] = value.split('-').map(Number);
  return dateYear === year && dateMonth === month;
}
