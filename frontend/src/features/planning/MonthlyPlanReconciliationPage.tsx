import { useMutation, useQuery } from '@tanstack/react-query';
import { Link, useParams, useSearchParams } from 'react-router-dom';
import { confirmPlanTransactionMatch, deletePlanTransactionMatch, getMonthlyPlanReconciliation } from '../../api/monthlyPlanReconciliationApi';
import { formatMoney } from '../../domain/formatters';
import { queryKeys } from '../../domain/queryKeys';
import type { MonthlyPlanReconciliationSummary, SuggestedPlanTransactionMatch, TransactionMatch } from '../../domain/types';
import { useInvalidateMonthlyViews } from '../../hooks/useInvalidateMonthlyViews';
import { planningRoutes } from './planningRoutes';

export function MonthlyPlanReconciliationPage(){
 const { profileId='' }=useParams(); const [sp,setSp]=useSearchParams(); const year=Number(sp.get('year')); const month=Number(sp.get('month')); const tab=sp.get('tab')??'overview';
 const invalidateMonthly=useInvalidateMonthlyViews(profileId,year,month);
 const q=useQuery<MonthlyPlanReconciliationSummary>({queryKey: queryKeys.monthlyPlanReconciliation(profileId,year,month), queryFn:()=>getMonthlyPlanReconciliation(profileId,year,month), enabled:Boolean(profileId&&year&&month)});
 const refresh=()=>{invalidateMonthly();};
 const confirm=useMutation({mutationFn:(m:SuggestedPlanTransactionMatch)=>confirmPlanTransactionMatch(profileId,{monthlyPlanItemId:m.itemId,moneyTransactionId:m.transactionId,matchedAmount:m.suggestedAmount,matchType:'SUGGESTED'}),onSuccess:refresh});
 const del=useMutation({mutationFn:(matchId:string)=>deletePlanTransactionMatch(profileId,matchId),onSuccess:refresh});
 if(q.isLoading) return <p>Cargando conciliación...</p>; if(q.isError||!q.data) return <p>Error de conciliación.</p>; const s=q.data;
 const setTab=(next:string)=>{const p=new URLSearchParams(sp);p.set('tab',next);setSp(p)};
 return <section className='panel'><h2>Conciliación mensual</h2><p>Sin planificar: {formatMoney(s.unplannedTransactionsTotal)}</p>
 <div className='planning-filter-row'>{['overview','unplanned','suggestions','items'].map(t=><button key={t} onClick={()=>setTab(t)}>{t}</button>)}</div>
 {tab==='overview' && <div><p>Movimientos sin plan: {s.unplannedCount}</p><p>Sugerencias: {s.suggestedCount}</p></div>}
 {tab==='unplanned' && <ul>{s.unplannedTransactions.map(t=><li key={t.transactionId}><strong>{t.description || 'Sin descripción'}</strong> · {t.budgetDate} · {formatMoney(t.amount)} · Estado: {t.status}</li>)}</ul>}
 {tab==='suggestions' && <ul>{s.suggestedMatches.map(m=><li key={`${m.itemId}-${m.transactionId}`}><strong>{m.reasons.join(', ') || 'Sugerencia automática'}</strong> · {formatMoney(m.suggestedAmount)} <button onClick={()=>confirm.mutate(m)}>Aceptar</button></li>)}</ul>}
 {tab==='items' && <ul>{s.planItems.map(i=><li key={i.itemId}><strong>{i.title}</strong> · {i.executionStatus} · {formatMoney(i.matchedAmount)}/{formatMoney(i.plannedAmount)}<TransactionMatchList matches={i.matches} onDelete={(id)=>del.mutate(id)} /></li>)}</ul>}
 <Link to={planningRoutes.monthly(profileId)}>Volver</Link></section>;
}

function TransactionMatchList({matches,onDelete}:{matches:TransactionMatch[];onDelete:(id:string)=>void}){return <ul>{matches.map(m=><li key={m.id}>{m.matchType} · {formatMoney(m.matchedAmount)} <button onClick={()=>onDelete(m.id)}>Eliminar</button></li>)}</ul>}
