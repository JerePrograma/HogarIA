import { Link, useNavigate } from 'react-router-dom';
import { Button } from '../../../shared/ui/Button';
import { MonthlyPlanningChecklist } from '../components/MonthlyPlanningChecklist';
import { PlanningRealityPanel } from '../components/PlanningRealityPanel';
import { PlanningSummaryCards } from '../components/PlanningSummaryCards';
import { usePlanningData } from '../hooks/usePlanningData';
import { planningRoutes } from '../planningRoutes';
import {
  parseTableFilterKey,
  preservePlanningPeriodParams,
} from '../planningSearchParams';
import { buildPlanningPath } from '../utils/buildPlanningPath';

type ChecklistFilter =
  | 'UNPRICED'
  | 'MISSING_CLASSIFICATION'
  | 'READY_TO_CONVERT'
  | 'DUE_NEXT_7_DAYS'
  | 'ALL';

export function PlanningOverviewPage() {
  const {
    profileId,
    summary,
    items,
    searchParams,
    realSummary,
    realVsPlanned,
    closingProjection,
  } = usePlanningData();
  const navigate = useNavigate();

  const goToFilter = (key: ChecklistFilter) => {
    const params = preservePlanningPeriodParams(searchParams);

    if (key === 'READY_TO_CONVERT') {
      navigate(buildPlanningPath(planningRoutes.convert(profileId), params));
      return;
    }

    params.set('filter', parseTableFilterKey(key));
    navigate(buildPlanningPath(planningRoutes.items(profileId), params));
  };

  return (
    <>
      <section className="planning-section-stack">
        <PlanningSummaryCards
          summary={summary}
          realSummary={realSummary}
          closingProjection={closingProjection}
        />

        <PlanningRealityPanel
          realSummary={realSummary}
          realVsPlanned={realVsPlanned}
          closingProjection={closingProjection}
        />

        <MonthlyPlanningChecklist
          summary={summary}
          items={items}
          realSummary={realSummary}
          realVsPlanned={realVsPlanned}
          onApply={goToFilter}
        />
      </section>

      <div className="page-actions">
        <Button
          to={buildPlanningPath(
            planningRoutes.monthly(profileId),
            preservePlanningPeriodParams(searchParams),
          )}
        >
          Ir a vista mensual
        </Button>

        <Link
          className="boton-secundario"
          to={buildPlanningPath(
            planningRoutes.items(profileId),
            preservePlanningPeriodParams(searchParams),
          )}
        >
          Gestionar compromisos
        </Link>
      </div>
    </>
  );
}

export function MonthlyPlanningHomePage() {
  const { summary, realSummary, realVsPlanned, closingProjection } =
    usePlanningData();

  return (
    <section className="planning-section-stack">
      <PlanningSummaryCards
        summary={summary}
        realSummary={realSummary}
        closingProjection={closingProjection}
      />

      <PlanningRealityPanel
        realSummary={realSummary}
        realVsPlanned={realVsPlanned}
        closingProjection={closingProjection}
      />
    </section>
  );
}
