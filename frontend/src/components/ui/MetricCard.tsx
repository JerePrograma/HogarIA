import type { ReactNode } from 'react';

type Props = { title: string; value: ReactNode; primary?: boolean; helper?: string };

export function MetricCard({ title, value, primary = false, helper }: Props) {
  return <article className={`card metric-card ${primary ? 'metric-card-primary' : ''}`.trim()}><b>{title}</b><div className='kpi-value'>{value}</div>{helper && <p className='compact-muted'>{helper}</p>}</article>;
}
