import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useState } from 'react';
import { useParams } from 'react-router-dom';
import { createHabit, listHabits, upsertHabitCheckin } from '../../api/habitsApi';
import { AppLayout } from '../../components/layout/AppLayout';
import { habitFrequencyLabels, labelOrMissing, type HabitFrequency } from '../../domain/financeLabels';
import { habitFrequencyOptions } from '../../domain/financeOptions';

export function HabitsPage() {
  const { profileId = '' } = useParams();
  const qc = useQueryClient();

  const today = new Date().toISOString().slice(0, 10);
  const [name, setName] = useState('');
  const [frequency, setFrequency] = useState<HabitFrequency>('DAILY');

  const habitsQuery = useQuery({
    queryKey: ['habits', profileId],
    queryFn: () => listHabits(profileId),
    enabled: Boolean(profileId),
  });

  const createHabitMutation = useMutation({
    mutationFn: () => createHabit(profileId, { name, frequency }),
    onSuccess: () => {
      setName('');
      qc.invalidateQueries({ queryKey: ['habits', profileId] });
    },
  });

  const checkHabitMutation = useMutation({
    mutationFn: ({ habitId, done }: { habitId: string; done: boolean }) =>
      upsertHabitCheckin(profileId, habitId, today, done),
    onSuccess: () => qc.invalidateQueries({ queryKey: ['habits', profileId] }),
  });

  return (
    <AppLayout>
      <div className="page-header">
        <div>
          <p className="eyebrow">Disciplina financiera</p>
          <h1>Hábitos financieros</h1>
          <p className="muted">Registrá rutinas que sostienen la salud financiera.</p>
        </div>
      </div>

      <section className="card">
        <h2>Crear hábito</h2>

        <div className="form-grid">
          <label className="field">
            <span>Nombre del hábito</span>
            <input
              value={name}
              placeholder="Ej: Registrar gastos, revisar presupuesto"
              onChange={(event) => setName(event.target.value)}
            />
          </label>

          <label className="field">
            <span>Frecuencia</span>
            <select value={frequency} onChange={(event) => setFrequency(event.target.value as HabitFrequency)}>
              {habitFrequencyOptions.map((option) => (
                <option key={option.value} value={option.value}>
                  {option.label}
                </option>
              ))}
            </select>
          </label>
        </div>

        <div className="actions">
          <button
            type="button"
            className="button primary"
            onClick={() => createHabitMutation.mutate()}
            disabled={!name.trim() || createHabitMutation.isPending}
          >
            Crear hábito
          </button>
        </div>
      </section>

      <section className="card">
        <h2>Hábitos registrados</h2>

        {habitsQuery.isLoading ? (
          <p className="muted">Cargando hábitos...</p>
        ) : !habitsQuery.data?.length ? (
          <p className="muted">No hay hábitos creados.</p>
        ) : (
          <div className="table-wrap">
            <table className="table">
              <thead>
                <tr>
                  <th>Hábito</th>
                  <th>Frecuencia</th>
                  <th>Estado</th>
                  <th>Hoy</th>
                </tr>
              </thead>
              <tbody>
                {habitsQuery.data.map((habit: any) => (
                  <tr key={habit.id}>
                    <td>{habit.name}</td>
                    <td>{labelOrMissing(habitFrequencyLabels, habit.frequency)}</td>
                    <td>
                      <span className={`badge ${habit.active ? 'good' : 'muted'}`}>
                        {habit.active ? 'Activo' : 'Inactivo'}
                      </span>
                    </td>
                    <td>
                      <button
                        type="button"
                        className="button secondary"
                        aria-label="Marcar hábito como completado"
                        onClick={() => checkHabitMutation.mutate({ habitId: habit.id, done: true })}
                      >
                        Completar
                      </button>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </section>
    </AppLayout>
  );
}