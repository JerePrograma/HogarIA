import { useMutation, useQueryClient } from '@tanstack/react-query';
import { useState } from 'react';
import { useParams } from 'react-router-dom';
import { commitQuickPlanText, previewQuickPlanText } from '../../../api/quickPlanTextImportApi';
import { queryKeys } from '../../../domain/queryKeys';
import { useMonthlyPeriod } from '../../../hooks/useMonthlyPeriod';
import { getApiErrorMessage } from '../../../api/http';
import { QuickPlanPreviewTable } from './QuickPlanPreviewTable';
import { QuickPlanSummaryPanel } from './QuickPlanSummaryPanel';
import { QuickPlanTextArea } from './QuickPlanTextArea';

export function QuickPlanTextImportPage(){const {profileId=''}=useParams(); const {year,month}=useMonthlyPeriod(); const qc=useQueryClient(); const [text,setText]=useState(''); const [rows,setRows]=useState<any[]>([]); const [error,setError]=useState<string|null>(null);
const preview=useMutation({mutationFn:()=>previewQuickPlanText(profileId,{rawText:text,periodYear:year,periodMonth:month,defaultAmountScale:'THOUSANDS'}),onSuccess:(d)=>{setRows(d.candidates);setError(null);},onError:(e)=>setError(getApiErrorMessage(e))});
const commit=useMutation({mutationFn:()=>commitQuickPlanText(profileId,{items:rows.map(r=>r.item)}),onSuccess:async()=>{await Promise.all([qc.invalidateQueries({queryKey:queryKeys.planning(profileId,year,month)}),qc.invalidateQueries({queryKey:queryKeys.dashboard(profileId,year,month)}),qc.invalidateQueries({queryKey:queryKeys.monthlyPlanReconciliation(profileId,year,month)})]);},onError:(e)=>setError(getApiErrorMessage(e))});
return <section className='panel'><h2>Carga rápida por texto</h2><QuickPlanTextArea value={text} onChange={setText}/><button className='boton-principal' onClick={()=>preview.mutate()} disabled={!text.trim()||preview.isPending}>Previsualizar</button>{error?<p role='alert'>{error}</p>:null}{rows.length>0?<><QuickPlanSummaryPanel rows={rows}/><QuickPlanPreviewTable rows={rows} setRows={setRows}/><button className='boton-principal' onClick={()=>commit.mutate()} disabled={commit.isPending}>Crear compromisos</button></>:null}</section>}
