import type { QuickPlanCandidate } from '../../../api/quickPlanTextImportApi';
import { monthLabels } from '../../../domain/financeLabels';

export function QuickPlanSummaryPanel({ rows }: { rows: QuickPlanCandidate[] }) {
  const dup = rows.filter((x) => x.duplicate).length;
  const periods = new Set(
    rows.map((row) => `${row.candidate.periodYear}-${row.candidate.periodMonth}`),
  );
  const periodText = [...periods]
    .map((key) => {
      const [year, month] = key.split('-').map(Number);
      return `${monthLabels[month] ?? `Mes ${month}`} ${year}`;
    })
    .join(', ');

  return (
    <p className="secondary-text">
      Candidatos: {rows.length} · Duplicados detectados: {dup} · Períodos: {periodText || '-'}
    </p>
  );
}
