import { useParams } from 'react-router-dom';
import { useInvalidateMonthlyViews } from '../../../hooks/useInvalidateMonthlyViews';
import { useMonthlyPeriod } from '../../../hooks/useMonthlyPeriod';
import { useMonthlyPlanningQueries } from '../../../hooks/useMonthlyPlanningQueries';

export function usePlanningData() {
  const { profileId = '' } = useParams();
  const { year, month, searchParams } = useMonthlyPeriod();
  const invalidatePlanningViews = useInvalidateMonthlyViews(profileId, year, month);
  const queries = useMonthlyPlanningQueries(profileId, year, month);
  return { profileId, year, month, searchParams, invalidatePlanningViews, ...queries };
}
