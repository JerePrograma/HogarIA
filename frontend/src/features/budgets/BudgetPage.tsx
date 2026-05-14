import axios from 'axios';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useState } from 'react';
import { useParams } from 'react-router-dom';
import {
  createBudgetMonth,
  createBudgetYear,
  getBudgetComparison,
  getBudgetMonth,
  getBudgetYear,
  upsertBudgetCategoryItem,
} from '../../api/budgetsApi';
import { listCategories } from '../../api/categoriesApi';
import { AppLayout } from '../../components/layout/AppLayout';
import { EmptyState } from '../../components/ui/EmptyState';
import { ErrorState } from '../../components/ui/ErrorState';
import { MetricCard } from '../../components/ui/MetricCard';
import { MonthSelector } from '../../components/ui/MonthSelector';
import { StatusBadge } from '../../components/ui/StatusBadge';
import {
  budgetComparisonStatusLabels,
  categoryTypeLabels,
  labelOrValue,
} from '../../domain/financeLabels';
import { formatMoney, formatPercent } from '../../domain/formatters';
import type {
  BudgetCategoryItem,
  BudgetComparisonItem,
  Category,
  CategoryType,
} from '../../domain/types';

const isNotFound = (error: unknown) =>
  axios.isAxiosError(error) && error.response?.status === 404;

const statusTone = (status: string): 'ok' | 'watch' | 'critical' => {
  if (status === 'EXCEEDED') return 'critical';
  if (status === 'WARNING') return 'watch';
  return 'ok';
};

export function BudgetPage() {
  const { profileId = '' } = useParams();
  const queryClient = useQueryClient();

  const today = new Date();
  const [year, setYear] = useState(today.getFullYear());
  const [month, setMonth] = useState(today.getMonth() + 1);

  const budgetYearQuery = useQuery({
    queryKey: ['budget-year', profileId, year],
    queryFn: () => getBudgetYear(profileId, year),
    retry: false,
    enabled: Boolean(profileId),
  });

  const budgetMonthQuery = useQuery({
    queryKey: ['budget-month', profileId, year, month],
    queryFn: () => getBudgetMonth(profileId, year, month),
    retry: false,
    enabled: Boolean(profileId),
  });

  const comparisonQuery = useQuery({
    queryKey: ['budget-comp', profileId, year, month],
    queryFn: () => getBudgetComparison(profileId, year, month),
    retry: false,
    enabled: Boolean(profileId),
  });

  const categoriesQuery = useQuery<Category[]>({
    queryKey: ['categories', profileId],
    queryFn: () => listCategories(profileId, true),
    enabled: Boolean(profileId),
  });

  const budgetYearMissing = budgetYearQuery.isError && isNotFound(budgetYearQuery.error);
  const budgetMonthMissing = budgetMonthQuery.isError && isNotFound(budgetMonthQuery.error);
  const comparisonMissing = comparisonQuery.isError && isNotFound(comparisonQuery.error);

  const budgetableCategories = (categoriesQuery.data ?? []).filter((category) => category.active);

  const createYearMutation = useMutation({
    mutationFn: () => createBudgetYear(profileId, { year }),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['budget-year', profileId, year] });
    },
  });

  const createMonthMutation = useMutation({
    mutationFn: () => createBudgetMonth(profileId, year, { month }),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['budget-month', profileId, year, month] });
    },
  });

  const saveItemMutation = useMutation({
    mutationFn: ({
      categoryId,
      budgetAmount,
    }: {
      categoryId: string;
      budgetAmount: number;
    }) => {
      if (!budgetMonthQuery.data?.id) {
        throw new Error('Primero tenés que crear el mes presupuestario.');
      }

      return upsertBudgetCategoryItem(budgetMonthQuery.data.id, {
        categoryId,
        budgetAmount,
      });
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['budget-month', profileId, year, month] });
      queryClient.invalidateQueries({ queryKey: ['budget-comp', profileId, year, month] });
    },
  });

  const findBudgetAmount = (categoryId: string) => {
    const item = budgetMonthQuery.data?.items?.find(
      (budgetItem: BudgetCategoryItem) => budgetItem.categoryId === categoryId,
    );

    return item?.budgetAmount ?? 0;
  };

  return (
    <AppLayout>
      <div className="page-stack">
        <section className="page-header">
          <div>
            <p className="eyebrow">Plan financiero</p>
            <h1>Presupuesto</h1>
            <p className="muted">
              Definí topes por categoría y compará el presupuesto contra movimientos reales.
            </p>
          </div>

          <div className="stack-ui md:min-w-[360px]">
            <MonthSelector
              year={year}
              month={month}
              onYearChange={setYear}
              onMonthChange={setMonth}
            />

            <div className="page-actions">
              <button
                type="button"
                className="boton-secundario"
                onClick={() => createYearMutation.mutate()}
                disabled={createYearMutation.isPending}
              >
                {createYearMutation.isPending ? 'Creando año...' : 'Crear año'}
              </button>

              <button
                type="button"
                className="boton-principal"
                onClick={() => createMonthMutation.mutate()}
                disabled={createMonthMutation.isPending}
              >
                {createMonthMutation.isPending ? 'Creando mes...' : 'Crear mes'}
              </button>
            </div>
          </div>
        </section>

        <section className="metric-grid">
          <MetricCard
            title="Año presupuestario"
            value={budgetYearQuery.data ? 'OK' : budgetYearMissing ? 'No existe' : 'Error'}
            helper="Estado del presupuesto anual."
            tone={budgetYearQuery.data ? 'success' : budgetYearMissing ? 'warning' : 'danger'}
          />

          <MetricCard
            title="Mes presupuestario"
            value={budgetMonthQuery.data ? 'OK' : budgetMonthMissing ? 'No existe' : 'Error'}
            helper="Estado del mes seleccionado."
            tone={budgetMonthQuery.data ? 'success' : budgetMonthMissing ? 'warning' : 'danger'}
          />

          <MetricCard
            title="Presupuesto total"
            value={formatMoney(comparisonQuery.data?.totalBudget)}
            helper="Suma presupuestada del período."
            tone="info"
          />

          <MetricCard
            title="Real total"
            value={formatMoney(comparisonQuery.data?.totalReal)}
            helper="Gasto/ejecución real registrado."
            tone="warning"
          />

          <MetricCard
            title="Diferencia"
            value={formatMoney(comparisonQuery.data?.totalDifference)}
            helper="Brecha entre presupuesto y realidad."
            tone={(comparisonQuery.data?.totalDifference ?? 0) >= 0 ? 'success' : 'danger'}
          />
        </section>

        {!budgetMonthQuery.data ? (
          <EmptyState
            title="Mes no creado"
            message="Primero creá el mes para poder cargar importes presupuestados."
          />
        ) : null}

        {budgetYearQuery.isError && !budgetYearMissing ? (
          <ErrorState message="No se pudo consultar el presupuesto anual." />
        ) : null}

        {budgetMonthQuery.isError && !budgetMonthMissing ? (
          <ErrorState message="No se pudo consultar el presupuesto mensual." />
        ) : null}

        {comparisonQuery.isError && !comparisonMissing ? (
          <ErrorState message="No se pudo consultar la comparación presupuesto vs real." />
        ) : null}

        {comparisonMissing ? (
          <EmptyState
            title="Sin comparación"
            message="No hay presupuesto cargado para este período."
          />
        ) : null}

        <section className="panel">
          <div className="section-title">
            <div>
              <p className="eyebrow">Carga</p>
              <h2>Carga presupuestaria</h2>
              <p className="secondary-text">
                Cargá el importe esperado por categoría. Se guarda al salir del campo.
              </p>
            </div>
          </div>

          <div className="tabla-ui">
            <table>
              <thead>
                <tr>
                  <th>Categoría</th>
                  <th>Tipo</th>
                  <th>Presupuesto</th>
                </tr>
              </thead>

              <tbody>
                {budgetableCategories.map((category) => (
                  <tr key={category.id}>
                    <td>
                      <strong>{category.name}</strong>
                    </td>

                    <td>{labelOrValue(categoryTypeLabels, category.type as CategoryType)}</td>

                    <td>
                      <input
                        className="input-ui"
                        type="number"
                        min={0}
                        defaultValue={findBudgetAmount(category.id)}
                        disabled={!budgetMonthQuery.data || saveItemMutation.isPending}
                        onBlur={(event) =>
                          budgetMonthQuery.data &&
                          saveItemMutation.mutate({
                            categoryId: category.id,
                            budgetAmount: Number(event.target.value),
                          })
                        }
                      />
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>

          {saveItemMutation.isError ? (
            <p className="mensaje-error mt-4">No se pudo guardar el importe presupuestado.</p>
          ) : null}
        </section>

        <section className="panel">
          <div className="section-title">
            <div>
              <p className="eyebrow">Ejecución</p>
              <h2>Presupuesto vs real</h2>
            </div>
          </div>

          <div className="tabla-ui">
            <table>
              <thead>
                <tr>
                  <th>Categoría</th>
                  <th>Tipo</th>
                  <th className="amount-cell">Presupuesto</th>
                  <th className="amount-cell">Real</th>
                  <th className="amount-cell">Diferencia</th>
                  <th>% usado</th>
                  <th>Estado</th>
                </tr>
              </thead>

              <tbody>
                {comparisonQuery.data?.items?.map((item: BudgetComparisonItem) => (
                  <tr key={item.categoryId}>
                    <td>
                      <strong>{item.categoryName}</strong>
                    </td>

                    <td>{labelOrValue(categoryTypeLabels, item.categoryType as CategoryType)}</td>
                    <td className="amount-cell">{formatMoney(item.budgetAmount)}</td>
                    <td className="amount-cell">{formatMoney(item.realAmount)}</td>
                    <td className="amount-cell">{formatMoney(item.difference)}</td>
                    <td>{formatPercent(item.percentUsed)}</td>

                    <td>
                      <StatusBadge
                        tone={statusTone(item.status)}
                        label={labelOrValue(budgetComparisonStatusLabels, item.status)}
                      />
                    </td>
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