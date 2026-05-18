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

  return (
    <section className="panel panel-soft">
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
