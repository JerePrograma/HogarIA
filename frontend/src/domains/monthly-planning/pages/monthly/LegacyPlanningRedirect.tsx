import { Navigate, useParams, useSearchParams } from 'react-router-dom';
import { planningRoutes } from '../../planningRoutes';
import { buildPlanningPath } from '../../utils/buildPlanningPath';

export function LegacyPlanningRedirect() {
  const { profileId = '' } = useParams();
  const [searchParams] = useSearchParams();

  return (
    <Navigate
      to={buildPlanningPath(planningRoutes.root(profileId), searchParams)}
      replace
    />
  );
}
