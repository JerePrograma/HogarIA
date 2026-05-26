import { EmptyState } from '../../../shared/ui/EmptyState';
import { StatusBadge } from '../../../shared/ui/StatusBadge';
import { formatMoney } from '../../../domain/formatters';
import type { ExternalLoan } from '../types';

type Props = {
  loans: ExternalLoan[];
};

const getLoanStatusTone = (status: string): 'ok' | 'watch' | 'risk' | 'critical' | 'neutral' => {
  const normalized = status.toUpperCase();

  if (['ACTIVE', 'CURRENT', 'OK', 'AL_DIA', 'AL DÍA'].includes(normalized)) return 'ok';
  if (['PENDING', 'DUE', 'POR_VENCER'].includes(normalized)) return 'watch';
  if (['OVERDUE', 'LATE', 'MORA', 'ATRASADO'].includes(normalized)) return 'risk';
  if (['DEFAULTED', 'CANCELLED', 'CANCELADO', 'CRITICAL'].includes(normalized)) return 'critical';

  return 'neutral';
};

export function ExternalLoansActiveTable({ loans }: Props) {
  if (!loans.length) {
    return <EmptyState title="Sin préstamos activos" message="No hay préstamos activos para mostrar." />;
  }

  return (
    <section className="panel">
      <div className="section-title">
        <div>
          <p className="eyebrow">Detalle</p>
          <h2>Préstamos activos</h2>
        </div>

        <span className="badge-count">{loans.length}</span>
      </div>

      <div className="tabla-ui">
        <table className="table-compact">
          <thead>
            <tr>
              <th>Prestatario</th>
              <th>Estado</th>
              <th className="amount-cell">Capital</th>
              <th className="amount-cell">Cobrado</th>
              <th className="amount-cell">Pendiente</th>
              <th className="amount-cell">Ganancia realizada</th>
              <th className="amount-cell">Ganancia proyectada</th>
            </tr>
          </thead>

          <tbody>
            {loans.map((loan) => (
              <tr key={loan.externalLoanId}>
                <td>
                  <strong>{loan.borrowerName}</strong>
                  <p className="compact-muted">ID externo: {loan.externalLoanId}</p>
                </td>

                <td>
                  <StatusBadge tone={getLoanStatusTone(loan.status)} label={loan.status} />
                </td>

                <td className="amount-cell">{formatMoney(loan.principalAmount)}</td>
                <td className="amount-cell">{formatMoney(loan.totalCollected)}</td>
                <td className="amount-cell">{formatMoney(loan.totalPending)}</td>
                <td className="amount-cell">{formatMoney(loan.realizedProfit)}</td>
                <td className="amount-cell">{formatMoney(loan.projectedProfit)}</td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </section>
  );
}