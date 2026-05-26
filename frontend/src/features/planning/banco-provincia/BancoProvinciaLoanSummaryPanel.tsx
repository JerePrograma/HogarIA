import type { BancoProvinciaPreviewCandidate } from '../../../api/bancoProvinciaLoanImportApi';

export function BancoProvinciaLoanSummaryPanel({rows}:{rows:BancoProvinciaPreviewCandidate[]}) { return <p className='muted'>Préstamos detectados: {rows.length}</p>; }
