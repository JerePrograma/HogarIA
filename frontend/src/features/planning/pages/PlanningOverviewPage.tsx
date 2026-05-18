import { Link, useNavigate } from 'react-router-dom';
import { MonthlyPlanningChecklist } from '../components/MonthlyPlanningChecklist';
import { PlanningSummaryCards } from '../components/PlanningSummaryCards';
import { planningRoutes } from '../planningRoutes';
import { parseTableFilterKey, preservePlanningPeriodParams } from '../planningSearchParams';
import { usePlanningData } from '../hooks/usePlanningData';
import { buildPlanningPath } from '../utils/buildPlanningPath';

export function PlanningOverviewPage() { const { profileId, summary, items, searchParams } = usePlanningData(); const navigate = useNavigate();
  const goToFilter = (key: 'UNPRICED'|'MISSING_CLASSIFICATION'|'READY_TO_CONVERT'|'DUE_NEXT_7_DAYS'|'ALL') => { const params = preservePlanningPeriodParams(searchParams); if (key === 'READY_TO_CONVERT') navigate(buildPlanningPath(planningRoutes.convert(profileId), params)); else { params.set('filter', parseTableFilterKey(key)); navigate(buildPlanningPath(planningRoutes.items(profileId), params)); } };
  return <section className='planning-section-stack'><PlanningSummaryCards summary={summary}/><MonthlyPlanningChecklist summary={summary} items={items} onApply={goToFilter}/><div className='page-actions'><Link className='boton-principal' to={buildPlanningPath(planningRoutes.monthly(profileId), preservePlanningPeriodParams(searchParams))}>Ir a vista mensual</Link></div></section>; }

export function MonthlyPlanningHomePage() { const { summary }=usePlanningData(); return <PlanningSummaryCards summary={summary}/>; }
