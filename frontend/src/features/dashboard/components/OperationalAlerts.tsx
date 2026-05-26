import { EmptyState } from '../../../components/ui/EmptyState';
import { StatusBadge } from '../../../components/ui/StatusBadge';
import { financialRiskLevelLabels } from '../../../domain/financeLabels';
import { financialRiskLevelTones } from '../../../domain/financeTones';
import { sortOperationalAlerts } from '../../../domain/sorting';
import type { DashboardAlert } from '../../../domain/types';

type Props = {
  alerts: Array<string | DashboardAlert>;
};

export function OperationalAlerts({ alerts }: Props) {
  const sortedAlerts = sortOperationalAlerts(alerts);

  return (
    <section className="panel">
      <div className="section-title">
        <div>
          <p className="eyebrow">Control</p>
          <h2>Alertas operativas</h2>
        </div>

        <span className="badge-count">{sortedAlerts.length}</span>
      </div>

      {sortedAlerts.length === 0 ? (
        <EmptyState
          title="Sin alertas"
          message="No hay alertas operativas para este período."
        />
      ) : (
        <ol className="grid gap-2 p-0 m-0" aria-label="Alertas priorizadas">
          {sortedAlerts.map((alert, index) => {
            const model = normalizeAlert(alert);

            return (
            <li key={`${model.title}-${model.message}-${index}`} className="surface-inset list-none">
              <div className="cluster-ui justify-between">
                <p className="label-ui mb-1">Prioridad {index + 1}</p>
                {model.riskLevel ? (
                  <StatusBadge
                    tone={financialRiskLevelTones[model.riskLevel]}
                    label={financialRiskLevelLabels[model.riskLevel]}
                  />
                ) : null}
              </div>
              <strong>{model.title}</strong>
              <p className="m-0 texto-secundario">{model.message}</p>
            </li>
            );
          })}
        </ol>
      )}
    </section>
  );
}

function normalizeAlert(alert: string | DashboardAlert): {
  title: string;
  message: string;
  riskLevel?: DashboardAlert['riskLevel'];
} {
  if (typeof alert === 'string') {
    return { title: 'Alerta operativa', message: alert };
  }

  return alert;
}
