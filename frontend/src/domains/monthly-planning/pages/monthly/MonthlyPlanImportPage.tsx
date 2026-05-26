import { Navigate, useParams } from 'react-router-dom';
import { useMonthlyPeriod } from '../../../../shared/hooks/useMonthlyPeriod';
import { planningRoutes } from '../../planningRoutes';
import { buildPlanningPath } from '../../utils/buildPlanningPath';

export function MonthlyPlanImportPage() {
  const { profileId = '' } = useParams();
  const { searchParams } = useMonthlyPeriod();

  return (
    <Navigate
      to={buildPlanningPath(planningRoutes.quickText(profileId), searchParams)}
      replace
    />
  );
}
