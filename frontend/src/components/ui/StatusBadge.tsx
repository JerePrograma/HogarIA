type Props = { label: string; tone?: 'neutral' | 'ok' | 'watch' | 'risk' | 'critical' };
const toneClass: Record<NonNullable<Props['tone']>, string> = { neutral: 'badge', ok: 'badge badge-ok', watch: 'badge badge-watch', risk: 'badge badge-risk', critical: 'badge badge-critical' };
export function StatusBadge({ label, tone = 'neutral' }: Props) { return <span className={toneClass[tone]}>{label}</span>; }
