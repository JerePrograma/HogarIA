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
import {
  budgetComparisonStatusLabels,
  categoryTypeLabels,
  labelOrMissing,
  monthLabels,
} from '../../domain/financeLabels';
import { formatMoney, formatPercent } from '../../domain/formatters';
import type {
  BudgetCategoryItem,
  BudgetComparison,
  BudgetComparisonItem,
  Category,
  CategoryType,
} from '../../domain/types';

export function BudgetPage() {
  const { profileId = '' } = useParams();
  const qc = useQueryClient();

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

  const categoriesQuery = useQuery({
    queryKey: ['categories', profileId],
    queryFn: () => listCategories(profileId, true),
    enabled: Boolean(profileId),
  });

  const budgetableCategories = (categoriesQuery.data ?? []).filter((category: Category) => category.active);

  const createYearMutation = useMutation({
    mutationFn: () => createBudgetYear(profileId, { year }),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ['budget-year', profileId, year] });
    },
  });

  const createMonthMutation = useMutation({
    mutationFn: () => createBudgetMonth(profileId, year, { month }),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ['budget-month', profileId, year, month] });
    },
  });

  const saveItemMutation = useMutation({
    mutationFn: ({ categoryId, budgetAmount }: { categoryId: string; budgetAmount: number }) => {
      if (!budgetMonthQuery.data?.id) {
        throw new Error('Primero tenés que crear el mes presupuestario.');
      }

      return upsertBudgetCategoryItem(budgetMonthQuery.data.id, {
        categoryId,
        budgetAmount,
      });
    },
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ['budget-month', profileId, year, month] });
      qc.invalidateQueries({ queryKey: ['budget-comp', profileId, year, month] });
    },
  });

  const findBudgetAmount = (categoryId: string) => {
    const item = budgetMonthQuery.data?.items?.find((i: BudgetCategoryItem) => i.categoryId === categoryId);
    return item?.budgetAmount ?? 0;
  };

  const comparison = comparisonQuery.data as BudgetComparison | undefined;

  return (
    <AppLayout>
      <div className="page-header">
        <div>
          <p className="eyebrow">Planificación</p>
          <h1>Presupuesto</h1>
          <p className="muted">Definí importes esperados y comparalos contra la ejecución real.</p>
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

      <section className="card">
        <div className="status-row">
          <span className={`badge ${budgetYearQuery.data ? 'good' : 'warning'}`}>
            Año: {budgetYearQuery.data ? 'Creado' : 'No creado'}
          </span>
          <span className={`badge ${budgetMonthQuery.data ? 'good' : 'warning'}`}>
            Mes: {budgetMonthQuery.data ? 'Creado' : 'No creado'}
          </span>
        </div>

        <div className="actions">
          <button
            type="button"
            className="button secondary"
            onClick={() => createYearMutation.mutate()}
            disabled={createYearMutation.isPending || Boolean(budgetYearQuery.data)}
          >
            Crear año
          </button>

          <button
            type="button"
            className="button secondary"
            onClick={() => createMonthMutation.mutate()}
            disabled={createMonthMutation.isPending || Boolean(budgetMonthQuery.data)}
          >
            Crear mes
          </button>
        </div>

        {!budgetMonthQuery.data && (
          <div className="alert warning">Primero creá el mes para poder cargar importes presupuestados.</div>
        )}
      </section>

      <section className="card">
        <h2>Carga presupuestaria</h2>

        {!budgetableCategories.length ? (
          <p className="muted">No hay categorías activas para presupuestar.</p>
        ) : (
          <div className="table-wrap">
            <table className="table">
              <thead>
                <tr>
                  <th>Categoría</th>
                  <th>Tipo</th>
                  <th className="amount">Presupuesto</th>
                </tr>
              </thead>
              <tbody>
                {budgetableCategories.map((category: Category) => (
                  <tr key={category.id}>
                    <td>{category.name}</td>
                    <td>{labelOrMissing(categoryTypeLabels, category.type as CategoryType)}</td>
                    <td className="amount">
                      <input
                        className="amount-input"
                        type="number"
                        min="0"
                        defaultValue={findBudgetAmount(category.id)}
                        disabled={!budgetMonthQuery.data}
                        onBlur={(event) =>
                          budgetMonthQuery.data &&
                          saveItemMutation.mutate({
                            categoryId: category.id,
                            budgetAmount: Number(event.currentTarget.value),
                          })
                        }
                      />
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </section>

      <section className="card">
        <h2>Presupuesto vs ejecutado</h2>

        {!comparison?.items?.length ? (
          <p className="muted">No hay datos comparativos para este período.</p>
        ) : (
          <>
            <div className="table-wrap">
              <table className="table">
                <thead>
                  <tr>
                    <th>Categoría</th>
                    <th>Tipo</th>
                    <th className="amount">Presupuesto</th>
                    <th className="amount">Ejecutado</th>
                    <th className="amount">Diferencia</th>
                    <th className="amount">% usado</th>
                    <th>Estado</th>
                  </tr>
                </thead>
                <tbody>
                  {comparison.items.map((item: BudgetComparisonItem) => (
                    <tr key={item.categoryId}>
                      <td>{item.categoryName}</td>
                      <td>{labelOrMissing(categoryTypeLabels, item.categoryType as CategoryType)}</td>
                      <td className="amount">{formatMoney(item.budgetAmount)}</td>
                      <td className="amount">{formatMoney(item.realAmount)}</td>
                      <td className="amount">{formatMoney(item.difference)}</td>
                      <td className="amount">{formatPercent(item.percentUsed)}</td>
                      <td>
                        <span className={`badge ${item.status === 'EXCEEDED' ? 'danger' : item.status === 'WARNING' ? 'warning' : 'good'}`}>
                          {labelOrMissing(budgetComparisonStatusLabels, item.status)}
                        </span>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>

            <p className="summary-line">
              Totales: Presupuesto {formatMoney(comparison.totalBudget)} | Ejecutado {formatMoney(comparison.totalReal)} | Diferencia {formatMoney(comparison.totalDifference)}
            </p>
          </>
        )}
      </section>
    </AppLayout>
  );
}