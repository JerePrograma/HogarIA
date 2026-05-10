import { Bar, BarChart, CartesianGrid, Pie, PieChart, ResponsiveContainer, Tooltip, XAxis, YAxis } from 'recharts';
import { formatMoney } from '../../../domain/formatters';
import type { DashboardSummary } from '../../../domain/types';

type Props = { summary: DashboardSummary };
export function DashboardCharts({ summary }: Props) {
  return <section className='grid'><div className='card chart-card'><h3 className='section-title'>Distribución del mes</h3><ResponsiveContainer width='100%' height={220}><PieChart><Pie data={[{ name: 'Gastos fijos', value: Number(summary.fixedExpenses ?? 0) }, { name: 'Gastos variables', value: Number(summary.variableExpenses ?? 0) }, { name: 'Ahorro', value: Number(summary.monthlyBalance?.savings ?? 0) }]} dataKey='value' nameKey='name' outerRadius={90} /><Tooltip formatter={(v) => formatMoney(v as number)} /></PieChart></ResponsiveContainer></div><div className='card chart-card'><h3 className='section-title'>Presupuesto vs real</h3><ResponsiveContainer width='100%' height={220}><BarChart data={summary.categoryBreakdown.map((i) => ({ categoria: i.categoryName, real: Number(i.totalAmount ?? 0) }))}><CartesianGrid strokeDasharray='3 3' /><XAxis dataKey='categoria' hide /><YAxis /><Tooltip formatter={(v) => formatMoney(v as number)} /><Bar dataKey='real' fill='#0f766e' /></BarChart></ResponsiveContainer></div></section>;
}
