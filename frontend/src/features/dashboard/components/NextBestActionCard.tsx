import { Link } from 'react-router-dom';
import type { DashboardSummary } from '../../../domain/types';

type Props = { profileId: string; summary: DashboardSummary };

export function NextBestActionCard({ profileId, summary }: Props) {
  const planning = summary.planningSummary;
  const operational = summary.operationalSummary;
  if (!planning || !operational) return null;

  let message = 'El mes está ordenado. Revisá movimientos o cargá novedades.';
  let cta: string | null = null;
  let href: string | null = null;
  if (planning.unpricedCount > 0) {
    message = `Tenés ${planning.unpricedCount} ítems sin cotizar. Completalos para mejorar la proyección.`;
    cta = 'Ir a planificación';
    href = `/profiles/${profileId}/planning`;
  } else if (planning.dueNext7DaysCount > 0) {
    message = `Hay ${planning.dueNext7DaysCount} vencimientos/cobros próximos.`;
  } else if (planning.pendingExpense > 0) {
    message = `Hay ${planning.pendingExpense} pagos pendientes.`;
  } else if (planning.pendingIncome > 0) {
    message = `Hay ${planning.pendingIncome} cobros pendientes.`;
  } else if (operational.alerts.length > 0) {
    message = operational.alerts[0];
  }

  return <section className='card next-action-card'><h3 className='section-title'>Siguiente mejor acción</h3><p>{message}</p>{cta && href ? <Link to={href} className='button-primary'>{cta}</Link> : null}</section>;
}
