import { useState } from "react";
import { Link, useParams } from "react-router-dom";
import { AppLayout } from "../../components/layout/AppLayout";
import { EmptyState } from "../../components/ui/EmptyState";
import { ErrorState } from "../../components/ui/ErrorState";
import { MonthSelector } from "../../components/ui/MonthSelector";
import { useInvalidateMonthlyViews } from "../../hooks/useInvalidateMonthlyViews";
import { useMonthlyPeriod } from "../../hooks/useMonthlyPeriod";
import { useMonthlyPlanItemActions } from "../../hooks/useMonthlyPlanItemActions";
import { useMonthlyPlanningQueries } from "../../hooks/useMonthlyPlanningQueries";
import { useQuickCaptureFlow } from "../../hooks/useQuickCaptureFlow";
import { useStructuredPlanItemDraft } from "../../hooks/useStructuredPlanItemDraft";
import {
  MonthlyPlanItemsTable,
  type TableFilterKey,
} from "./components/MonthlyPlanItemsTable";
import { MonthlyPlanningChecklist } from "./components/MonthlyPlanningChecklist";
import { MonthlyPlanningGuide } from "./components/MonthlyPlanningGuide";
import { PlanningSummaryCards } from "./components/PlanningSummaryCards";
import { QuickCapturePanel } from "./components/QuickCapturePanel";
import { QuickCapturePreviewForm } from "./components/QuickCapturePreviewForm";
import { StructuredPlanItemForm } from "./components/StructuredPlanItemForm";
import { useMonthlyPlanReconciliationActions } from "../../hooks/useMonthlyPlanReconciliationActions";

const DEFAULT_TABLE_FILTER: TableFilterKey = "ALL";

export function MonthlyPlanningPage() {
  const { profileId = "" } = useParams();
  const { year, month, setYear, setMonth } = useMonthlyPeriod();
  const [tableFilter, setTableFilter] =
    useState<TableFilterKey>(DEFAULT_TABLE_FILTER);

  const invalidatePlanningViews = useInvalidateMonthlyViews(
    profileId,
    year,
    month,
  );

  const {
    planningQuery,
    reconciliationQuery,
    summary,
    reconciliation,
    items,
    accounts,
    categories,
  } = useMonthlyPlanningQueries(profileId, year, month);

  const reconciliationActions = useMonthlyPlanReconciliationActions({
    profileId,
    invalidatePlanningViews,
  });

  const isLoading = planningQuery.isLoading || reconciliationQuery.isLoading;
  const isError = planningQuery.isError || reconciliationQuery.isError;

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

  const isReady = !isLoading && !isError;

  return (
    <AppLayout>
      <div className="page-stack monthly-planning-page">
        <section className="page-header planning-hero">
          <div className="planning-hero-copy">
            <p className="eyebrow">Plan operativo</p>
            <h1>Planificación mensual</h1>
            <p className="secondary-text">
              Cargá estimaciones, completá faltantes y convertí sólo lo que ya
              está confirmado.
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
              <a className="boton-secundario" href="#plan-items">
                Revisar ítems
              </a>

              <a className="boton-secundario" href="#new-plan-item">
                Cargar manual
              </a>

              <Link
                className="boton-principal"
                to={`/profiles/${profileId}/dashboard`}
              >
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

        {isError ? (
          <ErrorState message="No se pudo cargar la planificación mensual." />
        ) : null}

        {isReady ? (
          <>
            <PlanningSummaryCards
              summary={summary}
              reconciliation={reconciliation}
            />

            <section className="planning-layout-grid">
              <div className="planning-layout-main">
                <MonthlyPlanningChecklist
                  summary={summary}
                  reconciliation={reconciliation}
                  items={items}
                  onApply={setTableFilter}
                />
              </div>

              <aside className="planning-layout-side">
                <MonthlyPlanningGuide summary={summary} />
              </aside>
            </section>

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

            <div id="plan-items">
              <MonthlyPlanItemsTable
                profileId={profileId}
                items={items}
                reconciliation={reconciliation}
                accounts={accounts}
                categories={categories}
                onConvert={actions.convert}
                onCancel={actions.cancel}
                onDelete={actions.remove}
                onUpdate={actions.update}
                onMarkPaid={actions.markPaid}
                onMarkCollected={actions.markCollected}
                onConfirmMatch={reconciliationActions.confirmMatch}
                onDeleteMatch={reconciliationActions.deleteMatch}
                pendingActionId={actions.pendingActionId}
                pendingMatchId={reconciliationActions.pendingMatchId}
                actionError={
                  actions.actionErrorMessage ??
                  reconciliationActions.reconciliationErrorMessage
                }
                externalFilterKey={tableFilter}
                onExternalFilterChange={setTableFilter}
              />
            </div>

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
          </>
        ) : null}
      </div>
    </AppLayout>
  );
}
