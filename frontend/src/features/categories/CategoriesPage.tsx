import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useState } from 'react';
import { useParams } from 'react-router-dom';
import { createCategory, deleteCategory, listCategories, updateCategory } from '../../api/categoriesApi';
import { AppLayout } from '../../components/layout/AppLayout';

export function CategoriesPage() {
  const { profileId = '' } = useParams(); const qc = useQueryClient();
  const [includeGlobal, setIncludeGlobal] = useState(true); const [name, setName] = useState('');
  const q = useQuery({ queryKey: ['categories', profileId, includeGlobal], queryFn: () => listCategories(profileId, includeGlobal) });
  const c = useMutation({ mutationFn: () => createCategory(profileId, { name, type: 'VARIABLE_EXPENSE', scope: 'PERSONAL' }), onSuccess: () => { qc.invalidateQueries({ queryKey: ['categories', profileId] }); setName(''); } });
  const u = useMutation({ mutationFn: (x: any) => updateCategory(x.id, { ...x, active: !x.active }), onSuccess: () => qc.invalidateQueries({ queryKey: ['categories', profileId] }) });
  const d = useMutation({ mutationFn: (id: string) => deleteCategory(id), onSuccess: () => qc.invalidateQueries({ queryKey: ['categories', profileId] }) });
  return <AppLayout><div className='card'><h1>Categorías</h1><label><input type='checkbox' checked={includeGlobal} onChange={(e) => setIncludeGlobal(e.target.checked)} /> incluir globales</label><div className='form-row'><input className='input' value={name} onChange={(e)=>setName(e.target.value)} placeholder='Nombre categoría' /><button className='button-primary' onClick={()=>c.mutate()}>Crear</button></div>
  {!q.data?.length ? <p className='empty-state'>Sin categorías.</p> : <table className='table'><thead><tr><th>Nombre</th><th>Tipo</th><th>Scope</th><th>Activa</th><th></th></tr></thead><tbody>{q.data.map((c: any) => <tr key={c.id}><td>{c.name}</td><td>{c.type}</td><td>{c.scope}</td><td>{c.active?'Sí':'No'}</td><td>{c.scope==='GLOBAL' ? '-' : <><button onClick={()=>u.mutate(c)}>Editar/Toggle</button><button className='button-danger' onClick={()=>window.confirm('Desactivar categoría?') && d.mutate(c.id)}>Desactivar</button></>}</td></tr>)}</tbody></table>}</div></AppLayout>;
}
