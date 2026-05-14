import { Link } from 'react-router-dom';
import { formatMoney } from '../../../domain/formatters';
import type { DashboardSummary } from '../../../domain/types';

type Props = {
  profileId: string;
  summary: DashboardSummary;
};

export function NextBestActionCard({ profileId, summary }: Props) {
  const planning = summary.planningSummary;
  const operational = summary.operationalSummary;

  if (!planning || !operational) {
    return null;
  }

  let title = 'Mes bajo control';
  let message = 'El mes está ordenado. Revisá movimientos o cargá novedades.';
  let cta: string | null = 'Cargar movimiento';
  let href: string | null = `/profiles/${profileId}/transactions`;
  let toneClass = 'panel-accent';

  if (planning.unpricedCount > 0) {
    title = 'Faltan cotizaciones';
    message = `Tenés ${planning.unpricedCount} ítems sin cotizar. Completalos para mejorar la proyección.`;
    cta = 'Ir a planificación';
    href = `/profiles/${profileId}/planning`;
    toneClass = 'panel-accent';
  } else if (planning.dueNext7DaysCount > 0) {
    title = 'Vencimientos próximos';
    message = `Hay ${planning.dueNext7DaysCount} vencimientos o cobros próximos en los próximos 7 días.`;
    cta = 'Revisar planificación';
    href = `/profiles/${profileId}/planning`;
    toneClass = 'panel';
  } else if (planning.pendingExpense > 0) {
    title = 'Pagos pendientes';
    message = `Hay ${formatMoney(planning.pendingExpense)} en pagos pendientes.`;
    cta = 'Ver planificación';
    href = `/profiles/${profileId}/planning`;
    toneClass = 'panel';
  } else if (planning.pendingIncome > 0) {
    title = 'Cobros pendientes';
    message = `Hay ${formatMoney(planning.pendingIncome)} en cobros pendientes.`;
    cta = 'Ver planificación';
    href = `/profiles/${profileId}/planning`;
    toneClass = 'panel';
  } else if (operational.alerts.length > 0) {
    title = 'Primera alerta operativa';
    message = operational.alerts[0];
    cta = null;
    href = null;
    toneClass = 'panel';
  }

  return (
    <section className={`${toneClass} next-action-card`}>
      <div className="section-title">
        <div>
          <p className="eyebrow">Prioridad sugerida</p>
          <h2>Siguiente mejor acción</h2>
        </div>
      </div>

      <h3 className="mb-2 mt-0 text-xl font-semibold">{title}</h3>
      <p className="texto-secundario">{message}</p>

      {cta && href ? (
        <Link to={href} className="boton-principal mt-3">
          {cta}
        </Link>
      ) : null}
    </section>
  );
}