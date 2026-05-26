import { StatusBadge } from '../../../shared/ui/StatusBadge';
import type { ExternalLoanManualSyncResponse } from '../types';

type ExternalLoanSyncResultPanelProps = {
  title: string;
  result: ExternalLoanManualSyncResponse;
};

export function ExternalLoanSyncResultPanel({
  title,
  result,
}: ExternalLoanSyncResultPanelProps) {
  const hasErrors = result.errors.length > 0;

  return (
    <section className="panel-muted">
      <div className="section-title">
        <div>
          <p className="eyebrow">{result.dryRun ? 'Simulación' : 'Sincronización real'}</p>
          <h3>{title}</h3>
        </div>

        <StatusBadge
          tone={hasErrors ? 'critical' : 'ok'}
          label={hasErrors ? 'Con errores' : 'Correcto'}
        />
      </div>

      <div className="metric-grid">
        <article className="surface-inset">
          <p className="label-ui">Movimientos creados</p>
          <strong>{result.movementsCreated}</strong>
        </article>

        <article className="surface-inset">
          <p className="label-ui">Préstamos sincronizados</p>
          <strong>{result.loansSynced}</strong>
        </article>

        <article className="surface-inset">
          <p className="label-ui">Pagos sincronizados</p>
          <strong>{result.paymentsSynced}</strong>
        </article>

        <article className="surface-inset">
          <p className="label-ui">Duplicados omitidos</p>
          <strong>{result.skippedDuplicates}</strong>
        </article>
      </div>

      <div className="grid mt-4">
        <div className="surface-inset">
          <p className="label-ui">Préstamos detectados</p>
          <p className="mb-0">{result.detectedLoans.join(', ') || 'Ninguno'}</p>
        </div>

        <div className="surface-inset">
          <p className="label-ui">Pagos detectados</p>
          <p className="mb-0">{result.detectedPayments.join(', ') || 'Ninguno'}</p>
        </div>

        <div className="surface-inset">
          <p className="label-ui">Resumen por tipo</p>
          <p className="mb-0">
            Capital prestado: {result.summaryByType.DISBURSEMENT ?? 0} · Capital recuperado:{' '}
            {result.summaryByType.PAYMENT_PRINCIPAL_RECOVERY ?? 0} · Interés ganado:{' '}
            {result.summaryByType.PAYMENT_INTEREST_INCOME ?? 0}
          </p>
        </div>
      </div>

      {hasErrors ? (
        <p className="mensaje-error mt-4">{result.errors.join(' | ')}</p>
      ) : (
        <p className="mensaje-exito mt-4">Sin errores detectados.</p>
      )}
    </section>
  );
}
