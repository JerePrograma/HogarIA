import { useMemo, useState } from 'react';
import { useMutation, useQuery } from '@tanstack/react-query';
import { useParams } from 'react-router-dom';
import { listAccounts } from '../../api/accountsApi';
import { listCategories } from '../../api/categoriesApi';
import { commitTransactionImport, previewTransactionImport } from '../../api/transactionImportsApi';
import { AppLayout } from '../../components/layout/AppLayout';
import { EmptyState } from '../../components/ui/EmptyState';
import { ErrorState } from '../../components/ui/ErrorState';

const SOURCES = [
  { value: 'BANCO_PROVINCIA', label: 'Banco Provincia' },
  { value: 'MERCADO_PAGO', label: 'Mercado Pago' },
] as const;

type ImportMovementType = 'INCOME' | 'EXPENSE';
type RowStatus = 'READY' | 'DUPLICATE' | 'INVALID' | 'IGNORED';

type ImportPreviewRow = {
  rowNumber: number;
  realDate: string;
  normalizedDescription: string;
  amount: number;
  movementType: ImportMovementType;
  suggestedCategoryId: string | null;
  status: RowStatus;
};

type ImportPreviewResponse = {
  batchId: string;
  totalRows: number;
  importableRows: number;
  duplicateRows: number;
  rows: ImportPreviewRow[];
};

type ImportCommitResponse = {
  createdCount: number;
  duplicateCount: number;
  skippedCount?: number;
  errors?: string[];
};

export function TransactionImportPage() {
  const { profileId = '' } = useParams();

  const [source, setSource] = useState<(typeof SOURCES)[number]['value']>('BANCO_PROVINCIA');
  const [accountId, setAccountId] = useState('');
  const [file, setFile] = useState<File | null>(null);
  const [preview, setPreview] = useState<ImportPreviewResponse | null>(null);
  const [rows, setRows] = useState<ImportPreviewRow[]>([]);

  const accountsQuery = useQuery({
    queryKey: ['accounts', profileId],
    queryFn: () => listAccounts(profileId),
    enabled: Boolean(profileId),
  });

  const categoriesQuery = useQuery({
    queryKey: ['categories', profileId],
    queryFn: () => listCategories(profileId, true),
    enabled: Boolean(profileId),
  });

  const previewMutation = useMutation({
    mutationFn: () => previewTransactionImport(profileId, source, accountId, file!) as Promise<ImportPreviewResponse>,
    onSuccess: (data) => {
      setPreview(data);
      setRows(data.rows ?? []);
    },
  });

  const commitMutation = useMutation({
    mutationFn: () =>
      commitTransactionImport(profileId, preview!.batchId, {
        rows: rows.map((row) => ({
          rowNumber: row.rowNumber,
          categoryId: row.suggestedCategoryId,
          accountId,
          movementType: row.movementType,
          amount: row.amount,
          status: row.status,
          description: row.normalizedDescription,
        })),
        createMissingFallbackCategory: true,
        skipDuplicates: true,
      }) as Promise<ImportCommitResponse>,
  });

  const importableRows = useMemo(
    () => rows.filter((row) => row.status !== 'INVALID' && row.status !== 'IGNORED').length,
    [rows],
  );

  const hasMissingCategory = rows.some(
    (row) => row.status === 'READY' && row.movementType === 'EXPENSE' && !row.suggestedCategoryId,
  );

  const canPreview = Boolean(file && accountId && !previewMutation.isPending);
  const canCommit = Boolean(preview?.batchId && importableRows > 0 && !hasMissingCategory && !commitMutation.isPending);

  return (
    <AppLayout>
      <div className="page-stack">
        <section className="page-header">
          <div>
            <p className="eyebrow">Movimientos</p>
            <h1>Importación guiada</h1>
            <p className="secondary-text">
              Seleccioná origen, cuenta y archivo. Revisá la vista previa y confirmá solo cuando las filas sean válidas.
            </p>
          </div>
        </section>

        <section className="panel stack-ui">
          <h2>Paso 1: Fuente y archivo</h2>

          <label className="label-ui" htmlFor="source-select">Fuente</label>
          <select id="source-select" value={source} onChange={(event) => setSource(event.target.value as (typeof SOURCES)[number]['value'])}>
            {SOURCES.map((item) => (
              <option key={item.value} value={item.value}>{item.label}</option>
            ))}
          </select>

          <label className="label-ui" htmlFor="account-select">Cuenta destino</label>
          <select id="account-select" value={accountId} onChange={(event) => setAccountId(event.target.value)}>
            <option value="">Seleccionar cuenta</option>
            {(accountsQuery.data ?? []).map((account) => (
              <option key={account.id} value={account.id}>{account.name}</option>
            ))}
          </select>

          <label className="label-ui" htmlFor="file-input">Archivo</label>
          <input id="file-input" type="file" onChange={(event) => setFile(event.target.files?.[0] ?? null)} />

          <button type="button" disabled={!canPreview} onClick={() => previewMutation.mutate()}>
            {previewMutation.isPending ? 'Generando preview…' : 'Generar preview'}
          </button>
        </section>

        {previewMutation.isError ? <ErrorState message="No se pudo generar la previsualización del archivo." /> : null}

        {preview ? (
          <section className="panel stack-ui" aria-live="polite">
            <h2>Paso 2: Revisar y corregir</h2>
            <p className="muted">
              Total filas: {preview.totalRows} · Importables: {importableRows} · Duplicadas: {preview.duplicateRows}
            </p>

            {!importableRows ? (
              <EmptyState
                title="No hay filas importables"
                message="Corregí categorías, tipos o volvé a subir el archivo con datos válidos."
              />
            ) : null}

            <table>
              <thead>
                <tr>
                  <th>Fila</th><th>Fecha</th><th>Descripción</th><th>Monto</th><th>Tipo</th><th>Categoría</th><th>Estado</th>
                </tr>
              </thead>
              <tbody>
                {rows.map((row, index) => (
                  <tr key={row.rowNumber}>
                    <td>{row.rowNumber}</td><td>{row.realDate}</td><td>{row.normalizedDescription}</td><td>{row.amount}</td>
                    <td>
                      <select
                        value={row.movementType}
                        onChange={(event) => setRows((current) => current.map((item, itemIndex) => itemIndex === index ? { ...item, movementType: event.target.value as ImportMovementType } : item))}
                      >
                        <option value="INCOME">INCOME</option>
                        <option value="EXPENSE">EXPENSE</option>
                      </select>
                    </td>
                    <td>
                      <select
                        value={row.suggestedCategoryId ?? ''}
                        onChange={(event) => setRows((current) => current.map((item, itemIndex) => itemIndex === index ? { ...item, suggestedCategoryId: event.target.value || null } : item))}
                      >
                        <option value="">Sin categoría</option>
                        {(categoriesQuery.data ?? []).map((category) => (
                          <option key={category.id} value={category.id}>{category.name}</option>
                        ))}
                      </select>
                    </td>
                    <td>{row.status}</td>
                  </tr>
                ))}
              </tbody>
            </table>

            {hasMissingCategory ? (
              <p className="mensaje-warning">
                Hay gastos listos para importar sin categoría. Asigná categoría antes de confirmar.
              </p>
            ) : null}

            <button type="button" disabled={!canCommit} onClick={() => commitMutation.mutate()}>
              {commitMutation.isPending ? 'Importando…' : 'Confirmar importación'}
            </button>

            {commitMutation.isError ? (
              <ErrorState message="No se pudo confirmar la importación." />
            ) : null}

            {commitMutation.data ? (
              <p className="mensaje-exito" aria-live="polite">
                Creados: {commitMutation.data.createdCount} · Duplicados omitidos: {commitMutation.data.duplicateCount}
              </p>
            ) : null}
          </section>
        ) : null}
      </div>
    </AppLayout>
  );
}
