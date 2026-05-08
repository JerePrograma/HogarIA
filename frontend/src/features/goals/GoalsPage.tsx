import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useState } from 'react';
import { useParams } from 'react-router-dom';
import { createEmergencyFund, createGoal, deleteGoal, listGoals } from '../../api/goalsApi';
import { AppLayout } from '../../components/layout/AppLayout';
import { goalStatusLabels, goalTypeLabels, labelOrMissing, type GoalType } from '../../domain/financeLabels';
import { goalTypeOptions } from '../../domain/financeOptions';
import { formatMoney } from '../../domain/formatters';

export function GoalsPage() {
  const { profileId = '' } = useParams();
  const qc = useQueryClient();

  const [name, setName] = useState('');
  const [targetAmount, setTargetAmount] = useState(0);
  const [goalType, setGoalType] = useState<GoalType>('OTHER');

  const goalsQuery = useQuery({
    queryKey: ['goals', profileId],
    queryFn: () => listGoals(profileId),
    enabled: Boolean(profileId),
  });

  const createGoalMutation = useMutation({
    mutationFn: () =>
      createGoal(profileId, {
        name,
        goalType,
        targetAmount,
        currentAmount: 0,
        priority: 3,
      }),
    onSuccess: () => {
      setName('');
      setTargetAmount(0);
      qc.invalidateQueries({ queryKey: ['goals', profileId] });
    },
  });

  const emergencyFundMutation = useMutation({
    mutationFn: (coverageMonths: number) => createEmergencyFund(profileId, coverageMonths),
    onSuccess: () => qc.invalidateQueries({ queryKey: ['goals', profileId] }),
  });

  const deleteGoalMutation = useMutation({
    mutationFn: (goalId: string) => deleteGoal(profileId, goalId),
    onSuccess: () => qc.invalidateQueries({ queryKey: ['goals', profileId] }),
  });

  const canCreate = Boolean(name.trim()) && targetAmount > 0;

  return (
    <AppLayout>
      <div className="page-header">
        <div>
          <p className="eyebrow">Planificación</p>
          <h1>Objetivos financieros</h1>
          <p className="muted">Convertí metas abstractas en progreso medible.</p>
        </div>
      </div>

      <section className="card">
        <h2>Crear objetivo</h2>

        <div className="form-grid">
          <label className="field">
            <span>Nombre del objetivo</span>
            <input
              value={name}
              placeholder="Ej: Fondo de emergencia, viaje, inversión"
              onChange={(event) => setName(event.target.value)}
            />
          </label>

          <label className="field">
            <span>Tipo de objetivo</span>
            <select value={goalType} onChange={(event) => setGoalType(event.target.value as GoalType)}>
              {goalTypeOptions.map((option) => (
                <option key={option.value} value={option.value}>
                  {option.label}
                </option>
              ))}
            </select>
          </label>

          <label className="field">
            <span>Monto objetivo</span>
            <input
              type="number"
              min="0"
              value={targetAmount}
              placeholder="Ej: 500000"
              onChange={(event) => setTargetAmount(Number(event.target.value))}
            />
          </label>
        </div>

        <div className="actions">
          <button
            type="button"
            className="button primary"
            onClick={() => createGoalMutation.mutate()}
            disabled={!canCreate || createGoalMutation.isPending}
          >
            Crear objetivo
          </button>
        </div>
      </section>

      <section className="card">
        <h2>Fondo de emergencia sugerido</h2>
        <p className="muted">Creá una meta automática según cantidad de meses de cobertura.</p>

        <div className="actions">
          {[3, 4, 5, 6].map((months) => (
            <button
              key={months}
              type="button"
              className="button secondary"
              onClick={() => emergencyFundMutation.mutate(months)}
              disabled={emergencyFundMutation.isPending}
            >
              {months} meses
            </button>
          ))}
        </div>
      </section>

      <section className="goal-grid">
        {goalsQuery.isLoading ? (
          <p className="muted">Cargando objetivos...</p>
        ) : !goalsQuery.data?.length ? (
          <article className="card">
            <p className="muted">No hay objetivos financieros creados.</p>
          </article>
        ) : (
          goalsQuery.data.map((goal: any) => {
            const progress = Math.min(100, Math.max(0, Number(goal.progress ?? 0)));

            return (
              <article className="card goal-card" key={goal.id}>
                <div className="section-header">
                  <div>
                    <h2>{goal.name}</h2>
                    <p className="muted">
                      {labelOrMissing(goalTypeLabels, goal.goalType)} · {labelOrMissing(goalStatusLabels, goal.status)}
                    </p>
                  </div>

                  <span className="badge good">{progress.toFixed(1)}%</span>
                </div>

                <div className="goal-amounts">
                  <span>Meta: <strong>{formatMoney(goal.targetAmount)}</strong></span>
                  <span>Ahorrado: <strong>{formatMoney(goal.currentAmount)}</strong></span>
                </div>

                <div className="progress" aria-label={`Progreso ${progress.toFixed(1)}%`}>
                  <div className="progress-bar" style={{ width: `${progress}%` }} />
                </div>

                <div className="actions">
                  <button
                    type="button"
                    className="button danger"
                    onClick={() => window.confirm('¿Eliminar objetivo?') && deleteGoalMutation.mutate(goal.id)}
                  >
                    Eliminar
                  </button>
                </div>
              </article>
            );
          })
        )}
      </section>
    </AppLayout>
  );
}