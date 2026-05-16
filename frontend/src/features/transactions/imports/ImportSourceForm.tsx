import type { Account } from '../../../domain/types';
import type { TransactionImportSource } from './types';

const SOURCES: Array<{ value: TransactionImportSource; label: string }> = [
  { value: 'BANCO_PROVINCIA', label: 'Banco Provincia' },
  { value: 'MERCADO_PAGO', label: 'Mercado Pago' },
];

interface Props {
  source: TransactionImportSource;
  accountId: string;
  file: File | null;
  accounts: Account[];
  onSourceChange: (source: TransactionImportSource) => void;
  onAccountChange: (accountId: string) => void;
  onFileChange: (file: File | null) => void;
  onPreview: () => void;
  canPreview: boolean;
  previewLoading: boolean;
}

export function ImportSourceForm(props: Props) {
  const { source, accountId, file, accounts, onSourceChange, onAccountChange, onFileChange, onPreview, canPreview, previewLoading } = props;
  return (
    <section className="panel stack-ui">
      <h2>Paso 1: Fuente y archivo</h2>
      <label className="label-ui" htmlFor="source-select">Fuente</label>
      <select id="source-select" value={source} onChange={(event) => onSourceChange(event.target.value as TransactionImportSource)}>
        {SOURCES.map((item) => <option key={item.value} value={item.value}>{item.label}</option>)}
      </select>
      <label className="label-ui" htmlFor="account-select">Cuenta destino</label>
      <select id="account-select" value={accountId} onChange={(event) => onAccountChange(event.target.value)}>
        <option value="">Seleccionar cuenta</option>
        {accounts.map((account) => <option key={account.id} value={account.id}>{account.name}</option>)}
      </select>
      <label className="label-ui" htmlFor="file-input">Archivo</label>
      <input id="file-input" type="file" onChange={(event) => onFileChange(event.target.files?.[0] ?? null)} />
      <button type="button" disabled={!canPreview} onClick={onPreview}>
        {previewLoading ? 'Previsualizando archivo…' : 'Previsualizar archivo'}
      </button>
      {file ? <p className="muted">Archivo seleccionado: {file.name}</p> : null}
    </section>
  );
}
