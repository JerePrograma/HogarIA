import { useQuery } from '@tanstack/react-query';
import type { ReactNode } from 'react';
import { Link, useParams } from 'react-router-dom';
import { getMonthlyDashboard } from '../../api/dashboardApi';
import { routePaths } from '../../app/router/routePaths';
import { AppShell } from '../../app/shell/AppShell';
import { financialRiskLevelLabels } from '../../domain/financeLabels';
import { formatMoney } from '../../domain/formatters';
import { queryKeys } from '../../domain/queryKeys';
import type { DashboardSummary } from '../../domain/types';
import { useMonthlyPeriod } from '../../shared/hooks/useMonthlyPeriod';
import { Page, PageHeader, PageSection } from '../../shared/layout';
import { Button, EmptyState, ErrorState, MonthSelector } from '../../shared/ui';
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
  const cashFlow = summary?.monthlyCashFlowSummary;
  const dashboardAlerts = summary?.alerts?.length
    ? summary.alerts
    : (operational?.alerts ?? []);
  const canRenderDashboard = Boolean(summary && planning && operational);

  return (
    <AppShell>
      <Page>
        <PageHeader
          eyebrow="Inicio"
          title="Situación actual"
          description="Lectura ejecutiva del mes: qué está pasando, qué requiere atención y a dónde conviene entrar."
          actions={
            <div className="grid gap-3 md:min-w-[380px]">
              <MonthSelector
                year={year}
                month={month}
                onYearChange={setYear}
                onMonthChange={setMonth}
              />

              <div className="page-actions">
                <Button to={routePaths.transactions(profileId)}>
                  Revisar movimientos
                </Button>

                <Button tone="secondary" to={routePaths.planning(profileId)}>
                  Ir a planificación
                </Button>
              </div>
              <div className="page-actions">
                <Button tone="secondary" to={`${routePaths.planning(profileId)}/monthly`}>
                  Planificar próximo mes
                </Button>
                <Button tone="secondary" to={`${routePaths.planning(profileId)}/monthly/external-debts/banco-provincia`}>
                  Revisar cuotas futuras
                </Button>
                <Button tone="secondary" to={`${routePaths.transactions(profileId)}?onlyWithoutCategory=true`}>
                  Completar sin categoría
                </Button>
                <Button tone="secondary" to={`${routePaths.transactions(profileId)}?onlyInternalTransfers=true`}>
                  Resolver transferencias
                </Button>
              </div>
            </div>
          }
        />

        {dashboardQuery.isLoading ? (
          <EmptyState
            title="Cargando inicio"
            message="Estamos obteniendo la información financiera del período."
          />
        ) : null}

        {dashboardQuery.isError ? (
          <ErrorState message="No se pudo cargar el inicio. Revisá la conexión con el backend o intentá nuevamente." />
        ) : null}

        {dashboardQuery.isSuccess && !canRenderDashboard ? (
          <EmptyState
            title="Inicio incompleto"
            message="El backend respondió, pero faltan datos operativos o de planificación para construir la lectura mensual."
          />
        ) : null}

        {summary && planning && operational ? (
          <>
            <PageSection>
              <OperationalSummaryCards
                summary={operational}
                realSummary={realSummary}
              />
            </PageSection>

            <section className="grid gap-4 xl:grid-cols-[0.85fr_1.15fr]">
              <NextBestActionCard profileId={profileId} summary={summary} />
              <OperationalAlerts alerts={dashboardAlerts} />
            </section>

            <section className="grid gap-4 xl:grid-cols-4">
              <CompactSnapshot
                title="Resumen de presupuesto"
                value={formatMoney(summary.budgetSummary?.totalDifference ?? 0)}
                helper={`${summary.budgetSummary?.exceededCount ?? 0} exceso(s), ${summary.budgetSummary?.warningCount ?? 0} advertencia(s).`}
                actionLabel="Ajustar presupuesto"
                to={routePaths.budgets(profileId)}
              />

              <CompactSnapshot
                title="Resumen de planificación"
                value={formatMoney(planning.projectedNetMax)}
                helper={`${planning.plannedItemsCount} compromiso(s), ${planning.unpricedCount} sin cotizar.`}
                actionLabel="Ir a planificación"
                to={routePaths.planning(profileId)}
              />

              <CompactSnapshot
                title="Resumen de movimientos"
                value={realSummary?.confirmedCount ?? 0}
                helper={`${realSummary?.pendingCount ?? 0} pendiente(s), ${realSummary?.withoutCategoryCount ?? 0} sin categoría.`}
                actionLabel="Revisar movimientos"
                to={routePaths.transactions(profileId)}
              />

              <CompactSnapshot
                title="Préstamos externos"
                value="Seguimiento"
                helper="Consulta cartera, recuperos y sincronización cuando corresponda."
                actionLabel="Ver préstamos"
                to={routePaths.externalLoans(profileId)}
              />
            </section>

            {cashFlow ? (
              <section aria-label="Planificado vs real">
                <div className="section-title">
                  <div>
                    <p className="eyebrow">Planificado vs real</p>
                    <h2>Estado operativo del mes</h2>
                    <p className="muted">
                      Riesgo actual: {financialRiskLevelLabels[operational.financialRiskLevel]}. Esta lectura resume desvíos; el detalle vive en planificación y presupuesto.
                    </p>
                  </div>
                </div>
                <ConfirmedVsProjectedPanel
                  planning={planning}
                  operational={operational}
                  cashFlow={cashFlow}
                  realSummary={realSummary}
                  realVsPlanned={realVsPlanned}
                  closingProjection={closingProjection}
                />
              </section>
            ) : null}

            <section aria-label="Vista previa de gráficos">
              <div className="section-title">
                <div>
                  <p className="eyebrow">Análisis compacto</p>
                  <h2>Vista previa de gráficos</h2>
                  <p className="muted">
                    Señales visuales del período. Para decisiones finas, entrá a movimientos, presupuesto o planificación.
                  </p>
                </div>
              </div>
              <DashboardCharts summary={summary} />
            </section>
          </>
        ) : null}
      </Page>
    </AppShell>
  );
}

function CompactSnapshot({
  title,
  value,
  helper,
  actionLabel,
  to,
}: {
  title: string;
  value: ReactNode;
  helper: string;
  actionLabel: string;
  to: string;
}) {
  return (
    <article className="panel-muted">
      <p className="label-ui">{title}</p>
      <strong className="block text-2xl">{value}</strong>
      <p className="muted">{helper}</p>
      <Link className="boton-secundario mt-3" to={to}>
        {actionLabel}
      </Link>
    </article>
  );
}
