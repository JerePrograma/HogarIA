import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { createProfile, deleteProfile, listProfiles, updateProfile } from '../../api/profilesApi';
import { getApiErrorMessage } from '../../api/http';

export function ProfilesPage() {
  const nav = useNavigate(); const qc = useQueryClient();
  const [form, setForm] = useState({ name: '', type: 'PERSONAL', baseCurrency: 'ARS', activeYear: new Date().getFullYear() });
  const q = useQuery({ queryKey: ['profiles'], queryFn: listProfiles });
  const c = useMutation({ mutationFn: () => createProfile(form), onSuccess: () => { qc.invalidateQueries({ queryKey: ['profiles'] }); setForm({ ...form, name: '' }); } });
  const u = useMutation({ mutationFn: ({ id, payload }: { id: string; payload: Record<string, unknown> }) => updateProfile(id, payload), onSuccess: () => qc.invalidateQueries({ queryKey: ['profiles'] }) });
  const d = useMutation({ mutationFn: (id: string) => deleteProfile(id), onSuccess: () => qc.invalidateQueries({ queryKey: ['profiles'] }) });
  if (q.isLoading) return <p>Cargando perfiles...</p>; if (q.isError) return <p>{getApiErrorMessage(q.error)}</p>;
  return <div><h1>Perfiles</h1>
    <div><input value={form.name} placeholder='Nombre' onChange={(e) => setForm({ ...form, name: e.target.value })} /><select value={form.type} onChange={(e) => setForm({ ...form, type: e.target.value })}><option>PERSONAL</option><option>FAMILY</option><option>BUSINESS</option></select><input type='number' value={form.activeYear} onChange={(e) => setForm({ ...form, activeYear: Number(e.target.value) })} /><button onClick={() => c.mutate()} disabled={c.isPending}>Crear perfil</button></div>
    {!q.data?.length ? <p>No hay perfiles.</p> : <table><thead><tr><th>Nombre</th><th>Tipo</th><th>Moneda</th><th>Año</th><th>Activo</th><th>Acciones</th></tr></thead><tbody>{q.data.map((p: any) => <tr key={p.id}><td>{p.name}</td><td>{p.type}</td><td>{p.baseCurrency}</td><td>{p.activeYear}</td><td>{p.active ? 'Sí' : 'No'}</td><td><button onClick={() => { localStorage.setItem('selectedProfileId', p.id); nav(`/profiles/${p.id}/dashboard`); }}>Entrar</button><button onClick={() => u.mutate({ id: p.id, payload: { ...p, active: !p.active } })}>Toggle</button><button onClick={() => window.confirm('Desactivar perfil?') && d.mutate(p.id)}>Desactivar</button></td></tr>)}</tbody></table>}
  </div>;
}
