import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useState } from 'react';
import { useParams } from 'react-router-dom';
import { createBudgetMonth, createBudgetYear, getBudgetComparison, getBudgetMonth, getBudgetYear, upsertBudgetCategoryItem } from '../../api/budgetsApi';
import { listCategories } from '../../api/categoriesApi';
import { AppLayout } from '../../components/layout/AppLayout';

export function BudgetPage() {
  const { profileId = '' } = useParams(); const d = new Date(); const qc = useQueryClient();
  const [year, setYear] = useState(d.getFullYear()); const [month, setMonth] = useState(d.getMonth() + 1);
  const y = useQuery({ queryKey: ['budget-year', profileId, year], queryFn: () => getBudgetYear(profileId, year), retry: false });
  const m = useQuery({ queryKey: ['budget-month', profileId, year, month], queryFn: () => getBudgetMonth(profileId, year, month), retry: false });
  const cmp = useQuery({ queryKey: ['budget-comp', profileId, year, month], queryFn: () => getBudgetComparison(profileId, year, month), retry: false });
  const cats = useQuery({ queryKey: ['categories', profileId], queryFn: () => listCategories(profileId, true) });
  const budgetable = (cats.data ?? []).filter((c: any) => c.type !== 'INCOME');
  const mkY = useMutation({ mutationFn: () => createBudgetYear(profileId, { year }), onSuccess: () => qc.invalidateQueries() });
  const mkM = useMutation({ mutationFn: () => createBudgetMonth(profileId, year, { month }), onSuccess: () => qc.invalidateQueries() });
  const save = useMutation({ mutationFn: ({ categoryId, budgetAmount }: { categoryId: string; budgetAmount: number }) => upsertBudgetCategoryItem(m.data.id, { categoryId, budgetAmount }), onSuccess: () => qc.invalidateQueries() });
  return <AppLayout><div className='card'><h1>Presupuesto</h1><div className='form-row'><input className='input' type='number' value={year} onChange={e=>setYear(Number(e.target.value))}/><input className='input' type='number' value={month} onChange={e=>setMonth(Number(e.target.value))}/><button onClick={()=>mkY.mutate()}>Crear/obtener año</button><button onClick={()=>mkM.mutate()}>Crear/obtener mes</button></div>
  <p>Año: {y.data ? 'OK' : 'No existe'} | Mes: {m.data ? 'OK' : 'No existe'}</p>
  <table className='table'><thead><tr><th>Categoría</th><th>Tipo</th><th>Budget</th></tr></thead><tbody>{budgetable.map((c: any) => <tr key={c.id}><td>{c.name}</td><td>{c.type}</td><td><input className='input' type='number' defaultValue={m.data?.items?.find((i: any) => i.categoryId === c.id)?.budgetAmount ?? 0} onBlur={(e) => m.data && save.mutate({ categoryId: c.id, budgetAmount: Number(e.target.value) })} /></td></tr>)}</tbody></table>
  <h3>Budget vs Real</h3><table className='table'><thead><tr><th>Categoría</th><th>Tipo</th><th>Budget</th><th>Real</th><th>Diff</th><th>%</th><th>Status</th></tr></thead><tbody>{cmp.data?.items?.map((i:any)=><tr key={i.categoryId}><td>{i.categoryName}</td><td>{i.categoryType}</td><td>{i.budgetAmount}</td><td>{i.realAmount}</td><td>{i.difference}</td><td>{i.percentUsed}</td><td><span className={`badge ${i.status==='EXCEEDED'?'badge-danger':i.status==='WARNING'?'badge-warning':'badge-ok'}`}>{i.status}</span></td></tr>)}</tbody></table>
  <p>Totales: Budget {cmp.data?.totalBudget ?? 0} | Real {cmp.data?.totalReal ?? 0} | Diferencia {cmp.data?.totalDifference ?? 0}</p>
  </div></AppLayout>;
}
