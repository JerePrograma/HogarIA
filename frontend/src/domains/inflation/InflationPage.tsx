import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useMemo, useState } from 'react';
import {
  CartesianGrid,
  Line,
  LineChart,
  ResponsiveContainer,
  Tooltip,
  XAxis,
  YAxis,
} from 'recharts';
import {
  createInflation,
  getAccumulatedInflation,
  listInflation,
} from '../../api/inflationApi';
import { getApiErrorMessage } from '../../api/http';
import { AppLayout } from '../../app/shell/AppShell';
import { EmptyState } from '../../shared/ui/EmptyState';
import { ErrorState } from '../../shared/ui/ErrorState';
import { MetricCard } from '../../shared/ui/MetricCard';
import { StatusBadge } from '../../shared/ui/StatusBadge';
import { formatMonth, formatPercent } from '../../domain/formatters';
import { queryKeys } from '../../domain/queryKeys';
import type { InflationIndex } from '../../domain/types';

export function InflationPage() {
  const today = new Date();

  const [year, setYear] = useState(today.getFullYear());
  const [month, setMonth] = useState(today.getMonth() + 1);
  const [monthlyRate, setMonthlyRate] = useState(0.03);
  const [projection, setProjection] = useState(false);
  const [message, setMessage] = useState('');

  const queryClient = useQueryClient();

  const inflationQuery = useQuery<InflationIndex[]>({
    queryKey: queryKeys.inflationYear(year),
    queryFn: () => listInflation(year),
  });

  const accumulatedQuery = useQuery({
    queryKey: queryKeys.inflationAccumulatedYear(year),
    queryFn: () => getAccumulatedInflation(year, 1, year, 12),
  });

  const createMutation = useMutation({
    mutationFn: () =>
      createInflation({
        year,
        month,
        monthlyRate,
        source: 'MANUAL',
        projection,
      }),
    onSuccess: () => {
      setMessage('Índice guardado.');
      queryClient.invalidateQueries({ queryKey: queryKeys.inflationYear(year) });
      queryClient.invalidateQueries({ queryKey: queryKeys.inflationAccumulatedYear(year) });
    },
    onError: (error) => setMessage(getApiErrorMessage(error)),
  });

  const indexes = inflationQuery.data ?? [];

  const chartData = useMemo(
    () =>
      indexes.map((index) => ({
        mes: formatMonth(index.month),
        porcentaje: Number(index.monthlyRate) * 100,
        tipo: index.projection ? 'Proyectado' : 'Real',
      })),
    [indexes],
  );

  const averageRate = useMemo(() => {
    if (indexes.length === 0) return 0;

    const total = indexes.reduce((acc, index) => acc + Number(index.monthlyRate ?? 0), 0);
    return (total / indexes.length) * 100;
  }, [indexes]);

  const projectedCount = indexes.filter((index) => index.projection).length;
  const realCount = indexes.length - projectedCount;
  const accumulatedRate = Number(accumulatedQuery.data?.accumulatedRate ?? 0) * 100;

  return (
    <AppLayout>
      <div className="page-stack">
        <section className="page-header">
          <div>
            <p className="eyebrow">Variables económicas</p>
            <h1>Inflación y proyecciones</h1>
            <p className="muted">
              Cargá índices reales o proyectados para analizar evolución anual y supuestos financieros.
            </p>
          </div>
        </section>

        <section className="metric-grid">
          <MetricCard
            title="Acumulada anual"
            value={formatPercent(accumulatedRate)}
            helper="Tasa acumulada del año seleccionado."
            tone={accumulatedRate > 0 ? 'warning' : 'neutral'}
          />

          <MetricCard
            title="Promedio mensual"
            value={formatPercent(averageRate)}
            helper="Promedio simple de índices cargados."
            tone="info"
          />

          <MetricCard
            title="Meses reales"
            value={realCount}
            helper="Índices no proyectados."
            tone="success"
          />

          <MetricCard
            title="Meses proyectados"
            value={projectedCount}
            helper="Supuestos o estimaciones."
            tone={projectedCount > 0 ? 'warning' : 'neutral'}
          />
        </section>

        <section className="panel">
          <div className="section-title">
            <div>
              <p className="eyebrow">Carga manual</p>
              <h2>Guardar índice mensual</h2>
              <p className="secondary-text">
                Ingresá la tasa como porcentaje humano. Se envía al backend como decimal.
              </p>
            </div>
          </div>

          <div className="form-row">
            <label>
              Año
              <input
                className="input-ui"
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
                className="input-ui"
                type="number"
                value={month}
                min={1}
                max={12}
                onChange={(event) => setMonth(Number(event.target.value))}
              />
            </label>

            <label>
              Tasa mensual (%)
              <input
                className="input-ui"
                type="number"
                step="0.01"
                value={Number((monthlyRate * 100).toFixed(4))}
                onChange={(event) => setMonthlyRate(Number(event.target.value) / 100)}
              />
              <span className="compact-muted">
                Ejemplo: 3 representa 3%, guardado como 0.03.
              </span>
            </label>

            <label className="surface-inset cluster-ui">
              <input
                type="checkbox"
                checked={projection}
                onChange={(event) => setProjection(event.target.checked)}
              />
              <span>Proyectado</span>
            </label>

            <button
              type="button"
              className="boton-principal"
              disabled={createMutation.isPending}
              onClick={() => createMutation.mutate()}
            >
              {createMutation.isPending ? 'Guardando...' : 'Guardar índice'}
            </button>
          </div>

          {message ? (
            <p className={message.toLowerCase().includes('error') ? 'mensaje-error' : 'mensaje-exito'}>
              {message}
            </p>
          ) : null}
        </section>

        <section className="panel chart-card">
          <div className="section-title">
            <div>
              <p className="eyebrow">Evolución</p>
              <h2>Inflación mensual</h2>
            </div>
          </div>

          {inflationQuery.isLoading ? (
            <EmptyState title="Cargando índices" message="Estamos consultando la inflación del año." />
          ) : null}

          {inflationQuery.isError ? (
            <ErrorState message="No se pudo cargar la serie de inflación." />
          ) : null}

          {!inflationQuery.isLoading && !inflationQuery.isError && chartData.length === 0 ? (
            <EmptyState title="Sin índices" message="Todavía no hay datos de inflación para este año." />
          ) : null}

          {chartData.length > 0 ? (
            <ResponsiveContainer width="100%" height={280}>
              <LineChart data={chartData}>
                <CartesianGrid strokeDasharray="3 3" stroke="var(--app-border-subtle)" />
                <XAxis dataKey="mes" />
                <YAxis />
                <Tooltip formatter={(value) => formatPercent(Number(value))} />
                <Line
                  type="monotone"
                  dataKey="porcentaje"
                  stroke="var(--app-accent)"
                  strokeWidth={3}
                  dot={{ r: 4 }}
                  activeDot={{ r: 6 }}
                />
              </LineChart>
            </ResponsiveContainer>
          ) : null}
        </section>

        <section className="panel">
          <div className="section-title">
            <div>
              <p className="eyebrow">Detalle</p>
              <h2>Índices cargados</h2>
            </div>

            <span className="badge-count">{indexes.length}</span>
          </div>

          <div className="tabla-ui">
            <table className="table-compact">
              <thead>
                <tr>
                  <th>Mes</th>
                  <th>Tasa mensual</th>
                  <th>Tipo</th>
                  <th>Fuente</th>
                </tr>
              </thead>

              <tbody>
                {indexes.map((index) => (
                  <tr key={index.id}>
                    <td>
                      <strong>{formatMonth(index.month)}</strong>
                    </td>

                    <td>{formatPercent(Number(index.monthlyRate) * 100)}</td>

                    <td>
                      <StatusBadge
                        tone={index.projection ? 'watch' : 'ok'}
                        label={index.projection ? 'Proyectado' : 'Real'}
                      />
                    </td>

                    <td>{index.source ?? 'Sin fuente'}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </section>
      </div>
    </AppLayout>
  );
}
