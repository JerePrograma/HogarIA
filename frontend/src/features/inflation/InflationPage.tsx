import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useState } from 'react';
import { Line, LineChart, ResponsiveContainer, Tooltip, XAxis, YAxis } from 'recharts';
import { createInflation, getAccumulatedInflation, listInflation } from '../../api/inflationApi';
import { getApiErrorMessage } from '../../api/http';
import { AppLayout } from '../../components/layout/AppLayout';
import { formatPercent, formatMonth } from '../../domain/formatters';
import type { InflationIndex } from '../../domain/types';

export function InflationPage() {
  const d = new Date(); const [year, setYear] = useState(d.getFullYear()); const [month, setMonth] = useState(d.getMonth() + 1); const [monthlyRate, setMonthlyRate] = useState(0.03); const [projection, setProjection] = useState(false); const [message, setMessage] = useState(''); const qc = useQueryClient();
  const q = useQuery({ queryKey: ['inflation', year], queryFn: () => listInflation(year) });
  const acc = useQuery({ queryKey: ['inflation-acc', year], queryFn: () => getAccumulatedInflation(year, 1, year, 12) });
  const create = useMutation({ mutationFn: () => createInflation({ year, month, monthlyRate, source: 'MANUAL', projection }), onSuccess: () => { setMessage('Índice guardado.'); qc.invalidateQueries({ queryKey: ['inflation', year] }); }, onError: (e) => setMessage(getApiErrorMessage(e)) });
  return <AppLayout><div className='card'><h1>Inflación y proyecciones</h1><div className='form-row'><input className='input' type='number' value={year} onChange={e => setYear(Number(e.target.value))} /><input className='input' type='number' value={month} min={1} max={12} onChange={e => setMonth(Number(e.target.value))} /><input className='input' type='number' step='0.0001' value={monthlyRate} onChange={e => setMonthlyRate(Number(e.target.value))} /><label><input type='checkbox' checked={projection} onChange={e => setProjection(e.target.checked)} /> Proyectado</label><button className='button-primary' disabled={create.isPending} onClick={() => create.mutate()}>Guardar índice</button></div>
    <p>Acumulada anual: {formatPercent(Number(acc.data?.accumulatedRate ?? 0) * 100)}</p>{message && <p className='empty-state'>{message}</p>}
    <div style={{ height: 260 }}><ResponsiveContainer><LineChart data={(q.data ?? []).map((x: InflationIndex) => ({ mes: formatMonth(x.month), porcentaje: Number(x.monthlyRate) * 100 }))}><XAxis dataKey='mes' /><YAxis /><Tooltip formatter={(value: number) => formatPercent(value)} /><Line type='monotone' dataKey='porcentaje' stroke='#0f766e' /></LineChart></ResponsiveContainer></div>
    <table className='table'><thead><tr><th>Mes</th><th>Tasa mensual</th><th>Tipo</th><th>Fuente</th></tr></thead><tbody>{(q.data ?? []).map((x: InflationIndex) => <tr key={x.id}><td>{formatMonth(x.month)}</td><td>{formatPercent(Number(x.monthlyRate) * 100)}</td><td>{x.projection ? 'Proyectado' : 'Real'}</td><td>{x.source ?? 'Sin fuente'}</td></tr>)}</tbody></table>
  </div></AppLayout>;
}
