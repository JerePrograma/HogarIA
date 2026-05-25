import { formatMoney } from '../../../domain/formatters';
import type {
  BudgetPlanningSuggestionCommitResponse,
  BudgetPlanningSuggestionTotals,
} from '../../../domain/types';

type Props = {
  totals?: BudgetPlanningSuggestionTotals;
  warnings: string[];
  result?: BudgetPlanningSuggestionCommitResponse | null;
};

export function SuggestionSummaryPanel({ totals, warnings, result }: Props) {
  if (!totals && warnings.length === 0 && !result) {
    return null;
  }

  return (
    <section className="panel-muted suggestion-summary-panel">
      {totals ? (
        <div className="suggestion-summary-grid">
          <SummaryItem
            label="Presupuesto sugerido"
            value={formatMoney(totals.totalSuggestedBudgetAmount)}
            detail={`${totals.budgetSuggestionCount} categorías`}
          />
          <SummaryItem
            label="Real detectado"
            value={formatMoney(totals.totalBudgetRealAmount)}
            detail="Movimientos filtrados"
          />
          <SummaryItem
            label="Planificación"
            value={formatMoney(totals.totalMonthlyPlanAmount)}
            detail={`${totals.monthlyPlanSuggestionCount} ítems`}
          />
          <SummaryItem
            label="Outliers"
            value={String(totals.outlierCount)}
            detail="Requieren revisión"
          />
        </div>
      ) : null}

      {result ? (
        <div className="mensaje-exito">
          <strong>Commit aplicado</strong>
          <span>
            Presupuesto: {result.createdBudgetItems} creados, {result.updatedBudgetItems} actualizados.
            Planificación: {result.createdMonthlyPlanItems} ítems creados. Omitidos: {result.skippedDuplicates}.
          </span>
        </div>
      ) : null}

      {warnings.length > 0 || (result?.warnings.length ?? 0) > 0 ? (
        <div className="mensaje-warning">
          {[...warnings, ...(result?.warnings ?? [])].map((warning, index) => (
            <span key={`${warning}-${index}`}>{warning}</span>
          ))}
        </div>
      ) : null}

      {result?.errors.length ? (
        <div className="mensaje-error">
          {result.errors.map((error, index) => (
            <span key={`${error}-${index}`}>{error}</span>
          ))}
        </div>
      ) : null}
    </section>
  );
}

function SummaryItem({ label, value, detail }: { label: string; value: string; detail: string }) {
  return (
    <div className="suggestion-summary-item">
      <span>{label}</span>
      <strong>{value}</strong>
      <p>{detail}</p>
    </div>
  );
}
