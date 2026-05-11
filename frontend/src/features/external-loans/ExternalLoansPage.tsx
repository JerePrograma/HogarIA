import { useParams } from 'react-router-dom';
import { AppLayout } from '../../components/layout/AppLayout';
import { EmptyState } from '../../components/ui/EmptyState';
import { ErrorState } from '../../components/ui/ErrorState';
import { ExternalLoansActiveTable } from './components/ExternalLoansActiveTable';
import { ExternalLoansCashControlCards } from './components/ExternalLoansCashControlCards';
import { ExternalLoansSummaryCards } from './components/ExternalLoansSummaryCards';
import { useExternalLoansSummary } from './hooks/useExternalLoansSummary';

export function ExternalLoansPage() {
  const { profileId = '' } = useParams();
  const summaryQuery = useExternalLoansSummary(profileId);
  const summary = summaryQuery.data;
  const integrationDisabled = summary?.status === 'DISABLED';

  return (
    <AppLayout>
      <div className='page-stack'>
        <section className='card page-header'>
          <h1>Préstamos externos</h1>
          {summary && <p className='muted'>Estado de integración: {summary.status}</p>}
        </section>

        {summaryQuery.isLoading && <EmptyState message='Cargando resumen de préstamos externos...' />}
        {summaryQuery.isError && <ErrorState message='No se pudo cargar el resumen externo de préstamos.' />}
        {integrationDisabled && <EmptyState message='La integración de préstamos externos está deshabilitada para este perfil.' />}

        {summary && !integrationDisabled && (
          <>
            <ExternalLoansSummaryCards dashboard={summary.dashboard} />
            <ExternalLoansCashControlCards cashControl={summary.cashControl} />
            <ExternalLoansActiveTable loans={summary.activeLoans} />
          </>
        )}
      </div>
    </AppLayout>
  );
}
