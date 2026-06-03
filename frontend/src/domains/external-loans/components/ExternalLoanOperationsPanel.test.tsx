import '@testing-library/jest-dom/vitest';

import { cleanup, fireEvent, render, screen } from '@testing-library/react';
import type { ComponentProps } from 'react';
import { afterEach, describe, expect, it, vi } from 'vitest';
import { ExternalLoanOperationsPanel } from './ExternalLoanOperationsPanel';

type Props = ComponentProps<typeof ExternalLoanOperationsPanel>;

const syncResult = {
  dryRun: true,
  loansSynced: 1,
  paymentsSynced: 1,
  movementsCreated: 3,
  skippedDuplicates: 2,
  detectedExistingWithoutMapping: 0,
  backfillRecommended: false,
  errors: [],
  detectedLoans: ['10'],
  detectedPayments: ['20'],
  plannedMovements: ['DISBURSEMENT loan 10 amount=1000'],
  summaryByType: {
    DISBURSEMENT: 1,
    PAYMENT_PRINCIPAL_RECOVERY: 1,
    PAYMENT_INTEREST_INCOME: 1,
  },
};

const diagnostics = {
  cjTransactions: 10,
  mappedTransactions: 8,
  unmappedCandidates: 2,
  backfillRecommended: true,
  canRunSync: true,
  hasIndexBlockingDuplicates: false,
  requiresManualReview: false,
  candidateCountsByConfidence: {
    HIGH: 1,
    MEDIUM: 1,
    LOW: 0,
  },
  wouldCreateMappings: 2,
  alreadyMappedEvents: 8,
  alreadyMappedTransactions: 0,
  duplicateSourceOperationGroups: [],
  duplicateSourceHashGroups: [],
};

function renderPanel(overrides: Partial<Props> = {}) {
  const props: Props = {
    hasSyncConfig: true,
    canSync: true,
    readOnlyMode: false,
    configEnabled: true,
    missingConfigItems: [],
    onAnalyzeSync: vi.fn(),
    onSync: vi.fn(),
    onDiagnose: vi.fn(),
    onAnalyzeBackfill: vi.fn(),
    onApplyBackfill: vi.fn(),
    ...overrides,
  };

  render(<ExternalLoanOperationsPanel {...props} />);
  return props;
}

describe('ExternalLoanOperationsPanel', () => {
  afterEach(() => {
    cleanup();
  });

  it('renders the diagnostics panel', () => {
    renderPanel({ diagnostics });

    expect(
      screen.getByRole('heading', { name: 'CJPrestamos - Sincronizacion contable' }),
    ).toBeInTheDocument();
    expect(screen.getByRole('heading', { name: 'Idempotency diagnostics' })).toBeInTheDocument();
    expect(screen.getByText('CJ transactions')).toBeInTheDocument();
  });

  it('clicking Analizar sync calls the dry-run action and renders planned/skipped counts', () => {
    const onAnalyzeSync = vi.fn();
    renderPanel({ dryRunResult: syncResult, onAnalyzeSync });

    fireEvent.click(screen.getByRole('button', { name: 'Analizar sync' }));

    expect(onAnalyzeSync).toHaveBeenCalledTimes(1);
    expect(screen.getByText('Movimientos creados')).toBeInTheDocument();
    expect(screen.getByText('Duplicados omitidos')).toBeInTheDocument();
    expect(screen.getByText('3')).toBeInTheDocument();
    expect(screen.getByText('2')).toBeInTheDocument();
  });

  it('shows a warning when backfill is recommended', () => {
    renderPanel({ diagnostics });

    expect(
      screen.getByText('Backfill recomendado: ejecutar dry-run de backfill antes de sincronizar.'),
    ).toBeInTheDocument();
  });

  it('disables sync when index-blocking duplicates exist', () => {
    renderPanel({
      diagnostics: {
        ...diagnostics,
        canRunSync: false,
        hasIndexBlockingDuplicates: true,
        duplicateSourceOperationGroups: [
          {
            profileId: 'profile-1',
            source: 'CJPRESTAMOS',
            sourceOperationId: 'LOAN:1:DISBURSEMENT',
            count: 2,
            transactions: [],
          },
        ],
      },
    });

    expect(screen.getByRole('button', { name: 'Ejecutar sync' })).toBeDisabled();
    expect(screen.getAllByText('Resolver duplicados antes de sincronizar.').length).toBeGreaterThan(0);
  });

  it('disables concurrent operational actions while dry-run is pending', () => {
    renderPanel({ dryRunPending: true });

    expect(screen.getByRole('button', { name: 'Analizando...' })).toBeDisabled();
    expect(screen.getByRole('button', { name: 'Ejecutar sync' })).toBeDisabled();
    expect(screen.getByRole('button', { name: 'Diagnosticar idempotencia' })).toBeDisabled();
    expect(screen.getByRole('button', { name: 'Analizar backfill' })).toBeDisabled();
  });

  it('applying safe backfill sends includeLowConfidence=false', () => {
    const onApplyBackfill = vi.fn();
    renderPanel({
      onApplyBackfill,
      backfillDryRun: {
        candidates: [
          {
            transactionId: 'tx-1',
            realDate: '2026-05-10',
            description: 'Prestamo CJ #1',
            amount: 1000,
            inferredEntityType: 'LOAN',
            inferredEntityId: '1',
            inferredEventType: 'DISBURSEMENT',
            confidence: 'HIGH',
            warning: null,
            wouldCreateMapping: true,
          },
        ],
      },
    });

    fireEvent.click(screen.getByRole('button', { name: 'Aplicar backfill seguro' }));

    expect(onApplyBackfill).toHaveBeenCalledWith(false);
  });
});
