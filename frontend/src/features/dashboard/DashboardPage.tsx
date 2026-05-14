import { useQuery } from '@tanstack/react-query';
import { useState } from 'react';
import { Link, useParams } from 'react-router-dom';
import { getMonthlyDashboard } from '../../api/dashboardApi';
import { AppLayout } from '../../components/layout/AppLayout';
import { EmptyState } from '../../components/ui/EmptyState';
import { ErrorState } from '../../components/ui/ErrorState';
import { MetricCard } from '../../components/ui/MetricCard';
import { MonthSelector } from '../../components/ui/MonthSelector';
import type { DashboardSummary } from '../../domain/types';
import { ConfirmedVsProjectedPanel } from './components/ConfirmedVsProjectedPanel';
import { DashboardCharts } from './components/DashboardCharts';
import { NextBestActionCard } from './components/NextBestActionCard';
import { OperationalAlerts } from './components/OperationalAlerts';
import { OperationalSummaryCards } from './components/OperationalSummaryCards';

export function DashboardPage() {
  const { profileId = '' } = useParams();

  const today = new Date();
  const [year, setYear] = useState(today.getFullYear());
  const [month, setMonth] = useState(today.getMonth() + 1);

  const dashboardQuery = useQuery<DashboardSummary>({
    queryKey: ['dashboard', profileId, year, month],
    queryFn: () => getMonthlyDashboard(profileId, year, month),
    enabled: Boolean(profileId),
  });

  const summary = dashboardQuery.data;
  const planning = summary?.planningSummary;
  const operational = summary?.operationalSummary;

  const canRenderDashboard = Boolean(summary && planning && operational);

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