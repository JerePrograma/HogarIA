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
import { formatMoney } from '../../../domain/formatters';
import type { DashboardSummary } from '../../../domain/types';

type Props = {
  summary: DashboardSummary;
};

const distributionColors = [
  'var(--app-warning)',
  'var(--app-info)',
  'var(--app-success)',
];

export function DashboardCharts({ summary }: Props) {
  const distributionData = [
    {
      name: 'Gastos fijos',
      value: Number(summary.fixedExpenses ?? 0),
    },
    {
      name: 'Gastos variables',
      value: Number(summary.variableExpenses ?? 0),
    },
    {
      name: 'Ahorro',
      value: Number(summary.monthlyBalance?.savings ?? 0),
    },
  ];

  const categoryData = (summary.categoryBreakdown ?? []).map((item) => ({
    categoria: item.categoryName,
    real: Number(item.totalAmount ?? 0),
  }));

  return (
    <section className="grid">
      <article className="panel chart-card">
        <div className="section-title">
          <div>
            <p className="eyebrow">Composición</p>
            <h2>Distribución del mes</h2>
          </div>
        </div>

        <ResponsiveContainer width="100%" height={240}>
          <PieChart>
            <Pie
              data={distributionData}
              dataKey="value"
              nameKey="name"
              outerRadius={92}
              innerRadius={52}
              paddingAngle={3}
            >
              {distributionData.map((entry, index) => (
                <Cell
                  key={entry.name}
                  fill={distributionColors[index % distributionColors.length]}
                />
              ))}
            </Pie>

            <Tooltip formatter={(value) => formatMoney(Number(value))} />
          </PieChart>
        </ResponsiveContainer>
      </article>

      <article className="panel chart-card">
        <div className="section-title">
          <div>
            <p className="eyebrow">Ejecución</p>
            <h2>Presupuesto vs real</h2>
          </div>
        </div>

        {categoryData.length === 0 ? (
          <p className="texto-muted">Todavía no hay categorías con movimientos para graficar.</p>
        ) : (
          <ResponsiveContainer width="100%" height={240}>
            <BarChart data={categoryData}>
              <CartesianGrid strokeDasharray="3 3" stroke="var(--app-border-subtle)" />
              <XAxis dataKey="categoria" hide />
              <YAxis />
              <Tooltip formatter={(value) => formatMoney(Number(value))} />
              <Bar dataKey="real" fill="var(--app-accent)" radius={[8, 8, 0, 0]} />
            </BarChart>
          </ResponsiveContainer>
        )}
      </article>
    </section>
  );
}