import { useQuery } from '@tanstack/react-query';
import { useState } from 'react';
import { Link, useParams } from 'react-router-dom';
import {
  Bar,
  BarChart,
  CartesianGrid,
  Cell,
  Pie,
  PieChart,
  ResponsiveContainer,
  Tooltip,
  XAxis,
  YAxis,
} from 'recharts';

import { getMonthlyDashboard } from '../../api/dashboardApi';
import { AppLayout } from '../../components/layout/AppLayout';
import type {
  CategoryBreakdown,
  CategoryType,
  DashboardSummary,
  FinancialHealth,
} from '../../domain/types';

const moneyFormatter = new Intl.NumberFormat('es-AR', {
  style: 'currency',
  currency: 'ARS',
  maximumFractionDigits: 0,
});

const percentFormatter = new Intl.NumberFormat('es-AR', {
  maximumFractionDigits: 2,
});

const categoryTypeLabels: Record<CategoryType, string> = {
  INCOME: 'Ingreso',
  FIXED_EXPENSE: 'Gasto fijo',
  VARIABLE_EXPENSE: 'Gasto variable',
  SAVING: 'Ahorro',
  DEBT: 'Deuda',
  INVESTMENT: 'Inversión',
};

const financialHealthLabels: Record<FinancialHealth, string> = {
  EXCELLENT: 'Excelente',
  HEALTHY: 'Saludable',
  WARNING: 'Atención',
  CRITICAL: 'Crítico',
};

const formatMoney = (value: unknown) => {
  const numberValue = Number(value ?? 0);
  return Number.isFinite(numberValue) ? moneyFormatter.format(numberValue) : '-';
};

const formatPercent = (value: unknown) => {
  const numberValue = Number(value ?? 0);
  return Number.isFinite(numberValue) ? `${percentFormatter.format(numberValue)}%` : '-';
};

const getCategoryTypeLabel = (type: CategoryType) => {
  return categoryTypeLabels[type] ?? type;
};

const getFinancialHealthLabel = (health?: FinancialHealth) => {
  if (!health) return '-';
  return financialHealthLabels[health] ?? health;
};

export function DashboardPage() {
  const { profileId = '' } = useParams();

  const today = new Date();
  const [year, setYear] = useState(today.getFullYear());
  const [month, setMonth] = useState(today.getMonth() + 1);

  const dashboardQuery = useQuery<DashboardSummary>({
    queryKey: ['dashboard', profileId, year, month],
    queryFn: () => getMonthlyDashboard(profileId, year, month),
    enabled: Boolean(profileId),
  });

  const summary = dashboardQuery.data;

  const distributionData = [
    {
      name: 'Gastos fijos',
      value: Number(summary?.fixedExpenses ?? 0),
    },
    {
      name: 'Gastos variables',
      value: Number(summary?.variableExpenses ?? 0),
    },
    {
      name: 'Ahorro',
      value: Number(summary?.monthlyBalance?.savings ?? 0),
    },
  ];

  const categoryChartData = (summary?.categoryBreakdown ?? []).map(
    (item: CategoryBreakdown) => ({
      categoria: item.categoryName,
      real: Number(item.totalAmount ?? 0),
    }),
  );

  const kpis = [
    {
      title: 'Ingresos',
      value: formatMoney(summary?.monthlyBalance?.totalIncome),
    },
    {
      title: 'Gastos fijos',
      value: formatMoney(summary?.fixedExpenses),
    },
    {
      title: 'Gastos variables',
      value: formatMoney(summary?.variableExpenses),
    },
    {
      title: 'Ahorro',
      value: formatMoney(summary?.monthlyBalance?.savings),
    },
    {
      title: 'Balance',
      value: formatMoney(summary?.monthlyBalance?.balance),
    },
    {
      title: 'Salud financiera',
      value: getFinancialHealthLabel(summary?.financialHealth),
    },
  ];

  const chartColors = ['#14b8a6', '#f59e0b', '#22c55e'];

  return (
    <AppLayout>
      <div className="card">
        <h1>Panel mensual</h1>

        <div className="form-row">
          <label>
            Año
            <input
              className="input"
              type="number"
              min={2000}
              max={2100}
              value={year}
              onChange={(event) => setYear(Number(event.target.value))}
            />
          </label>

          <label>
            Mes
            <input
              className="input"
              type="number"
              min={1}
              max={12}
              value={month}
              onChange={(event) => setMonth(Number(event.target.value))}
            />
          </label>
        </div>

        {dashboardQuery.isLoading && (
          <p className="empty-state">Cargando información financiera...</p>
        )}

        {dashboardQuery.isError && (
          <p className="error-box">
            No se pudo cargar el panel mensual. Revisá que el backend esté corriendo y que el perfil exista.
          </p>
        )}

        {!dashboardQuery.isLoading && !dashboardQuery.isError && !summary && (
          <p className="empty-state">Todavía no hay información para este período.</p>
        )}

        {summary && (
          <>
            <div className="grid">
              {kpis.map((item) => (
                <div className="card" key={item.title}>
                  <b>{item.title}</b>
                  <div className="kpi-value">{item.value}</div>
                </div>
              ))}
            </div>

            <div className="grid">
              <div className="card chart-card">
                <h3>Distribución del mes</h3>

                <ResponsiveContainer width="100%" height={240}>
                  <PieChart>
                    <Pie
                      data={distributionData}
                      dataKey="value"
                      nameKey="name"
                      outerRadius={90}
                    >
                      {distributionData.map((_, index) => (
                        <Cell
                          key={`distribution-${index}`}
                          fill={chartColors[index % chartColors.length]}
                        />
                      ))}
                    </Pie>

                    <Tooltip formatter={(value) => formatMoney(value)} />
                  </PieChart>
                </ResponsiveContainer>
              </div>

              <div className="card chart-card">
                <h3>Presupuesto vs real</h3>

                <ResponsiveContainer width="100%" height={240}>
                  <BarChart data={categoryChartData}>
                    <CartesianGrid strokeDasharray="3 3" />
                    <XAxis dataKey="categoria" hide />
                    <YAxis />
                    <Tooltip formatter={(value) => formatMoney(value)} />
                    <Bar dataKey="real" name="Real" fill="#0f766e" />
                  </BarChart>
                </ResponsiveContainer>
              </div>
            </div>

            <div className="card">
              <h3>Regla 50/30/20</h3>

              <p>
                Gastos fijos: {formatPercent(summary.fiftyThirtyTwenty?.fixedPercent)} |{' '}
                Gastos variables: {formatPercent(summary.fiftyThirtyTwenty?.variablePercent)} |{' '}
                Ahorro: {formatPercent(summary.fiftyThirtyTwenty?.savingPercent)}
              </p>
            </div>

            <div className="card">
              <h3>Desglose por categoría</h3>

              <table className="table">
                <thead>
                  <tr>
                    <th>Categoría</th>
                    <th>Tipo</th>
                    <th>Total</th>
                    <th>% del ingreso</th>
                    <th>Movimientos</th>
                  </tr>
                </thead>

                <tbody>
                  {summary.categoryBreakdown.map((item: CategoryBreakdown) => (
                    <tr key={item.categoryId}>
                      <td>{item.categoryName}</td>
                      <td>{getCategoryTypeLabel(item.categoryType)}</td>
                      <td>{formatMoney(item.totalAmount)}</td>
                      <td>{formatPercent(item.percentOfIncome)}</td>
                      <td>{item.movementCount}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>

            <p>
              <Link to={`/profiles/${profileId}/transactions`}>Movimientos</Link>
              {' · '}
              <Link to={`/profiles/${profileId}/budgets`}>Presupuesto</Link>
              {' · '}
              <Link to={`/profiles/${profileId}/goals`}>Objetivos</Link>
              {' · '}
              <Link to={`/profiles/${profileId}/habits`}>Hábitos</Link>
            </p>
          </>
        )}
      </div>
    </AppLayout>
  );
}