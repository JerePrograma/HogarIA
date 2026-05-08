import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { createDevUser, listDevUsers } from '../../api/devUsersApi';
import { getApiErrorMessage } from '../../api/http';

export function DevUserPage() {
  const nav = useNavigate(); const qc = useQueryClient();
  const [form, setForm] = useState({ fullName: '', email: '', password: '' });
  const q = useQuery({ queryKey: ['dev-users'], queryFn: listDevUsers });
  const c = useMutation({ mutationFn: () => createDevUser(form), onSuccess: (u: { id: string }) => { qc.invalidateQueries({ queryKey: ['dev-users'] }); localStorage.setItem('devUserId', u.id); nav('/profiles'); } });
  return <div className='card'><h1>Dev users</h1>
    {q.isError && <div className='error-box'>{getApiErrorMessage(q.error)}</div>}
    <div className='form-row'>
      <input className='input' placeholder='Nombre completo' value={form.fullName} onChange={(e) => setForm({ ...form, fullName: e.target.value })} />
      <input className='input' placeholder='Email' value={form.email} onChange={(e) => setForm({ ...form, email: e.target.value })} />
      <input className='input' type='password' placeholder='Password' value={form.password} onChange={(e) => setForm({ ...form, password: e.target.value })} />
      <button className='button-primary' onClick={() => c.mutate()} disabled={c.isPending}>Crear y seleccionar</button>
    </div>
    {q.isLoading ? <p>Cargando...</p> : !q.data?.length ? <p className='empty-state'>No hay usuarios creados.</p> : <table className='table'><thead><tr><th>Nombre</th><th>Email</th><th></th></tr></thead><tbody>{q.data.map((u: { id: string; fullName: string; email: string }) => <tr key={u.id}><td>{u.fullName}</td><td>{u.email}</td><td><button onClick={() => { localStorage.setItem('devUserId', u.id); nav('/profiles'); }}>Seleccionar</button></td></tr>)}</tbody></table>}
  </div>;
}
