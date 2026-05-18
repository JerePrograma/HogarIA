import { useQuery } from '@tanstack/react-query';
import { Link, useParams, useSearchParams } from 'react-router-dom';
import { getMonthlyPlanReconciliation } from '../../api/monthlyPlanReconciliationApi';
import { queryKeys } from '../../domain/queryKeys';
import type { MonthlyPlanReconciliationSummary, PlanItemReconciliation, SuggestedPlanTransactionMatch, UnplannedTransaction } from '../../domain/types';
import { planningRoutes } from './planningRoutes';

export function MonthlyPlanReconciliationPage(){
  const { profileId='' }=useParams(); const [sp]=useSearchParams(); const year=Number(sp.get('year')); const month=Number(sp.get('month')); const tab=sp.get('tab')??'overview';
  const q=useQuery<MonthlyPlanReconciliationSummary>({queryKey: queryKeys.monthlyPlanReconciliation(profileId,year,month), queryFn:()=>getMonthlyPlanReconciliation(profileId,year,month), enabled:Boolean(profileId&&year&&month)});
  if(q.isLoading) return <p>Cargando conciliación...</p>; if(q.isError||!q.data) return <p>Error de conciliación.</p>;
  const s=q.data;
  return <section className='panel'><h2>Conciliación mensual</h2><p>No planificado: {s.unplannedTransactionsTotal}</p><p>Sin planificación: {s.unplannedCount} · Sugerencias: {s.suggestedCount}</p>
  <div className='planning-filter-row'><Link to={`?year=${year}&month=${month}&tab=unplanned`}>Movimientos sin planificación</Link> <Link to={`?year=${year}&month=${month}&tab=suggestions`}>Sugerencias</Link> <Link to={`?year=${year}&month=${month}&tab=items`}>Ítems</Link></div>
  {tab==='unplanned' ? <ul>{s.unplannedTransactions.map((t:UnplannedTransaction)=><li key={t.transactionId}>{t.budgetDate} - {t.description} - {t.amount} - {t.status}</li>)}</ul>:null}
  {tab==='suggestions' ? <ul>{s.suggestedMatches.map((m:SuggestedPlanTransactionMatch)=><li key={`${m.itemId}-${m.transactionId}`}>{m.itemId} ↔ {m.transactionId} · {m.suggestedAmount}</li>)}</ul>:null}
  {tab==='items' ? <ul>{s.planItems.map((i:PlanItemReconciliation)=><li key={i.itemId}>{i.title} - {i.executionStatus} - {i.matchedAmount}/{i.plannedAmount}</li>)}</ul>:null}
  <Link to={planningRoutes.monthly(profileId)}>Volver</Link></section>;
}
