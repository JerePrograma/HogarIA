import type { ReactNode } from 'react';

type Props = {
  input: string;
  onChange: (value: string) => void;
  onAnalyze: () => void;
  canAnalyze: boolean;
  error?: string;
  children?: ReactNode;
  onDiscard: () => void;
  showDiscard: boolean;
  onClear: () => void;
  isAnalyzing?: boolean;
};

const examples = [
  '05/06 95000 Juliana cuota 3/5',
  'hostel 550000 recupero 50% Agus',
  'escuela Megu 150000 a 180000',
  'inflables Megu reservar fecha',
  '18/06 sueldo programación 1450000',
];

export function QuickCapturePanel({
  input,
  onChange,
  onAnalyze,
  canAnalyze,
  error,
  children,
  onDiscard,
  showDiscard,
  onClear,
  isAnalyzing = false,
}: Props) {
  return (
    <section className="panel-accent quick-capture-card">
      <div className="section-title">
        <div>
          <p className="eyebrow">Entrada rápida</p>
          <h2>Cargar rápido</h2>
          <p className="secondary-text">
            Escribí un solo compromiso como lo anotarías en WhatsApp. Para listas con varias líneas usá Cargar por texto.
          </p>
        </div>
      </div>

      <div className="quick-examples">
        {examples.map((example) => (
          <button
            key={example}
            type="button"
            className="boton-secundario"
            onClick={() => onChange(example)}
          >
            {example}
          </button>
        ))}
      </div>

      <textarea
        className="input-ui"
        placeholder="Ej: 05/06 95000 Juliana cuota 3/5"
        value={input}
        onChange={(event) => onChange(event.target.value)}
      />

      <div className="form-actions">
        <button
          type="button"
          className="boton-principal"
          onClick={onAnalyze}
          disabled={!canAnalyze || isAnalyzing}
        >
          {isAnalyzing ? 'Analizando...' : 'Analizar'}
        </button>

        <button type="button" className="boton-secundario" onClick={onClear}>
          Limpiar
        </button>

        {showDiscard ? (
          <button type="button" className="boton-fantasma" onClick={onDiscard}>
            Descartar análisis
          </button>
        ) : null}
      </div>

      {error ? <p className="mensaje-error">{error}</p> : null}

      {children ? <div className="mt-4">{children}</div> : null}
    </section>
  );
}
