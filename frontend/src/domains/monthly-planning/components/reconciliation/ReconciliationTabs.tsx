import type { ReconciliationTabKey } from './types';
import { reconciliationTabs } from './types';

export function ReconciliationTabs({ tab, onChange }: { tab: ReconciliationTabKey; onChange: (next: ReconciliationTabKey) => void }) {
  return (
    <div className='planning-filter-row'>
      {reconciliationTabs.map((current) => (
        <button key={current.key} type='button' onClick={() => onChange(current.key)} aria-pressed={tab === current.key}>
          {current.label}
        </button>
      ))}
    </div>
  );
}
