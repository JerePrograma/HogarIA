import { EmptyState } from '../../../../components/ui/EmptyState';
import { ErrorState } from '../../../../components/ui/ErrorState';
import { useMonthlyPlanItemActions } from '../../../../hooks/useMonthlyPlanItemActions';
import { useStructuredPlanItemDraft } from '../../../../hooks/useStructuredPlanItemDraft';
import { useNavigate, useParams, useSearchParams } from 'react-router-dom';
import { MonthlyPlanItemsTable } from '../../components/MonthlyPlanItemsTable';
import { usePlanningData } from '../../hooks/usePlanningData';
import { preservePlanningPeriodParams } from '../../planningSearchParams';
import { planningRoutes } from '../../planningRoutes';
import { buildPlanningPath } from '../../utils/buildPlanningPath';

export function MonthlyPlanItemEditPage(){ const { profileId, items, accounts, categories, searchParams, year, month, invalidatePlanningViews } = usePlanningData(); const { itemId = '' } = useParams(); const nav = useNavigate(); const [params] = useSearchParams(); const item = items.find((current) => current.id === itemId); const draft=useStructuredPlanItemDraft(year,month); const actions=useMonthlyPlanItemActions({profileId,year,month,form:draft.form,resetFormAfterCreate:draft.resetAfterCreate,invalidatePlanningViews}); if (!itemId) return <ErrorState title='Parámetros inválidos' message='Falta itemId en la URL.'/>
if (!item) return <EmptyState title='Ítem no encontrado' message='El ítem ya no existe o no pertenece al período activo.'/>
return <MonthlyPlanItemsTable profileId={profileId} items={items} accounts={accounts} categories={categories} onConvert={actions.convert} onCancel={actions.cancel} onDelete={actions.remove} onUpdate={async(id,payload)=>{ await actions.update(id,payload); nav(buildPlanningPath(planningRoutes.items(profileId), preservePlanningPeriodParams(searchParams))); }} onMarkPaid={actions.markPaid} onMarkCollected={actions.markCollected} pendingActionId={actions.pendingActionId} actionError={actions.actionErrorMessage} focusedItemId={item.id} focusedMode={params.get('mode') ?? 'FULL'} filter='ALL' statusFilter='ALL' typeFilter='ALL' search='' sortBy='DATE'/>; }
