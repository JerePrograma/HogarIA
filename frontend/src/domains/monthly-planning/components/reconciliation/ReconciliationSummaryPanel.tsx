import { formatMoney } from '../../../../domain/formatters';
import type { MonthlyPlanReconciliationSummary } from './types';

export function ReconciliationSummaryPanel({ summary }: { summary: MonthlyPlanReconciliationSummary }) {
  return (
    <section className="surface-inset mt-4">
      <div className="section-title">
        <div>
          <h3>Resumen operativo</h3>
          <p className="secondary-text">
            Mide cuánto del plan del período ya está vinculado con movimientos reales.
          </p>
        </div>
      </div>

      <div className="planning-items-kpis">
        <span className="badge-ui badge-info">Planificado {formatMoney(summary.plannedTotal)}</span>
        <span className="badge-ui badge-ok">Vinculado {formatMoney(summary.matchedTotal)}</span>
        <span className="badge-ui badge-warning">Pendiente {formatMoney(summary.pendingTotal)}</span>
        <span className="badge-ui badge-info">Reales sin plan {summary.unplannedCount}</span>
        <span className="badge-ui badge-info">Sugerencias {summary.suggestedCount}</span>
      </div>
    </section>
  );
}
