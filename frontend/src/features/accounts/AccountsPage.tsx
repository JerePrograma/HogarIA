import { useState } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useParams } from 'react-router-dom';
import { createAccount, deleteAccount, listAccounts, updateAccount } from '../../api/accountsApi';
import { AppLayout } from '../../components/layout/AppLayout';

export function AccountsPage() {
  const { profileId = '' } = useParams(); const qc = useQueryClient();
  const [name, setName] = useState('');
  const q = useQuery({ queryKey: ['accounts', profileId], queryFn: () => listAccounts(profileId) });
  const c = useMutation({ mutationFn: () => createAccount(profileId, { name, accountType: 'CASH', currency: 'ARS' }), onSuccess: () => { qc.invalidateQueries({ queryKey: ['accounts', profileId] }); setName(''); } });
  const u = useMutation({ mutationFn: (a: Record<string, unknown>) => updateAccount(String(a.id), { ...a, active: !a.active }), onSuccess: () => qc.invalidateQueries({ queryKey: ['accounts', profileId] }) });
  const d = useMutation({ mutationFn: (id: string) => deleteAccount(id), onSuccess: () => qc.invalidateQueries({ queryKey: ['accounts', profileId] }) });
  return <AppLayout><div className='card'><h1>Cuentas</h1><div className='form-row'><input className='input' value={name} onChange={(e)=>setName(e.target.value)} placeholder='Nombre cuenta' /><button className='button-primary' onClick={() => c.mutate()}>Crear</button></div>
  {!q.data?.length ? <p className='empty-state'>Sin cuentas.</p> : <table className='table'><thead><tr><th>Nombre</th><th>Tipo</th><th>Moneda</th><th>Límite</th><th>Cierre/Venc.</th><th>Activa</th><th></th></tr></thead><tbody>{q.data.map((a: any) => <tr key={a.id}><td>{a.name}</td><td>{a.accountType}</td><td>{a.currency}</td><td>{a.creditLimit ?? '-'}</td><td>{a.statementCloseDay ?? '-'} / {a.dueDay ?? '-'}</td><td>{a.active ? 'Sí' : 'No'}</td><td><button onClick={()=>u.mutate(a)}>Editar/Toggle</button><button className='button-danger' onClick={()=>window.confirm('Desactivar cuenta?') && d.mutate(a.id)}>Desactivar</button></td></tr>)}</tbody></table>}</div></AppLayout>;
}
