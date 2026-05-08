import { useQuery } from '@tanstack/react-query';
import { useState } from 'react';
import { Link, useParams } from 'react-router-dom';
import { Bar, BarChart, CartesianGrid, Pie, PieChart, ResponsiveContainer, Tooltip, XAxis, YAxis, Cell } from 'recharts';
import { getMonthlyDashboard } from '../../api/dashboardApi';
import { AppLayout } from '../../components/layout/AppLayout';

export function DashboardPage() {
  const { profileId = '' } = useParams(); const d = new Date();
  const [year, setYear] = useState(d.getFullYear()); const [month, setMonth] = useState(d.getMonth()+1);
  const q = useQuery({ queryKey: ['dash', profileId, year, month], queryFn: () => getMonthlyDashboard(profileId, year, month) });
  const s = q.data;
  const pie=[{name:'Fijos',value:Number(s?.fixedExpenses??0)},{name:'Variables',value:Number(s?.variableExpenses??0)},{name:'Ahorro',value:Number(s?.monthlyBalance?.savings??0)}];
  const colors=['#14b8a6','#f59e0b','#22c55e'];
  return <AppLayout><div className='card'><h1>Dashboard mensual</h1><div className='form-row'><input className='input' type='number' value={year} onChange={e=>setYear(Number(e.target.value))}/><input className='input' type='number' value={month} onChange={e=>setMonth(Number(e.target.value))}/></div>
  <div className='grid'>{[['Ingresos',s?.monthlyBalance?.totalIncome],['Gastos fijos',s?.fixedExpenses],['Gastos variables',s?.variableExpenses],['Ahorro',s?.monthlyBalance?.savings],['Balance',s?.monthlyBalance?.balance],['Salud',s?.financialHealth]].map(([k,v])=><div className='card' key={String(k)}><b>{k}</b><div style={{fontSize:24,fontWeight:700}}>{String(v ?? '-')}</div></div>)}</div>
  <div className='grid'><div className='card' style={{height:280}}><h3>Distribución</h3><ResponsiveContainer><PieChart><Pie data={pie} dataKey='value' nameKey='name' outerRadius={90}>{pie.map((_,i)=><Cell key={i} fill={colors[i%colors.length]} />)}</Pie><Tooltip/></PieChart></ResponsiveContainer></div>
  <div className='card' style={{height:280}}><h3>Budget vs Real</h3><ResponsiveContainer><BarChart data={(s?.categoryBreakdown??[]).map((x:any)=>({name:x.categoryName,real:Number(x.totalAmount)}))}><CartesianGrid strokeDasharray='3 3'/><XAxis dataKey='name' hide/><YAxis/><Tooltip/><Bar dataKey='real' fill='#0f766e'/></BarChart></ResponsiveContainer></div></div>
  <div className='card'><h3>Regla 50/30/20</h3><p>Fixed: {s?.fiftyThirtyTwenty?.fixedPercent ?? 0}% | Variable: {s?.fiftyThirtyTwenty?.variablePercent ?? 0}% | Saving: {s?.fiftyThirtyTwenty?.savingPercent ?? 0}%</p></div>
  <div className='card'><h3>Desglose por categoría</h3><table className='table'><thead><tr><th>Categoría</th><th>Tipo</th><th>Total</th><th>% ingreso</th><th>Movs</th></tr></thead><tbody>{s?.categoryBreakdown?.map((x:any)=><tr key={x.categoryId}><td>{x.categoryName}</td><td>{x.categoryType}</td><td>{x.totalAmount}</td><td>{x.percentOfIncome}</td><td>{x.movementCount}</td></tr>)}</tbody></table></div>
  <p><Link to={`/profiles/${profileId}/transactions`}>Movimientos</Link> · <Link to={`/profiles/${profileId}/budgets`}>Presupuesto</Link> · <Link to={`/profiles/${profileId}/goals`}>Objetivos</Link> · <Link to={`/profiles/${profileId}/habits`}>Hábitos</Link></p></div></AppLayout>;
}
