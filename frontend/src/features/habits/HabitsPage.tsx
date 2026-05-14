import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useMemo, useState } from 'react';
import { useParams } from 'react-router-dom';
import { createHabit, listHabits, upsertHabitCheckin } from '../../api/habitsApi';
import { getApiErrorMessage } from '../../api/http';
import { AppLayout } from '../../components/layout/AppLayout';
import { EmptyState } from '../../components/ui/EmptyState';
import { ErrorState } from '../../components/ui/ErrorState';
import { MetricCard } from '../../components/ui/MetricCard';
import { StatusBadge } from '../../components/ui/StatusBadge';
import { habitFrequencyLabels, labelOrValue } from '../../domain/financeLabels';
import { normalizeOptionalText } from '../../domain/formatters';
import type { Habit, HabitFrequency } from '../../domain/types';

const frequencyOptions: Array<{ value: HabitFrequency; label: string }> = [
  { value: 'DAILY', label: 'Diario' },
  { value: 'WEEKLY', label: 'Semanal' },
  { value: 'MONTHLY', label: 'Mensual' },
];

function getTodayIsoDate() {
  return new Date().toISOString().slice(0, 10);
}

export function HabitsPage() {
  const { profileId = '' } = useParams();
  const queryClient = useQueryClient();

  const today = getTodayIsoDate();

  const [description, setDescription] = useState('');
  const [frequency, setFrequency] = useState<HabitFrequency>('DAILY');
  const [area, setArea] = useState('FINANZAS');
  const [feedback, setFeedback] = useState('');

  const habitsQuery = useQuery<Habit[]>({
    queryKey: ['habits', profileId],
    queryFn: () => listHabits(profileId),
    enabled: Boolean(profileId),
  });

  const habits = habitsQuery.data ?? [];

  const activeHabits = useMemo(() => habits.filter((habit) => habit.active), [habits]);
  const inactiveHabits = useMemo(() => habits.filter((habit) => !habit.active), [habits]);
  const dailyHabits = useMemo(
    () => habits.filter((habit) => habit.frequency === 'DAILY'),
    [habits],
  );

  const createHabitMutation = useMutation({
    mutationFn: () =>
      createHabit(profileId, {
        description: description.trim(),
        area: area.trim() || 'FINANZAS',
        frequency,
      }),
    onSuccess: () => {
      setDescription('');
      setArea('FINANZAS');
      setFrequency('DAILY');
      setFeedback('Hábito creado correctamente.');
      queryClient.invalidateQueries({ queryKey: ['habits', profileId] });
    },
    onError: (error) => {
      setFeedback(getApiErrorMessage(error));
    },
  });

  const checkinMutation = useMutation({
    mutationFn: ({ habitId, completed }: { habitId: string; completed: boolean }) =>
      upsertHabitCheckin(profileId, habitId, today, {
        completed,
        note: completed ? 'Completado desde la web' : 'Marcado como pendiente desde la web',
      }),
    onSuccess: () => {
      setFeedback('Check-in actualizado.');
      queryClient.invalidateQueries({ queryKey: ['habits', profileId] });
    },
    onError: (error) => {
      setFeedback(getApiErrorMessage(error));
    },
  });

  const canCreate =
    description.trim().length > 0 && Boolean(profileId) && !createHabitMutation.isPending;

  const feedbackIsSuccess =
    feedback.includes('correctamente') || feedback.includes('actualizado');

  return (
    <AppLayout>
      <div className="page-stack">
        <section className="page-header">
          <div>
            <p className="eyebrow">Seguimiento financiero</p>
            <h1>Hábitos financieros</h1>
            <p className="muted">
              Registrá rutinas simples para sostener el control mensual: revisar gastos, anotar
              movimientos, controlar presupuesto y evitar desvíos.
            </p>
          </div>
        </section>

        <section className="metric-grid">
          <MetricCard title="Hábitos activos" value={activeHabits.length} helper="Rutinas vigentes." tone="success" />
          <MetricCard title="Total cargados" value={habits.length} helper="Histórico completo." tone="info" />
          <MetricCard title="Frecuencia diaria" value={dailyHabits.length} helper="Hábitos de ejecución diaria." tone="success" />
          <MetricCard title="Pausados" value={inactiveHabits.length} helper="Rutinas fuera de uso." tone={inactiveHabits.length > 0 ? 'warning' : 'neutral'} />
        </section>

        <section className="panel">
          <div className="section-title">
            <div>
              <p className="eyebrow">Alta</p>
              <h2>Crear hábito</h2>
              <p className="muted">
                Cargá hábitos concretos y medibles. Si no se puede marcar como hecho, está mal definido.
              </p>
            </div>
          </div>

          <div className="form-grid">
            <label className="form-field-wide">
              Descripción
              <input
                className="input-ui"
                placeholder="Ej: revisar gastos del día"
                value={description}
                onChange={(event) => setDescription(event.target.value)}
              />
            </label>

            <label>
              Área
              <input
                className="input-ui"
                placeholder="Ej: FINANZAS"
                value={area}
                onChange={(event) => setArea(event.target.value)}
              />
            </label>

            <label>
              Frecuencia
              <select
                className="input-ui"
                value={frequency}
                onChange={(event) => setFrequency(event.target.value as HabitFrequency)}
              >
                {frequencyOptions.map((option) => (
                  <option key={option.value} value={option.value}>
                    {option.label}
                  </option>
                ))}
              </select>
            </label>
          </div>

          <div className="form-actions">
            <button
              type="button"
              className="boton-principal"
              disabled={!canCreate}
              onClick={() => createHabitMutation.mutate()}
            >
              {createHabitMutation.isPending ? 'Creando...' : 'Crear hábito'}
            </button>

            {!canCreate ? (
              <span className="muted">Completá una descripción para crear el hábito.</span>
            ) : null}
          </div>

          {feedback ? (
            <p className={feedbackIsSuccess ? 'mensaje-exito' : 'mensaje-error'}>{feedback}</p>
          ) : null}
        </section>

        <section className="panel">
          <div className="section-title">
            <div>
              <p className="eyebrow">Listado</p>
              <h2>Hábitos cargados</h2>
              <p className="muted">
                {habits.length} hábito{habits.length === 1 ? '' : 's'} registrado
                {habits.length === 1 ? '' : 's'}.
              </p>
            </div>
          </div>

          {habitsQuery.isLoading ? (
            <EmptyState title="Cargando hábitos" message="Estamos consultando hábitos financieros." />
          ) : null}

          {habitsQuery.isError ? (
            <ErrorState message="No se pudieron cargar los hábitos financieros." />
          ) : null}

          {!habitsQuery.isLoading && !habitsQuery.isError && habits.length === 0 ? (
            <EmptyState
              title="Sin hábitos"
              message="Todavía no tenés hábitos cargados. Empezá con uno simple: “anotar gastos del día”."
            />
          ) : null}

          {habits.length > 0 ? (
            <div className="tabla-ui">
              <table className="table-compact">
                <thead>
                  <tr>
                    <th>Hábito</th>
                    <th>Área</th>
                    <th>Frecuencia</th>
                    <th>Estado</th>
                    <th>Check-in de hoy</th>
                  </tr>
                </thead>

                <tbody>
                  {habits.map((habit) => (
                    <tr key={habit.id}>
                      <td>
                        <strong>{habit.description}</strong>
                      </td>
                      <td>{normalizeOptionalText(habit.area)}</td>
                      <td>{labelOrValue(habitFrequencyLabels, habit.frequency)}</td>
                      <td>
                        <StatusBadge
                          tone={habit.active ? 'ok' : 'watch'}
                          label={habit.active ? 'Activo' : 'Pausado'}
                        />
                      </td>
                      <td>
                        <div className="row-actions">
                          <button
                            type="button"
                            className="boton-secundario"
                            disabled={checkinMutation.isPending || !habit.active}
                            onClick={() =>
                              checkinMutation.mutate({
                                habitId: habit.id,
                                completed: true,
                              })
                            }
                          >
                            Marcar completo
                          </button>

                          <button
                            type="button"
                            className="boton-fantasma"
                            disabled={checkinMutation.isPending || !habit.active}
                            onClick={() =>
                              checkinMutation.mutate({
                                habitId: habit.id,
                                completed: false,
                              })
                            }
                          >
                            Marcar pendiente
                          </button>
                        </div>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          ) : null}
        </section>
      </div>
    </AppLayout>
  );
}