import { useNavigate } from 'react-router-dom';
import { MonthlyPlanningChecklist } from '../../components/MonthlyPlanningChecklist';
import { PlanningMappingsAuditPanel } from '../../components/PlanningMappingsAuditPanel';
import { usePlanningData } from '../../hooks/usePlanningData';
import { planningRoutes } from '../../planningRoutes';
import { preservePlanningPeriodParams } from '../../planningSearchParams';
import { buildPlanningPath } from '../../utils/buildPlanningPath';

type AlertFilter =
  | 'UNPRICED'
  | 'MISSING_CLASSIFICATION'
  | 'READY_TO_CONVERT'
  | 'DUE_NEXT_7_DAYS'
  | 'ALL';

export function MonthlyPlanAlertsPage() {
  const {
    summary,
    items,
    accounts,
    categories,
    profileId,
    searchParams,
  } = usePlanningData();
  const navigate = useNavigate();

  const handleApply = (key: AlertFilter) => {
    const next = preservePlanningPeriodParams(searchParams);

    if (key === 'READY_TO_CONVERT') {
      navigate(buildPlanningPath(planningRoutes.convert(profileId), next));
      return;
    }

    next.set('filter', key);
    navigate(buildPlanningPath(planningRoutes.items(profileId), next));
  };

  return (
    <>
      <MonthlyPlanningChecklist
        summary={summary}
        items={items}
        onApply={handleApply}
      />

      <PlanningMappingsAuditPanel
        profileId={profileId}
        items={items}
        accounts={accounts}
        categories={categories}
        periodParams={preservePlanningPeriodParams(searchParams)}
      />
    </>
  );
}
