import { useParams } from 'react-router-dom';
import { useInvalidateMonthlyViews } from './useInvalidateMonthlyViews';
import { useMonthlyPeriod } from '../../../shared/hooks/useMonthlyPeriod';
import { useMonthlyPlanningQueries } from './useMonthlyPlanningQueries';

export function usePlanningData() {
  const { profileId = '' } = useParams();
  const { year, month, searchParams } = useMonthlyPeriod();
  const invalidatePlanningViews = useInvalidateMonthlyViews(profileId, year, month);
  const queries = useMonthlyPlanningQueries(profileId, year, month);
  return { profileId, year, month, searchParams, invalidatePlanningViews, ...queries };
}
