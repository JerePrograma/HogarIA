import { useNavigate } from 'react-router-dom';
import { useMonthlyPlanItemActions } from '../../hooks/useMonthlyPlanItemActions';
import { useStructuredPlanItemDraft } from '../../hooks/useStructuredPlanItemDraft';
import { StructuredPlanItemForm } from '../../components/StructuredPlanItemForm';
import { usePlanningData } from '../../hooks/usePlanningData';
import { planningRoutes } from '../../planningRoutes';
import { preservePlanningPeriodParams } from '../../planningSearchParams';
import { buildPlanningPath } from '../../utils/buildPlanningPath';

export function MonthlyPlanItemCreatePage() {
  const {
    profileId,
    year,
    month,
    accounts,
    categories,
    invalidatePlanningViews,
    searchParams,
  } = usePlanningData();
  const draft = useStructuredPlanItemDraft(year, month);
  const navigate = useNavigate();
  const actions = useMonthlyPlanItemActions({
    profileId,
    year,
    month,
    form: draft.form,
    resetFormAfterCreate: draft.resetAfterCreate,
    invalidatePlanningViews,
  });

  const handleCreate = async () => {
    await actions.createAsync();
    navigate(
      buildPlanningPath(
        planningRoutes.items(profileId),
        preservePlanningPeriodParams(searchParams),
      ),
    );
  };

  return (
    <StructuredPlanItemForm
      form={draft.form}
      setForm={draft.setForm}
      accounts={accounts}
      categories={categories}
      onCreate={handleCreate}
      isCreating={actions.isCreating}
      error={actions.createErrorMessage}
    />
  );
}
