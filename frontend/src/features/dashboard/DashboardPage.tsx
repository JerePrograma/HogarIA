import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useMemo, useState } from 'react';
import { Link, useParams } from 'react-router-dom';
import { getMonthlyDashboard } from '../../api/dashboardApi';
import { getApiErrorMessage } from '../../api/http';
import { createMonthlyPlanItem } from '../../api/monthlyPlanningApi';
import { AppLayout } from '../../components/layout/AppLayout';
import { EmptyState } from '../../components/ui/EmptyState';
import { ErrorState } from '../../components/ui/ErrorState';
import { MetricCard } from '../../components/ui/MetricCard';
import { MonthSelector } from '../../components/ui/MonthSelector';
import type { MonthlyPlanItemCreatePayload } from '../../domain/types';
import type { DashboardSummary } from '../../domain/types';
import { ConfirmedVsProjectedPanel } from './components/ConfirmedVsProjectedPanel';
import { DashboardCharts } from './components/DashboardCharts';
import { NextBestActionCard } from './components/NextBestActionCard';
import { OperationalAlerts } from './components/OperationalAlerts';
import { OperationalSummaryCards } from './components/OperationalSummaryCards';

export function DashboardPage() {
  const { profileId = '' } = useParams();
  const queryClient = useQueryClient();

  const today = new Date();
  const [year, setYear] = useState(today.getFullYear());
  const [month, setMonth] = useState(today.getMonth() + 1);
  const [incomeTitle, setIncomeTitle] = useState('');
  const [incomeAmount, setIncomeAmount] = useState('');
  const [incomeDate, setIncomeDate] = useState('');
  const [incomePeriods, setIncomePeriods] = useState(1);

  const periodLabel = useMemo(() => {
    if (incomePeriods === 1) return '1 período';
    return `${incomePeriods} períodos`;
  }, [incomePeriods]);

  const dashboardQuery = useQuery<DashboardSummary>({
    queryKey: ['dashboard', profileId, year, month],
    queryFn: () => getMonthlyDashboard(profileId, year, month),
    enabled: Boolean(profileId),
  });

  const summary = dashboardQuery.data;
  const planning = summary?.planningSummary;
  const operational = summary?.operationalSummary;

  const canRenderDashboard = Boolean(summary && planning && operational);

  const addFixedIncomeMutation = useMutation({
    mutationFn: async () => {
      const amount = Number(incomeAmount);
      const sanitizedTitle = incomeTitle.trim();
      const periods = Math.max(1, Math.min(12, incomePeriods));
      const createCalls: Promise<unknown>[] = [];

      for (let index = 0; index < periods; index += 1) {
        const nextDate = incomeDate ? new Date(`${incomeDate}T00:00:00`) : null;
        if (nextDate) nextDate.setMonth(nextDate.getMonth() + index);
        const nextYear = nextDate ? nextDate.getFullYear() : year;
        const nextMonth = nextDate ? nextDate.getMonth() + 1 : month;

        const payload: MonthlyPlanItemCreatePayload = {
          type: 'INCOME',
          title: sanitizedTitle,
          amount,
          periodYear: nextYear,
          periodMonth: nextMonth,
          expectedDate: nextDate ? nextDate.toISOString().slice(0, 10) : null,
          priority: 'ESSENTIAL',
          status: 'SCHEDULED',
          source: 'QUICK_CAPTURE',
          currency: 'ARS',
        };

        createCalls.push(createMonthlyPlanItem(profileId, payload));
      }

      await Promise.all(createCalls);
    },
    onSuccess: () => {
      setIncomeTitle('');
      setIncomeAmount('');
      setIncomeDate('');
      setIncomePeriods(1);
      queryClient.invalidateQueries({ queryKey: ['dashboard', profileId, year, month] });
      queryClient.invalidateQueries({ queryKey: ['planning', profileId] });
    },
  });

  return (
    <AppLayout>
      <div className="page-stack">
        <section className="page-header">
          <div>
            <p className="eyebrow">Resumen operativo</p>
            <h1>Panel mensual</h1>
            <p className="muted">
              Seguimiento de ingresos, egresos, planificación y riesgo financiero del período.
            </p>
          </div>

          <div className="grid gap-3 md:min-w-[360px]">
            <MonthSelector
              year={year}
              month={month}
              onYearChange={setYear}
              onMonthChange={setMonth}
            />

            <div className="page-actions">
              <Link className="boton-principal" to={`/profiles/${profileId}/planning`}>
                Planificar
              </Link>

              <Link className="boton-secundario" to={`/profiles/${profileId}/transactions`}>
                Cargar movimiento
              </Link>
            </div>
          </div>
        </section>

        {dashboardQuery.isLoading && (
          <EmptyState
            title="Cargando panel"
            message="Estamos obteniendo la información financiera del período."
          />
        )}

        {dashboardQuery.isError && (
          <ErrorState message="No se pudo cargar el panel mensual. Revisá la conexión con el backend o intentá nuevamente." />
        )}

        {dashboardQuery.isSuccess && !canRenderDashboard && (
          <EmptyState
            title="Panel incompleto"
            message="El backend respondió, pero faltan datos operativos o de planificación para construir el panel."
          />
        )}

        {summary && planning && operational && (
          <>
            <OperationalSummaryCards summary={operational} />

            <NextBestActionCard profileId={profileId} summary={summary} />

            <section className="panel">
              <div className="section-title">
                <div>
                  <p className="eyebrow">Carga guiada</p>
                  <h2>Ingresos fijos en 3 pasos</h2>
                </div>
              </div>

              <div className="grid md:grid-cols-2 gap-4">
                <article className="panel-muted">
                  <ol className="space-y-2 text-sm secondary-text">
                    <li><strong>Paso 1:</strong> escribí el nombre del ingreso fijo (ej: sueldo).</li>
                    <li><strong>Paso 2:</strong> definí monto y fecha de inicio.</li>
                    <li><strong>Paso 3:</strong> elegí cuántos períodos querés precargar.</li>
                  </ol>
                  <p className="text-sm muted mt-3">
                    Esto no reemplaza tu flujo actual: solo simplifica la carga repetitiva.
                  </p>
                </article>

                <article className="panel-muted">
                  <div className="grid gap-3">
                    <input
                      className="input"
                      value={incomeTitle}
                      onChange={(event) => setIncomeTitle(event.target.value)}
                      placeholder="Ingreso fijo (ej: Sueldo principal)"
                    />
                    <input
                      className="input"
                      type="number"
                      min="0"
                      step="0.01"
                      value={incomeAmount}
                      onChange={(event) => setIncomeAmount(event.target.value)}
                      placeholder="Monto"
                    />
                    <input
                      className="input"
                      type="date"
                      value={incomeDate}
                      onChange={(event) => setIncomeDate(event.target.value)}
                    />
                    <label className="text-sm secondary-text">
                      Repetir por
                      <input
                        className="input mt-1"
                        type="number"
                        min="1"
                        max="12"
                        value={incomePeriods}
                        onChange={(event) => setIncomePeriods(Number(event.target.value) || 1)}
                      />
                    </label>
                    <button
                      className="boton-principal"
                      type="button"
                      disabled={
                        addFixedIncomeMutation.isPending ||
                        !incomeTitle.trim() ||
                        Number(incomeAmount) <= 0
                      }
                      onClick={() => addFixedIncomeMutation.mutate()}
                    >
                      {addFixedIncomeMutation.isPending
                        ? 'Guardando ingresos...'
                        : `Guardar ${periodLabel}`}
                    </button>
                    {addFixedIncomeMutation.isError && (
                      <p className="mensaje-error text-sm">
                        {getApiErrorMessage(addFixedIncomeMutation.error)}
                      </p>
                    )}
                    {addFixedIncomeMutation.isSuccess && (
                      <p className="text-sm text-emerald-300">
                        Ingresos fijos cargados correctamente.
                      </p>
                    )}
                  </div>
                </article>
              </div>
            </section>

            <OperationalAlerts alerts={operational.alerts} />

            <ConfirmedVsProjectedPanel planning={planning} operational={operational} />

            <section>
              <div className="section-title">
                <div>
                  <p className="eyebrow">Planificación</p>
                  <h2>Estado de ítems del mes</h2>
                </div>
              </div>

              <div className="metric-grid">
                <MetricCard
                  title="Sin cotizar"
                  value={planning.unpricedCount}
                  helper="Ítems que todavía no tienen monto estimado."
                  tone={planning.unpricedCount > 0 ? 'warning' : 'success'}
                />

                <MetricCard
                  title="Próximos 7 días"
                  value={planning.dueNext7DaysCount}
                  helper="Cobros o pagos próximos a vencer."
                  tone={planning.dueNext7DaysCount > 0 ? 'info' : 'neutral'}
                />

                <MetricCard
                  title="Items convertidos"
                  value={planning.convertedItemsCount}
                  helper="Planificaciones ya pasadas a movimiento real."
                  tone="success"
                />

                <MetricCard
                  title="Items cancelados"
                  value={planning.cancelledItemsCount}
                  helper="Ítems descartados del período."
                  tone={planning.cancelledItemsCount > 0 ? 'warning' : 'neutral'}
                />
              </div>
            </section>

            <section className="panel">
              <div className="section-title">
                <div>
                  <p className="eyebrow">Distribución sugerida</p>
                  <h2>Regla 50/30/20</h2>
                </div>
              </div>

              <div className="grid">
                <MetricCard
                  title="Gastos fijos"
                  value={`${summary.fiftyThirtyTwenty?.fixedPercent ?? 0}%`}
                  helper="Peso de gastos fijos sobre el período."
                  tone="warning"
                />

                <MetricCard
                  title="Gastos variables"
                  value={`${summary.fiftyThirtyTwenty?.variablePercent ?? 0}%`}
                  helper="Consumo flexible o no esencial."
                  tone="info"
                />

                <MetricCard
                  title="Ahorro"
                  value={`${summary.fiftyThirtyTwenty?.savingPercent ?? 0}%`}
                  helper="Capacidad de acumulación mensual."
                  tone="success"
                />
              </div>
            </section>

            <DashboardCharts summary={summary} />
          </>
        )}
      </div>
    </AppLayout>
  );
}
