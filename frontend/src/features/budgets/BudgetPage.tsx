// src/features/budgets/BudgetPage.tsx

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

import {
  budgetComparisonStatusLabels,
  categoryTypeLabels,
  labelOrValue,
  monthLabels,
} from '../../domain/financeLabels';

import { formatMoney, formatPercent } from '../../domain/formatters';

import type {
  BudgetCategoryItem,
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


  const isNotFound = (error: unknown) => axios.isAxiosError(error) && error.response?.status === 404;

  const budgetYearMissing = budgetYearQuery.isError && isNotFound(budgetYearQuery.error);
  const budgetMonthMissing = budgetMonthQuery.isError && isNotFound(budgetMonthQuery.error);
  const comparisonMissing = comparisonQuery.isError && isNotFound(comparisonQuery.error);

  const budgetableCategories = (categoriesQuery.data ?? []).filter(
    (category: Category) => category.active,
  );

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
      qc.invalidateQueries({ queryKey: ['budget-month', profileId, year, month] });
      qc.invalidateQueries({ queryKey: ['budget-comp', profileId, year, month] });
    },
  });

  const findBudgetAmount = (categoryId: string) => {
    const item = budgetMonthQuery.data?.items?.find(
      (i: BudgetCategoryItem) => i.categoryId === categoryId,
    );

    return item?.budgetAmount ?? 0;
  };

  return (
    <AppLayout>
      <div className="card">
        <h1>Presupuesto</h1>

        <div className="form-row">
          <input
            className="input"
            type="number"
            value={year}
            min={2000}
            max={2100}
            onChange={(e) => setYear(Number(e.target.value))}
          />

          <select
            className="select"
            value={month}
            onChange={(e) => setMonth(Number(e.target.value))}
          >
            {Object.entries(monthLabels).map(([value, label]) => (
              <option key={value} value={value}>
                {label}
              </option>
            ))}
          </select>

          <button
            onClick={() => createYearMutation.mutate()}
            disabled={createYearMutation.isPending}
          >
            Crear año
          </button>

          <button
            onClick={() => createMonthMutation.mutate()}
            disabled={createMonthMutation.isPending}
          >
            Crear mes
          </button>
        </div>

        <p>
          Año: {budgetYearQuery.data ? 'OK' : budgetYearMissing ? 'No existe' : 'Error'} | Mes:{' '}
          {budgetMonthQuery.data ? 'OK' : budgetMonthMissing ? 'No existe' : 'Error'}
        </p>

        {!budgetMonthQuery.data && (
          <p className="empty-state">
            Primero creá el mes para poder cargar importes presupuestados.
          </p>
        )}


        {budgetYearQuery.isError && !budgetYearMissing && (
          <p className="error-box">No se pudo consultar el presupuesto anual.</p>
        )}

        {budgetMonthQuery.isError && !budgetMonthMissing && (
          <p className="error-box">No se pudo consultar el presupuesto mensual.</p>
        )}

        {comparisonQuery.isError && !comparisonMissing && (
          <p className="error-box">No se pudo consultar la comparación presupuesto vs real.</p>
        )}

        {comparisonMissing && (
          <p className="empty-state">No hay presupuesto cargado para este período.</p>
        )}

        <h3>Carga presupuestaria</h3>

        <table className="table">
          <thead>
            <tr>
              <th>Categoría</th>
              <th>Tipo</th>
              <th>Presupuesto</th>
            </tr>
          </thead>

          <tbody>
            {budgetableCategories.map((category: Category) => (
              <tr key={category.id}>
                <td>{category.name}</td>

                <td>
                  {labelOrValue(
                    categoryTypeLabels,
                    category.type as CategoryType,
                  )}
                </td>

                <td>
                  <input
                    className="input"
                    type="number"
                    min={0}
                    defaultValue={findBudgetAmount(category.id)}
                    disabled={!budgetMonthQuery.data}
                    onBlur={(e) =>
                      budgetMonthQuery.data &&
                      saveItemMutation.mutate({
                        categoryId: category.id,
                        budgetAmount: Number(e.target.value),
                      })
                    }
                  />
                </td>
              </tr>
            ))}
          </tbody>
        </table>

        <h3>Presupuesto vs Real</h3>

        <table className="table">
          <thead>
            <tr>
              <th>Categoría</th>
              <th>Tipo</th>
              <th>Presupuesto</th>
              <th>Real</th>
              <th>Diferencia</th>
              <th>% usado</th>
              <th>Estado</th>
            </tr>
          </thead>

          <tbody>
            {comparisonQuery.data?.items?.map((item: BudgetComparisonItem) => (
              <tr key={item.categoryId}>
                <td>{item.categoryName}</td>

                <td>
                  {labelOrValue(
                    categoryTypeLabels,
                    item.categoryType as CategoryType,
                  )}
                </td>

                <td>{formatMoney(item.budgetAmount)}</td>
                <td>{formatMoney(item.realAmount)}</td>
                <td>{formatMoney(item.difference)}</td>
                <td>{formatPercent(item.percentUsed)}</td>

                <td>
                  <span
                    className={`badge ${
                      item.status === 'EXCEEDED'
                        ? 'badge-danger'
                        : item.status === 'WARNING'
                          ? 'badge-warning'
                          : 'badge-ok'
                    }`}
                  >
                    {labelOrValue(budgetComparisonStatusLabels, item.status)}
                  </span>
                </td>
              </tr>
            ))}
          </tbody>
        </table>

        <p>
          Totales: Presupuesto{' '}
          {formatMoney(comparisonQuery.data?.totalBudget)} | Real{' '}
          {formatMoney(comparisonQuery.data?.totalReal)} | Diferencia{' '}
          {formatMoney(comparisonQuery.data?.totalDifference)}
        </p>
      </div>
    </AppLayout>
  );
}