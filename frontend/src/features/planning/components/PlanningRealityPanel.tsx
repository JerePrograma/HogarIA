import { MetricCard } from "../../../components/ui/MetricCard";
import { StatusBadge } from "../../../components/ui/StatusBadge";
import {
  getFinancialExecutionLabel,
  getFinancialExecutionRiskLevel,
  type ClosingProjection,
  type RealConfirmedSummary,
  type RealVsPlannedSummary,
} from "../../../domain/financialSemantics";
import { financialRiskLevelTones } from "../../../domain/financeTones";
import { formatMoney, formatPercent } from "../../../domain/formatters";

type Props = {
  realSummary: RealConfirmedSummary;
  realVsPlanned: RealVsPlannedSummary;
  closingProjection: ClosingProjection;
};

export function PlanningRealityPanel({
  realSummary,
  realVsPlanned,
  closingProjection,
}: Props) {
  const statusRisk = getFinancialExecutionRiskLevel(realVsPlanned.status);

  return (
    <section className="panel">
      <div className="section-title">
        <div>
          <p className="eyebrow">Plan mensual + movimientos</p>
          <h2>Planificado, real, pendiente y estimado</h2>
          <p className="muted">
            El real sale de movimientos confirmados. Lo pendiente sale de ítems
            planificados todavía no convertidos. El estimado combina ambas
            lecturas.
          </p>
        </div>

        <StatusBadge
          tone={financialRiskLevelTones[statusRisk]}
          label={getFinancialExecutionLabel(realVsPlanned.status)}
        />
      </div>

      <div className="metric-grid">
        <MetricCard
          title="Planificado"
          value={formatMoney(realVsPlanned.totalPlanned)}
          helper="Total del plan mensual vigente."
          tone="info"
        />

        <MetricCard
          title="Real confirmado"
          value={formatMoney(realVsPlanned.totalRealConfirmed)}
          helper={`${realSummary.confirmedCount} movimiento${realSummary.confirmedCount === 1 ? "" : "s"} confirmado${realSummary.confirmedCount === 1 ? "" : "s"}.`}
          tone="success"
        />

        <MetricCard
          title="Diferencia"
          value={formatMoney(realVsPlanned.totalDifference)}
          helper={`${formatPercent(realVsPlanned.totalExecutedPercent)} ejecutado del plan.`}
          tone={realVsPlanned.totalDifference > 0 ? "warning" : "neutral"}
        />

        <MetricCard
          title="Estimado de cierre"
          value={formatMoney(closingProjection.estimatedClosing)}
          helper={`Real acumulado ${formatMoney(closingProjection.realAccumulated)} + pendiente ${formatMoney(closingProjection.pendingPlannedNet)}.`}
          primary
          tone={closingProjection.estimatedClosing >= 0 ? "success" : "danger"}
        />
      </div>

      <div className="tabla-ui mt-4">
        <table className="table-compact">
          <thead>
            <tr>
              <th>Categoría</th>
              <th className="amount-cell">Planificado</th>
              <th className="amount-cell">Real confirmado</th>
              <th className="amount-cell">Diferencia</th>
              <th>% ejecutado</th>
              <th>Pendiente</th>
              <th>Estado</th>
            </tr>
          </thead>

          <tbody>
            {realVsPlanned.categories.slice(0, 12).map((item) => {
              const risk = getFinancialExecutionRiskLevel(item.status);

              return (
                <tr key={item.key}>
                  <td>
                    <strong>{item.categoryName}</strong>
                    {item.realUnplannedAmount > 0 ? (
                      <p className="compact-muted">
                        Real no planificado: {formatMoney(item.realUnplannedAmount)}
                      </p>
                    ) : null}
                  </td>
                  <td className="amount-cell">{formatMoney(item.plannedAmount)}</td>
                  <td className="amount-cell">
                    {formatMoney(item.realConfirmedAmount)}
                  </td>
                  <td className="amount-cell">{formatMoney(item.difference)}</td>
                  <td>{formatPercent(item.executedPercent)}</td>
                  <td>{formatMoney(item.pendingPlannedAmount)}</td>
                  <td>
                    <StatusBadge
                      tone={financialRiskLevelTones[risk]}
                      label={getFinancialExecutionLabel(item.status)}
                    />
                  </td>
                </tr>
              );
            })}
          </tbody>
        </table>
      </div>

      {realVsPlanned.categories.length === 0 ? (
        <p className="mensaje-info mt-4">
          Todavía no hay plan ni movimientos confirmados para comparar en este
          período.
        </p>
      ) : null}
    </section>
  );
}
