import type { ReactNode } from 'react';
import { useQuery } from '@tanstack/react-query';
import { Link, useParams } from 'react-router-dom';
import { getMonthlyDashboard } from '../../api/dashboardApi';
import { AppLayout } from '../../components/layout/AppLayout';
import { EmptyState } from '../../components/ui/EmptyState';
import { ErrorState } from '../../components/ui/ErrorState';
import { MetricCard } from '../../components/ui/MetricCard';
import { MonthSelector } from '../../components/ui/MonthSelector';
import { financialRiskLevelLabels } from '../../domain/financeLabels';
import { queryKeys } from '../../domain/queryKeys';
import type { DashboardSummary } from '../../domain/types';
import { useMonthlyPeriod } from '../../hooks/useMonthlyPeriod';
import { ConfirmedVsProjectedPanel } from './components/ConfirmedVsProjectedPanel';
import { DashboardCharts } from './components/DashboardCharts';
import { NextBestActionCard } from './components/NextBestActionCard';
import { OperationalAlerts } from './components/OperationalAlerts';
import { OperationalSummaryCards } from './components/OperationalSummaryCards';

export function DashboardPage() {
  const { profileId = '' } = useParams();
  const { year, month, setYear, setMonth } = useMonthlyPeriod();

  const dashboardQuery = useQuery<DashboardSummary>({
    queryKey: queryKeys.dashboard(profileId, year, month),
    queryFn: () => getMonthlyDashboard(profileId, year, month),
    enabled: Boolean(profileId),
  });

  const summary = dashboardQuery.data;
  const planning = summary?.planningSummary;
  const operational = summary?.operationalSummary;
  const realSummary = summary?.realConfirmedSummary;
  const realVsPlanned = summary?.realVsPlanned;
  const closingProjection = summary?.closingProjection;
  const dashboardAlerts = summary?.alerts?.length ? summary.alerts : (operational?.alerts ?? []);
  const canRenderDashboard = Boolean(summary && planning && operational);
  const alertCount = dashboardAlerts.length;
  const riskLabel = operational ? financialRiskLevelLabels[operational.financialRiskLevel] : null;

  return (
    <AppLayout>
      <div className="page-stack">
        <section className="page-header items-start">
          <div className="max-w-3xl">
            <p className="eyebrow">Panel mensual</p>
            <h1>Resumen financiero del período</h1>
            <p className="muted">
              Lectura rápida de ingresos, gastos, desvíos y acciones importantes del mes.
            </p>
          </div>

          <div className="grid gap-3 md:min-w-[380px]">
            <MonthSelector year={year} month={month} onYearChange={setYear} onMonthChange={setMonth} />

            <div className="page-actions">
              <Link className="boton-principal" to={`/profiles/${profileId}/transactions`}>
                Cargar movimiento
              </Link>

              <Link className="boton-secundario" to={`/profiles/${profileId}/planning`}>
                Ver planificación
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
            <section className="grid gap-4 xl:grid-cols-[1.4fr_0.6fr]" aria-label="Resumen ejecutivo del período">
              <DashboardIntroCard
                title="Orden de lectura recomendado"
                description="1) Revisá resultado operativo. 2) Verificá alertas. 3) Ajustá desvíos contra planificación."
              />

              <DashboardIntroCard
                title={alertCount > 0 ? `${alertCount} alerta${alertCount === 1 ? '' : 's'} activa${alertCount === 1 ? '' : 's'}` : 'Sin alertas operativas'}
                description={
                  alertCount > 0
                    ? 'Hay puntos que requieren revisión antes de cerrar el mes.'
                    : 'No hay alertas operativas relevantes para este período.'
                }
                status={riskLabel ? `Riesgo actual: ${riskLabel}` : undefined}
              />
            </section>

            <DashboardSection
              eyebrow="Estado del mes"
              title="Resultado operativo"
              description="Lectura ejecutiva con datos reales confirmados y salud financiera del período."
            >
              <OperationalSummaryCards summary={operational} realSummary={realSummary} />
            </DashboardSection>

            <section className="grid gap-4 xl:grid-cols-[0.9fr_1.1fr]">
              <DashboardSection
                eyebrow="Siguiente paso"
                title="Acción recomendada"
                description="La prioridad práctica según el estado actual del mes."
                compact
              >
                <NextBestActionCard profileId={profileId} summary={summary} />
              </DashboardSection>

              <DashboardSection
                eyebrow="Riesgos"
                title="Alertas operativas"
                description="Movimientos, desvíos o señales que conviene revisar."
                compact
              >
                <OperationalAlerts alerts={dashboardAlerts} />
              </DashboardSection>
            </section>

            <DashboardSection
              eyebrow="Control presupuestario"
              title="Planificado vs real"
              description="Comparación entre lo esperado y lo efectivamente confirmado en movimientos."
            >
              <ConfirmedVsProjectedPanel
                planning={planning}
                operational={operational}
                cashFlow={summary.monthlyCashFlowSummary!}
                realVsPlanned={realVsPlanned}
                closingProjection={closingProjection}
              />
            </DashboardSection>

            <DashboardSection
              eyebrow="Distribución sugerida"
              title="Regla 50/30/20"
              description="Una lectura simple de estructura: gastos fijos, consumo variable y capacidad de ahorro."
            >
              <div className="grid gap-4 md:grid-cols-3">
                <MetricCard
                  title="Gastos fijos"
                  value={`${summary.fiftyThirtyTwenty?.fixedPercent ?? 0}%`}
                  helper="Peso de obligaciones y compromisos sobre el período."
                  tone="warning"
                />

                <MetricCard
                  title="Gastos variables"
                  value={`${summary.fiftyThirtyTwenty?.variablePercent ?? 0}%`}
                  helper="Consumo flexible, compras y gastos no estructurales."
                  tone="info"
                />

                <MetricCard
                  title="Ahorro"
                  value={`${summary.fiftyThirtyTwenty?.savingPercent ?? 0}%`}
                  helper="Capacidad real de acumulación mensual."
                  tone="success"
                />
              </div>
            </DashboardSection>

            <DashboardSection
              eyebrow="Análisis visual"
              title="Gráficos del período"
              description="Usalos para detectar concentración de gastos, patrones y desvíos."
            >
              <DashboardCharts summary={summary} />
            </DashboardSection>
          </>
        )}
      </div>
    </AppLayout>
  );
}

function DashboardSection({
  eyebrow,
  title,
  description,
  compact = false,
  children,
}: {
  eyebrow: string;
  title: string;
  description?: string;
  compact?: boolean;
  children: ReactNode;
}) {
  return (
    <section className={`panel ${compact ? 'h-full' : ''}`}>
      <div className="section-title">
        <div>
          <p className="eyebrow">{eyebrow}</p>
          <h2>{title}</h2>
          {description && <p className="muted">{description}</p>}
        </div>
      </div>

      {children}
    </section>
  );
}

function DashboardIntroCard({
  title,
  description,
  status,
}: {
  title: string;
  description: string;
  status?: string;
}) {
  return (
    <article className="panel">
      <p className="eyebrow">Orientación</p>
      <h2>{title}</h2>
      <p className="muted">{description}</p>
      {status ? <p className="label-ui mt-3 mb-0">{status}</p> : null}
    </article>
  );
}

