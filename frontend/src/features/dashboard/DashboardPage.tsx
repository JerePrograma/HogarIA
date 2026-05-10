import { useQuery } from '@tanstack/react-query';
import { useState } from 'react';
import { Link, useParams } from 'react-router-dom';
import { getMonthlyDashboard } from '../../api/dashboardApi';
import { AppLayout } from '../../components/layout/AppLayout';
import { EmptyState } from '../../components/ui/EmptyState';
import { ErrorState } from '../../components/ui/ErrorState';
import { MonthSelector } from '../../components/ui/MonthSelector';
import { MetricCard } from '../../components/ui/MetricCard';
import { ConfirmedVsProjectedPanel } from './components/ConfirmedVsProjectedPanel';
import { DashboardCharts } from './components/DashboardCharts';
import { OperationalAlerts } from './components/OperationalAlerts';
import { OperationalSummaryCards } from './components/OperationalSummaryCards';
import type { DashboardSummary } from '../../domain/types';

export function DashboardPage() { const { profileId = '' } = useParams(); const d = new Date(); const [year, setYear] = useState(d.getFullYear()); const [month, setMonth] = useState(d.getMonth() + 1); const q = useQuery<DashboardSummary>({ queryKey: ['dashboard', profileId, year, month], queryFn: () => getMonthlyDashboard(profileId, year, month), enabled: Boolean(profileId) }); const s = q.data; const planning = s?.planningSummary; const operational = s?.operationalSummary;
  return <AppLayout><div className='page-stack'><section className='card page-header'><h1>Panel mensual</h1><MonthSelector year={year} month={month} onYearChange={setYear} onMonthChange={setMonth} /><div className='page-actions'><Link to={`/profiles/${profileId}/planning`}>Planificar</Link><Link to={`/profiles/${profileId}/transactions`}>Cargar movimiento</Link></div></section>{q.isLoading && <EmptyState message='Cargando información financiera...' />}{q.isError && <ErrorState message='No se pudo cargar el panel mensual.' />}{s && planning && operational && <><OperationalSummaryCards summary={operational} /><OperationalAlerts alerts={operational.alerts} /><ConfirmedVsProjectedPanel planning={planning} operational={operational} /><section className='metric-grid'><MetricCard title='Sin cotizar' value={planning.unpricedCount} /><MetricCard title='Próximos 7 días' value={planning.dueNext7DaysCount} /><MetricCard title='Items convertidos' value={planning.convertedItemsCount} /><MetricCard title='Items cancelados' value={planning.cancelledItemsCount} /></section><section className='card'><h3 className='section-title'>Regla 50/30/20</h3><p>Gastos fijos: {s.fiftyThirtyTwenty?.fixedPercent ?? 0}% | Gastos variables: {s.fiftyThirtyTwenty?.variablePercent ?? 0}% | Ahorro: {s.fiftyThirtyTwenty?.savingPercent ?? 0}%</p></section><DashboardCharts summary={s} /></>}</div></AppLayout>; }
