import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useMemo, useState } from 'react';
import { useParams } from 'react-router-dom';

import {
  createHabit,
  listHabits,
  upsertHabitCheckin,
} from '../../api/habitsApi';

import { getApiErrorMessage } from '../../api/http';
import { AppLayout } from '../../components/layout/AppLayout';

import {
  habitFrequencyLabels,
  labelOrValue,
} from '../../domain/financeLabels';

import { normalizeOptionalText } from '../../domain/formatters';

import type {
  Habit,
  HabitFrequency,
} from '../../domain/types';

const frequencyOptions: Array<{ value: HabitFrequency; label: string }> = [
  { value: 'DAILY', label: 'Diario' },
  { value: 'WEEKLY', label: 'Semanal' },
  { value: 'MONTHLY', label: 'Mensual' },
];

function getTodayIsoDate() {
  return new Date().toISOString().slice(0, 10);
}

function getHabitStatusBadgeClass(active: boolean) {
  return active ? 'badge badge-ok' : 'badge badge-muted';
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

  const activeHabits = useMemo(
    () => habits.filter((habit) => habit.active),
    [habits],
  );

  const inactiveHabits = useMemo(
    () => habits.filter((habit) => !habit.active),
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
    mutationFn: ({
      habitId,
      completed,
    }: {
      habitId: string;
      completed: boolean;
    }) =>
      upsertHabitCheckin(profileId, habitId, today, {
        completed,
        note: completed
          ? 'Completado desde la web'
          : 'Marcado como pendiente desde la web',
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
    description.trim().length > 0 &&
    Boolean(profileId) &&
    !createHabitMutation.isPending;

  return (
    <AppLayout>
      <div className="page-stack">
        <section className="page-header">
          <div>
            <p className="eyebrow">Seguimiento financiero</p>
            <h1>Hábitos financieros</h1>
            <p className="muted">
              Registrá rutinas simples para sostener el control mensual:
              revisar gastos, anotar movimientos, controlar presupuesto y evitar desvíos.
            </p>
          </div>
        </section>

        <section className="summary-grid">
          <div className="metric-card metric-income">
            <span>Hábitos activos</span>
            <strong>{activeHabits.length}</strong>
          </div>

          <div className="metric-card">
            <span>Total cargados</span>
            <strong>{habits.length}</strong>
          </div>

          <div className="metric-card metric-saving">
            <span>Frecuencia diaria</span>
            <strong>
              {habits.filter((habit) => habit.frequency === 'DAILY').length}
            </strong>
          </div>

          <div className="metric-card metric-expense">
            <span>Pausados</span>
            <strong>{inactiveHabits.length}</strong>
          </div>
        </section>

        <section className="card">
          <div className="section-title">
            <div>
              <h2>Crear hábito</h2>
              <p className="muted">
                Cargá hábitos concretos y medibles. Si no se puede marcar como hecho, está mal definido.
              </p>
            </div>
          </div>

          <div className="form-grid habits-form-grid">
            <label className="form-field-wide">
              Descripción
              <input
                className="input"
                placeholder="Ej: revisar gastos del día"
                value={description}
                onChange={(event) => setDescription(event.target.value)}
              />
            </label>

            <label>
              Área
              <input
                className="input"
                placeholder="Ej: FINANZAS"
                value={area}
                onChange={(event) => setArea(event.target.value)}
              />
            </label>

            <label>
              Frecuencia
              <select
                className="select"
                value={frequency}
                onChange={(event) =>
                  setFrequency(event.target.value as HabitFrequency)
                }
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
              className="button-primary"
              disabled={!canCreate}
              onClick={() => createHabitMutation.mutate()}
            >
              {createHabitMutation.isPending ? 'Creando...' : 'Crear hábito'}
            </button>

            {!canCreate && (
              <span className="muted">
                Completá una descripción para crear el hábito.
              </span>
            )}
          </div>

          {feedback && (
            <p
              className={
                feedback.includes('correctamente') ||
                feedback.includes('actualizado')
                  ? 'success-box'
                  : 'error-box'
              }
            >
              {feedback}
            </p>
          )}
        </section>

        <section className="card">
          <div className="section-title">
            <div>
              <h2>Hábitos cargados</h2>
              <p className="muted">
                {habits.length} hábito{habits.length === 1 ? '' : 's'} registrado
                {habits.length === 1 ? '' : 's'}.
              </p>
            </div>
          </div>

          {habitsQuery.isLoading && (
            <p className="empty-state">Cargando hábitos financieros...</p>
          )}

          {habitsQuery.isError && (
            <p className="error-box">
              No se pudieron cargar los hábitos financieros.
            </p>
          )}

          {!habitsQuery.isLoading &&
            !habitsQuery.isError &&
            habits.length === 0 && (
              <p className="empty-state">
                Todavía no tenés hábitos cargados. Empezá con uno simple:
                “anotar gastos del día”.
              </p>
            )}

          {habits.length > 0 && (
            <div className="table-wrapper">
              <table className="table table-compact">
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

                      <td>
                        {labelOrValue(
                          habitFrequencyLabels,
                          habit.frequency,
                        )}
                      </td>

                      <td>
                        <span className={getHabitStatusBadgeClass(habit.active)}>
                          {habit.active ? 'Activo' : 'Pausado'}
                        </span>
                      </td>

                      <td>
                        <div className="row-actions">
                          <button
                            className="button-secondary"
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
          )}
        </section>
      </div>
    </AppLayout>
  );
}