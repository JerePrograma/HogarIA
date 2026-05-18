import { Link, Navigate, useNavigate, useParams, useSearchParams } from 'react-router-dom';
import { EmptyState } from '../../components/ui/EmptyState';
import { ErrorState } from '../../components/ui/ErrorState';
import { useInvalidateMonthlyViews } from '../../hooks/useInvalidateMonthlyViews';
import { useMonthlyPeriod } from '../../hooks/useMonthlyPeriod';
import { useMonthlyPlanItemActions } from '../../hooks/useMonthlyPlanItemActions';
import { useMonthlyPlanningQueries } from '../../hooks/useMonthlyPlanningQueries';
import { useQuickCaptureFlow } from '../../hooks/useQuickCaptureFlow';
import { useStructuredPlanItemDraft } from '../../hooks/useStructuredPlanItemDraft';
import { MonthlyPlanItemsTable } from './components/MonthlyPlanItemsTable';
import { MonthlyPlanningChecklist } from './components/MonthlyPlanningChecklist';
import { PlanningMappingsAuditPanel } from './components/PlanningMappingsAuditPanel';
import { PlanningSummaryCards } from './components/PlanningSummaryCards';
import { QuickCapturePanel } from './components/QuickCapturePanel';
import { QuickCapturePreviewForm } from './components/QuickCapturePreviewForm';
import { StructuredPlanItemForm } from './components/StructuredPlanItemForm';
import { planningRoutes } from './planningRoutes';

function usePlanningData() {
  const { profileId = '' } = useParams();
  const { year, month, searchParams } = useMonthlyPeriod();
  const invalidatePlanningViews = useInvalidateMonthlyViews(profileId, year, month);
  const queries = useMonthlyPlanningQueries(profileId, year, month);
  return { profileId, year, month, searchParams, invalidatePlanningViews, ...queries };
}

export function PlanningOverviewPage() { const { profileId, summary, items, searchParams } = usePlanningData(); const q=`?${searchParams.toString()}`;
  return <section className='planning-section-stack'><PlanningSummaryCards summary={summary}/><MonthlyPlanningChecklist summary={summary} items={items} onApply={()=>{}}/><div className='page-actions'><Link className='boton-principal' to={`${planningRoutes.monthly(profileId)}${q}`}>Ir a vista mensual</Link></div></section>; }

export function MonthlyPlanningHomePage() { const { summary }=usePlanningData(); return <PlanningSummaryCards summary={summary}/>; }

export function MonthlyPlanItemsPage() { const { profileId,year,month,items,accounts,categories,invalidatePlanningViews,searchParams }=usePlanningData(); const draft=useStructuredPlanItemDraft(year,month); const actions=useMonthlyPlanItemActions({profileId,year,month,form:draft.form,resetFormAfterCreate:draft.resetAfterCreate,invalidatePlanningViews});
  const [params,setParams]=useSearchParams();
  const filter=(params.get('filter') as any) ?? 'ALL';
  return <MonthlyPlanItemsTable profileId={profileId} items={items} accounts={accounts} categories={categories} onConvert={actions.convert} onCancel={actions.cancel} onDelete={actions.remove} onUpdate={actions.update} onMarkPaid={actions.markPaid} onMarkCollected={actions.markCollected} pendingActionId={actions.pendingActionId} actionError={actions.actionErrorMessage} externalFilterKey={filter} onExternalFilterChange={(f)=>{const n=new URLSearchParams(params);n.set('filter',f);setParams(n,{replace:true});}}/>; }

export function MonthlyPlanItemCreatePage(){ const { profileId,year,month,accounts,categories,invalidatePlanningViews,searchParams }=usePlanningData(); const draft=useStructuredPlanItemDraft(year,month); const nav=useNavigate(); const actions=useMonthlyPlanItemActions({profileId,year,month,form:draft.form,resetFormAfterCreate:draft.resetAfterCreate,invalidatePlanningViews});
 return <StructuredPlanItemForm form={draft.form} setForm={draft.setForm} accounts={accounts} categories={categories} onCreate={()=>{actions.create();nav(`${planningRoutes.items(profileId)}?${searchParams.toString()}`);}} isCreating={actions.isCreating} error={actions.createErrorMessage}/>; }

export function MonthlyPlanItemEditPage(){ return <EmptyState title='Edición dedicada' message='Usá la edición en línea de la tabla operativa por ahora. Refactor pendiente para formulario standalone.'/>
}

export function MonthlyPlanImportPage(){ const { profileId,year,month,accounts,categories,invalidatePlanningViews }=usePlanningData(); const quick=useQuickCaptureFlow({profileId,year,month,invalidatePlanningViews});
 return <QuickCapturePanel input={quick.quickText} onChange={quick.setQuickText} onAnalyze={quick.analyze} canAnalyze={quick.canAnalyze} error={quick.quickError} showDiscard={quick.hasPreview} onDiscard={quick.discard} onClear={quick.clear} isAnalyzing={quick.isAnalyzing}>{quick.quickPreview && quick.quickForm ? <QuickCapturePreviewForm profileId={profileId} preview={quick.quickPreview} form={quick.quickForm} setForm={quick.setQuickForm} accounts={accounts} categories={categories} onConfirm={quick.confirm} isConfirming={quick.isConfirming} error={quick.commitError}/> : null}</QuickCapturePanel>; }

export function MonthlyPlanAlertsPage(){ const { summary,items }=usePlanningData(); return <MonthlyPlanningChecklist summary={summary} items={items} onApply={()=>{}}/>; }
export function MonthlyPlanConvertPage(){ const {items,accounts,categories,profileId,year,month,invalidatePlanningViews }=usePlanningData(); const draft=useStructuredPlanItemDraft(year,month); const actions=useMonthlyPlanItemActions({profileId,year,month,form:draft.form,resetFormAfterCreate:draft.resetAfterCreate,invalidatePlanningViews}); return <MonthlyPlanItemsTable profileId={profileId} items={items} accounts={accounts} categories={categories} onConvert={actions.convert} onCancel={actions.cancel} onDelete={actions.remove} onUpdate={actions.update} onMarkPaid={actions.markPaid} onMarkCollected={actions.markCollected} pendingActionId={actions.pendingActionId} actionError={actions.actionErrorMessage} externalFilterKey='READY_TO_CONVERT'/>; }

export function LegacyPlanningRedirect(){ const { profileId='' }=useParams(); const [searchParams]=useSearchParams(); return <Navigate to={`${planningRoutes.root(profileId)}?${searchParams.toString()}`} replace/>; }
