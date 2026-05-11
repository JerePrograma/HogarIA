import { EmptyState } from '../../../components/ui/EmptyState';
import { formatMoney } from '../../../domain/formatters';
import type { ExternalLoan } from '../types';

type Props = { loans: ExternalLoan[] };

export function ExternalLoansActiveTable({ loans }: Props) {
  if (!loans.length) return <EmptyState message='No hay préstamos activos.' />;

  return (
    <section className='card'>
      <h2 className='section-title'>Préstamos activos</h2>
      <div className='table-wrapper'>
        <table className='table table-compact'>
          <thead>
            <tr>
              <th>Prestatario</th>
              <th>Estado</th>
              <th className='amount-cell'>Capital</th>
              <th className='amount-cell'>Cobrado</th>
              <th className='amount-cell'>Pendiente</th>
              <th className='amount-cell'>Ganancia proyectada</th>
            </tr>
          </thead>
          <tbody>
            {loans.map((loan) => (
              <tr key={loan.externalLoanId}>
                <td>{loan.borrowerName}</td>
                <td>{loan.status}</td>
                <td className='amount-cell'>{formatMoney(loan.principalAmount)}</td>
                <td className='amount-cell'>{formatMoney(loan.totalCollected)}</td>
                <td className='amount-cell'>{formatMoney(loan.totalPending)}</td>
                <td className='amount-cell'>{formatMoney(loan.projectedProfit)}</td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </section>
  );
}
