import { useNavigate } from 'react-router-dom';
import { MonthlyPlanningChecklist } from '../../components/MonthlyPlanningChecklist';
import { PlanningMappingsAuditPanel } from '../../components/PlanningMappingsAuditPanel';
import { usePlanningData } from '../../hooks/usePlanningData';
import { preservePlanningPeriodParams } from '../../planningSearchParams';
import { planningRoutes } from '../../planningRoutes';
import { buildPlanningPath } from '../../utils/buildPlanningPath';

export function MonthlyPlanAlertsPage(){ const { summary,items,accounts,categories,profileId,searchParams }=usePlanningData(); const navigate = useNavigate(); const onApply = (key: 'UNPRICED'|'MISSING_CLASSIFICATION'|'READY_TO_CONVERT'|'DUE_NEXT_7_DAYS'|'ALL') => { const next = preservePlanningPeriodParams(searchParams); if (key === 'READY_TO_CONVERT') navigate(buildPlanningPath(planningRoutes.convert(profileId), next)); else { next.set('filter', key); navigate(buildPlanningPath(planningRoutes.items(profileId), next)); } }; return <section className='planning-section-stack'><MonthlyPlanningChecklist summary={summary} items={items} onApply={onApply}/><PlanningMappingsAuditPanel profileId={profileId} items={items} accounts={accounts} categories={categories} periodParams={preservePlanningPeriodParams(searchParams)} /></section>; }
