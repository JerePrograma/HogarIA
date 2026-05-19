import { useMemo, useState } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useParams, useSearchParams } from 'react-router-dom';
import { listAccounts } from '../../../api/accountsApi';
import { applyBulkRecategorize, previewBulkRecategorize, type BulkRecategorizePreviewPayload } from '../../../api/bulkRecategorizeApi';
import { listCategories, createCategory } from '../../../api/categoriesApi';
import { getApiErrorMessage } from '../../../api/http';
import { AppLayout } from '../../../components/layout/AppLayout';

export function TransactionRecategorizationPage() {
  const { profileId = '' } = useParams();
  const [search] = useSearchParams();
  const qc = useQueryClient();
  const [form, setForm] = useState<BulkRecategorizePreviewPayload>({
    accountId: search.get('accountId'), from: search.get('from'), to: search.get('to'), fromCategoryId: null,
    toCategoryId: search.get('toCategoryId') ?? '', movementType: (search.get('movementType') as BulkRecategorizePreviewPayload['movementType']) ?? null,
    descriptionContains: search.get('descriptionContains'), exactAmount: null, minAmount: null, maxAmount: null, onlyImported: null,
  });
  const [missingCategoryName, setMissingCategoryName] = useState(search.get('suggestedCategoryName') ?? '');

  const accounts = useQuery({ queryKey: ['accounts', profileId], queryFn: () => listAccounts(profileId), enabled: !!profileId });
  const categories = useQuery({ queryKey: ['categories', profileId], queryFn: () => listCategories(profileId, true), enabled: !!profileId });
  const previewMutation = useMutation({ mutationFn: () => previewBulkRecategorize(profileId, form) });
  const createCategoryMutation = useMutation({ mutationFn: () => createCategory(profileId, { name: missingCategoryName, type: 'VARIABLE_EXPENSE', scope: 'PERSONAL' }), onSuccess: async (created) => { setForm((prev) => ({ ...prev, toCategoryId: created.id })); await qc.invalidateQueries({ queryKey: ['categories', profileId] }); } });

  const readyIds = useMemo(() => (previewMutation.data?.candidates ?? []).filter((c) => c.previewStatus === 'READY').map((c) => c.transactionId), [previewMutation.data]);
  const applyMutation = useMutation({
    mutationFn: () => applyBulkRecategorize(profileId, { toCategoryId: form.toCategoryId, transactionIds: readyIds }),
    onSuccess: async () => { await Promise.all([qc.invalidateQueries({ queryKey: ['tx', profileId] }), qc.invalidateQueries({ queryKey: ['transactions', profileId] }), qc.invalidateQueries({ queryKey: ['categories', profileId] }), qc.invalidateQueries({ queryKey: ['dashboard', profileId] }), qc.invalidateQueries({ queryKey: ['budget-comp', profileId] })]); }
  });

  return <AppLayout><div className='page-stack'><section className='page-header'><h1>Recategorizar movimientos</h1></section>
    <section className='panel'><div className='form-grid'>
      <label>Cuenta<select className='input-ui' value={form.accountId ?? ''} onChange={(e) => setForm({ ...form, accountId: e.target.value || null })}><option value=''>Todas</option>{accounts.data?.map((a) => <option key={a.id} value={a.id}>{a.name}</option>)}</select></label>
      <label>Desde<input className='input-ui' type='date' value={form.from ?? ''} onChange={(e) => setForm({ ...form, from: e.target.value || null })} /></label>
      <label>Hasta<input className='input-ui' type='date' value={form.to ?? ''} onChange={(e) => setForm({ ...form, to: e.target.value || null })} /></label>
      <label>Categoría actual<select className='input-ui' value={form.fromCategoryId ?? ''} onChange={(e) => setForm({ ...form, fromCategoryId: e.target.value || null })}><option value=''>Todas</option>{categories.data?.map((c) => <option key={c.id} value={c.id}>{c.name}</option>)}</select></label>
      <label>Categoría destino<select className='input-ui' value={form.toCategoryId} onChange={(e) => setForm({ ...form, toCategoryId: e.target.value })}><option value=''>Seleccionar</option>{categories.data?.map((c) => <option key={c.id} value={c.id}>{c.name}</option>)}</select></label>
      <label>Tipo<select className='input-ui' value={form.movementType ?? ''} onChange={(e) => setForm({ ...form, movementType: (e.target.value || null) as BulkRecategorizePreviewPayload['movementType'] })}><option value=''>Todos</option><option value='INCOME'>Ingreso</option><option value='EXPENSE'>Gasto</option><option value='SAVING'>Ahorro</option><option value='TRANSFER'>Transferencia</option><option value='ADJUSTMENT'>Ajuste</option></select></label>
      <label>Descripción contiene<input className='input-ui' value={form.descriptionContains ?? ''} onChange={(e) => setForm({ ...form, descriptionContains: e.target.value || null })} /></label>
      <label>Monto exacto<input className='input-ui' type='number' value={form.exactAmount ?? ''} onChange={(e) => setForm({ ...form, exactAmount: e.target.value ? Number(e.target.value) : null })} /></label>
      <label>Monto mínimo<input className='input-ui' type='number' value={form.minAmount ?? ''} onChange={(e) => setForm({ ...form, minAmount: e.target.value ? Number(e.target.value) : null })} /></label>
      <label>Monto máximo<input className='input-ui' type='number' value={form.maxAmount ?? ''} onChange={(e) => setForm({ ...form, maxAmount: e.target.value ? Number(e.target.value) : null })} /></label>
      <label>Solo importados<select className='input-ui' value={form.onlyImported == null ? '' : String(form.onlyImported)} onChange={(e) => setForm({ ...form, onlyImported: e.target.value === '' ? null : e.target.value === 'true' })}><option value=''>Todos</option><option value='true'>Sí</option><option value='false'>No</option></select></label>
    </div>
    {missingCategoryName && !form.toCategoryId ? <div className='mensaje-warning'>La categoría sugerida todavía no existe: {missingCategoryName}. <button className='btn btn-secondary' onClick={() => createCategoryMutation.mutate()} disabled={createCategoryMutation.isPending}>Crear categoría</button></div> : null}
    <button className='btn btn-primary' onClick={() => previewMutation.mutate()} disabled={!form.toCategoryId || previewMutation.isPending}>Previsualizar cambios</button>
    {previewMutation.isError ? <p className='mensaje-error'>{getApiErrorMessage(previewMutation.error)}</p> : null}
    {previewMutation.data ? <div className='stack-ui'><p>Encontrados: {previewMutation.data.totalMatched} | Actualizables: {previewMutation.data.updatableCount} | Ambiguos: {previewMutation.data.ambiguousCount} | Omitidos: {previewMutation.data.skippedCount}</p>
      <table><thead><tr><th>Fecha</th><th>Descripción</th><th>Monto</th><th>Tipo</th><th>Estado</th><th>Warning</th></tr></thead><tbody>{previewMutation.data.candidates.map((c) => <tr key={c.transactionId}><td>{c.realDate}</td><td>{c.description}</td><td>{c.amount}</td><td>{c.movementType}</td><td>{c.previewStatus}</td><td>{c.warning}</td></tr>)}</tbody></table>
      <button className='btn btn-primary' onClick={() => applyMutation.mutate()} disabled={!readyIds.length || applyMutation.isPending}>Aplicar recategorización</button>
    </div> : null}
    {applyMutation.data ? <p>Actualizados: {applyMutation.data.updatedCount} | Omitidos: {applyMutation.data.skippedCount} | Fallidos: {applyMutation.data.failedCount}</p> : null}
  </section></div></AppLayout>;
}
