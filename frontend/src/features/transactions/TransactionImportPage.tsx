import { useMemo, useState } from 'react';
import { useMutation, useQuery } from '@tanstack/react-query';
import { useParams } from 'react-router-dom';
import { listAccounts } from '../../api/accountsApi';
import { listCategories } from '../../api/categoriesApi';
import { commitTransactionImport, previewTransactionImport } from '../../api/transactionImportsApi';
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
  const [source, setSource] = useState<TransactionImportSource>('BANCO_PROVINCIA');
  const [accountId, setAccountId] = useState('');
  const [file, setFile] = useState<File | null>(null);
  const [preview, setPreview] = useState<TransactionImportPreview | null>(null);
  const [rows, setRows] = useState<TransactionImportRow[]>([]);

  const accountsQuery = useQuery({ queryKey: ['accounts', profileId], queryFn: () => listAccounts(profileId), enabled: Boolean(profileId) });
  const categoriesQuery = useQuery({ queryKey: ['categories', profileId], queryFn: () => listCategories(profileId, true), enabled: Boolean(profileId) });

  const previewMutation = useMutation({
    mutationFn: () => previewTransactionImport(profileId, source, accountId, file!),
    onSuccess: (data: TransactionImportPreview) => { setPreview(data); setRows(data.rows ?? []); },
  });

  const commitMutation = useMutation({
    mutationFn: () => {
      const payload: TransactionImportCommitPayload = {
        rows: rows.map((row) => ({ rowNumber: row.rowNumber, categoryId: row.suggestedCategoryId, accountId, movementType: row.movementType, amount: row.amount, status: row.status, description: row.normalizedDescription })),
        createMissingFallbackCategory: true,
        skipDuplicates: true,
      };
      return commitTransactionImport(profileId, preview!.batchId, payload);
    },
  });

  const importableRows = useMemo(() => rows.filter((row) => row.status !== 'INVALID' && row.status !== 'IGNORED').length, [rows]);
  const invalidRows = useMemo(() => rows.filter((row) => row.status === 'INVALID').length, [rows]);
  const ignoredRows = useMemo(() => rows.filter((row) => row.status === 'IGNORED').length, [rows]);
  const hasMissingCategory = rows.some((row) => row.status === 'READY' && row.movementType === 'EXPENSE' && !row.suggestedCategoryId);

  const canPreview = Boolean(file && accountId && !previewMutation.isPending);
  const canCommit = Boolean(preview?.batchId && importableRows > 0 && !hasMissingCategory && !commitMutation.isPending);

  return (
    <AppLayout>
      <div className="page-stack">
        <section className="page-header"><div><p className="eyebrow">Movimientos</p><h1>Importación guiada</h1></div></section>
        <ImportSourceForm
          source={source}
          accountId={accountId}
          file={file}
          accounts={accountsQuery.data ?? []}
          onSourceChange={setSource}
          onAccountChange={setAccountId}
          onFileChange={setFile}
          onPreview={() => previewMutation.mutate()}
          canPreview={canPreview}
          previewLoading={previewMutation.isPending}
        />

        {previewMutation.isError ? <ErrorState message="No se pudo generar la previsualización del archivo." /> : null}

        {preview ? (
          <section className="panel stack-ui" aria-live="polite">
            <h2>Paso 2: Revisar y confirmar</h2>
            <ImportPreviewSummary totalRows={preview.totalRows} importableRows={importableRows} duplicateRows={preview.duplicateRows} invalidRows={invalidRows} ignoredRows={ignoredRows} />
            {!importableRows ? <EmptyState title="No hay filas importables" message="Corregí categorías, tipos o volvé a subir el archivo con datos válidos." /> : null}
            <ImportRowsTable rows={rows} categories={categoriesQuery.data ?? []} onRowsChange={setRows} />
            <ImportCommitPanel canCommit={canCommit} pending={commitMutation.isPending} hasMissingCategory={hasMissingCategory} onCommit={() => commitMutation.mutate()} />
            {commitMutation.isError ? <ErrorState message="No se pudo confirmar la importación." /> : null}
          </section>
        ) : null}

        {commitMutation.data ? <ImportResultPanel result={commitMutation.data as TransactionImportCommitResult} profileId={profileId} /> : null}
      </div>
    </AppLayout>
  );
}
