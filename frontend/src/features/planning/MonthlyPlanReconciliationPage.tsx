import { useMutation, useQuery } from '@tanstack/react-query';
import { Link, useParams } from 'react-router-dom';
import { confirmPlanTransactionMatch, deletePlanTransactionMatch, getMonthlyPlanReconciliation } from '../../api/monthlyPlanReconciliationApi';
import { ErrorState } from '../../components/ui/ErrorState';
import { queryKeys } from '../../domain/queryKeys';
import type { MonthlyPlanReconciliationSummary, SuggestedPlanTransactionMatch } from '../../domain/types';
import { useMonthlyPeriod } from '../../hooks/useMonthlyPeriod';
import { useInvalidateMonthlyViews } from '../../hooks/useInvalidateMonthlyViews';
import { PlanItemsReconciliationPanel } from './components/reconciliation/PlanItemsReconciliationPanel';
import { ReconciliationSummaryPanel } from './components/reconciliation/ReconciliationSummaryPanel';
import { ReconciliationTabs } from './components/reconciliation/ReconciliationTabs';
import { SuggestedMatchesPanel } from './components/reconciliation/SuggestedMatchesPanel';
import type { ReconciliationTabKey } from './components/reconciliation/types';
import { UnplannedTransactionsPanel } from './components/reconciliation/UnplannedTransactionsPanel';
import { planningRoutes } from './planningRoutes';
import { buildPlanningPath } from './utils/buildPlanningPath';

export function MonthlyPlanReconciliationPage() {
  const { profileId = '' } = useParams();
  const { year, month, searchParams, setSearchParams } = useMonthlyPeriod();
  const tab = (searchParams.get('tab') ?? 'overview') as ReconciliationTabKey;
  const invalidateMonthly = useInvalidateMonthlyViews(profileId, year, month);
  const query = useQuery<MonthlyPlanReconciliationSummary>({ queryKey: queryKeys.monthlyPlanReconciliation(profileId, year, month), queryFn: () => getMonthlyPlanReconciliation(profileId, year, month), enabled: Boolean(profileId) });
  const confirm = useMutation({ mutationFn: (m: SuggestedPlanTransactionMatch) => confirmPlanTransactionMatch(profileId, { monthlyPlanItemId: m.itemId, moneyTransactionId: m.transactionId, matchedAmount: m.suggestedAmount, matchType: 'SUGGESTED' }), onSuccess: invalidateMonthly });
  const del = useMutation({ mutationFn: (matchId: string) => deletePlanTransactionMatch(profileId, matchId), onSuccess: invalidateMonthly });

  const setTab = (next: ReconciliationTabKey) => {
    const p = new URLSearchParams(searchParams);
    p.set('tab', next);
    p.set('year', String(year));
    p.set('month', String(month));
    setSearchParams(p);
  };

  const onDeleteMatch = (id: string) => {
    if (!window.confirm('¿Querés eliminar esta conciliación?')) return;
    del.mutate(id);
  };

  if (query.isLoading) return <p>Cargando conciliación...</p>;
  if (query.isError || !query.data) return <ErrorState title='Error de conciliación' message='No se pudo cargar la conciliación mensual.' />;

  return <section className='panel'><h2>Conciliación mensual</h2><ReconciliationTabs tab={tab} onChange={setTab} />{confirm.isError ? <p role='alert'>No se pudo confirmar la sugerencia.</p> : null}{del.isError ? <p role='alert'>No se pudo eliminar la conciliación.</p> : null}
    {tab === 'overview' ? <ReconciliationSummaryPanel summary={query.data} /> : null}
    {tab === 'unplanned' ? <UnplannedTransactionsPanel transactions={query.data.unplannedTransactions} /> : null}
    {tab === 'suggestions' ? <SuggestedMatchesPanel matches={query.data.suggestedMatches} isConfirming={confirm.isPending} onConfirm={(m) => confirm.mutate(m)} /> : null}
    {tab === 'items' ? <PlanItemsReconciliationPanel items={query.data.planItems} isDeleting={del.isPending} onDelete={onDeleteMatch} /> : null}
    <Link to={buildPlanningPath(planningRoutes.monthly(profileId), new URLSearchParams([['year', String(year)], ['month', String(month)]]))}>Volver</Link></section>;
}
