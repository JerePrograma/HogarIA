import { useMutation, useQuery } from '@tanstack/react-query';
import { Link } from 'react-router-dom';
import { confirmPlanTransactionMatch, deletePlanTransactionMatch, getMonthlyPlanReconciliation } from '../../api/monthlyPlanReconciliationApi';
import { ErrorState } from '../../components/ui/ErrorState';
import { queryKeys } from '../../domain/queryKeys';
import type { MonthlyPlanReconciliationSummary, SuggestedPlanTransactionMatch } from '../../domain/types';
import { useInvalidateMonthlyViews } from '../../hooks/useInvalidateMonthlyViews';
import { PlanItemsReconciliationPanel } from './components/reconciliation/PlanItemsReconciliationPanel';
import { ReconciliationSummaryPanel } from './components/reconciliation/ReconciliationSummaryPanel';
import { SuggestedMatchesPanel } from './components/reconciliation/SuggestedMatchesPanel';
import { UnplannedTransactionsPanel } from './components/reconciliation/UnplannedTransactionsPanel';
import { usePlanningData } from './hooks/usePlanningData';
import { planningRoutes } from './planningRoutes';
import { buildPlanningPath } from './utils/buildPlanningPath';

export function MonthlyPlanReconciliationPage() {
  const { profileId, year, month, accounts, categories } = usePlanningData();
  const invalidateMonthly = useInvalidateMonthlyViews(profileId, year, month);
  const query = useQuery<MonthlyPlanReconciliationSummary>({
    queryKey: queryKeys.monthlyPlanReconciliation(profileId, year, month),
    queryFn: () => getMonthlyPlanReconciliation(profileId, year, month),
    enabled: Boolean(profileId),
  });
  const confirm = useMutation({
    mutationFn: (match: SuggestedPlanTransactionMatch) =>
      confirmPlanTransactionMatch(profileId, {
        monthlyPlanItemId: match.itemId,
        moneyTransactionId: match.transactionId,
        matchedAmount: match.suggestedAmount,
        matchType: 'SUGGESTED',
        confidence: match.confidence,
      }),
    onSuccess: invalidateMonthly,
  });
  const del = useMutation({ mutationFn: (matchId: string) => deletePlanTransactionMatch(profileId, matchId), onSuccess: invalidateMonthly });

  if (query.isLoading) return <p>Cargando vínculos...</p>;
  if (query.isError || !query.data) return <ErrorState title="Error de conciliación" message="No se pudo cargar la conciliación mensual." />;

  const pendingPlanItems = query.data.planItems.filter((item) => item.executionStatus !== 'MATCHED');
  const matchedPlanItems = query.data.planItems.filter((item) => item.executionStatus === 'MATCHED');

  return (
    <section className="panel">
      <div className="section-title">
        <div>
          <p className="eyebrow">Vincular reales</p>
          <h2>Conciliar planificación con movimientos existentes</h2>
          <p className="secondary-text">
            Conciliar vincula un compromiso planificado con un movimiento real que ya existe. Convertir, en cambio, crea un movimiento nuevo desde la planificación.
          </p>
        </div>
      </div>

      {confirm.isError ? <p className="mensaje-error">No se pudo confirmar el vínculo.</p> : null}
      {del.isError ? <p className="mensaje-error">No se pudo eliminar el vínculo.</p> : null}

      <ReconciliationSummaryPanel summary={query.data} />
      <UnplannedTransactionsPanel transactions={query.data.unplannedTransactions} accounts={accounts} categories={categories} />
      <SuggestedMatchesPanel matches={query.data.suggestedMatches} accounts={accounts} categories={categories} isConfirming={confirm.isPending} onConfirm={(match) => confirm.mutate(match)} />
      <PlanItemsReconciliationPanel title="Planificados sin movimiento" items={pendingPlanItems} accounts={accounts} categories={categories} isDeleting={del.isPending} onDelete={(id) => del.mutate(id)} />
      <PlanItemsReconciliationPanel title="Conciliados" items={matchedPlanItems} accounts={accounts} categories={categories} isDeleting={del.isPending} onDelete={(id) => del.mutate(id)} />

      <Link to={buildPlanningPath(planningRoutes.monthly(profileId), new URLSearchParams([['year', String(year)], ['month', String(month)]]))}>
        Volver al mensual
      </Link>
    </section>
  );
}
