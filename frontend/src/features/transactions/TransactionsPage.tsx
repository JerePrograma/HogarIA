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
import { EmptyState } from '../../components/ui/EmptyState';
import { ErrorState } from '../../components/ui/ErrorState';
import { MetricCard } from '../../components/ui/MetricCard';
import { MonthSelector } from '../../components/ui/MonthSelector';
import { StatusBadge } from '../../components/ui/StatusBadge';
import {
  labelOrValue,
  movementTypeLabels,
  transactionStatusLabels,
} from '../../domain/financeLabels';
import { movementTypeOptions, transactionStatusOptions } from '../../domain/financeOptions';
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

function getStatusTone(status: TransactionStatus): 'ok' | 'watch' | 'critical' {
  if (status === 'CONFIRMED') return 'ok';
  if (status === 'PENDING') return 'watch';
  return 'critical';
}

function getMovementTone(type: MovementType): 'ok' | 'watch' | 'risk' | 'critical' | 'neutral' {
  if (type === 'INCOME') return 'ok';
  if (type === 'SAVING') return 'ok';
  if (type === 'TRANSFER') return 'neutral';
  if (type === 'ADJUSTMENT') return 'watch';
  return 'critical';
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

  const accountsById = useMemo(
    () => new Map((accountsQuery.data ?? []).map((account) => [account.id, account])),
    [accountsQuery.data],
  );

  const categoriesById = useMemo(
    () => new Map((categoriesQuery.data ?? []).map((category) => [category.id, category])),
    [categoriesQuery.data],
  );

  const transactions = transactionsQuery.data ?? [];

  const totals = useMemo(
    () =>
      transactions.reduce(
        (acc, transaction) => {
          const amount = Number(transaction.amount ?? 0);

          if (transaction.status === 'IGNORED') {
            acc.ignored += amount;
            return acc;
          }

          if (transaction.movementType === 'INCOME') {
            acc.income += amount;
            return acc;
          }

          if (transaction.movementType === 'SAVING') {
            acc.saving += amount;
            return acc;
          }

          if (transaction.movementType === 'EXPENSE') {
            acc.expenses += amount;
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
      ),
    [transactions],
  );

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

  const balance = totals.income - totals.expenses - totals.saving;

  return (
    <AppLayout>
      <div className="page-stack">
        <section className="page-header">
          <div>
            <p className="eyebrow">Gestión diaria</p>
            <h1>Movimientos</h1>
            <p className="muted">
              Cargá ingresos, gastos, ahorros y ajustes para alimentar presupuesto y panel mensual.
            </p>
          </div>

          <div className="stack-ui md:min-w-[360px]">
            <Link className='btn btn-secondary' to={`/profiles/${profileId}/transactions/import`}>Importar movimientos</Link>
            <MonthSelector
              year={year}
              month={month}
              onYearChange={(nextYear) => handlePeriodChange(nextYear, month)}
              onMonthChange={(nextMonth) => handlePeriodChange(year, nextMonth)}
            />
          </div>
        </section>

        <section className="metric-grid">
          <MetricCard title="Ingresos" value={formatMoney(totals.income)} helper="Entradas confirmadas/no ignoradas." tone="success" />
          <MetricCard title="Gastos" value={formatMoney(totals.expenses)} helper="Egresos del período." tone="danger" />
          <MetricCard title="Ahorro" value={formatMoney(totals.saving)} helper="Movimientos de ahorro." tone="info" />
          <MetricCard title="Balance operativo" value={formatMoney(balance)} helper="Ingresos menos gastos y ahorro." tone={balance >= 0 ? 'success' : 'danger'} />
        </section>

        {!accountsQuery.isLoading && !accountsQuery.data?.length ? (
          <p className="mensaje-warning">
            No hay cuentas cargadas.{' '}
            <Link to={`/profiles/${profileId}/accounts`}>Crear cuenta</Link>
          </p>
        ) : null}

        {!categoriesQuery.isLoading && !categoriesQuery.data?.length ? (
          <p className="mensaje-warning">
            No hay categorías cargadas.{' '}
            <Link to={`/profiles/${profileId}/categories`}>Crear categoría</Link>
          </p>
        ) : null}

        <section className="panel">
          <div className="section-title">
            <div>
              <p className="eyebrow">Alta</p>
              <h2>Cargar movimiento</h2>
              <p className="muted">
                Los movimientos confirmados impactan en reportes y comparación presupuesto vs real.
              </p>
            </div>
          </div>

          <div className="form-grid">
            <label>
              Cuenta
              <select
                className="input-ui"
                value={form.accountId}
                onChange={(event) => setForm({ ...form, accountId: event.target.value })}
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
                className="input-ui"
                value={form.categoryId}
                onChange={(event) => setForm({ ...form, categoryId: event.target.value })}
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
                className="input-ui"
                value={form.movementType}
                onChange={(event) =>
                  setForm({ ...form, movementType: event.target.value as MovementType })
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
                className="input-ui"
                value={form.status}
                onChange={(event) =>
                  setForm({ ...form, status: event.target.value as TransactionStatus })
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
                className="input-ui"
                type="date"
                value={form.realDate}
                onChange={(event) => setForm({ ...form, realDate: event.target.value })}
              />
            </label>

            <label>
              Fecha de presupuesto
              <input
                className="input-ui"
                type="date"
                value={form.budgetDate}
                onChange={(event) => setForm({ ...form, budgetDate: event.target.value })}
              />
            </label>

            <label>
              Monto
              <input
                className="input-ui"
                type="number"
                min={0}
                value={form.amount}
                onChange={(event) => setForm({ ...form, amount: Number(event.target.value) })}
              />
            </label>

            <label className="form-field-wide">
              Descripción
              <input
                className="input-ui"
                value={form.description}
                placeholder="Ej: supermercado, sueldo, alquiler"
                onChange={(event) => setForm({ ...form, description: event.target.value })}
              />
            </label>
          </div>

          <div className="form-actions">
            <button
              type="button"
              className="boton-principal"
              onClick={() => createTransactionMutation.mutate()}
              disabled={!canSave}
            >
              {createTransactionMutation.isPending ? 'Guardando...' : 'Guardar movimiento'}
            </button>

            {!canSave ? (
              <span className="muted">Completá cuenta, categoría, monto y fechas para guardar.</span>
            ) : null}
          </div>

          {createTransactionMutation.isError ? (
            <p className="mensaje-error">No se pudo guardar el movimiento. Revisá los datos ingresados.</p>
          ) : null}
        </section>

        <section className="panel">
          <div className="section-title">
            <div>
              <p className="eyebrow">Listado</p>
              <h2>Movimientos del período</h2>
              <p className="muted">
                {transactions.length} movimiento{transactions.length === 1 ? '' : 's'} registrado
                {transactions.length === 1 ? '' : 's'}.
              </p>
            </div>
          </div>

          {transactionsQuery.isLoading ? (
            <EmptyState title="Cargando movimientos" message="Estamos consultando los movimientos del período." />
          ) : null}

          {transactionsQuery.isError ? (
            <ErrorState message="No se pudieron cargar los movimientos del período." />
          ) : null}

          {!transactionsQuery.isLoading && !transactionsQuery.isError && transactions.length === 0 ? (
            <EmptyState title="Sin movimientos" message="Todavía no hay movimientos cargados para este mes." />
          ) : null}

          {transactions.length > 0 ? (
            <div className="tabla-ui">
              <table className="table-compact">
                <thead>
                  <tr>
                    <th>Fecha</th>
                    <th>Movimiento</th>
                    <th>Cuenta</th>
                    <th>Categoría</th>
                    <th className="amount-cell">Monto</th>
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
                          <StatusBadge
                            tone={getMovementTone(transaction.movementType)}
                            label={labelOrValue(movementTypeLabels, transaction.movementType)}
                          />
                          <p className="compact-muted">
                            {transaction.description || 'Sin descripción'}
                          </p>
                        </td>

                        <td>{accountName}</td>
                        <td>{categoryName}</td>

                        <td className="amount-cell">
                          {formatMoney(transaction.amount, transaction.currency)}
                        </td>

                        <td>
                          <StatusBadge
                            tone={getStatusTone(transaction.status)}
                            label={labelOrValue(transactionStatusLabels, transaction.status)}
                          />
                        </td>

                        <td>
                          <div className="row-actions">
                            <button
                              type="button"
                              className="boton-secundario"
                              disabled={updateTransactionMutation.isPending}
                              onClick={() => updateTransactionMutation.mutate(transaction)}
                            >
                              {transaction.status === 'CONFIRMED' ? 'Pasar a pendiente' : 'Confirmar'}
                            </button>

                            <button
                              type="button"
                              className="boton-danger"
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
          ) : null}
        </section>
      </div>
    </AppLayout>
  );
}