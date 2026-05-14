import { EmptyState } from '../../../components/ui/EmptyState';

type Props = {
  alerts: string[];
};

export function OperationalAlerts({ alerts }: Props) {
  return (
    <section className="panel">
      <div className="section-title">
        <div>
          <p className="eyebrow">Control</p>
          <h2>Alertas operativas</h2>
        </div>

        <span className="badge-count">{alerts.length}</span>
      </div>

      {alerts.length === 0 ? (
        <EmptyState
          title="Sin alertas"
          message="No hay alertas operativas para este período."
        />
      ) : (
        <ul className="grid gap-2 p-0">
          {alerts.map((alert) => (
            <li key={alert} className="surface-inset list-none">
              {alert}
            </li>
          ))}
        </ul>
      )}
    </section>
  );
}