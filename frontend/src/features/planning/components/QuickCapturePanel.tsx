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
  '18/06 sueldo programación 1450000'
];

export function QuickCapturePanel({ input, onChange, onAnalyze, canAnalyze, error, children, onDiscard, showDiscard, onClear, isAnalyzing = false }: Props) {
  return <section className='card quick-capture-card'><h3 className='section-title'>Cargar rápido</h3><p className='secondary-text'>Escribí como lo anotarías en WhatsApp. Después podés revisar antes de guardar.</p><div className='quick-examples'>{examples.map((example) => <button key={example} type='button' className='button-secondary' onClick={() => onChange(example)}>{example}</button>)}</div><textarea className='input' placeholder='Ej: 05/06 95000 Juliana cuota 3/5' value={input} onChange={(e) => onChange(e.target.value)} /><div className='form-row'><button className='button-primary' onClick={onAnalyze} disabled={!canAnalyze || isAnalyzing}>{isAnalyzing ? 'Analizando...' : 'Analizar'}</button><button className='button-secondary' onClick={onClear}>Limpiar</button>{showDiscard && <button className='button-secondary' onClick={onDiscard}>Descartar análisis</button>}</div>{error && <p className='error-box'>{error}</p>}{children}</section>;
}
