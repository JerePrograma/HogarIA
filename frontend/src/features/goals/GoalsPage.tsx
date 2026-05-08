import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useState } from 'react';
import { useParams } from 'react-router-dom';
import { createEmergencyFund, createGoal, deleteGoal, listGoals } from '../../api/goalsApi';
import { AppLayout } from '../../components/layout/AppLayout';

export function GoalsPage(){
  const {profileId=''}=useParams(); const qc=useQueryClient();
  const [name,setName]=useState(''); const [targetAmount,setTargetAmount]=useState(0); const [goalType,setGoalType]=useState('OTHER');
  const q=useQuery({queryKey:['goals',profileId],queryFn:()=>listGoals(profileId)});
  const create=useMutation({mutationFn:()=>createGoal(profileId,{name,goalType,targetAmount,currentAmount:0,priority:3}),onSuccess:()=>{setName('');setTargetAmount(0);qc.invalidateQueries({queryKey:['goals',profileId]});}});
  const emergency=useMutation({mutationFn:(m:number)=>createEmergencyFund(profileId,m),onSuccess:()=>qc.invalidateQueries({queryKey:['goals',profileId]})});
  const del=useMutation({mutationFn:(id:string)=>deleteGoal(profileId,id),onSuccess:()=>qc.invalidateQueries({queryKey:['goals',profileId]})});
  return <AppLayout><div className='card'><h1>Objetivos financieros</h1><div className='form-row'><input className='input' placeholder='Nombre' value={name} onChange={e=>setName(e.target.value)}/><select className='select' value={goalType} onChange={e=>setGoalType(e.target.value)}><option>OTHER</option><option>EMERGENCY_FUND</option><option>TRAVEL</option><option>HOME</option><option>INVESTMENT</option></select><input className='input' type='number' value={targetAmount} onChange={e=>setTargetAmount(Number(e.target.value))}/><button className='button-primary' onClick={()=>create.mutate()}>Crear objetivo</button></div>
  <div className='form-row'><button onClick={()=>emergency.mutate(3)}>Fondo emergencia 3 meses</button><button onClick={()=>emergency.mutate(4)}>4 meses</button><button onClick={()=>emergency.mutate(5)}>5 meses</button><button onClick={()=>emergency.mutate(6)}>6 meses</button></div>
  <div className='grid'>{q.data?.map((g:any)=><div key={g.id} className='card'><b>{g.name}</b><p>{g.goalType} · {g.status}</p><p>Target: {g.targetAmount} | Actual: {g.currentAmount}</p><p>Progreso: {g.progress}%</p><button className='button-danger' onClick={()=>del.mutate(g.id)}>Eliminar</button></div>)}</div></div></AppLayout>
}
