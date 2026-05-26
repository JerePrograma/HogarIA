import { useMutation, useQueryClient } from '@tanstack/react-query';
import { useState } from 'react';
import { useParams } from 'react-router-dom';
import { commitBancoProvinciaLoans, previewBancoProvinciaLoans, type BancoProvinciaPreviewCandidate } from '../../../api/bancoProvinciaLoanImportApi';
import { getApiErrorMessage } from '../../../api/http';
import { queryKeys } from '../../../domain/queryKeys';
import { useMonthlyPeriod } from '../../../hooks/useMonthlyPeriod';
import { BancoProvinciaLoanPreviewTable } from './BancoProvinciaLoanPreviewTable';
import { BancoProvinciaLoanSummaryPanel } from './BancoProvinciaLoanSummaryPanel';

export function BancoProvinciaLoanImportPage() {
  const { profileId = '' } = useParams(); const { year, month } = useMonthlyPeriod(); const qc = useQueryClient();
  const [file, setFile] = useState<File | null>(null); const [rows, setRows] = useState<BancoProvinciaPreviewCandidate[]>([]); const [error, setError] = useState<string | null>(null);
  const preview = useMutation({ mutationFn: () => previewBancoProvinciaLoans(profileId, file as File, year, month), onSuccess: (d) => { setRows(d.candidates ?? []); setError(null); }, onError: (e) => setError(getApiErrorMessage(e)) });
  const commit = useMutation({ mutationFn: () => commitBancoProvinciaLoans(profileId, { periodYear: year, periodMonth: month, candidates: rows, skipDuplicates: true, createMonthlyPlanItems: true }), onSuccess: async () => { await Promise.all([qc.invalidateQueries({ queryKey: queryKeys.planning(profileId, year, month) }), qc.invalidateQueries({ queryKey: queryKeys.dashboard(profileId, year, month) }), qc.invalidateQueries({ queryKey: queryKeys.monthlyPlanReconciliation(profileId, year, month) })]); }, onError: (e) => setError(getApiErrorMessage(e)) });
  return <section className='panel'><h2>Importar préstamos Banco Provincia</h2><input type='file' accept='.xlsx,.xls' onChange={e => setFile(e.target.files?.[0] ?? null)} /><button className='boton-principal' disabled={!file || preview.isPending} onClick={() => preview.mutate()}>Previsualizar préstamos</button>{error ? <p role='alert'>{error}</p> : null}{rows.length > 0 ? <><BancoProvinciaLoanSummaryPanel rows={rows} /><BancoProvinciaLoanPreviewTable rows={rows} /><button className='boton-principal' disabled={commit.isPending} onClick={() => commit.mutate()}>Crear compromisos estimados</button></> : null}</section>;
}
