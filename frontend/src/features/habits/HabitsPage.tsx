import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useState } from 'react';
import { useParams } from 'react-router-dom';
import { createHabit, listHabits, upsertHabitCheckin } from '../../api/habitsApi';
import { AppLayout } from '../../components/layout/AppLayout';

export function HabitsPage(){
  const {profileId=''}=useParams(); const qc=useQueryClient(); const today=new Date().toISOString().slice(0,10);
  const [name,setName]=useState(''); const [frequency,setFrequency]=useState('DAILY');
  const q=useQuery({queryKey:['habits',profileId],queryFn:()=>listHabits(profileId)});
  const create=useMutation({mutationFn:()=>createHabit(profileId,{name,frequency}),onSuccess:()=>{setName('');qc.invalidateQueries({queryKey:['habits',profileId]});}});
  const check=useMutation({mutationFn:({habitId,done}:{habitId:string,done:boolean})=>upsertHabitCheckin(profileId,habitId,today,done)});
  return <AppLayout><div className='card'><h1>Hábitos financieros</h1><div className='form-row'><input className='input' placeholder='Nuevo hábito' value={name} onChange={e=>setName(e.target.value)}/><select className='select' value={frequency} onChange={e=>setFrequency(e.target.value)}><option>DAILY</option><option>WEEKLY</option><option>MONTHLY</option></select><button className='button-primary' onClick={()=>create.mutate()}>Crear</button></div>
  <table className='table'><thead><tr><th>Hábito</th><th>Frecuencia</th><th>Estado</th><th>Hoy</th></tr></thead><tbody>{q.data?.map((h:any)=><tr key={h.id}><td>{h.name}</td><td>{h.frequency}</td><td>{h.active?'Activo':'Inactivo'}</td><td><button onClick={()=>check.mutate({habitId:h.id,done:true})}>✔</button></td></tr>)}</tbody></table></div></AppLayout>
}
