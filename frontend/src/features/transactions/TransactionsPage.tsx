import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useState } from 'react';
import { Link, useParams } from 'react-router-dom';
import { listAccounts } from '../../api/accountsApi';
import { listCategories } from '../../api/categoriesApi';
import { createTransaction, deleteTransaction, listTransactions, updateTransaction } from '../../api/transactionsApi';
import { AppLayout } from '../../components/layout/AppLayout';

export function TransactionsPage() {
  const { profileId = '' } = useParams(); const qc = useQueryClient(); const d = new Date();
  const [year, setYear] = useState(d.getFullYear()); const [month, setMonth] = useState(d.getMonth() + 1);
  const [form, setForm] = useState({ accountId: '', categoryId: '', movementType: 'EXPENSE', realDate: `${d.getFullYear()}-${String(d.getMonth()+1).padStart(2,'0')}-01`, budgetDate: `${d.getFullYear()}-${String(d.getMonth()+1).padStart(2,'0')}-01`, amount: 0, currency: 'ARS', description: '', status: 'CONFIRMED' });
  const acc = useQuery({ queryKey: ['accounts', profileId], queryFn: () => listAccounts(profileId) });
  const cat = useQuery({ queryKey: ['categories', profileId], queryFn: () => listCategories(profileId, true) });
  const tx = useQuery({ queryKey: ['tx', profileId, year, month], queryFn: () => listTransactions(profileId, year, month) });
  const c = useMutation({ mutationFn: () => createTransaction({ ...form, profileId, amount: Number(form.amount), origin: 'MANUAL' }), onSuccess: () => qc.invalidateQueries() });
  const u = useMutation({ mutationFn: (t: any) => updateTransaction(t.id, t), onSuccess: () => qc.invalidateQueries() });
  const del = useMutation({ mutationFn: (id: string) => deleteTransaction(id), onSuccess: () => qc.invalidateQueries() });
  const byId = (arr: any[], id: string) => arr.find((x) => x.id === id)?.name ?? id;
  return <AppLayout><div className='card'><h1>Movimientos</h1>
  {!acc.data?.length && <p className='empty-state'>No hay cuentas. <Link to={`/profiles/${profileId}/accounts`}>Crear cuenta</Link></p>}
  {!cat.data?.length && <p className='empty-state'>No hay categorías. <Link to={`/profiles/${profileId}/categories`}>Crear categoría</Link></p>}
  <div className='form-row'><input className='input' type='number' value={year} onChange={e=>setYear(Number(e.target.value))}/><input className='input' type='number' value={month} onChange={e=>setMonth(Number(e.target.value))}/></div>
  <div className='form-row'><select className='select' value={form.accountId} onChange={e=>setForm({...form,accountId:e.target.value})}><option value=''>Cuenta</option>{acc.data?.map((a:any)=><option key={a.id} value={a.id}>{a.name}</option>)}</select><select className='select' value={form.categoryId} onChange={e=>setForm({...form,categoryId:e.target.value})}><option value=''>Categoría</option>{cat.data?.map((c:any)=><option key={c.id} value={c.id}>{c.name}</option>)}</select><input className='input' type='number' value={form.amount} onChange={e=>setForm({...form,amount:Number(e.target.value)})}/><button className='button-primary' onClick={()=>c.mutate()} disabled={c.isPending}>Guardar</button></div>
  <table className='table'><thead><tr><th>Real</th><th>Budget</th><th>Tipo</th><th>Cuenta</th><th>Categoría</th><th>Monto</th><th>Status</th><th>Desc</th><th></th></tr></thead><tbody>{tx.data?.map((t:any)=><tr key={t.id}><td>{t.realDate}</td><td>{t.budgetDate}</td><td>{t.movementType}</td><td>{byId(acc.data??[],t.accountId)}</td><td>{byId(cat.data??[],t.categoryId)}</td><td>{t.amount} {t.currency}</td><td>{t.status}</td><td>{t.description}</td><td><button onClick={()=>u.mutate({...t,status:t.status==='CONFIRMED'?'PENDING':'CONFIRMED'})}>Editar</button><button className='button-danger' onClick={()=>window.confirm('Eliminar movimiento?') && del.mutate(t.id)}>Eliminar</button></td></tr>)}</tbody></table>
  </div></AppLayout>;
}
