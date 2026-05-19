import { useMemo, useState } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useParams } from 'react-router-dom';
import { listAccounts } from '../../api/accountsApi';
import { listCategories } from '../../api/categoriesApi';
import { commitTransactionImport, previewTransactionImport } from '../../api/transactionImportsApi';
import { getApiErrorMessage } from '../../api/http';
import { AppLayout } from '../../components/layout/AppLayout';
import { EmptyState } from '../../components/ui/EmptyState';
import { ErrorState } from '../../components/ui/ErrorState';
import { ImportCommitPanel } from './imports/ImportCommitPanel';
import { ImportPreviewSummary } from './imports/ImportPreviewSummary';
import { ImportResultPanel } from './imports/ImportResultPanel';
import { ImportRowsTable } from './imports/ImportRowsTable';
import { ImportSourceForm } from './imports/ImportSourceForm';
import type { TransactionImportCommitPayload, TransactionImportCommitResult, TransactionImportPreview, TransactionImportRow, TransactionImportSource } from './imports/types';

export function TransactionImportPage() {
  const { profileId = '' } = useParams();
  const qc = useQueryClient();
  const [source, setSource] = useState<TransactionImportSource>('BANCO_PROVINCIA');
  const [accountId, setAccountId] = useState('');
  const [file, setFile] = useState<File | null>(null);
  const [preview, setPreview] = useState<TransactionImportPreview | null>(null);
  const [rows, setRows] = useState<TransactionImportRow[]>([]);
  const [createMissingFallbackCategory] = useState(true);

  const accountsQuery = useQuery({ queryKey: ['accounts', profileId], queryFn: () => listAccounts(profileId), enabled: Boolean(profileId) });
  const categoriesQuery = useQuery({ queryKey: ['categories', profileId], queryFn: () => listCategories(profileId, true), enabled: Boolean(profileId) });
  const previewMutation = useMutation({ mutationFn: () => previewTransactionImport(profileId, source, accountId, file!), onSuccess: (data) => { setPreview(data); setRows(data.rows ?? []); } });
  const unresolvedRows = useMemo(() => rows.filter((r) => r.status === 'NEEDS_CATEGORY' && !r.suggestedCategoryId && !createMissingFallbackCategory).length, [rows, createMissingFallbackCategory]);
  const importableRows = useMemo(() => rows.filter((r) => (r.status === 'READY' || r.status === 'NEEDS_CATEGORY') && (createMissingFallbackCategory || !!r.suggestedCategoryId)).length, [rows, createMissingFallbackCategory]);
  const invalidRows = useMemo(() => rows.filter((r) => r.status === 'ERROR').length, [rows]);
  const ignoredRows = useMemo(() => rows.filter((r) => r.status === 'SKIPPED').length, [rows]);

  const commitMutation = useMutation({
    mutationFn: () => {
      const payload: TransactionImportCommitPayload = { rows: rows.map((row) => ({ rowNumber: row.rowNumber, categoryId: row.suggestedCategoryId, accountId, movementType: row.movementType, amount: row.amount, status: row.status, description: row.rawDescription ?? row.normalizedDescription })), createMissingFallbackCategory, skipDuplicates: true };
      return commitTransactionImport(profileId, preview!.batchId, payload);
    },
    onSuccess: async () => {
      await Promise.all([
        qc.invalidateQueries({ queryKey: ['transactions', profileId] }),
        qc.invalidateQueries({ queryKey: ['tx', profileId] }),
        qc.invalidateQueries({ queryKey: ['categories', profileId] }),
        qc.invalidateQueries({ queryKey: ['dashboard', profileId] }),
        qc.invalidateQueries({ queryKey: ['budget-comp', profileId] }),
      ]);
    }
  });

  const canPreview = Boolean(file && accountId && !previewMutation.isPending);
  const canCommit = Boolean(preview?.batchId && importableRows > 0 && unresolvedRows === 0 && !commitMutation.isPending);

  return <AppLayout><div className="page-stack"><section className="page-header"><div><p className="eyebrow">Movimientos</p><h1>Importación guiada</h1></div></section>
    <ImportSourceForm source={source} accountId={accountId} file={file} accounts={accountsQuery.data ?? []} onSourceChange={setSource} onAccountChange={setAccountId} onFileChange={setFile} onPreview={() => previewMutation.mutate()} canPreview={canPreview} previewLoading={previewMutation.isPending} />
    {previewMutation.isError ? <ErrorState message={getApiErrorMessage(previewMutation.error)} /> : null}
    {preview ? <section className="panel stack-ui" aria-live="polite"><h2>Paso 2: Revisar y confirmar</h2><ImportPreviewSummary totalRows={preview.totalRows} importableRows={importableRows} duplicateRows={preview.duplicateRows} invalidRows={invalidRows} ignoredRows={ignoredRows} />
      {!importableRows ? <EmptyState title="No hay filas importables" message="Corregí categorías, tipos o volvé a subir el archivo con datos válidos." /> : null}
      <ImportRowsTable rows={rows} categories={categoriesQuery.data ?? []} onRowsChange={setRows} />
      <ImportCommitPanel canCommit={canCommit} pending={commitMutation.isPending} hasMissingCategory={unresolvedRows > 0} onCommit={() => commitMutation.mutate()} />
      {commitMutation.isError ? <ErrorState message={getApiErrorMessage(commitMutation.error)} /> : null}
    </section> : null}
    {commitMutation.data ? <ImportResultPanel result={commitMutation.data as TransactionImportCommitResult} profileId={profileId} /> : null}
  </div></AppLayout>;
}
