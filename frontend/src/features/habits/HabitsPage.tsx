import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useState } from 'react';
import { useParams } from 'react-router-dom';
import { createHabit, listHabits, upsertHabitCheckin } from '../../api/habitsApi';
import { getApiErrorMessage } from '../../api/http';
import { AppLayout } from '../../components/layout/AppLayout';
import { habitFrequencyLabels, labelOrValue } from '../../domain/financeLabels';
import { normalizeOptionalText } from '../../domain/formatters';
import type { Habit, HabitFrequency } from '../../domain/types';

export function HabitsPage() {
  const { profileId = '' } = useParams();
  const qc = useQueryClient();
  const today = new Date().toISOString().slice(0, 10);
  const [description, setDescription] = useState('');
  const [frequency, setFrequency] = useState<HabitFrequency>('DAILY');
  const [area, setArea] = useState('FINANZAS');
  const [feedback, setFeedback] = useState('');

  const q = useQuery({ queryKey: ['habits', profileId], queryFn: () => listHabits(profileId), enabled: Boolean(profileId) });
  const create = useMutation({ mutationFn: () => createHabit(profileId, { description, area, frequency }), onSuccess: () => { setDescription(''); qc.invalidateQueries({ queryKey: ['habits', profileId] }); setFeedback('Hábito creado correctamente.'); }, onError: (e) => setFeedback(getApiErrorMessage(e)) });
  const check = useMutation({ mutationFn: ({ habitId, completed }: { habitId: string; completed: boolean }) => upsertHabitCheckin(profileId, habitId, today, { completed, note: completed ? 'Completado desde la web' : 'Marcado como pendiente' }), onSuccess: () => setFeedback('Check-in actualizado.'), onError: (e) => setFeedback(getApiErrorMessage(e)) });

  return <AppLayout><div className='card'><h1>Hábitos financieros</h1>
    <div className='form-row'><input className='input' placeholder='Descripción del hábito' value={description} onChange={e => setDescription(e.target.value)} /><input className='input' placeholder='Área' value={area} onChange={e => setArea(e.target.value)} /><select className='select' value={frequency} onChange={e => setFrequency(e.target.value as HabitFrequency)}><option value='DAILY'>Diario</option><option value='WEEKLY'>Semanal</option><option value='MONTHLY'>Mensual</option></select><button className='button-primary' disabled={create.isPending || !description.trim()} onClick={() => create.mutate()}>Crear hábito</button></div>
    {feedback && <p className={feedback.includes('correct') ? 'empty-state' : 'error-box'}>{feedback}</p>}
    <table className='table'><thead><tr><th>Hábito</th><th>Área</th><th>Frecuencia</th><th>Estado</th><th>Check-in</th></tr></thead><tbody>{(q.data ?? []).map((h: Habit) => <tr key={h.id}><td>{h.description}</td><td>{normalizeOptionalText(h.area)}</td><td>{labelOrValue(habitFrequencyLabels, h.frequency)}</td><td>{h.active ? 'Activo' : 'Pausado'}</td><td><button className='button-secondary' disabled={check.isPending} onClick={() => check.mutate({ habitId: h.id, completed: true })}>Marcar completo</button></td></tr>)}</tbody></table></div></AppLayout>;
}
