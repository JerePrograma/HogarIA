import type { ReactNode } from 'react';

type MetricTone = 'neutral' | 'info' | 'success' | 'warning' | 'danger';

type Props = {
  title: string;
  value: ReactNode;
  primary?: boolean;
  helper?: string;
  tone?: MetricTone;
};

export function MetricCard({
  title,
  value,
  primary = false,
  helper,
  tone = 'neutral',
}: Props) {
  const resolvedTone: MetricTone = primary && tone === 'neutral' ? 'info' : tone;

  return (
    <article className="metric-card" data-tone={resolvedTone}>
      <div className="cluster-ui justify-between">
        <p className="metric-title">{title}</p>
        <span className="metric-dot" data-tone={resolvedTone} aria-hidden="true" />
      </div>

      <div className="metric-value">{value}</div>

      {helper && <p className="metric-description">{helper}</p>}
    </article>
  );
}