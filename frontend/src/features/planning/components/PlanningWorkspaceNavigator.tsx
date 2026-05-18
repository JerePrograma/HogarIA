import type { MonthlyPlanItem } from '../../../domain/types';

export type PlanningWorkspaceView = 'CHECKLIST' | 'CAPTURE' | 'ITEMS' | 'MAPPINGS';

type Props = {
  activeView: PlanningWorkspaceView;
  onChange: (view: PlanningWorkspaceView) => void;
  items: MonthlyPlanItem[];
};

const views: Array<{ key: PlanningWorkspaceView; label: string; description: string }> = [
  { key: 'CHECKLIST', label: 'Checklist', description: 'Control mensual y prioridades' },
  { key: 'CAPTURE', label: 'Captura', description: 'Ingreso rápido + estructurado' },
  { key: 'ITEMS', label: 'Items', description: 'Tabla operativa y acciones' },
  { key: 'MAPPINGS', label: 'Mapeos', description: 'Auditoría de cuenta/categoría' },
];

export function PlanningWorkspaceNavigator({ activeView, onChange, items }: Props) {
  const unmapped = items.filter((item) => !item.accountId || !item.categoryId).length;
  const converted = items.filter((item) => item.status === 'PAID' || item.status === 'COLLECTED').length;
  const pending = items.filter((item) => item.status === 'DRAFT' || item.status === 'ESTIMATED' || item.status === 'SCHEDULED' || item.status === 'DUE').length;

  return (
    <section className="panel panel-soft">
      <div className="planning-workspace-grid" aria-label="Atajos de planificación">
        <button type="button" className="planning-workspace-card" onClick={() => onChange('CHECKLIST')}>
          <strong>Flujo recomendado</strong>
          <span>{pending} ítems pendientes para controlar y priorizar.</span>
        </button>

        <button type="button" className="planning-workspace-card" onClick={() => onChange('CAPTURE')}>
          <strong>Nueva carga</strong>
          <span>Sumá gastos/ingresos rápidos o estructurados en el momento.</span>
        </button>

        <button type="button" className="planning-workspace-card" onClick={() => onChange('ITEMS')}>
          <strong>Operar ítems</strong>
          <span>{converted} ítems convertidos y seguimiento de acciones.</span>
        </button>

        <button type="button" className="planning-workspace-card" onClick={() => onChange('MAPPINGS')}>
          <strong>Auditar mapeos</strong>
          <span>{unmapped} ítems con cuenta/categoría incompleta.</span>
        </button>
      </div>

      <div className="planning-filter-row" role="tablist" aria-label="Secciones de planificación">
        {views.map((view) => (
          <button
            key={view.key}
            type="button"
            role="tab"
            aria-selected={activeView === view.key}
            className={`planning-filter-chip ${activeView === view.key ? 'active' : ''}`}
            onClick={() => onChange(view.key)}
          >
            {view.label}
            {view.key === 'MAPPINGS' && unmapped > 0 ? ` (${unmapped})` : ''}
          </button>
        ))}
      </div>
      <p className="secondary-text">{views.find((view) => view.key === activeView)?.description}</p>
    </section>
  );
}
