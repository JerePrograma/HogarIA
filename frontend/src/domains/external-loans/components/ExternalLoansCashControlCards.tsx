import { MetricCard } from '../../../shared/ui/MetricCard';
import { formatMoney, formatNumber, formatPercent } from '../../../domain/formatters';
import type { ExternalLoanCashControl } from '../types';

type Props = {
  cashControl: ExternalLoanCashControl;
};

export function ExternalLoansCashControlCards({ cashControl }: Props) {
  return (
    <section>
      <div className="section-title">
        <div>
          <p className="eyebrow">Caja y recupero</p>
          <h2>Control financiero de cartera</h2>
        </div>
      </div>

      <div className="metric-grid">
        <MetricCard
          title="Caja disponible"
          value={formatMoney(cashControl.availableCash)}
          helper="Liquidez disponible según cartera externa."
          tone={cashControl.availableCash >= 0 ? 'success' : 'danger'}
        />

        <MetricCard
          title="Inversión activa"
          value={formatMoney(cashControl.activeInvestment)}
          helper="Capital todavía expuesto."
          tone="info"
        />

        <MetricCard
          title="Capital recuperado"
          value={formatMoney(cashControl.recoveredCapital)}
          helper="Principal ya retornado."
          tone="success"
        />

        <MetricCard
          title="Capital pendiente"
          value={formatMoney(cashControl.pendingCapital)}
          helper="Principal pendiente de recuperar."
          tone={cashControl.pendingCapital > 0 ? 'warning' : 'success'}
        />

        <MetricCard
          title="Ganancia realizada"
          value={formatMoney(cashControl.realizedProfit)}
          helper="Interés efectivamente cobrado."
          tone="success"
        />

        <MetricCard
          title="Ganancia proyectada"
          value={formatMoney(cashControl.projectedProfit)}
          helper="Interés esperado futuro."
          tone="info"
        />

        <MetricCard
          title="Cuotas pendientes"
          value={formatNumber(cashControl.pendingInstallments)}
          helper="Cantidad de cuotas aún no cobradas."
          tone={cashControl.pendingInstallments > 0 ? 'warning' : 'success'}
        />

        <MetricCard
          title="Cuotas próximas 7 días"
          value={formatNumber(cashControl.dueNext7DaysInstallments)}
          helper="Vencimientos inmediatos."
          tone={cashControl.dueNext7DaysInstallments > 0 ? 'warning' : 'neutral'}
        />

        <MetricCard
          title="Cartera en mora"
          value={formatMoney(cashControl.overduePortfolio)}
          helper="Monto vencido o con atraso."
          tone={cashControl.overduePortfolio > 0 ? 'danger' : 'success'}
        />

        <MetricCard
          title="Cobro proyectado a 30 días"
          value={formatMoney(cashControl.projectedCollection30Days)}
          helper="Flujo esperado corto plazo."
          tone="info"
        />

        <MetricCard
          title="Cobro proyectado a 60 días"
          value={formatMoney(cashControl.projectedCollection60Days)}
          helper="Flujo esperado mediano plazo."
          tone="info"
        />

        <MetricCard
          title="Cobro proyectado a 90 días"
          value={formatMoney(cashControl.projectedCollection90Days)}
          helper="Flujo esperado extendido."
          tone="info"
        />

        <MetricCard
          title="Recupero de capital"
          value={formatPercent(cashControl.capitalRecoveryPercentage)}
          helper="Porcentaje de capital recuperado."
          tone={cashControl.capitalRecoveryPercentage >= 80 ? 'success' : 'warning'}
        />

        <MetricCard
          title="Rendimiento esperado"
          value={formatPercent(cashControl.expectedYieldPercentage)}
          helper="Rentabilidad esperada de la cartera."
          tone="success"
        />
      </div>
    </section>
  );
}