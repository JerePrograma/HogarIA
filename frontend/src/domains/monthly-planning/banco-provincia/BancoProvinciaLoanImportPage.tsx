import { useMutation, useQueryClient } from '@tanstack/react-query';
import { useState } from 'react';
import { useParams } from 'react-router-dom';
import {
  commitBancoProvinciaLoans,
  previewBancoProvinciaLoans,
  type BancoProvinciaPreviewCandidate,
} from '../../../api/bancoProvinciaLoanImportApi';
import { getApiErrorMessage } from '../../../api/http';
import { queryKeys } from '../../../domain/queryKeys';
import { useMonthlyPeriod } from '../../../shared/hooks/useMonthlyPeriod';
import { BancoProvinciaLoanPreviewTable } from './BancoProvinciaLoanPreviewTable';
import { BancoProvinciaLoanSummaryPanel } from './BancoProvinciaLoanSummaryPanel';

export function BancoProvinciaLoanImportPage() {
  const { profileId = '' } = useParams();
  const { year, month } = useMonthlyPeriod();
  const queryClient = useQueryClient();
  const [file, setFile] = useState<File | null>(null);
  const [rows, setRows] = useState<BancoProvinciaPreviewCandidate[]>([]);
  const [error, setError] = useState<string | null>(null);

  const preview = useMutation({
    mutationFn: () => previewBancoProvinciaLoans(profileId, file as File, year, month),
    onSuccess: (data) => {
      setRows(data.candidates ?? []);
      setError(null);
    },
    onError: (err) => setError(getApiErrorMessage(err)),
  });

  const commit = useMutation({
    mutationFn: () =>
      commitBancoProvinciaLoans(profileId, {
        periodYear: year,
        periodMonth: month,
        candidates: rows,
        skipDuplicates: true,
        createMonthlyPlanItems: true,
      }),
    onSuccess: async () => {
      await Promise.all([
        queryClient.invalidateQueries({
          queryKey: queryKeys.planning(profileId, year, month),
        }),
        queryClient.invalidateQueries({
          queryKey: queryKeys.dashboard(profileId, year, month),
        }),
        queryClient.invalidateQueries({
          queryKey: queryKeys.monthlyPlanReconciliation(profileId, year, month),
        }),
      ]);
    },
    onError: (err) => setError(getApiErrorMessage(err)),
  });

  return (
    <section className="panel">
      <div className="section-title">
        <div>
          <p className="eyebrow">Importación de préstamos</p>
          <h2>Banco Provincia</h2>
          <p className="muted">
            Previsualizá cuotas externas y creá compromisos estimados del período activo.
          </p>
        </div>
      </div>

      <div className="form-actions">
        <input
          type="file"
          accept=".xlsx,.xls"
          onChange={(event) => setFile(event.target.files?.[0] ?? null)}
        />

        <button
          type="button"
          className="boton-principal"
          disabled={!file || preview.isPending}
          onClick={() => preview.mutate()}
        >
          {preview.isPending ? 'Previsualizando...' : 'Previsualizar préstamos'}
        </button>
      </div>

      {error ? <p className="mensaje-error">{error}</p> : null}

      {rows.length > 0 ? (
        <div className="stack-ui">
          <BancoProvinciaLoanSummaryPanel rows={rows} />
          <BancoProvinciaLoanPreviewTable rows={rows} />

          <button
            type="button"
            className="boton-principal"
            disabled={commit.isPending}
            onClick={() => commit.mutate()}
          >
            {commit.isPending ? 'Creando...' : 'Crear compromisos estimados'}
          </button>
        </div>
      ) : null}
    </section>
  );
}
