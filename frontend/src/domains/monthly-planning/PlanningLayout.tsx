import { Link, Outlet, useParams } from 'react-router-dom';
import { routePaths } from '../../app/router/routePaths';
import { PageHeader, WorkspaceLayout, WorkspaceTabs } from '../../shared/layout';
import { MonthSelector } from '../../shared/ui/MonthSelector';
import { useMonthlyPeriod } from '../../shared/hooks/useMonthlyPeriod';
import { planningRoutes } from './planningRoutes';

export function PlanningLayout() {
  const { profileId = '' } = useParams();
  const { year, month, setYear, setMonth, searchParams } = useMonthlyPeriod();
  const query = `?${searchParams.toString()}`;

  const tabs = [
    { label: 'Inicio', to: `${planningRoutes.root(profileId)}${query}`, end: true },
    { label: 'Mensual', to: `${planningRoutes.monthly(profileId)}${query}` },
    { label: 'Compromisos', to: `${planningRoutes.items(profileId)}${query}` },
    { label: 'Sugerencias', to: `${planningRoutes.suggestions(profileId)}${query}` },
    { label: 'Cargar por texto', to: `${planningRoutes.quickText(profileId)}${query}` },
    { label: 'Alertas', to: `${planningRoutes.alerts(profileId)}${query}` },
    { label: 'Crear movimientos', to: `${planningRoutes.convert(profileId)}${query}` },
    { label: 'Vincular reales', to: `${planningRoutes.reconciliation(profileId)}${query}` },
  ];

  return (
    <WorkspaceLayout className="monthly-planning-page">
      <PageHeader
        eyebrow="Planificación"
        title="Workspace mensual"
        description="Decidí compromisos, revisá alertas, convertí pendientes y reconciliá el plan contra movimientos reales."
        actions={
          <MonthSelector
            year={year}
            month={month}
            onYearChange={setYear}
            onMonthChange={setMonth}
          />
        }
      />

      <WorkspaceTabs items={tabs} ariaLabel="Secciones de planificación mensual" />

      <p className="secondary-text">
        Período {month}/{year} ·{' '}
        <Link to={routePaths.dashboard(profileId)}>Volver a Inicio</Link>
      </p>

      <section className="planning-section-stack">
        <Outlet />
      </section>
    </WorkspaceLayout>
  );
}
