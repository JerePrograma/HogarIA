import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useState } from 'react';
import { Link, useParams } from 'react-router-dom';
import { listAccounts } from '../../api/accountsApi';
import { listCategories } from '../../api/categoriesApi';
import { getApiErrorMessage } from '../../api/http';
import {
  commitMonthlyPlanQuickCapture,
  previewMonthlyPlanQuickCapture,
} from '../../api/monthlyPlanQuickCaptureApi';
import {
  convertMonthlyPlanItemToTransaction,
  createMonthlyPlanItem,
  deleteMonthlyPlanItem,
  getMonthlyPlan,
  updateMonthlyPlanItem,
} from '../../api/monthlyPlanningApi';
import { AppLayout } from '../../components/layout/AppLayout';
import { EmptyState } from '../../components/ui/EmptyState';
import { ErrorState } from '../../components/ui/ErrorState';
import { MonthSelector } from '../../components/ui/MonthSelector';
import type {
  MonthlyPlanItemCreatePayload,
  MonthlyPlanItemUpdatePayload,
  MonthlyPlanSummary,
  QuickCapturePreviewResponse,
} from '../../domain/types';
import { MonthlyPlanItemsTable, type TableFilterKey } from './components/MonthlyPlanItemsTable';
import { MonthlyPlanningChecklist } from './components/MonthlyPlanningChecklist';
import { MonthlyPlanningGuide } from './components/MonthlyPlanningGuide';
import { PlanningSummaryCards } from './components/PlanningSummaryCards';
import { QuickCapturePanel } from './components/QuickCapturePanel';
import { QuickCapturePreviewForm } from './components/QuickCapturePreviewForm';
import { StructuredPlanItemForm } from './components/StructuredPlanItemForm';

const createEmptyForm = (year: number, month: number): MonthlyPlanItemCreatePayload => ({
  type: 'EXPENSE',
  title: '',
  periodYear: year,
  periodMonth: month,
  priority: 'IMPORTANT',
  status: 'ESTIMATED',
  currency: 'ARS',
});

export function MonthlyPlanningPage() {
  const { profileId = '' } = useParams();
  const queryClient = useQueryClient();

  const today = new Date();
  const [year, setYear] = useState(today.getFullYear());
  const [month, setMonth] = useState(today.getMonth() + 1);
  const [tableFilter, setTableFilter] = useState<TableFilterKey>('ALL');

  const [quickText, setQuickText] = useState('');
  const [quickPreview, setQuickPreview] = useState<QuickCapturePreviewResponse | null>(null);
  const [quickForm, setQuickForm] = useState<MonthlyPlanItemCreatePayload | null>(null);
  const [quickError, setQuickError] = useState('');

  const [form, setForm] = useState<MonthlyPlanItemCreatePayload>(() =>
    createEmptyForm(year, month),
  );

  const invalidatePlanningViews = () => {
    queryClient.invalidateQueries({ queryKey: ['planning', profileId, year, month] });
    queryClient.invalidateQueries({ queryKey: ['dashboard', profileId, year, month] });
    queryClient.invalidateQueries({ queryKey: ['tx', profileId, year, month] });
  };

  const planningQuery = useQuery<MonthlyPlanSummary>({
    queryKey: ['planning', profileId, year, month],
    queryFn: () => getMonthlyPlan(profileId, year, month),
    enabled: Boolean(profileId),
  });

  const accountsQuery = useQuery({
    queryKey: ['accounts', profileId],
    queryFn: () => listAccounts(profileId),
    enabled: Boolean(profileId),
  });

  const categoriesQuery = useQuery({
    queryKey: ['categories', profileId],
    queryFn: () => listCategories(profileId, true),
    enabled: Boolean(profileId),
  });

  const createMutation = useMutation({
    mutationFn: () =>
      createMonthlyPlanItem(profileId, {
        ...form,
        periodYear: year,
        periodMonth: month,
      }),
    onSuccess: () => {
      setForm((current) => ({
        ...createEmptyForm(year, month),
        type: current.type,
        priority: current.priority,
        status: current.status,
        accountId: current.accountId,
        categoryId: current.categoryId,
      }));
      invalidatePlanningViews();
    },
  });

  const deleteMutation = useMutation({
    mutationFn: (id: string) => deleteMonthlyPlanItem(profileId, id),
    onSuccess: invalidatePlanningViews,
  });

  const convertMutation = useMutation({
    mutationFn: (id: string) => convertMonthlyPlanItemToTransaction(profileId, id),
    onSuccess: invalidatePlanningViews,
  });

  const updateMutation = useMutation({
    mutationFn: ({ id, payload }: { id: string; payload: MonthlyPlanItemUpdatePayload }) =>
      updateMonthlyPlanItem(profileId, id, payload),
    onSuccess: invalidatePlanningViews,
  });

  const cancelMutation = useMutation({
    mutationFn: (id: string) => updateMonthlyPlanItem(profileId, id, { status: 'CANCELLED' }),
    onSuccess: invalidatePlanningViews,
  });

  const markPaidMutation = useMutation({
    mutationFn: (id: string) => updateMonthlyPlanItem(profileId, id, { status: 'PAID' }),
    onSuccess: invalidatePlanningViews,
  });

  const markCollectedMutation = useMutation({
    mutationFn: (id: string) => updateMonthlyPlanItem(profileId, id, { status: 'COLLECTED' }),
    onSuccess: invalidatePlanningViews,
  });

  const lastActionError =
    convertMutation.error ??
    updateMutation.error ??
    cancelMutation.error ??
    markPaidMutation.error ??
    markCollectedMutation.error ??
    deleteMutation.error;

  const actionErrorMessage = lastActionError ? getApiErrorMessage(lastActionError) : null;

  const quickPreviewMutation = useMutation({
    mutationFn: () =>
      previewMonthlyPlanQuickCapture(profileId, {
        rawText: quickText,
        defaultYear: year,
        defaultMonth: month,
        defaultCurrency: 'ARS',
      }),
    onSuccess: (response) => {
      setQuickError('');
      setQuickPreview(response);
      setQuickForm(response.parsed);
    },
    onError: (error) => {
      setQuickPreview(null);
      setQuickForm(null);
      setQuickError(getApiErrorMessage(error));
    },
  });

  const quickCommitMutation = useMutation({
    mutationFn: () =>
      commitMonthlyPlanQuickCapture(profileId, {
        rawText: quickText,
        payload: quickForm as MonthlyPlanItemCreatePayload,
      }),
    onSuccess: () => {
      setQuickText('');
      setQuickPreview(null);
      setQuickForm(null);
      invalidatePlanningViews();
    },
  });

  const accounts = accountsQuery.data ?? [];
  const categories = categoriesQuery.data ?? [];
  const summary = planningQuery.data;
  const items = summary?.items ?? [];

  const pendingActionId =
    convertMutation.variables ??
    updateMutation.variables?.id ??
    cancelMutation.variables ??
    markPaidMutation.variables ??
    markCollectedMutation.variables ??
    deleteMutation.variables ??
    null;

  return (
    <AppLayout>
      <div className="page-stack">
        <section className="page-header">
          <div>
            <p className="eyebrow">Plan operativo</p>
            <h1>Planificación mensual</h1>
            <p className="secondary-text">
              Cargá ingresos, gastos y pendientes del mes. Después convertí lo confirmado en
              movimientos reales.
            </p>
          </div>

          <div className="stack-ui md:min-w-[360px]">
            <MonthSelector
              year={year}
              month={month}
              onYearChange={setYear}
              onMonthChange={setMonth}
            />

            <div className="page-actions">
              <Link className="boton-secundario" to={`/profiles/${profileId}/dashboard`}>
                Ver dashboard
              </Link>
            </div>
          </div>
        </section>

        {planningQuery.isLoading && (
          <EmptyState
            title="Cargando planificación"
            message="Estamos obteniendo los ítems planificados del período."
          />
        )}

        {planningQuery.isError && (
          <ErrorState message="No se pudo cargar la planificación mensual." />
        )}

        <MonthlyPlanningGuide summary={summary} />

        <QuickCapturePanel
          input={quickText}
          onChange={setQuickText}
          onAnalyze={() => quickPreviewMutation.mutate()}
          canAnalyze={Boolean(quickText.trim())}
          error={quickError}
          showDiscard={Boolean(quickPreview)}
          onDiscard={() => {
            setQuickPreview(null);
            setQuickForm(null);
          }}
          onClear={() => {
            setQuickText('');
            setQuickError('');
            setQuickPreview(null);
            setQuickForm(null);
          }}
          isAnalyzing={quickPreviewMutation.isPending}
        >
          {quickPreview && quickForm ? (
            <QuickCapturePreviewForm
              profileId={profileId}
              preview={quickPreview}
              form={quickForm}
              setForm={setQuickForm}
              accounts={accounts}
              categories={categories}
              onConfirm={() => quickCommitMutation.mutate()}
              isConfirming={quickCommitMutation.isPending}
              error={quickCommitMutation.error ? getApiErrorMessage(quickCommitMutation.error) : null}
            />
          ) : null}
        </QuickCapturePanel>

        <PlanningSummaryCards summary={summary} />

        <MonthlyPlanningChecklist summary={summary} items={items} onApply={setTableFilter} />

        <MonthlyPlanItemsTable
          profileId={profileId}
          items={items}
          accounts={accounts}
          categories={categories}
          onConvert={(id) => convertMutation.mutate(id)}
          onCancel={(id) => cancelMutation.mutate(id)}
          onDelete={(id) => deleteMutation.mutate(id)}
          onUpdate={(id, payload) =>
            updateMutation.mutateAsync({ id, payload }).then(() => undefined)
          }
          onMarkPaid={(id) => markPaidMutation.mutate(id)}
          onMarkCollected={(id) => markCollectedMutation.mutate(id)}
          pendingActionId={pendingActionId}
          actionError={actionErrorMessage}
          externalFilterKey={tableFilter}
        />

        <StructuredPlanItemForm
          form={form}
          setForm={setForm}
          accounts={accounts}
          categories={categories}
          onCreate={() => createMutation.mutate()}
          isCreating={createMutation.isPending}
          error={createMutation.error ? getApiErrorMessage(createMutation.error) : null}
        />
      </div>
    </AppLayout>
  );
}