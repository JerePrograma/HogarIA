import type { ReactNode } from 'react';

type Props = { input: string; onChange: (value: string) => void; onAnalyze: () => void; canAnalyze: boolean; error?: string; children?: ReactNode; onDiscard: () => void; showDiscard: boolean };
export function QuickCapturePanel({ input, onChange, onAnalyze, canAnalyze, error, children, onDiscard, showDiscard }: Props) {
  return <section className='card quick-capture-card'><h3 className='section-title'>Captura rápida</h3><textarea className='input' placeholder='Ej: 05/06 95000 Juliana cuota 3/5' value={input} onChange={(e) => onChange(e.target.value)} /><div className='form-row'><button className='button-primary' onClick={onAnalyze} disabled={!canAnalyze}>Analizar</button>{showDiscard && <button className='button-secondary' onClick={onDiscard}>Descartar</button>}</div>{error && <p className='error-box'>{error}</p>}{children}</section>;
}
