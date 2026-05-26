type StatusTone = 'neutral' | 'ok' | 'watch' | 'risk' | 'critical';

type Props = {
  label: string;
  tone?: StatusTone;
};

const toneClass: Record<StatusTone, string> = {
  neutral: 'badge badge-muted',
  ok: 'badge badge-ok',
  watch: 'badge badge-watch',
  risk: 'badge badge-risk',
  critical: 'badge badge-critical',
};

export function StatusBadge({ label, tone = 'neutral' }: Props) {
  return <span className={toneClass[tone]}>{label}</span>;
}