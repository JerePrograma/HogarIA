import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useState } from 'react';
import { useParams } from 'react-router-dom';

import {
  createEmergencyFund,
  createGoal,
  deleteGoal,
  listGoals,
} from '../../api/goalsApi';

import { AppLayout } from '../../components/layout/AppLayout';

import type {
  FinancialGoal,
  GoalStatus,
  GoalType,
} from '../../domain/types';

const moneyFormatter = new Intl.NumberFormat('es-AR', {
  style: 'currency',
  currency: 'ARS',
  maximumFractionDigits: 0,
});

const percentFormatter = new Intl.NumberFormat('es-AR', {
  maximumFractionDigits: 2,
});

const goalTypeLabels: Record<GoalType, string> = {
  EMERGENCY_FUND: 'Fondo de emergencia',
  DEBT_PAYOFF: 'Cancelación de deuda',
  SAVING_TARGET: 'Meta de ahorro',
  INVESTMENT: 'Inversión',
  BUSINESS: 'Negocio',
  TRAVEL: 'Viaje',
  EDUCATION: 'Educación',
  OTHER: 'Otro',
};

const goalStatusLabels: Record<GoalStatus, string> = {
  ACTIVE: 'Activo',
  PAUSED: 'Pausado',
  COMPLETED: 'Completado',
  CANCELLED: 'Cancelado',
};

const goalTypeOptions: Array<{ value: GoalType; label: string }> = [
  { value: 'EMERGENCY_FUND', label: 'Fondo de emergencia' },
  { value: 'DEBT_PAYOFF', label: 'Cancelación de deuda' },
  { value: 'SAVING_TARGET', label: 'Meta de ahorro' },
  { value: 'INVESTMENT', label: 'Inversión' },
  { value: 'BUSINESS', label: 'Negocio' },
  { value: 'TRAVEL', label: 'Viaje' },
  { value: 'EDUCATION', label: 'Educación' },
  { value: 'OTHER', label: 'Otro' },
];

const formatMoney = (value: unknown) => {
  const numberValue = Number(value ?? 0);
  return Number.isFinite(numberValue) ? moneyFormatter.format(numberValue) : '-';
};

const formatPercent = (value: unknown) => {
  const numberValue = Number(value ?? 0);
  return Number.isFinite(numberValue)
    ? `${percentFormatter.format(numberValue)}%`
    : '-';
};

const formatDate = (value?: string | null) => {
  if (!value) return 'Sin fecha límite';

  return new Intl.DateTimeFormat('es-AR').format(new Date(value));
};

const getGoalTypeLabel = (type: GoalType) => {
  return goalTypeLabels[type] ?? type;
};

const getGoalStatusLabel = (status: GoalStatus) => {
  return goalStatusLabels[status] ?? status;
};

export function GoalsPage() {
  const { profileId = '' } = useParams();
  const queryClient = useQueryClient();

  const [name, setName] = useState('');
  const [targetAmount, setTargetAmount] = useState(0);
  const [currentAmount, setCurrentAmount] = useState(0);
  const [monthlyContribution, setMonthlyContribution] = useState(0);
  const [targetDate, setTargetDate] = useState('');
  const [notes, setNotes] = useState('');
  const [goalType, setGoalType] = useState<GoalType>('OTHER');

  const goalsQuery = useQuery<FinancialGoal[]>({
    queryKey: ['goals', profileId],
    queryFn: () => listGoals(profileId),
    enabled: Boolean(profileId),
  });

  const createMutation = useMutation({
    mutationFn: () =>
      createGoal(profileId, {
        name,
        goalType,
        targetAmount,
        currentAmount,
        monthlyContribution: monthlyContribution > 0 ? monthlyContribution : null,
        targetDate: targetDate || null,
        notes: notes || null,
      }),
    onSuccess: () => {
      setName('');
      setTargetAmount(0);
      setCurrentAmount(0);
      setMonthlyContribution(0);
      setTargetDate('');
      setNotes('');
      setGoalType('OTHER');

      queryClient.invalidateQueries({ queryKey: ['goals', profileId] });
    },
  });

  const emergencyMutation = useMutation({
    mutationFn: (coverageMonths: number) =>
      createEmergencyFund(profileId, coverageMonths),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['goals', profileId] });
    },
  });

  const deleteMutation = useMutation({
    mutationFn: (goalId: string) => deleteGoal(profileId, goalId),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['goals', profileId] });
    },
  });

  const canCreate =
    name.trim().length > 0 &&
    targetAmount > 0 &&
    Boolean(profileId) &&
    !createMutation.isPending;

  return (
    <AppLayout>
      <div className="card">
        <h1>Objetivos financieros</h1>

        <p className="empty-state">
          Definí metas concretas para ordenar ahorro, deuda, inversión y fondo de emergencia.
        </p>

        <div className="card">
          <h3>Crear objetivo</h3>

          <div className="form-row">
            <label>
              Nombre
              <input
                className="input"
                placeholder="Ej: Fondo de emergencia"
                value={name}
                onChange={(event) => setName(event.target.value)}
              />
            </label>

            <label>
              Tipo
              <select
                className="select"
                value={goalType}
                onChange={(event) => setGoalType(event.target.value as GoalType)}
              >
                {goalTypeOptions.map((option) => (
                  <option key={option.value} value={option.value}>
                    {option.label}
                  </option>
                ))}
              </select>
            </label>

            <label>
              Monto objetivo
              <input
                className="input"
                type="number"
                min={0}
                value={targetAmount}
                onChange={(event) => setTargetAmount(Number(event.target.value))}
              />
            </label>

            <label>
              Monto actual
              <input
                className="input"
                type="number"
                min={0}
                value={currentAmount}
                onChange={(event) => setCurrentAmount(Number(event.target.value))}
              />
            </label>

            <label>
              Aporte mensual
              <input
                className="input"
                type="number"
                min={0}
                value={monthlyContribution}
                onChange={(event) =>
                  setMonthlyContribution(Number(event.target.value))
                }
              />
            </label>

            <label>
              Fecha límite
              <input
                className="input"
                type="date"
                value={targetDate}
                onChange={(event) => setTargetDate(event.target.value)}
              />
            </label>
          </div>

          <label>
            Notas
            <textarea
              className="input"
              placeholder="Detalle opcional del objetivo"
              value={notes}
              onChange={(event) => setNotes(event.target.value)}
            />
          </label>

          <div className="form-row">
            <button
              className="button-primary"
              disabled={!canCreate}
              onClick={() => createMutation.mutate()}
            >
              {createMutation.isPending ? 'Creando...' : 'Crear objetivo'}
            </button>
          </div>

          {createMutation.isError && (
            <p className="error-box">
              No se pudo crear el objetivo. Revisá los datos ingresados.
            </p>
          )}
        </div>

        <div className="card">
          <h3>Crear fondo de emergencia sugerido</h3>

          <p>
            Generá automáticamente un objetivo para cubrir entre 3 y 6 meses de gastos.
          </p>

          <div className="form-row">
            {[3, 4, 5, 6].map((months) => (
              <button
                key={months}
                className="button-secondary"
                disabled={emergencyMutation.isPending}
                onClick={() => emergencyMutation.mutate(months)}
              >
                {months} meses
              </button>
            ))}
          </div>

          {emergencyMutation.isError && (
            <p className="error-box">
              No se pudo crear el fondo de emergencia sugerido.
            </p>
          )}
        </div>

        {goalsQuery.isLoading && (
          <p className="empty-state">Cargando objetivos financieros...</p>
        )}

        {goalsQuery.isError && (
          <p className="error-box">
            No se pudieron cargar los objetivos financieros.
          </p>
        )}

        {!goalsQuery.isLoading &&
          !goalsQuery.isError &&
          goalsQuery.data?.length === 0 && (
            <p className="empty-state">
              Todavía no tenés objetivos cargados.
            </p>
          )}

        <div className="grid">
          {goalsQuery.data?.map((goal) => {
            const progressPercent = Number(goal.progressPercent ?? 0);
            const safeProgress = Math.min(Math.max(progressPercent, 0), 100);

            return (
              <div key={goal.id} className="card">
                <b>{goal.name}</b>

                <p>
                  {getGoalTypeLabel(goal.goalType)} ·{' '}
                  {getGoalStatusLabel(goal.status)}
                </p>

                <p>
                  Objetivo: {formatMoney(goal.targetAmount)}
                  <br />
                  Actual: {formatMoney(goal.currentAmount)}
                  <br />
                  Aporte mensual: {formatMoney(goal.monthlyContribution)}
                  <br />
                  Fecha límite: {formatDate(goal.targetDate)}
                </p>

                <div className="progress-track">
                  <div
                    className="progress-fill"
                    style={{ width: `${safeProgress}%` }}
                  />
                </div>

                <p>
                  Progreso: {formatPercent(goal.progressPercent)}
                  {goal.monthsRemaining != null && (
                    <>
                      <br />
                      Meses estimados restantes: {goal.monthsRemaining}
                    </>
                  )}
                </p>

                {goal.notes && <p>{goal.notes}</p>}

                <button
                  className="button-danger"
                  disabled={deleteMutation.isPending}
                  onClick={() => deleteMutation.mutate(goal.id)}
                >
                  Eliminar
                </button>
              </div>
            );
          })}
        </div>
      </div>
    </AppLayout>
  );
}