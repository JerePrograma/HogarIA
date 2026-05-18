import { useNavigate } from 'react-router-dom';
import { useMonthlyPlanItemActions } from '../../../../hooks/useMonthlyPlanItemActions';
import { useStructuredPlanItemDraft } from '../../../../hooks/useStructuredPlanItemDraft';
import { StructuredPlanItemForm } from '../../components/StructuredPlanItemForm';
import { usePlanningData } from '../../hooks/usePlanningData';
import { preservePlanningPeriodParams } from '../../planningSearchParams';
import { planningRoutes } from '../../planningRoutes';
import { buildPlanningPath } from '../../utils/buildPlanningPath';

export function MonthlyPlanItemCreatePage(){ const { profileId,year,month,accounts,categories,invalidatePlanningViews,searchParams }=usePlanningData(); const draft=useStructuredPlanItemDraft(year,month); const nav=useNavigate(); const actions=useMonthlyPlanItemActions({profileId,year,month,form:draft.form,resetFormAfterCreate:draft.resetAfterCreate,invalidatePlanningViews}); return <StructuredPlanItemForm form={draft.form} setForm={draft.setForm} accounts={accounts} categories={categories} onCreate={async()=>{ await actions.createAsync(); nav(buildPlanningPath(planningRoutes.items(profileId), preservePlanningPeriodParams(searchParams))); }} isCreating={actions.isCreating} error={actions.createErrorMessage}/>; }
