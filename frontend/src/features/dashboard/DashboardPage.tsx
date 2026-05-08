import { useQuery } from '@tanstack/react-query';
import { useState } from 'react';
import { Link, useParams } from 'react-router-dom';
import {
  Bar,
  BarChart,
  CartesianGrid,
  Cell,
  Legend,
  Pie,
  PieChart,
  ResponsiveContainer,
  Tooltip,
  XAxis,
  YAxis,
} from 'recharts';
import { getMonthlyDashboard } from '../../api/dashboardApi';
import { AppLayout } from '../../components/layout/AppLayout';
import { categoryTypeLabels, financialHealthLabels, labelOrMissing, monthLabels } from '../../domain/financeLabels';
import { formatMoney, formatPercent } from '../../domain/formatters';
import type { CategoryType, DashboardSummary, FinancialHealth } from '../../domain/types';

const chartColors = [
  'var(--chart-1)',
  'var(--chart-2)',
  'var(--chart-3)',
  'var(--chart-4)',
];

export function DashboardPage() {
  const { profileId = '' } = useParams();
  const today = new Date();

  const [year, setYear] = useState(today.getFullYear());
  const [month, setMonth] = useState(today.getMonth() + 1);

  const dashboardQuery = useQuery({
    queryKey: ['dash', profileId, year, month],
    queryFn: () => getMonthlyDashboard(profileId, year, month),
    enabled: Boolean(profileId),
  });

  const summary = dashboardQuery.data as DashboardSummary | undefined;

  const distributionData = [
    { name: 'Gastos fijos', value: Number(summary?.fixedExpenses ?? 0) },
    { name: 'Gastos variables', value: Number(summary?.variableExpenses ?? 0) },
    { name: 'Ahorro', value: Number(summary?.monthlyBalance?.savings ?? 0) },
  ];

  const budgetData = [
    { name: 'Presupuestado', monto: Number(summary?.budgetSummary?.totalBudget ?? 0) },
    { name: 'Ejecutado', monto: Number(summary?.budgetSummary?.totalReal ?? 0) },
  ];

  const ruleData = [
    { name: 'Gastos fijos', porcentaje: Number(summary?.fiftyThirtyTwenty?.fixedPercent ?? 0) },
    { name: 'Gastos variables', porcentaje: Number(summary?.fiftyThirtyTwenty?.variablePercent ?? 0) },
    { name: 'Ahorro', porcentaje: Number(summary?.fiftyThirtyTwenty?.savingPercent ?? 0) },
  ];

  const kpis = [
    { label: 'Ingresos', value: formatMoney(summary?.monthlyBalance?.totalIncome), tone: 'good' },
    { label: 'Gastos fijos', value: formatMoney(summary?.fixedExpenses), tone: 'warning' },
    { label: 'Gastos variables', value: formatMoney(summary?.variableExpenses), tone: 'warning' },
    { label: 'Ahorro', value: formatMoney(summary?.monthlyBalance?.savings), tone: 'good' },
    { label: 'Balance', value: formatMoney(summary?.monthlyBalance?.balance), tone: 'neutral' },
    {
      label: 'Salud financiera',
      value: labelOrMissing(financialHealthLabels, summary?.financialHealth as FinancialHealth | undefined),
      tone: summary?.financialHealth === 'CRITICAL' ? 'danger' : summary?.financialHealth === 'WARNING' ? 'warning' : 'good',
    },
  ];

  return (
    <AppLayout>
      <div className="page-header">
        <div>
          <p className="eyebrow">Resumen mensual</p>
          <h1>Panel financiero</h1>
          <p className="muted">
            {monthLabels[month] ?? month} {year}: estado general, desvíos y composición del mes.
          </p>
        </div>

        <div className="toolbar">
          <label className="field compact-field">
            <span>Año</span>
            <input type="number" value={year} onChange={(event) => setYear(Number(event.target.value))} />
          </label>

          <label className="field compact-field">
            <span>Mes</span>
            <select value={month} onChange={(event) => setMonth(Number(event.target.value))}>
              {Object.entries(monthLabels).map(([value, label]) => (
                <option key={value} value={value}>
                  {label}
                </option>
              ))}
            </select>
          </label>
        </div>
      </div>

      {dashboardQuery.isLoading ? (
        <p className="muted">Cargando panel financiero...</p>
      ) : dashboardQuery.isError ? (
        <div className="alert danger">No se pudo cargar el panel mensual.</div>
      ) : (
        <>
          <section className="kpi-grid">
            {kpis.map((kpi) => (
              <article className="kpi-card" key={kpi.label}>
                <span>{kpi.label}</span>
                <strong className={kpi.label === 'Salud financiera' ? '' : 'amount'}>{kpi.value}</strong>
                <small className={`badge ${kpi.tone}`}>{kpi.label}</small>
              </article>
            ))}
          </section>

          <section className="chart-grid">
            <article className="card">
              <h2>Distribución del mes</h2>
              <div className="chart-box">
                <ResponsiveContainer width="100%" height={260}>
                  <PieChart>
                    <Pie data={distributionData} dataKey="value" nameKey="name" outerRadius={90} label>
                      {distributionData.map((entry, index) => (
                        <Cell key={entry.name} fill={chartColors[index % chartColors.length]} />
                      ))}
                    </Pie>
                    <Tooltip formatter={(value) => formatMoney(Number(value))} />
                    <Legend />
                  </PieChart>
                </ResponsiveContainer>
              </div>
            </article>

            <article className="card">
              <h2>Presupuesto vs ejecutado</h2>
              <div className="chart-box">
                <ResponsiveContainer width="100%" height={260}>
                  <BarChart data={budgetData}>
                    <CartesianGrid strokeDasharray="3 3" />
                    <XAxis dataKey="name" />
                    <YAxis />
                    <Tooltip formatter={(value) => formatMoney(Number(value))} />
                    <Bar dataKey="monto" name="Monto" fill="var(--chart-1)" radius={[8, 8, 0, 0]} />
                  </BarChart>
                </ResponsiveContainer>
              </div>
            </article>

            <article className="card">
              <h2>Regla 50/30/20</h2>
              <p className="muted">Comparación rápida entre gastos fijos, variables y ahorro.</p>
              <div className="chart-box">
                <ResponsiveContainer width="100%" height={260}>
                  <BarChart data={ruleData}>
                    <CartesianGrid strokeDasharray="3 3" />
                    <XAxis dataKey="name" />
                    <YAxis />
                    <Tooltip formatter={(value) => formatPercent(Number(value))} />
                    <Bar dataKey="porcentaje" name="Porcentaje" fill="var(--chart-2)" radius={[8, 8, 0, 0]} />
                  </BarChart>
                </ResponsiveContainer>
              </div>
            </article>
          </section>

          <section className="card">
            <div className="section-header">
              <div>
                <h2>Desglose por categoría</h2>
                <p className="muted">Lectura rápida de peso relativo y cantidad de movimientos.</p>
              </div>
            </div>

            {!summary?.categoryBreakdown?.length ? (
              <p className="muted">No hay movimientos para mostrar en este período.</p>
            ) : (
              <div className="table-wrap">
                <table className="table">
                  <thead>
                    <tr>
                      <th>Categoría</th>
                      <th>Tipo</th>
                      <th className="amount">Total</th>
                      <th className="amount">% ingreso</th>
                      <th className="amount">Movimientos</th>
                    </tr>
                  </thead>
                  <tbody>
                    {summary.categoryBreakdown.map((item) => (
                      <tr key={item.categoryId}>
                        <td>{item.categoryName}</td>
                        <td>{labelOrMissing(categoryTypeLabels, item.categoryType as CategoryType)}</td>
                        <td className="amount">{formatMoney(item.totalAmount)}</td>
                        <td className="amount">{formatPercent(item.percentOfIncome)}</td>
                        <td className="amount">{item.movementCount}</td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            )}
          </section>

          <section className="dashboard-links">
            <Link to={`../transactions`} className="quick-link">Movimientos</Link>
            <Link to={`../budgets`} className="quick-link">Presupuesto</Link>
            <Link to={`../goals`} className="quick-link">Objetivos</Link>
            <Link to={`../habits`} className="quick-link">Hábitos</Link>
          </section>
        </>
      )}
    </AppLayout>
  );
}