import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useState } from 'react';
import { useParams } from 'react-router-dom';
import { listAccounts } from '../../../api/accountsApi';
import {
  commitBudgetPlanningSuggestions,
  previewBudgetPlanningSuggestions,
} from '../../../api/budgetPlanningSuggestionsApi';
import { listCategories } from '../../../api/categoriesApi';
import { getApiErrorMessage } from '../../../api/http';
import { queryKeys } from '../../../domain/queryKeys';
import type {
  BudgetPlanningSuggestionCommitRequest,
  BudgetPlanningSuggestionCommitResponse,
  BudgetPlanningSuggestionMode,
} from '../../../domain/types';
import { useMonthlyPeriod } from '../../../hooks/useMonthlyPeriod';
import {
  BudgetSuggestionsTable,
  toBudgetDrafts,
  type BudgetSuggestionDraft,
} from './BudgetSuggestionsTable';
import {
  MonthlyPlanSuggestionsTable,
  getMonthlyPlanSuggestionValidationMessage,
  toMonthlyPlanDrafts,
  type MonthlyPlanSuggestionDraft,
} from './MonthlyPlanSuggestionsTable';
import { SuggestionModeSelector } from './SuggestionModeSelector';
import { SuggestionSummaryPanel } from './SuggestionSummaryPanel';

type CommitScope = 'BUDGET' | 'MONTHLY_PLAN' | 'BOTH';

function periodLabelForOffset(year: number, month: number, offsetMonths: number) {
  const date = new Date(year, month - 1 + offsetMonths, 1);
  return `${date.getMonth() + 1}/${date.getFullYear()}`;
}

export function BudgetPlanningSuggestionsPage() {
  const { profileId = '' } = useParams();
  const { year, month } = useMonthlyPeriod();
  const queryClient = useQueryClient();

  const [mode, setMode] = useState<BudgetPlanningSuggestionMode>('LAST_3_MONTHS_AVERAGE');
  const [includeImportedOnly, setIncludeImportedOnly] = useState(true);
  const [includeManual, setIncludeManual] = useState(false);
  const [includeReview, setIncludeReview] = useState(false);
  const [nextMonth, setNextMonth] = useState(true);
  const [skipDuplicates, setSkipDuplicates] = useState(true);
  const [overwriteExistingBudgetItems, setOverwriteExistingBudgetItems] = useState(false);
  const [roundingMultiple, setRoundingMultiple] = useState(1000);
  const [budgetRows, setBudgetRows] = useState<BudgetSuggestionDraft[]>([]);
  const [planRows, setPlanRows] = useState<MonthlyPlanSuggestionDraft[]>([]);
  const [warnings, setWarnings] = useState<string[]>([]);
  const [result, setResult] = useState<BudgetPlanningSuggestionCommitResponse | null>(null);
  const [error, setError] = useState<string | null>(null);

  const categoriesQuery = useQuery({
    queryKey: queryKeys.categories(profileId, true),
    queryFn: () => listCategories(profileId, true),
    enabled: Boolean(profileId),
  });

  const accountsQuery = useQuery({
    queryKey: queryKeys.accounts(profileId),
    queryFn: () => listAccounts(profileId),
    enabled: Boolean(profileId),
  });

  const previewMutation = useMutation({
    mutationFn: () =>
      previewBudgetPlanningSuggestions(profileId, {
        year,
        month,
        mode,
        includeImportedOnly,
        includeManual,
        includeReview,
        target: 'BOTH',
        nextMonth,
        roundingMultiple,
      }),
    onSuccess: (data) => {
      setBudgetRows(toBudgetDrafts(data.budgetSuggestions));
      setPlanRows(toMonthlyPlanDrafts(data.monthlyPlanSuggestions));
      setWarnings(data.warnings);
      setResult(null);
      setError(null);
    },
    onError: (unknownError) => setError(getApiErrorMessage(unknownError)),
  });

  const commitMutation = useMutation({
    mutationFn: (scope: CommitScope) =>
      commitBudgetPlanningSuggestions(profileId, buildCommitPayload(scope)),
    onSuccess: async (data, scope) => {
      setResult(data);
      setError(null);
      await invalidateAfterCommit(scope);
    },
    onError: (unknownError) => setError(getApiErrorMessage(unknownError)),
  });

  const hasBudgetSelection = budgetRows.some((row) => row.apply);
  const hasPlanSelection = planRows.some((row) => row.apply);
  const invalidPlanSelectionCount = planRows.filter(
    (row) => row.apply && getMonthlyPlanSuggestionValidationMessage(row),
  ).length;
  const hasInvalidPlanSelection = invalidPlanSelectionCount > 0;
  const planningPeriods = new Set(
    planRows
      .filter((row) => row.apply)
      .map((row) => `${row.periodMonth}/${row.periodYear}`),
  );
  const planningPeriodLabel =
    planningPeriods.size === 0
      ? nextMonth
        ? periodLabelForOffset(year, month, 1)
        : `${month}/${year}`
      : Array.from(planningPeriods).join(', ');

  const buildCommitPayload = (scope: CommitScope): BudgetPlanningSuggestionCommitRequest => ({
    year,
    month,
    applyBudgetSuggestions:
      scope === 'BUDGET' || scope === 'BOTH'
        ? budgetRows
            .filter((row) => row.apply)
            .map(({
              categoryName: _categoryName,
              realAmount: _realAmount,
              transactionCount: _transactionCount,
              confidence: _confidence,
              sourceTransactionIds: _sourceTransactionIds,
              ...row
            }) => row)
        : [],
    applyMonthlyPlanSuggestions:
      scope === 'MONTHLY_PLAN' || scope === 'BOTH'
        ? planRows
            .filter((row) => row.apply)
            .map(({
              categoryName: _categoryName,
              accountName: _accountName,
              confidence: _confidence,
              reason: _reason,
              ...row
            }) => row)
        : [],
    skipDuplicates,
    overwriteExistingBudgetItems,
  });

  const invalidateAfterCommit = async (scope: CommitScope) => {
    const invalidations = [
      queryClient.invalidateQueries({ queryKey: queryKeys.dashboard(profileId, year, month) }),
      queryClient.invalidateQueries({ queryKey: queryKeys.transactions(profileId, year, month) }),
      queryClient.invalidateQueries({ queryKey: queryKeys.budgetPlanningSuggestions(profileId, year, month) }),
    ];

    if (scope === 'BUDGET' || scope === 'BOTH') {
      invalidations.push(
        queryClient.invalidateQueries({ queryKey: queryKeys.budgetMonth(profileId, year, month) }),
        queryClient.invalidateQueries({ queryKey: queryKeys.budgetComparison(profileId, year, month) }),
      );
    }

    if (scope === 'MONTHLY_PLAN' || scope === 'BOTH') {
      const periods = new Set(
        planRows
          .filter((row) => row.apply)
          .map((row) => `${row.periodYear}-${row.periodMonth}`),
      );

      periods.add(`${year}-${month}`);

      for (const period of periods) {
        const [periodYear, periodMonth] = period.split('-').map(Number);
        invalidations.push(
          queryClient.invalidateQueries({ queryKey: queryKeys.planning(profileId, periodYear, periodMonth) }),
          queryClient.invalidateQueries({ queryKey: queryKeys.dashboard(profileId, periodYear, periodMonth) }),
          queryClient.invalidateQueries({
            queryKey: queryKeys.monthlyPlanReconciliation(profileId, periodYear, periodMonth),
          }),
        );
      }
    }

    await Promise.all(invalidations);
  };

  return (
    <section className="planning-section-stack suggestions-page">
      <section className="panel suggestions-control-panel">
        <div className="section-title">
          <div>
            <p className="eyebrow">Sugerencias</p>
            <h2>Presupuesto y planificación desde movimientos reales</h2>
          </div>
        </div>

        <SuggestionModeSelector mode={mode} onChange={setMode} />

        <div className="suggestions-controls-grid">
          <label className="suggestion-check">
            <input
              type="checkbox"
              checked={includeImportedOnly}
              onChange={(event) => {
                setIncludeImportedOnly(event.target.checked);
                if (event.target.checked) setIncludeManual(false);
              }}
            />
            Solo importados
          </label>

          <label className="suggestion-check">
            <input
              type="checkbox"
              checked={includeManual}
              disabled={includeImportedOnly}
              onChange={(event) => setIncludeManual(event.target.checked)}
            />
            Incluir manuales
          </label>

          <label className="suggestion-check">
            <input
              type="checkbox"
              checked={includeReview}
              onChange={(event) => setIncludeReview(event.target.checked)}
            />
            Incluir movimientos en revisión
          </label>

          <label className="suggestion-check">
            <input
              type="checkbox"
              checked={nextMonth}
              onChange={(event) => setNextMonth(event.target.checked)}
            />
            Planificar mes siguiente
          </label>

          <label>
            Redondeo
            <select
              value={roundingMultiple}
              onChange={(event) => setRoundingMultiple(Number(event.target.value))}
            >
              <option value={100}>$100</option>
              <option value={1000}>$1.000</option>
            </select>
          </label>
        </div>

        <div className="page-actions">
          <button
            type="button"
            className="boton-principal"
            disabled={previewMutation.isPending}
            onClick={() => previewMutation.mutate()}
          >
            {previewMutation.isPending ? 'Generando...' : 'Generar sugerencias'}
          </button>
        </div>
      </section>

      {error ? <p className="mensaje-error">{error}</p> : null}

      <SuggestionSummaryPanel
        totals={previewMutation.data?.totals}
        warnings={warnings}
        result={result}
        planningPeriodLabel={planningPeriodLabel}
      />

      {budgetRows.length > 0 || planRows.length > 0 ? (
        <section className="panel suggestions-commit-panel">
          <div className="suggestion-commit-options">
            <label className="suggestion-check">
              <input
                type="checkbox"
                checked={skipDuplicates}
                onChange={(event) => setSkipDuplicates(event.target.checked)}
              />
              Omitir duplicados
            </label>

            <label className="suggestion-check">
              <input
                type="checkbox"
                checked={overwriteExistingBudgetItems}
                onChange={(event) => setOverwriteExistingBudgetItems(event.target.checked)}
              />
              Reemplazar presupuesto existente
            </label>
          </div>

          {hasInvalidPlanSelection ? (
            <p className="mensaje-error compact-message">
              Hay {invalidPlanSelectionCount} sugerencia{invalidPlanSelectionCount === 1 ? '' : 's'} de planificación activa{invalidPlanSelectionCount === 1 ? '' : 's'} con datos incompletos.
            </p>
          ) : null}

          <div className="page-actions">
            <button
              type="button"
              className="boton-secundario"
              disabled={!hasBudgetSelection || commitMutation.isPending}
              onClick={() => commitMutation.mutate('BUDGET')}
            >
              Aplicar al presupuesto
            </button>
            <button
              type="button"
              className="boton-secundario"
              disabled={!hasPlanSelection || hasInvalidPlanSelection || commitMutation.isPending}
              onClick={() => commitMutation.mutate('MONTHLY_PLAN')}
            >
              Crear planificación
            </button>
            <button
              type="button"
              className="boton-principal"
              disabled={(!hasBudgetSelection && !hasPlanSelection) || hasInvalidPlanSelection || commitMutation.isPending}
              onClick={() => commitMutation.mutate('BOTH')}
            >
              Aplicar ambos
            </button>
          </div>
        </section>
      ) : null}

      {budgetRows.length > 0 ? (
        <BudgetSuggestionsTable rows={budgetRows} onRowsChange={setBudgetRows} />
      ) : null}

      {planRows.length > 0 ? (
        <MonthlyPlanSuggestionsTable
          rows={planRows}
          categories={categoriesQuery.data ?? []}
          accounts={accountsQuery.data ?? []}
          onRowsChange={setPlanRows}
        />
      ) : null}
    </section>
  );
}
