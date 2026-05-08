import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useState } from 'react';
import { Line, LineChart, ResponsiveContainer, Tooltip, XAxis, YAxis } from 'recharts';
import { createInflation, getAccumulatedInflation, listInflation } from '../../api/inflationApi';
import { AppLayout } from '../../components/layout/AppLayout';

export function InflationPage(){
  const d=new Date(); const [year,setYear]=useState(d.getFullYear()); const [month,setMonth]=useState(d.getMonth()+1); const [rate,setRate]=useState(0.03); const qc=useQueryClient();
  const q=useQuery({queryKey:['inflation',year],queryFn:()=>listInflation(year)});
  const acc=useQuery({queryKey:['inflation-acc',year],queryFn:()=>getAccumulatedInflation(year,1,year,12)});
  const create=useMutation({mutationFn:()=>createInflation({year,month,rate,source:'MANUAL',observed:true}),onSuccess:()=>qc.invalidateQueries({queryKey:['inflation',year]})});
  return <AppLayout><div className='card'><h1>Inflación y proyecciones</h1><div className='form-row'><input className='input' type='number' value={year} onChange={e=>setYear(Number(e.target.value))}/><input className='input' type='number' value={month} min={1} max={12} onChange={e=>setMonth(Number(e.target.value))}/><input className='input' type='number' step='0.0001' value={rate} onChange={e=>setRate(Number(e.target.value))}/><button className='button-primary' onClick={()=>create.mutate()}>Guardar índice</button></div>
  <p>Acumulada anual: {Number(acc.data?.accumulatedRate ?? 0).toFixed(4)}</p>
  <div style={{height:260}}><ResponsiveContainer><LineChart data={(q.data??[]).map((x:any)=>({month:x.month,rate:x.rate*100}))}><XAxis dataKey='month'/><YAxis/><Tooltip/><Line type='monotone' dataKey='rate' stroke='#0f766e'/></LineChart></ResponsiveContainer></div></div></AppLayout>
}
