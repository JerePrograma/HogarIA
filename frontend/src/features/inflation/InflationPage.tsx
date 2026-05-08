import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useState } from 'react';
import { Line, LineChart, ResponsiveContainer, Tooltip, XAxis, YAxis, CartesianGrid } from 'recharts';
import { createInflation, getAccumulatedInflation, listInflation } from '../../api/inflationApi';
import { AppLayout } from '../../components/layout/AppLayout';
import { monthLabels } from '../../domain/financeLabels';
import { formatPercent } from '../../domain/formatters';

export function InflationPage() {
  const today = new Date();

  const [year, setYear] = useState(today.getFullYear());
  const [month, setMonth] = useState(today.getMonth() + 1);
  const [ratePercent, setRatePercent] = useState(3);

  const qc = useQueryClient();

  const inflationQuery = useQuery({
    queryKey: ['inflation', year],
    queryFn: () => listInflation(year),
  });

  const accumulatedQuery = useQuery({
    queryKey: ['inflation-acc', year],
    queryFn: () => getAccumulatedInflation(year, 1, year, 12),
  });

  const createInflationMutation = useMutation({
    mutationFn: () =>
      createInflation({
        year,
        month,
        rate: ratePercent / 100,
        source: 'MANUAL',
        observed: true,
      }),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ['inflation', year] });
      qc.invalidateQueries({ queryKey: ['inflation-acc', year] });
    },
  });

  const chartData =
    inflationQuery.data?.map((item: any) => ({
      month: monthLabels[item.month as number] ?? item.month,
      rate: Number(item.rate ?? 0) * 100,
    })) ?? [];

  return (
    <AppLayout>
      <div className="page-header">
        <div>
          <p className="eyebrow">Contexto económico</p>
          <h1>Inflación y proyecciones</h1>
          <p className="muted">Registrá índices mensuales y revisá el acumulado anual.</p>
        </div>
      </div>

      <section className="card">
        <h2>Cargar índice mensual</h2>

        <div className="form-grid">
          <label className="field">
            <span>Año</span>
            <input type="number" value={year} onChange={(event) => setYear(Number(event.target.value))} />
          </label>

          <label className="field">
            <span>Mes</span>
            <select value={month} onChange={(event) => setMonth(Number(event.target.value))}>
              {Object.entries(monthLabels).map(([value, label]) => (
                <option key={value} value={value}>
                  {label}
                </option>
              ))}
            </select>
          </label>

          <label className="field">
            <span>Tasa mensual de inflación (%)</span>
            <input
              type="number"
              step="0.01"
              value={ratePercent}
              placeholder="Ej: 12.4"
              onChange={(event) => setRatePercent(Number(event.target.value))}
            />
          </label>
        </div>

        <div className="actions">
          <button
            type="button"
            className="button primary"
            onClick={() => createInflationMutation.mutate()}
            disabled={createInflationMutation.isPending}
          >
            Guardar índice
          </button>
        </div>
      </section>

      <section className="card">
        <div className="section-header">
          <div>
            <h2>Evolución anual</h2>
            <p className="muted">
              Acumulada anual:{' '}
              <strong>{formatPercent(Number(accumulatedQuery.data?.accumulatedRate ?? 0) * 100)}</strong>
            </p>
          </div>
        </div>

        <div className="chart-box">
          <ResponsiveContainer width="100%" height={320}>
            <LineChart data={chartData}>
              <CartesianGrid strokeDasharray="3 3" />
              <XAxis dataKey="month" />
              <YAxis />
              <Tooltip
                formatter={(value) => [formatPercent(Number(value)), 'Inflación mensual']}
                labelFormatter={(label) => `Mes: ${label}`}
              />
              <Line type="monotone" dataKey="rate" name="Inflación mensual" stroke="var(--chart-1)" strokeWidth={3} />
            </LineChart>
          </ResponsiveContainer>
        </div>
      </section>
    </AppLayout>
  );
}