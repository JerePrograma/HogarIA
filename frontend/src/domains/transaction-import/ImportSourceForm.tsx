import type { Account } from '../../domain/types';
import type { TransactionImportSource } from './types';

const SOURCES: Array<{
  value: TransactionImportSource;
  label: string;
  description: string;
}> = [
  {
    value: 'BANCO_PROVINCIA',
    label: 'Banco Provincia',
    description: 'Extractos bancarios y movimientos de cuenta.',
  },
  {
    value: 'MERCADO_PAGO',
    label: 'Mercado Pago',
    description: 'Movimientos exportados desde Mercado Pago.',
  },
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

function formatFileSize(bytes: number) {
  if (!Number.isFinite(bytes) || bytes <= 0) return '0 KB';

  const kb = bytes / 1024;
  if (kb < 1024) return `${kb.toFixed(1)} KB`;

  return `${(kb / 1024).toFixed(1)} MB`;
}

export function ImportSourceForm({
  source,
  accountId,
  file,
  accounts,
  onSourceChange,
  onAccountChange,
  onFileChange,
  onPreview,
  canPreview,
  previewLoading,
}: Props) {
  const selectedSource = SOURCES.find((item) => item.value === source);
  const selectedAccount = accounts.find((account) => account.id === accountId);

  return (
    <div className="import-source-form">
      <div className="import-source-grid">
        <fieldset className="import-source-fieldset">
          <legend>
            <span className="label-ui">Fuente</span>
            <strong>Origen del archivo</strong>
          </legend>

          <div className="import-source-options">
            {SOURCES.map((item) => (
              <button
                key={item.value}
                type="button"
                className={`import-source-option ${source === item.value ? 'active' : ''}`}
                onClick={() => onSourceChange(item.value)}
              >
                <strong>{item.label}</strong>
                <span>{item.description}</span>
              </button>
            ))}
          </div>
        </fieldset>

        <div className="import-source-fields">
          <label>
            Cuenta destino
            <select
              id="account-select"
              className="input-ui"
              value={accountId}
              onChange={(event) => onAccountChange(event.target.value)}
            >
              <option value="">Seleccionar cuenta</option>

              {accounts.map((account) => (
                <option key={account.id} value={account.id}>
                  {account.name}
                </option>
              ))}
            </select>
          </label>

          <label>
            Archivo
            <input
              id="file-input"
              className="input-ui"
              type="file"
              accept=".csv,.txt,.xls,.xlsx"
              onChange={(event) => onFileChange(event.target.files?.[0] ?? null)}
            />
          </label>
        </div>
      </div>

      <div className="import-source-preview-card">
        <div>
          <p className="eyebrow">Selección actual</p>

          <div className="import-source-selection">
            <span className="badge badge-info">
              {selectedSource?.label ?? source}
            </span>

            <span className={accountId ? 'badge badge-ok' : 'badge badge-warning'}>
              {selectedAccount?.name ?? 'Sin cuenta'}
            </span>

            <span className={file ? 'badge badge-ok' : 'badge badge-muted'}>
              {file ? file.name : 'Sin archivo'}
            </span>
          </div>

          {file ? (
            <p className="muted">
              Tamaño: {formatFileSize(file.size)}
            </p>
          ) : (
            <p className="muted">
              Seleccioná un archivo para habilitar la previsualización.
            </p>
          )}
        </div>

        <button
          type="button"
          className="boton-principal"
          disabled={!canPreview}
          onClick={onPreview}
        >
          {previewLoading ? 'Previsualizando...' : 'Previsualizar archivo'}
        </button>
      </div>
    </div>
  );
}