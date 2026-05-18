import { memo, useMemo, type CSSProperties } from 'react';
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
import type { CategoryType, DashboardSummary } from '../../../domain/types';

type Props = {
  summary: DashboardSummary;
};

type CategoryDatum = {
  id: string;
  categoria: string;
  tipo: CategoryType | null;
  real: number;
  percentOfTotal: number;
  cumulativePercent: number;
  movementCount: number;
  averageMovementAmount: number;
  color: string;
  rank: number;
};

type PieDatum = {
  name: string;
  value: number;
  percentOfTotal: number;
  color: string;
};

type ChartModel = {
  categoryData: CategoryDatum[];
  pieData: PieDatum[];
  barData: CategoryDatum[];
  totalExpense: number;
  totalMovements: number;
  topCategory: CategoryDatum | null;
  visibleCategoriesCount: number;
  concentrationLabel: string;
  averageMovementAmount: number;
};

const MAX_BAR_CATEGORIES = 10;
const MAX_PIE_CATEGORIES = 7;

/**
 * Paleta ordenada por contraste perceptual.
 * Evita usar --app-accent + --app-info como primeros colores porque en dark mode
 * pueden quedar casi idénticos.
 */
const highContrastChartColors = [
  '#4f46e5', // indigo
  '#f97316', // orange
  '#059669', // emerald
  '#dc2626', // red
  '#0284c7', // sky
  '#9333ea', // purple
  '#ca8a04', // amber
  '#0d9488', // teal
  '#be185d', // pink
  '#475569', // slate
  '#2563eb', // blue
  '#16a34a', // green
  '#ea580c', // deep orange
  '#7c3aed', // violet
  '#0891b2', // cyan
];

const otherCategoriesColor = '#64748b';

const excludedTypesFromExpenseCharts: Array<CategoryType | null> = ['INCOME', null];

export function DashboardCharts({ summary }: Props) {
  const model = useMemo(() => buildChartModel(summary), [summary]);

  const {
    categoryData,
    pieData,
    barData,
    totalExpense,
    totalMovements,
    topCategory,
    visibleCategoriesCount,
    concentrationLabel,
    averageMovementAmount,
  } = model;

  const hasData = categoryData.length > 0;

  return (
    <section className="dashboard-charts-grid">
      <article className="panel chart-card chart-card-large">
        <div className="section-title">
          <div>
            <p className="eyebrow">Composición</p>
            <h2>Composición por categoría</h2>
            <p className="muted">
              Distribución de gastos confirmados según las categorías cargadas.
            </p>
          </div>
        </div>

        {!hasData ? (
          <p className="texto-muted">
            Todavía no hay movimientos confirmados por categoría para graficar.
          </p>
        ) : (
          <>
            <div className="chart-summary-grid">
              <ChartMiniStat
                label="Total analizado"
                value={formatMoney(totalExpense)}
                helper="Gastos, deudas, ahorro e inversión"
              />

              <ChartMiniStat
                label="Categoría principal"
                value={topCategory?.categoria ?? 'Sin datos'}
                helper={
                  topCategory
                    ? `${formatPercent(topCategory.percentOfTotal)} del total`
                    : 'Sin movimientos'
                }
                color={topCategory?.color}
              />

              <ChartMiniStat
                label="Concentración"
                value={concentrationLabel}
                helper={`${visibleCategoriesCount} categoría${visibleCategoriesCount === 1 ? '' : 's'} con gasto`}
              />

              <ChartMiniStat
                label="Promedio por mov."
                value={formatMoney(averageMovementAmount)}
                helper={`${totalMovements} movimiento${totalMovements === 1 ? '' : 's'} confirmado${totalMovements === 1 ? '' : 's'}`}
              />
            </div>

            <div className="chart-layout">
              <div className="chart-shell chart-shell-pie">
                <ResponsiveContainer width="100%" height={290}>
                  <PieChart>
                    <Pie
                      data={pieData}
                      dataKey="value"
                      nameKey="name"
                      outerRadius={102}
                      innerRadius={62}
                      paddingAngle={3}
                      labelLine={false}
                      isAnimationActive={pieData.length <= 8}
                    >
                      {pieData.map((entry) => (
                        <Cell key={entry.name} fill={entry.color} />
                      ))}
                    </Pie>

                    <Tooltip
                      formatter={(value, _, payload) => {
                        const item = payload?.payload as PieDatum | undefined;

                        return [
                          `${formatMoney(Number(value))} · ${formatPercent(item?.percentOfTotal ?? 0)}`,
                          'Participación',
                        ];
                      }}
                    />
                  </PieChart>
                </ResponsiveContainer>

                <div className="chart-center-label" aria-hidden="true">
                  <span>Total</span>
                  <strong>{compactMoney(totalExpense)}</strong>
                </div>
              </div>

              <CategoryLegend items={pieData} />
            </div>
          </>
        )}
      </article>

      <article className="panel chart-card chart-card-large">
        <div className="section-title">
          <div>
            <p className="eyebrow">Ranking</p>
            <h2>Gasto real por categoría</h2>
            <p className="muted">
              Ranking de categorías con movimientos confirmados durante el período.
            </p>
          </div>
        </div>

        {barData.length === 0 ? (
          <p className="texto-muted">
            Todavía no hay categorías con movimientos de gasto para graficar.
          </p>
        ) : (
          <>
            <div className="chart-shell">
              <ResponsiveContainer width="100%" height={320}>
                <BarChart data={barData} margin={{ top: 8, right: 8, bottom: 8, left: 0 }}>
                  <CartesianGrid strokeDasharray="3 3" stroke="var(--app-border-subtle)" />
                  <XAxis dataKey="categoria" hide />
                  <YAxis tickFormatter={(value) => compactMoney(Number(value))} width={56} />
                  <Tooltip
                    formatter={(value, _, payload) => {
                      const item = payload?.payload as CategoryDatum | undefined;

                      return [
                        `${formatMoney(Number(value))} · ${formatPercent(item?.percentOfTotal ?? 0)}`,
                        item ? formatCategoryType(item.tipo) : 'Total',
                      ];
                    }}
                    labelFormatter={(_, payload) => {
                      const item = payload?.[0]?.payload as CategoryDatum | undefined;
                      return item ? `#${item.rank} · ${item.categoria}` : '';
                    }}
                  />
                  <Bar dataKey="real" radius={[8, 8, 0, 0]} isAnimationActive={barData.length <= 12}>
                    {barData.map((entry) => (
                      <Cell key={entry.id} fill={entry.color} />
                    ))}
                  </Bar>
                </BarChart>
              </ResponsiveContainer>
            </div>

            {categoryData.length > MAX_BAR_CATEGORIES && (
              <p className="texto-muted chart-note">
                Se muestran las {MAX_BAR_CATEGORIES} categorías principales. El detalle completo está en la tabla.
              </p>
            )}
          </>
        )}
      </article>

      <article className="panel chart-card chart-card-table">
        <div className="section-title">
          <div>
            <p className="eyebrow">Detalle</p>
            <h2>Participación por categoría</h2>
            <p className="muted">
              Monto, porcentaje, acumulado, promedio y cantidad de movimientos por categoría.
            </p>
          </div>

          {totalMovements > 0 && (
            <span className="badge-ui badge-info">
              {totalMovements} movimiento{totalMovements === 1 ? '' : 's'}
            </span>
          )}
        </div>

        {!hasData ? (
          <p className="texto-muted">
            No hay gastos confirmados para detallar en este período.
          </p>
        ) : (
          <CategoryBreakdownTable items={categoryData} />
        )}
      </article>
    </section>
  );
}

const ChartMiniStat = memo(function ChartMiniStat({
  label,
  value,
  helper,
  color,
}: {
  label: string;
  value: string;
  helper: string;
  color?: string;
}) {
  return (
    <div
      className="chart-mini-stat"
      style={
        color
          ? ({
              '--category-accent': color,
            } as CSSProperties)
          : undefined
      }
    >
      <span>{label}</span>
      <strong>{value}</strong>
      <p>{helper}</p>
    </div>
  );
});

const CategoryLegend = memo(function CategoryLegend({ items }: { items: PieDatum[] }) {
  return (
    <div className="chart-legend-list" aria-label="Referencias de composición por categoría">
      {items.map((item) => (
        <div key={item.name} className="chart-legend-item">
          <span
            className="category-color-dot"
            style={{ backgroundColor: item.color }}
            aria-hidden="true"
          />

          <div className="min-w-0">
            <p>{item.name}</p>
            <span>
              {formatMoney(item.value)} · {formatPercent(item.percentOfTotal)}
            </span>
          </div>
        </div>
      ))}
    </div>
  );
});

const CategoryBreakdownTable = memo(function CategoryBreakdownTable({
  items,
}: {
  items: CategoryDatum[];
}) {
  return (
    <div className="tabla-ui category-breakdown-table">
      <table>
        <thead>
          <tr>
            <th className="w-12 text-right">#</th>
            <th>Categoría</th>
            <th>Tipo</th>
            <th className="text-right">Monto</th>
            <th className="text-right">%</th>
            <th className="text-right">Acum.</th>
            <th>Participación</th>
            <th className="text-right">Prom.</th>
            <th className="text-right">Mov.</th>
          </tr>
        </thead>

        <tbody>
          {items.map((item) => (
            <tr key={item.id}>
              <td className="amount-cell text-faint">#{item.rank}</td>

              <td>
                <div className="category-name-cell">
                  <span
                    className="category-color-dot"
                    style={{ backgroundColor: item.color }}
                    aria-hidden="true"
                  />

                  <span className="truncate">{item.categoria}</span>
                </div>
              </td>

              <td>
                <span className={`badge-ui ${getCategoryBadgeClass(item.tipo)}`}>
                  {formatCategoryType(item.tipo)}
                </span>
              </td>

              <td className="amount-cell">{formatMoney(item.real)}</td>

              <td className="amount-cell">{formatPercent(item.percentOfTotal)}</td>

              <td className="amount-cell texto-muted">{formatPercent(item.cumulativePercent)}</td>

              <td>
                <div
                  className="category-progress-track"
                  aria-label={`${item.categoria}: ${formatPercent(item.percentOfTotal)}`}
                >
                  <div
                    className="category-progress-fill"
                    style={
                      {
                        width: `${clampPercent(item.percentOfTotal)}%`,
                        backgroundColor: item.color,
                      } as CSSProperties
                    }
                  />
                </div>
              </td>

              <td className="amount-cell texto-muted">
                {formatMoney(item.averageMovementAmount)}
              </td>

              <td className="amount-cell">{item.movementCount}</td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
});

function buildChartModel(summary: DashboardSummary): ChartModel {
  const categoryData = buildCategoryData(summary);
  const pieData = buildPieData(categoryData);
  const barData = categoryData.slice(0, MAX_BAR_CATEGORIES);
  const totalExpense = sumValues(categoryData.map((item) => item.real));
  const totalMovements = sumValues(categoryData.map((item) => item.movementCount));
  const topCategory = categoryData[0] ?? null;
  const visibleCategoriesCount = categoryData.length;
  const concentrationLabel = getConcentrationLabel(topCategory?.percentOfTotal ?? 0);
  const averageMovementAmount = totalMovements > 0 ? totalExpense / totalMovements : 0;

  return {
    categoryData,
    pieData,
    barData,
    totalExpense,
    totalMovements,
    topCategory,
    visibleCategoriesCount,
    concentrationLabel,
    averageMovementAmount,
  };
}

function buildCategoryData(summary: DashboardSummary): CategoryDatum[] {
  const sortedItems = (summary.categoryBreakdown ?? [])
    .filter((item) => !excludedTypesFromExpenseCharts.includes(item.categoryType))
    .map((item) => ({
      id: item.categoryId,
      categoria: item.categoryName,
      tipo: item.categoryType,
      real: toPositiveNumber(item.totalAmount),
      movementCount: toPositiveInteger(item.movementCount),
    }))
    .filter((item) => item.real > 0)
    .sort((a, b) => {
      if (b.real !== a.real) {
        return b.real - a.real;
      }

      return a.categoria.localeCompare(b.categoria, 'es');
    });

  const total = sumValues(sortedItems.map((item) => item.real));

  let cumulativePercent = 0;

  return sortedItems.map((item, index) => {
    const percentOfTotal = total > 0 ? (item.real * 100) / total : 0;
    cumulativePercent += percentOfTotal;

    return {
      ...item,
      rank: index + 1,
      color: getRankColor(index),
      percentOfTotal,
      cumulativePercent,
      averageMovementAmount: item.movementCount > 0 ? item.real / item.movementCount : 0,
    };
  });
}

function buildPieData(categoryData: CategoryDatum[]): PieDatum[] {
  if (categoryData.length <= MAX_PIE_CATEGORIES + 1) {
    return categoryData.map(toPieDatum);
  }

  const visibleItems = categoryData.slice(0, MAX_PIE_CATEGORIES);
  const hiddenItems = categoryData.slice(MAX_PIE_CATEGORIES);
  const hiddenTotal = sumValues(hiddenItems.map((item) => item.real));
  const hiddenPercent = sumValues(hiddenItems.map((item) => item.percentOfTotal));

  return [
    ...visibleItems.map(toPieDatum),
    {
      name: 'Otras categorías',
      value: hiddenTotal,
      percentOfTotal: hiddenPercent,
      color: otherCategoriesColor,
    },
  ].filter((item) => item.value > 0);
}

function toPieDatum(item: CategoryDatum): PieDatum {
  return {
    name: item.categoria,
    value: item.real,
    percentOfTotal: item.percentOfTotal,
    color: item.color,
  };
}

function getRankColor(index: number): string {
  return highContrastChartColors[index % highContrastChartColors.length];
}

function toPositiveNumber(value: number | null | undefined): number {
  const numericValue = Number(value ?? 0);

  if (!Number.isFinite(numericValue)) {
    return 0;
  }

  return Math.max(numericValue, 0);
}

function toPositiveInteger(value: number | null | undefined): number {
  const numericValue = Number(value ?? 0);

  if (!Number.isFinite(numericValue)) {
    return 0;
  }

  return Math.max(Math.trunc(numericValue), 0);
}

function sumValues(values: number[]): number {
  return values.reduce((total, value) => total + value, 0);
}

function clampPercent(value: number): number {
  return Math.min(Math.max(value, 0), 100);
}

function formatPercent(value: number): string {
  return `${clampPercent(value).toFixed(value >= 10 ? 0 : 1)}%`;
}

function compactMoney(value: number): string {
  if (Math.abs(value) >= 1_000_000) {
    return `$${(value / 1_000_000).toFixed(1)}M`;
  }

  if (Math.abs(value) >= 1_000) {
    return `$${Math.round(value / 1_000)}k`;
  }

  return `$${Math.round(value)}`;
}

function getConcentrationLabel(topPercent: number): string {
  if (topPercent >= 50) {
    return 'Alta';
  }

  if (topPercent >= 30) {
    return 'Media';
  }

  return 'Distribuida';
}

function formatCategoryType(type: CategoryType | null | undefined): string {
  if (!type) {
    return 'Sin tipo';
  }

  const labels: Record<CategoryType, string> = {
    INCOME: 'Ingreso',
    FIXED_EXPENSE: 'Gasto fijo',
    VARIABLE_EXPENSE: 'Gasto variable',
    SAVING: 'Ahorro',
    DEBT: 'Deuda',
    INVESTMENT: 'Inversión',
  };

  return labels[type];
}

function getCategoryBadgeClass(type: CategoryType | null | undefined): string {
  if (type === 'FIXED_EXPENSE') {
    return 'badge-warning';
  }

  if (type === 'VARIABLE_EXPENSE') {
    return 'badge-info';
  }

  if (type === 'SAVING') {
    return 'badge-success';
  }

  if (type === 'DEBT') {
    return 'badge-danger';
  }

  if (type === 'INVESTMENT') {
    return 'badge-muted';
  }

  return 'badge-muted';
}