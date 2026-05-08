import { useState } from 'react';
import { useParams, Link } from 'react-router-dom';
import { commitBudgetExcelImport, previewBudgetExcelImport } from '../../api/importsApi';
import type { BudgetExcelImportCommitRequest, BudgetExcelImportPreviewResponse } from '../../domain/types';
import { getApiErrorMessage } from '../../api/http';

const defaultOptions: BudgetExcelImportCommitRequest = {
  createCategories: true, createAccounts: true, createBudgets: true, createTransactions: true,
  createGoals: true, createHabits: true, createInflation: true, updateExisting: true, ignoreInvalidRows: true,
  year: new Date().getFullYear(), currency: 'ARS', profileType: 'PERSONAL'
};

export function BudgetExcelImportPage() { const { profileId = '' } = useParams(); const [file, setFile] = useState<File | null>(null); const [preview, setPreview] = useState<BudgetExcelImportPreviewResponse | null>(null); const [options, setOptions] = useState(defaultOptions); const [loading, setLoading] = useState(false); const [msg, setMsg] = useState('');
  const analyze = async () => { if (!file) return; setLoading(true); setMsg(''); try { setPreview(await previewBudgetExcelImport(profileId, file)); } catch (e) { setMsg(getApiErrorMessage(e)); } finally { setLoading(false); } };
  const commit = async () => { if (!preview) return; setLoading(true); setMsg(''); try { const r = await commitBudgetExcelImport(profileId, preview.batchId, options); setMsg(`Importación ${r.status}.`); } catch (e) { setMsg(getApiErrorMessage(e)); } finally { setLoading(false); } };
  return <section className='card'><h1>Carga guiada de Excel</h1><input type='file' accept='.xlsx' onChange={(e) => setFile(e.target.files?.[0] ?? null)} /><button onClick={analyze} disabled={!file || loading}>Analizar Excel</button>
    {preview && <><p>Hojas detectadas: {preview.detectedSheets.join(', ') || '-'}</p><p>Hojas faltantes: {preview.missingSheets.join(', ') || 'Ninguna'}</p><label><input type='checkbox' checked={options.createTransactions} onChange={(e) => setOptions({ ...options, createTransactions: e.target.checked })}/> Importar movimientos reales</label><button onClick={commit} disabled={loading}>Confirmar importación</button>
      <table><thead><tr><th>Hoja</th><th>Fila</th><th>Concepto</th><th>Mes</th><th>Monto</th><th>Estado</th></tr></thead><tbody>{preview.rows.slice(0, 200).map((r) => <tr key={r.id}><td>{r.sheetName}</td><td>{r.rowNumber ?? '-'}</td><td>{r.concept ?? '-'}</td><td>{r.month ?? '-'}</td><td>{r.amount ?? '-'}</td><td>{r.status}</td></tr>)}</tbody></table>
      <p><Link to={`/profiles/${profileId}/dashboard`}>Ir al Dashboard</Link></p></>}
    {msg && <p>{msg}</p>}</section>; }
