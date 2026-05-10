import { EmptyState } from '../../../components/ui/EmptyState';

type Props = { alerts: string[] };
export function OperationalAlerts({ alerts }: Props) { return <section className='card'><h3 className='section-title'>Alertas operativas</h3>{alerts.length === 0 ? <EmptyState message='Sin alertas operativas para este período.' /> : <ul>{alerts.map((a) => <li key={a}>{a}</li>)}</ul>}</section>; }
