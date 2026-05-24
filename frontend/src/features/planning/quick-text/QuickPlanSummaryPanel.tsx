import type { QuickPlanCandidate } from '../../../api/quickPlanTextImportApi';
export function QuickPlanSummaryPanel({rows}:{rows:QuickPlanCandidate[]}){const dup=rows.filter(x=>x.duplicate).length;return <p className='secondary-text'>Candidatos: {rows.length} · Duplicados detectados: {dup}</p>}
