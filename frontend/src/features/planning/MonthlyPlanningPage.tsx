import { useState } from 'react';
import { Link, useParams, useSearchParams } from 'react-router-dom';
import { AppLayout } from '../../components/layout/AppLayout';
import { EmptyState } from '../../components/ui/EmptyState';
import { ErrorState } from '../../components/ui/ErrorState';
import { MonthSelector } from '../../components/ui/MonthSelector';
import { useInvalidateMonthlyViews } from '../../hooks/useInvalidateMonthlyViews';
import { useMonthlyPeriod } from '../../hooks/useMonthlyPeriod';
import { useMonthlyPlanItemActions } from '../../hooks/useMonthlyPlanItemActions';
import { useMonthlyPlanningQueries } from '../../hooks/useMonthlyPlanningQueries';
import { useQuickCaptureFlow } from '../../hooks/useQuickCaptureFlow';
import { useStructuredPlanItemDraft } from '../../hooks/useStructuredPlanItemDraft';
import { MonthlyPlanItemsTable, type TableFilterKey } from './components/MonthlyPlanItemsTable';
import { MonthlyPlanningChecklist } from './components/MonthlyPlanningChecklist';
import { MonthlyPlanningGuide } from './components/MonthlyPlanningGuide';
import { PlanningMappingsAuditPanel } from './components/PlanningMappingsAuditPanel';
import { PlanningWorkspaceNavigator, type PlanningWorkspaceView } from './components/PlanningWorkspaceNavigator';
import { PlanningSummaryCards } from './components/PlanningSummaryCards';
import { QuickCapturePanel } from './components/QuickCapturePanel';
import { QuickCapturePreviewForm } from './components/QuickCapturePreviewForm';
import { StructuredPlanItemForm } from './components/StructuredPlanItemForm';

const DEFAULT_TABLE_FILTER: TableFilterKey = 'ALL';

export function MonthlyPlanningPage() {
  const { profileId = '' } = useParams();
  const { year, month, setYear, setMonth } = useMonthlyPeriod();
  const [tableFilter, setTableFilter] = useState<TableFilterKey>(DEFAULT_TABLE_FILTER);
  const [searchParams, setSearchParams] = useSearchParams();
  const currentView = (searchParams.get('view') as PlanningWorkspaceView | null) ?? 'CHECKLIST';

  const invalidatePlanningViews = useInvalidateMonthlyViews(profileId, year, month);

  const { planningQuery, summary, items, accounts, categories } = useMonthlyPlanningQueries(
    profileId,
    year,
    month,
  );

  const draft = useStructuredPlanItemDraft(year, month);

  const actions = useMonthlyPlanItemActions({
    profileId,
    year,
    month,
    form: draft.form,
    resetFormAfterCreate: draft.resetAfterCreate,
    invalidatePlanningViews,
  });

  const quickCapture = useQuickCaptureFlow({
    profileId,
    year,
    month,
    invalidatePlanningViews,
  });

  const isLoading = planningQuery.isLoading;
  const isError = planningQuery.isError;
  const isReady = !isLoading && !isError;

  const setCurrentView = (view: PlanningWorkspaceView) => {
    const next = new URLSearchParams(searchParams);
    next.set('view', view);
    setSearchParams(next, { replace: true });
  };

  return (
    <AppLayout>
      <div className="page-stack monthly-planning-page">
        <section className="page-header planning-hero">
          <div className="planning-hero-copy">
            <p className="eyebrow">Plan operativo</p>
            <h1>Planificación mensual</h1>
            <p className="secondary-text">
              Cargá estimaciones, completá faltantes y convertí sólo lo que ya está confirmado.
            </p>
          </div>

          <div className="planning-hero-controls">
            <MonthSelector
              year={year}
              month={month}
              onYearChange={setYear}
              onMonthChange={setMonth}
            />

            <div className="page-actions planning-hero-actions">
              <button className="boton-secundario" type="button" onClick={() => setCurrentView('ITEMS')}>
                Revisar ítems
              </button>

              <button className="boton-secundario" type="button" onClick={() => setCurrentView('CAPTURE')}>
                Cargar manual
              </button>

              <Link className="boton-principal" to={`/profiles/${profileId}/dashboard`}>
                Ver dashboard
              </Link>
            </div>
          </div>
        </section>

        {isLoading ? (
          <EmptyState
            title="Cargando planificación"
            message="Estamos obteniendo los ítems planificados del período."
          />
        ) : null}

        {isError ? <ErrorState message="No se pudo cargar la planificación mensual." /> : null}

        {isReady ? (
          <>
            <PlanningSummaryCards summary={summary} />

            <PlanningWorkspaceNavigator
              activeView={currentView}
              onChange={setCurrentView}
              items={items}
            />

            {currentView === 'CHECKLIST' ? (
              <section className="planning-layout-grid">
                <div className="planning-layout-main">
                  <MonthlyPlanningChecklist
                    summary={summary}
                    items={items}
                    onApply={setTableFilter}
                  />
                </div>

                <aside className="planning-layout-side">
                  <MonthlyPlanningGuide summary={summary} />
                </aside>
              </section>
            ) : null}

            {currentView === 'CAPTURE' ? (
              <section className="planning-section-stack">
                <div className="panel panel-soft planning-section-header">
                  <div>
                    <p className="eyebrow">Paso 1</p>
                    <h2>Captura rápida</h2>
                    <p className="secondary-text">Escribí en lenguaje natural para generar un borrador.</p>
                  </div>
                  <button className="boton-fantasma" type="button" onClick={() => setCurrentView('ITEMS')}>
                    Ir a operaciones
                  </button>
                </div>

                <QuickCapturePanel
                  input={quickCapture.quickText}
                  onChange={quickCapture.setQuickText}
                  onAnalyze={quickCapture.analyze}
                  canAnalyze={quickCapture.canAnalyze}
                  error={quickCapture.quickError}
                  showDiscard={quickCapture.hasPreview}
                  onDiscard={quickCapture.discard}
                  onClear={quickCapture.clear}
                  isAnalyzing={quickCapture.isAnalyzing}
                >
                  {quickCapture.quickPreview && quickCapture.quickForm ? (
                    <QuickCapturePreviewForm
                      profileId={profileId}
                      preview={quickCapture.quickPreview}
                      form={quickCapture.quickForm}
                      setForm={quickCapture.setQuickForm}
                      accounts={accounts}
                      categories={categories}
                      onConfirm={quickCapture.confirm}
                      isConfirming={quickCapture.isConfirming}
                      error={quickCapture.commitError}
                    />
                  ) : null}
                </QuickCapturePanel>
              </section>
            ) : null}

            {currentView === 'ITEMS' ? (
              <section className="planning-section-stack" id="plan-items">
                <div className="panel panel-soft planning-section-header">
                  <div>
                    <p className="eyebrow">Paso 2</p>
                    <h2>Operación y confirmación</h2>
                    <p className="secondary-text">Convertí, editá y marcá el estado de cada ítem mensual.</p>
                  </div>
                  <button className="boton-fantasma" type="button" onClick={() => setCurrentView('MAPPINGS')}>
                    Auditar mapeos
                  </button>
                </div>
                <MonthlyPlanItemsTable
                  profileId={profileId}
                  items={items}
                  accounts={accounts}
                  categories={categories}
                  onConvert={actions.convert}
                  onCancel={actions.cancel}
                  onDelete={actions.remove}
                  onUpdate={actions.update}
                  onMarkPaid={actions.markPaid}
                  onMarkCollected={actions.markCollected}
                  pendingActionId={actions.pendingActionId}
                  actionError={actions.actionErrorMessage}
                  externalFilterKey={tableFilter}
                  onExternalFilterChange={setTableFilter}
                />
              </section>
            ) : null}

            {currentView === 'CAPTURE' ? (
              <div id="new-plan-item">
                <StructuredPlanItemForm
                  form={draft.form}
                  setForm={draft.setForm}
                  accounts={accounts}
                  categories={categories}
                  onCreate={actions.create}
                  isCreating={actions.isCreating}
                  error={actions.createErrorMessage}
                />
              </div>
            ) : null}

            {currentView === 'MAPPINGS' ? (
              <PlanningMappingsAuditPanel items={items} accounts={accounts} categories={categories} />
            ) : null}
          </>
        ) : null}
      </div>
    </AppLayout>
  );
}
