import { useSearchParams } from 'react-router-dom';
import { useMonthlyPlanItemActions } from '../../../../hooks/useMonthlyPlanItemActions';
import { useStructuredPlanItemDraft } from '../../../../hooks/useStructuredPlanItemDraft';
import { MonthlyPlanItemsTable } from '../../components/MonthlyPlanItemsTable';
import { usePlanningData } from '../../hooks/usePlanningData';
import { parseSortKey, parseStatusFilterKey, parseTableFilterKey, parseTypeFilterKey } from '../../planningSearchParams';

export function MonthlyPlanItemsPage(){ const { profileId,year,month,items,accounts,categories,invalidatePlanningViews }=usePlanningData(); const draft=useStructuredPlanItemDraft(year,month); const actions=useMonthlyPlanItemActions({profileId,year,month,form:draft.form,resetFormAfterCreate:draft.resetAfterCreate,invalidatePlanningViews}); const [params]=useSearchParams(); return <MonthlyPlanItemsTable profileId={profileId} items={items} accounts={accounts} categories={categories} onConvert={actions.convert} onCancel={actions.cancel} onDelete={actions.remove} onUpdate={actions.update} onMarkPaid={actions.markPaid} onMarkCollected={actions.markCollected} pendingActionId={actions.pendingActionId} actionError={actions.actionErrorMessage} filter={parseTableFilterKey(params.get('filter'))} statusFilter={parseStatusFilterKey(params.get('status'))} typeFilter={parseTypeFilterKey(params.get('type'))} search={params.get('search') ?? ''} sortBy={parseSortKey(params.get('sort'))}/>; }
