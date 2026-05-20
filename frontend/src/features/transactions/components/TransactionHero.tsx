import { Link } from 'react-router-dom';
import { MonthSelector } from '../../../components/ui/MonthSelector';
import type { TransactionTotals } from '../hooks/useTransactionTotals';

interface Props {
  profileId: string;
  year: number;
  month: number;
  periodLabel: string;
  transactionsCount: number;
  totals: TransactionTotals;
  onPeriodChange: (year: number, month: number) => void;
  onShiftPeriod: (delta: number) => void;
  onCurrentPeriod: () => void;
}

export function TransactionHero({
  profileId,
  year,
  month,
  periodLabel,
  transactionsCount,
  totals,
  onPeriodChange,
  onShiftPeriod,
  onCurrentPeriod,
}: Props) {
  return (
    <section className="page-header transactions-hero">
      <div className="transactions-hero-copy">
        <p className="eyebrow">Gestión diaria</p>
        <h1>Movimientos</h1>
        <p className="muted">
          Registrá ingresos, gastos, ahorros y ajustes. Esta información alimenta el
          presupuesto, el dashboard y la comparación mensual.
        </p>

        <div className="transactions-period-summary">
          <span className="badge badge-info">{periodLabel}</span>
          <span className="badge badge-muted">
            {transactionsCount} movimiento{transactionsCount === 1 ? '' : 's'}
          </span>
          <span className="badge badge-ok">
            {totals.confirmedCount} confirmado(s)
          </span>
          <span className="badge badge-warning">
            {totals.pendingCount} pendiente(s)
          </span>
          {totals.withoutCategoryCount > 0 ? (
            <span className="badge badge-warning">
              {totals.withoutCategoryCount} sin categoría
            </span>
          ) : null}
          {totals.technicalCount > 0 ? (
            <span className="badge badge-muted">
              {totals.technicalCount} técnico(s)
            </span>
          ) : null}
          {totals.ignoredCount > 0 ? (
            <span className="badge badge-muted">
              {totals.ignoredCount} ignorado(s)
            </span>
          ) : null}
        </div>
      </div>

      <div className="transactions-hero-controls">
        <div className="transactions-actions">
          <Link
            className="boton-secundario"
            to={`/profiles/${profileId}/transactions/import`}
          >
            Importar
          </Link>

          <Link
            className="boton-secundario"
            to={`/profiles/${profileId}/transactions/recategorize`}
          >
            Recategorizar
          </Link>
        </div>

        <div className="transactions-period-card">
          <div className="transactions-period-card-header">
            <span className="label-ui">Período operativo</span>
            <strong>{periodLabel}</strong>
          </div>

          <div className="transactions-period-buttons">
            <button
              type="button"
              className="boton-fantasma"
              onClick={() => onShiftPeriod(-1)}
            >
              ← Anterior
            </button>

            <button
              type="button"
              className="boton-fantasma"
              onClick={onCurrentPeriod}
            >
              Hoy
            </button>

            <button
              type="button"
              className="boton-fantasma"
              onClick={() => onShiftPeriod(1)}
            >
              Siguiente →
            </button>
          </div>

          <MonthSelector
            year={year}
            month={month}
            onYearChange={(nextYear) => onPeriodChange(nextYear, month)}
            onMonthChange={(nextMonth) => onPeriodChange(year, nextMonth)}
          />
        </div>
      </div>
    </section>
  );
}