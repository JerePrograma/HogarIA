import { MetricCard } from "../../../shared/ui/MetricCard";
import { StatusBadge } from "../../../shared/ui/StatusBadge";
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
  const comparableRealCount = realVsPlanned.categories.reduce(
    (total, item) => total + item.realCount,
    0,
  );
  return (
    <section className="panel">
      <div className="section-title">
        <div>
          <p className="eyebrow">Plan mensual + movimientos</p>
          <h2>Planificado, real, pendiente y estimado</h2>
          <p className="muted">
            La comparación toma egresos, ahorros y deudas confirmadas contra el
            plan. Ingresos, recuperos, transferencias y ajustes no se evalúan
            como excesos. El estimado de cierre usa el balance operativo
            completo.
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
          title="Egresos reales"
          value={formatMoney(realVsPlanned.totalRealConfirmed)}
          helper={`${comparableRealCount} egreso${comparableRealCount === 1 ? "" : "s"} comparable${comparableRealCount === 1 ? "" : "s"} contra el plan.`}
          tone="warning"
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

      <div className="transactions-filter-grid mt-4">
        <RealityBreakdownItem
          label="Egresos operativos reales"
          value={formatMoney(realSummary.operationalOutflows)}
          helper="Gastos, ahorro, inversión y deuda confirmados."
        />
        <RealityBreakdownItem
          label="Excluidos por transferencia interna"
          value={formatMoney(realSummary.excludedInternalTransferAmount)}
          helper={`${realSummary.excludedInternalTransferCount} movimiento${realSummary.excludedInternalTransferCount === 1 ? "" : "s"} neutral${realSummary.excludedInternalTransferCount === 1 ? "" : "es"}.`}
        />
        <RealityBreakdownItem
          label="Excluidos por duplicado"
          value={formatMoney(realSummary.excludedDuplicateAmount)}
          helper={`${realSummary.excludedDuplicateCount} movimiento${realSummary.excludedDuplicateCount === 1 ? "" : "s"} cross-source o exacto.`}
        />
        <RealityBreakdownItem
          label="En revisión"
          value={formatMoney(realSummary.reviewAmount)}
          helper={`${realSummary.reviewCount} movimiento${realSummary.reviewCount === 1 ? "" : "s"} con clasificación pendiente.`}
        />
        <RealityBreakdownItem
          label="Neto operativo"
          value={formatMoney(realSummary.operationalBalance)}
          helper="Resultado confirmado sin duplicados ni transferencias internas."
        />
      </div>

      <div className="tabla-ui mt-4">
        <table className="table-compact">
          <thead>
            <tr>
              <th>Categoría</th>
              <th className="amount-cell">Planificado</th>
              <th className="amount-cell">Egreso real</th>
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
                        Real no planificado:{" "}
                        {formatMoney(item.realUnplannedAmount)}
                      </p>
                    ) : null}
                  </td>
                  <td className="amount-cell">
                    {formatMoney(item.plannedAmount)}
                  </td>
                  <td className="amount-cell">
                    {formatMoney(item.realConfirmedAmount)}
                  </td>
                  <td className="amount-cell">
                    {formatMoney(item.difference)}
                  </td>
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

function RealityBreakdownItem({
  label,
  value,
  helper,
}: {
  label: string;
  value: string;
  helper: string;
}) {
  return (
    <div className="surface-inset">
      <p className="label-ui">{label}</p>
      <strong>{value}</strong>
      <p className="compact-muted">{helper}</p>
    </div>
  );
}
