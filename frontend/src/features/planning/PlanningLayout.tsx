import { Link, NavLink, Outlet, useParams } from 'react-router-dom';
import { AppLayout } from '../../components/layout/AppLayout';
import { MonthSelector } from '../../components/ui/MonthSelector';
import { useMonthlyPeriod } from '../../hooks/useMonthlyPeriod';
import { planningRoutes } from './planningRoutes';

export function PlanningLayout() {
  const { profileId = '' } = useParams();
  const { year, month, setYear, setMonth, searchParams } = useMonthlyPeriod();
  const query = `?${searchParams.toString()}`;

  const tabs = [
    ['Workspace', planningRoutes.root(profileId)],
    ['Mensual', planningRoutes.monthly(profileId)],
    ['Ítems', planningRoutes.items(profileId)],
    ['Importar', planningRoutes.import(profileId)],
    ['Alertas', planningRoutes.alerts(profileId)],
    ['Convertir', planningRoutes.convert(profileId)],
    ['Conciliar', planningRoutes.reconciliation(profileId)],
  ] as const;

  return (
    <AppLayout>
      <div className="page-stack monthly-planning-page">
        <section className="page-header planning-hero">
          <div><p className="eyebrow">Planificación</p><h1>Workspace mensual</h1></div>
          <MonthSelector year={year} month={month} onYearChange={setYear} onMonthChange={setMonth} />
        </section>
        <nav className="planning-filter-row">
          {tabs.map(([label, to]) => (
            <NavLink key={to} to={`${to}${query}`} className={({isActive}) => `planning-filter-chip ${isActive ? 'active':''}`} end={to.endsWith('/planning')}>
              {label}
            </NavLink>
          ))}
        </nav>
        <p className="secondary-text">Período {month}/{year} · <Link to={`/profiles/${profileId}/dashboard`}>Volver a dashboard</Link></p>
        <Outlet />
      </div>
    </AppLayout>
  );
}
