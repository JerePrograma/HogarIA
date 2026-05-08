// src/features/transactions/TransactionsPage.tsx

import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useMemo, useState } from 'react';
import { Link, useParams } from 'react-router-dom';

import { listAccounts } from '../../api/accountsApi';
import { listCategories } from '../../api/categoriesApi';

import {
  createTransaction,
  deleteTransaction,
  listTransactions,
  updateTransaction,
} from '../../api/transactionsApi';

import { AppLayout } from '../../components/layout/AppLayout';

import {
  labelOrValue,
  movementTypeLabels,
  transactionStatusLabels,
} from '../../domain/financeLabels';

import {
  movementTypeOptions,
  transactionStatusOptions,
} from '../../domain/financeOptions';

import { formatMoney } from '../../domain/formatters';

import type {
  Account,
  Category,
  MoneyTransaction,
  MovementType,
  TransactionStatus,
} from '../../domain/types';

interface TransactionForm {
  accountId: string;
  categoryId: string;
  movementType: MovementType;
  realDate: string;
  budgetDate: string;
  amount: number;
  currency: string;
  description: string;
  status: TransactionStatus;
}

const monthOptions = [
  { value: 1, label: 'Enero' },
  { value: 2, label: 'Febrero' },
  { value: 3, label: 'Marzo' },
  { value: 4, label: 'Abril' },
  { value: 5, label: 'Mayo' },
  { value: 6, label: 'Junio' },
  { value: 7, label: 'Julio' },
  { value: 8, label: 'Agosto' },
  { value: 9, label: 'Septiembre' },
  { value: 10, label: 'Octubre' },
  { value: 11, label: 'Noviembre' },
  { value: 12, label: 'Diciembre' },
];

function getDefaultDate(year: number, month: number) {
  return `${year}-${String(month).padStart(2, '0')}-01`;
}

function formatDate(value: string) {
  if (!value) return '-';

  return new Intl.DateTimeFormat('es-AR', {
    day: '2-digit',
    month: '2-digit',
    year: 'numeric',
  }).format(new Date(`${value}T00:00:00`));
}

function getStatusBadgeClass(status: TransactionStatus) {
  if (status === 'CONFIRMED') return 'badge badge-ok';
  if (status === 'PENDING') return 'badge badge-warning';
  return 'badge badge-danger';
}

function getMovementBadgeClass(type: MovementType) {
  if (type === 'INCOME') return 'badge badge-ok';
  if (type === 'SAVING') return 'badge badge-info';
  if (type === 'TRANSFER') return 'badge badge-muted';
  if (type === 'ADJUSTMENT') return 'badge badge-warning';
  return 'badge badge-danger';
}

function toTransactionUpdatePayload(transaction: MoneyTransaction) {
  return {
    accountId: transaction.accountId,
    categoryId: transaction.categoryId,
    movementType: transaction.movementType,
    realDate: transaction.realDate,
    budgetDate: transaction.budgetDate,
    amount: transaction.amount,
    currency: transaction.currency,
    description: transaction.description ?? '',
    status: transaction.status === 'CONFIRMED' ? 'PENDING' : 'CONFIRMED',
  };
}

export function TransactionsPage() {
  const { profileId = '' } = useParams();
  const queryClient = useQueryClient();

  const today = new Date();
  const initialYear = today.getFullYear();
  const initialMonth = today.getMonth() + 1;

  const [year, setYear] = useState(initialYear);
  const [month, setMonth] = useState(initialMonth);

  const [form, setForm] = useState<TransactionForm>({
    accountId: '',
    categoryId: '',
    movementType: 'EXPENSE',
    realDate: getDefaultDate(initialYear, initialMonth),
    budgetDate: getDefaultDate(initialYear, initialMonth),
    amount: 0,
    currency: 'ARS',
    description: '',
    status: 'CONFIRMED',
  });

  const accountsQuery = useQuery<Account[]>({
    queryKey: ['accounts', profileId],
    queryFn: () => listAccounts(profileId),
    enabled: Boolean(profileId),
  });

  const categoriesQuery = useQuery<Category[]>({
    queryKey: ['categories', profileId],
    queryFn: () => listCategories(profileId, true),
    enabled: Boolean(profileId),
  });

  const transactionsQuery = useQuery<MoneyTransaction[]>({
    queryKey: ['tx', profileId, year, month],
    queryFn: () => listTransactions(profileId, year, month),
    enabled: Boolean(profileId),
  });

  const accountsById = useMemo(() => {
    return new Map((accountsQuery.data ?? []).map((account) => [account.id, account]));
  }, [accountsQuery.data]);

  const categoriesById = useMemo(() => {
    return new Map((categoriesQuery.data ?? []).map((category) => [category.id, category]));
  }, [categoriesQuery.data]);

  const transactions = transactionsQuery.data ?? [];

  const totals = useMemo(() => {
    return transactions.reduce(
      (acc, transaction) => {
        if (transaction.status === 'IGNORED') {
          acc.ignored += Number(transaction.amount ?? 0);
          return acc;
        }

        if (transaction.movementType === 'INCOME') {
          acc.income += Number(transaction.amount ?? 0);
          return acc;
        }

        if (transaction.movementType === 'SAVING') {
          acc.saving += Number(transaction.amount ?? 0);
          return acc;
        }

        if (transaction.movementType === 'EXPENSE') {
          acc.expenses += Number(transaction.amount ?? 0);
          return acc;
        }

        return acc;
      },
      {
        income: 0,
        expenses: 0,
        saving: 0,
        ignored: 0,
      },
    );
  }, [transactions]);

  const createTransactionMutation = useMutation({
    mutationFn: () =>
      createTransaction({
        ...form,
        profileId,
        amount: Number(form.amount),
        origin: 'MANUAL',
      }),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['tx', profileId, year, month] });
      queryClient.invalidateQueries({ queryKey: ['budget-comp', profileId, year, month] });

      setForm((current) => ({
        ...current,
        amount: 0,
        description: '',
      }));
    },
  });

  const updateTransactionMutation = useMutation({
    mutationFn: (transaction: MoneyTransaction) =>
      updateTransaction(transaction.id, toTransactionUpdatePayload(transaction)),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['tx', profileId, year, month] });
      queryClient.invalidateQueries({ queryKey: ['budget-comp', profileId, year, month] });
    },
  });

  const deleteTransactionMutation = useMutation({
    mutationFn: (id: string) => deleteTransaction(id),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['tx', profileId, year, month] });
      queryClient.invalidateQueries({ queryKey: ['budget-comp', profileId, year, month] });
    },
  });

  const canSave =
    Boolean(form.accountId) &&
    Boolean(form.categoryId) &&
    form.amount > 0 &&
    Boolean(form.realDate) &&
    Boolean(form.budgetDate) &&
    !createTransactionMutation.isPending;

  const handlePeriodChange = (nextYear: number, nextMonth: number) => {
    setYear(nextYear);
    setMonth(nextMonth);

    const nextDefaultDate = getDefaultDate(nextYear, nextMonth);

    setForm((current) => ({
      ...current,
      realDate: nextDefaultDate,
      budgetDate: nextDefaultDate,
    }));
  };

  return (
    <AppLayout>
      <div className="page-stack">
        <section className="page-header">
          <div>
            <p className="eyebrow">Gestión diaria</p>
            <h1>Movimientos</h1>
            <p className="muted">
              Cargá ingresos, gastos, ahorros y ajustes para alimentar el presupuesto y el panel mensual.
            </p>
          </div>

          <div className="period-selector">
            <label>
              Año
              <input
                className="input"
                type="number"
                value={year}
                min={2000}
                max={2100}
                onChange={(event) =>
                  handlePeriodChange(Number(event.target.value), month)
                }
              />
            </label>

            <label>
              Mes
              <select
                className="select"
                value={month}
                onChange={(event) =>
                  handlePeriodChange(year, Number(event.target.value))
                }
              >
                {monthOptions.map((option) => (
                  <option key={option.value} value={option.value}>
                    {option.label}
                  </option>
                ))}
              </select>
            </label>
          </div>
        </section>

        <section className="summary-grid">
          <div className="metric-card metric-income">
            <span>Ingresos</span>
            <strong>{formatMoney(totals.income)}</strong>
          </div>

          <div className="metric-card metric-expense">
            <span>Gastos</span>
            <strong>{formatMoney(totals.expenses)}</strong>
          </div>

          <div className="metric-card metric-saving">
            <span>Ahorro</span>
            <strong>{formatMoney(totals.saving)}</strong>
          </div>

          <div className="metric-card">
            <span>Balance operativo</span>
            <strong>{formatMoney(totals.income - totals.expenses - totals.saving)}</strong>
          </div>
        </section>

        {!accountsQuery.isLoading && !accountsQuery.data?.length && (
          <p className="empty-state">
            No hay cuentas cargadas.{' '}
            <Link to={`/profiles/${profileId}/accounts`}>Crear cuenta</Link>
          </p>
        )}

        {!categoriesQuery.isLoading && !categoriesQuery.data?.length && (
          <p className="empty-state">
            No hay categorías cargadas.{' '}
            <Link to={`/profiles/${profileId}/categories`}>Crear categoría</Link>
          </p>
        )}

        <section className="card">
          <div className="section-title">
            <div>
              <h2>Cargar movimiento</h2>
              <p className="muted">
                Los movimientos confirmados impactan en reportes y comparación presupuesto vs real.
              </p>
            </div>
          </div>

          <div className="form-grid transaction-form-grid">
            <label>
              Cuenta
              <select
                className="select"
                value={form.accountId}
                onChange={(event) =>
                  setForm({
                    ...form,
                    accountId: event.target.value,
                  })
                }
              >
                <option value="">Seleccionar cuenta</option>

                {accountsQuery.data?.map((account) => (
                  <option key={account.id} value={account.id}>
                    {account.name}
                  </option>
                ))}
              </select>
            </label>

            <label>
              Categoría
              <select
                className="select"
                value={form.categoryId}
                onChange={(event) =>
                  setForm({
                    ...form,
                    categoryId: event.target.value,
                  })
                }
              >
                <option value="">Seleccionar categoría</option>

                {categoriesQuery.data?.map((category) => (
                  <option key={category.id} value={category.id}>
                    {category.name}
                  </option>
                ))}
              </select>
            </label>

            <label>
              Tipo de movimiento
              <select
                className="select"
                value={form.movementType}
                onChange={(event) =>
                  setForm({
                    ...form,
                    movementType: event.target.value as MovementType,
                  })
                }
              >
                {movementTypeOptions.map((option) => (
                  <option key={option.value} value={option.value}>
                    {option.label}
                  </option>
                ))}
              </select>
            </label>

            <label>
              Estado
              <select
                className="select"
                value={form.status}
                onChange={(event) =>
                  setForm({
                    ...form,
                    status: event.target.value as TransactionStatus,
                  })
                }
              >
                {transactionStatusOptions.map((option) => (
                  <option key={option.value} value={option.value}>
                    {option.label}
                  </option>
                ))}
              </select>
            </label>

            <label>
              Fecha real
              <input
                className="input"
                type="date"
                value={form.realDate}
                onChange={(event) =>
                  setForm({
                    ...form,
                    realDate: event.target.value,
                  })
                }
              />
            </label>

            <label>
              Fecha de presupuesto
              <input
                className="input"
                type="date"
                value={form.budgetDate}
                onChange={(event) =>
                  setForm({
                    ...form,
                    budgetDate: event.target.value,
                  })
                }
              />
            </label>

            <label>
              Monto
              <input
                className="input"
                type="number"
                min={0}
                value={form.amount}
                onChange={(event) =>
                  setForm({
                    ...form,
                    amount: Number(event.target.value),
                  })
                }
              />
            </label>

            <label className="form-field-wide">
              Descripción
              <input
                className="input"
                value={form.description}
                placeholder="Ej: supermercado, sueldo, alquiler"
                onChange={(event) =>
                  setForm({
                    ...form,
                    description: event.target.value,
                  })
                }
              />
            </label>
          </div>

          <div className="form-actions">
            <button
              className="button-primary"
              onClick={() => createTransactionMutation.mutate()}
              disabled={!canSave}
            >
              {createTransactionMutation.isPending ? 'Guardando...' : 'Guardar movimiento'}
            </button>

            {!canSave && (
              <span className="muted">
                Completá cuenta, categoría, monto y fechas para guardar.
              </span>
            )}
          </div>

          {createTransactionMutation.isError && (
            <p className="error-box">
              No se pudo guardar el movimiento. Revisá los datos ingresados.
            </p>
          )}
        </section>

        <section className="card">
          <div className="section-title">
            <div>
              <h2>Movimientos del período</h2>
              <p className="muted">
                {transactions.length} movimiento{transactions.length === 1 ? '' : 's'} registrado
                {transactions.length === 1 ? '' : 's'}.
              </p>
            </div>
          </div>

          {transactionsQuery.isLoading && (
            <p className="empty-state">Cargando movimientos...</p>
          )}

          {transactionsQuery.isError && (
            <p className="error-box">
              No se pudieron cargar los movimientos del período.
            </p>
          )}

          {!transactionsQuery.isLoading &&
            !transactionsQuery.isError &&
            transactions.length === 0 && (
              <p className="empty-state">
                Todavía no hay movimientos cargados para este mes.
              </p>
            )}

          {transactions.length > 0 && (
            <div className="table-wrapper">
              <table className="table table-compact">
                <thead>
                  <tr>
                    <th>Fecha</th>
                    <th>Movimiento</th>
                    <th>Cuenta</th>
                    <th>Categoría</th>
                    <th>Monto</th>
                    <th>Estado</th>
                    <th>Acciones</th>
                  </tr>
                </thead>

                <tbody>
                  {transactions.map((transaction) => {
                    const accountName =
                      accountsById.get(transaction.accountId)?.name ?? 'Cuenta no encontrada';

                    const categoryName =
                      categoriesById.get(transaction.categoryId)?.name ?? 'Categoría no encontrada';

                    return (
                      <tr key={transaction.id}>
                        <td>
                          <strong>{formatDate(transaction.realDate)}</strong>
                          <br />
                          <span className="muted">
                            Presupuesto: {formatDate(transaction.budgetDate)}
                          </span>
                        </td>

                        <td>
                          <span className={getMovementBadgeClass(transaction.movementType)}>
                            {labelOrValue(movementTypeLabels, transaction.movementType)}
                          </span>

                          <br />

                          <span className="muted">
                            {transaction.description || 'Sin descripción'}
                          </span>
                        </td>

                        <td>{accountName}</td>
                        <td>{categoryName}</td>

                        <td className="amount-cell">
                          {formatMoney(transaction.amount, transaction.currency)}
                        </td>

                        <td>
                          <span className={getStatusBadgeClass(transaction.status)}>
                            {labelOrValue(transactionStatusLabels, transaction.status)}
                          </span>
                        </td>

                        <td>
                          <div className="row-actions">
                            <button
                              className="button-secondary"
                              disabled={updateTransactionMutation.isPending}
                              onClick={() => updateTransactionMutation.mutate(transaction)}
                            >
                              {transaction.status === 'CONFIRMED'
                                ? 'Pasar a pendiente'
                                : 'Confirmar'}
                            </button>

                            <button
                              className="button-danger"
                              disabled={deleteTransactionMutation.isPending}
                              onClick={() =>
                                window.confirm('¿Eliminar este movimiento?') &&
                                deleteTransactionMutation.mutate(transaction.id)
                              }
                            >
                              Eliminar
                            </button>
                          </div>
                        </td>
                      </tr>
                    );
                  })}
                </tbody>
              </table>
            </div>
          )}
        </section>
      </div>
    </AppLayout>
  );
}