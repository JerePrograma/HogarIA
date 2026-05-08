import { useQuery } from '@tanstack/react-query';
import { useState } from 'react';
import { Link, useParams } from 'react-router-dom';
import { getMonthlyDashboard } from '../../api/dashboardApi';
import { AppLayout } from '../../components/layout/AppLayout';

export function DashboardPage() {
  const { profileId = '' } = useParams(); const d = new Date();
  const [year, setYear] = useState(d.getFullYear()); const [month, setMonth] = useState(d.getMonth()+1);
  const q = useQuery({ queryKey: ['dash', profileId, year, month], queryFn: () => getMonthlyDashboard(profileId, year, month) });
  const s = q.data;
  return <AppLayout><div className='card'><h1>Dashboard mensual</h1><div className='form-row'><input className='input' type='number' value={year} onChange={e=>setYear(Number(e.target.value))}/><input className='input' type='number' value={month} onChange={e=>setMonth(Number(e.target.value))}/></div>
  <div className='grid'>{[['Ingresos',s?.monthlyBalance?.totalIncome],['Gastos fijos',s?.fixedExpenses],['Gastos variables',s?.variableExpenses],['Ahorro',s?.monthlyBalance?.savings],['Balance',s?.monthlyBalance?.balance],['Salud',s?.financialHealth]].map(([k,v])=><div className='card' key={String(k)}><b>{k}</b><div>{String(v ?? '-')}</div></div>)}</div>
  <div className='card'><h3>50/30/20</h3><p>Fixed: {s?.fiftyThirtyTwenty?.fixedPercent ?? 0}% | Variable: {s?.fiftyThirtyTwenty?.variablePercent ?? 0}% | Saving: {s?.fiftyThirtyTwenty?.savingPercent ?? 0}%</p></div>
  <div className='card'><h3>Budget summary</h3>{!s?.budgetSummary ? <p className='empty-state'>Sin presupuesto</p> : <p>Budget {s.budgetSummary.totalBudget} | Real {s.budgetSummary.totalReal} | Diff {s.budgetSummary.totalDifference} | Exceeded {s.budgetSummary.exceededCount}</p>}</div>
  <div className='card'><h3>Desglose por categoría</h3><table className='table'><thead><tr><th>Categoría</th><th>Tipo</th><th>Total</th><th>% ingreso</th><th>Movs</th></tr></thead><tbody>{s?.categoryBreakdown?.map((x:any)=><tr key={x.categoryId}><td>{x.categoryName}</td><td>{x.categoryType}</td><td>{x.totalAmount}</td><td>{x.percentOfIncome}</td><td>{x.movementCount}</td></tr>)}</tbody></table></div>
  <p><Link to={`/profiles/${profileId}/transactions`}>Agregar movimiento</Link> · <Link to={`/profiles/${profileId}/budgets`}>Ir a presupuesto</Link></p></div></AppLayout>;
}
