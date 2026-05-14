import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useMemo, useState } from 'react';
import { useParams } from 'react-router-dom';
import { createEmergencyFund, createGoal, deleteGoal, listGoals } from '../../api/goalsApi';
import { AppLayout } from '../../components/layout/AppLayout';
import { EmptyState } from '../../components/ui/EmptyState';
import { ErrorState } from '../../components/ui/ErrorState';
import { MetricCard } from '../../components/ui/MetricCard';
import { StatusBadge } from '../../components/ui/StatusBadge';
import type { FinancialGoal, GoalStatus, GoalType } from '../../domain/types';

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
  return Number.isFinite(numberValue) ? `${percentFormatter.format(numberValue)}%` : '-';
};

const formatDate = (value?: string | null) => {
  if (!value) return 'Sin fecha límite';
  return new Intl.DateTimeFormat('es-AR').format(new Date(`${value}T00:00:00`));
};

const getGoalStatusTone = (status: GoalStatus): 'ok' | 'watch' | 'critical' | 'neutral' => {
  if (status === 'ACTIVE') return 'ok';
  if (status === 'PAUSED') return 'watch';
  if (status === 'COMPLETED') return 'ok';
  if (status === 'CANCELLED') return 'critical';
  return 'neutral';
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

  const goals = goalsQuery.data ?? [];

  const activeGoals = useMemo(
    () => goals.filter((goal) => goal.status === 'ACTIVE'),
    [goals],
  );

  const completedGoals = useMemo(
    () => goals.filter((goal) => goal.status === 'COMPLETED'),
    [goals],
  );

  const totalTarget = useMemo(
    () => goals.reduce((acc, goal) => acc + Number(goal.targetAmount ?? 0), 0),
    [goals],
  );

  const totalCurrent = useMemo(
    () => goals.reduce((acc, goal) => acc + Number(goal.currentAmount ?? 0), 0),
    [goals],
  );

  const createMutation = useMutation({
    mutationFn: () =>
      createGoal(profileId, {
        name: name.trim(),
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
    mutationFn: (coverageMonths: number) => createEmergencyFund(profileId, coverageMonths),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['goals', profileId] }),
  });

  const deleteMutation = useMutation({
    mutationFn: (goalId: string) => deleteGoal(profileId, goalId),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['goals', profileId] }),
  });

  const canCreate =
    name.trim().length > 0 &&
    targetAmount > 0 &&
    Boolean(profileId) &&
    !createMutation.isPending;

  return (
    <AppLayout>
      <div className="page-stack">
        <section className="page-header">
          <div>
            <p className="eyebrow">Dirección financiera</p>
            <h1>Objetivos financieros</h1>
            <p className="muted">
              Definí metas concretas para ordenar ahorro, deuda, inversión y fondo de emergencia.
            </p>
          </div>
        </section>

        <section className="metric-grid">
          <MetricCard title="Objetivos activos" value={activeGoals.length} helper="Metas actualmente en seguimiento." tone="info" />
          <MetricCard title="Completados" value={completedGoals.length} helper="Metas ya alcanzadas." tone="success" />
          <MetricCard title="Monto objetivo total" value={formatMoney(totalTarget)} helper="Suma de metas cargadas." tone="warning" />
          <MetricCard title="Monto acumulado" value={formatMoney(totalCurrent)} helper="Avance económico consolidado." tone="success" />
        </section>

        <section className="panel">
          <div className="section-title">
            <div>
              <p className="eyebrow">Alta</p>
              <h2>Crear objetivo</h2>
            </div>
          </div>

          <div className="form-grid">
            <label>
              Nombre
              <input
                className="input-ui"
                placeholder="Ej: Fondo de emergencia"
                value={name}
                onChange={(event) => setName(event.target.value)}
              />
            </label>

            <label>
              Tipo
              <select
                className="input-ui"
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
                className="input-ui"
                type="number"
                min={0}
                value={targetAmount}
                onChange={(event) => setTargetAmount(Number(event.target.value))}
              />
            </label>

            <label>
              Monto actual
              <input
                className="input-ui"
                type="number"
                min={0}
                value={currentAmount}
                onChange={(event) => setCurrentAmount(Number(event.target.value))}
              />
            </label>

            <label>
              Aporte mensual
              <input
                className="input-ui"
                type="number"
                min={0}
                value={monthlyContribution}
                onChange={(event) => setMonthlyContribution(Number(event.target.value))}
              />
            </label>

            <label>
              Fecha límite
              <input
                className="input-ui"
                type="date"
                value={targetDate}
                onChange={(event) => setTargetDate(event.target.value)}
              />
            </label>

            <label className="form-field-wide">
              Notas
              <textarea
                className="input-ui"
                placeholder="Detalle opcional del objetivo"
                value={notes}
                onChange={(event) => setNotes(event.target.value)}
              />
            </label>
          </div>

          <div className="form-actions">
            <button
              type="button"
              className="boton-principal"
              disabled={!canCreate}
              onClick={() => createMutation.mutate()}
            >
              {createMutation.isPending ? 'Creando...' : 'Crear objetivo'}
            </button>
          </div>

          {createMutation.isError ? (
            <p className="mensaje-error">No se pudo crear el objetivo. Revisá los datos ingresados.</p>
          ) : null}
        </section>

        <section className="panel-soft">
          <div className="section-title">
            <div>
              <p className="eyebrow">Plantilla rápida</p>
              <h2>Crear fondo de emergencia sugerido</h2>
              <p className="secondary-text">
                Generá automáticamente un objetivo para cubrir entre 3 y 6 meses de gastos.
              </p>
            </div>
          </div>

          <div className="form-actions">
            {[3, 4, 5, 6].map((months) => (
              <button
                key={months}
                type="button"
                className="boton-secundario"
                disabled={emergencyMutation.isPending}
                onClick={() => emergencyMutation.mutate(months)}
              >
                {months} meses
              </button>
            ))}
          </div>

          {emergencyMutation.isError ? (
            <p className="mensaje-error">No se pudo crear el fondo de emergencia sugerido.</p>
          ) : null}
        </section>

        <section className="panel">
          <div className="section-title">
            <div>
              <p className="eyebrow">Listado</p>
              <h2>Objetivos cargados</h2>
            </div>

            <span className="badge-count">{goals.length}</span>
          </div>

          {goalsQuery.isLoading ? (
            <EmptyState title="Cargando objetivos" message="Estamos consultando tus objetivos financieros." />
          ) : null}

          {goalsQuery.isError ? (
            <ErrorState message="No se pudieron cargar los objetivos financieros." />
          ) : null}

          {!goalsQuery.isLoading && !goalsQuery.isError && goals.length === 0 ? (
            <EmptyState title="Sin objetivos" message="Todavía no tenés objetivos cargados." />
          ) : null}

          <div className="grid">
            {goals.map((goal) => {
              const progressPercent = Number(goal.progressPercent ?? 0);
              const safeProgress = Math.min(Math.max(progressPercent, 0), 100);

              return (
                <article key={goal.id} className="panel-muted">
                  <div className="section-title">
                    <div>
                      <h3>{goal.name}</h3>
                      <p className="secondary-text">
                        {goalTypeLabels[goal.goalType] ?? goal.goalType}
                      </p>
                    </div>

                    <StatusBadge
                      tone={getGoalStatusTone(goal.status)}
                      label={goalStatusLabels[goal.status] ?? goal.status}
                    />
                  </div>

                  <div className="grid">
                    <MetricCard title="Objetivo" value={formatMoney(goal.targetAmount)} tone="warning" />
                    <MetricCard title="Actual" value={formatMoney(goal.currentAmount)} tone="success" />
                    <MetricCard title="Aporte mensual" value={formatMoney(goal.monthlyContribution)} tone="info" />
                    <MetricCard title="Fecha límite" value={formatDate(goal.targetDate)} tone="neutral" />
                  </div>

                  <div className="progress-track">
                    <div className="progress-fill" style={{ width: `${safeProgress}%` }} />
                  </div>

                  <p className="secondary-text">
                    Progreso: {formatPercent(goal.progressPercent)}
                    {goal.monthsRemaining != null ? (
                      <>
                        <br />
                        Meses estimados restantes: {goal.monthsRemaining}
                      </>
                    ) : null}
                  </p>

                  {goal.notes ? <p className="surface-inset">{goal.notes}</p> : null}

                  <button
                    type="button"
                    className="boton-danger"
                    disabled={deleteMutation.isPending}
                    onClick={() =>
                      window.confirm('¿Eliminar este objetivo?') && deleteMutation.mutate(goal.id)
                    }
                  >
                    Eliminar
                  </button>
                </article>
              );
            })}
          </div>
        </section>
      </div>
    </AppLayout>
  );
}