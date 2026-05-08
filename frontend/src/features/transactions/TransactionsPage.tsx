// src/features/transactions/TransactionsPage.tsx

import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useState } from 'react';
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
    status:
      transaction.status === 'CONFIRMED'
        ? 'PENDING'
        : 'CONFIRMED',
  };
}

export function TransactionsPage() {
  const { profileId = '' } = useParams();
  const qc = useQueryClient();

  const today = new Date();
  const yyyy = today.getFullYear();
  const mm = String(today.getMonth() + 1).padStart(2, '0');

  const [year, setYear] = useState(yyyy);
  const [month, setMonth] = useState(today.getMonth() + 1);

  const [form, setForm] = useState<TransactionForm>({
    accountId: '',
    categoryId: '',
    movementType: 'EXPENSE',
    realDate: `${yyyy}-${mm}-01`,
    budgetDate: `${yyyy}-${mm}-01`,
    amount: 0,
    currency: 'ARS',
    description: '',
    status: 'CONFIRMED',
  });

  const accountsQuery = useQuery({
    queryKey: ['accounts', profileId],
    queryFn: () => listAccounts(profileId),
    enabled: Boolean(profileId),
  });

  const categoriesQuery = useQuery({
    queryKey: ['categories', profileId],
    queryFn: () => listCategories(profileId, true),
    enabled: Boolean(profileId),
  });

  const transactionsQuery = useQuery({
    queryKey: ['tx', profileId, year, month],
    queryFn: () => listTransactions(profileId, year, month),
    enabled: Boolean(profileId),
  });

  const createTransactionMutation = useMutation({
    mutationFn: () =>
      createTransaction({
        ...form,
        profileId,
        amount: Number(form.amount),
        origin: 'MANUAL',
      }),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ['tx', profileId, year, month] });
      qc.invalidateQueries({ queryKey: ['budget-comp', profileId, year, month] });
    },
  });

  const updateTransactionMutation = useMutation({
    mutationFn: (transaction: MoneyTransaction) =>
      updateTransaction(transaction.id, toTransactionUpdatePayload(transaction)),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ['tx', profileId, year, month] });
      qc.invalidateQueries({ queryKey: ['budget-comp', profileId, year, month] });
    },
  });

  const deleteTransactionMutation = useMutation({
    mutationFn: (id: string) => deleteTransaction(id),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ['tx', profileId, year, month] });
      qc.invalidateQueries({ queryKey: ['budget-comp', profileId, year, month] });
    },
  });

  const nameById = <T extends { id: string; name: string }>(
    items: T[] | undefined,
    id: string,
  ) => items?.find((item) => item.id === id)?.name ?? id;

  const canSave =
    Boolean(form.accountId) &&
    Boolean(form.categoryId) &&
    form.amount > 0 &&
    Boolean(form.realDate) &&
    Boolean(form.budgetDate);

  return (
    <AppLayout>
      <div className="card">
        <h1>Movimientos</h1>

        {!accountsQuery.data?.length && (
          <p className="empty-state">
            No hay cuentas.{' '}
            <Link to={`/profiles/${profileId}/accounts`}>Crear cuenta</Link>
          </p>
        )}

        {!categoriesQuery.data?.length && (
          <p className="empty-state">
            No hay categorías.{' '}
            <Link to={`/profiles/${profileId}/categories`}>
              Crear categoría
            </Link>
          </p>
        )}

        <div className="form-row">
          <input
            className="input"
            type="number"
            value={year}
            min={2000}
            max={2100}
            onChange={(e) => setYear(Number(e.target.value))}
          />

          <input
            className="input"
            type="number"
            value={month}
            min={1}
            max={12}
            onChange={(e) => setMonth(Number(e.target.value))}
          />
        </div>

        <div className="form-row">
          <select
            className="select"
            value={form.accountId}
            onChange={(e) =>
              setForm({
                ...form,
                accountId: e.target.value,
              })
            }
          >
            <option value="">Cuenta</option>

            {accountsQuery.data?.map((account: Account) => (
              <option key={account.id} value={account.id}>
                {account.name}
              </option>
            ))}
          </select>

          <select
            className="select"
            value={form.categoryId}
            onChange={(e) =>
              setForm({
                ...form,
                categoryId: e.target.value,
              })
            }
          >
            <option value="">Categoría</option>

            {categoriesQuery.data?.map((category: Category) => (
              <option key={category.id} value={category.id}>
                {category.name}
              </option>
            ))}
          </select>

          <select
            className="select"
            value={form.movementType}
            onChange={(e) =>
              setForm({
                ...form,
                movementType: e.target.value as MovementType,
              })
            }
          >
            {movementTypeOptions.map((option) => (
              <option key={option.value} value={option.value}>
                {option.label}
              </option>
            ))}
          </select>

          <select
            className="select"
            value={form.status}
            onChange={(e) =>
              setForm({
                ...form,
                status: e.target.value as TransactionStatus,
              })
            }
          >
            {transactionStatusOptions.map((option) => (
              <option key={option.value} value={option.value}>
                {option.label}
              </option>
            ))}
          </select>
        </div>

        <div className="form-row">
          <input
            className="input"
            type="date"
            value={form.realDate}
            onChange={(e) =>
              setForm({
                ...form,
                realDate: e.target.value,
              })
            }
          />

          <input
            className="input"
            type="date"
            value={form.budgetDate}
            onChange={(e) =>
              setForm({
                ...form,
                budgetDate: e.target.value,
              })
            }
          />

          <input
            className="input"
            type="number"
            min={0}
            value={form.amount}
            onChange={(e) =>
              setForm({
                ...form,
                amount: Number(e.target.value),
              })
            }
          />

          <input
            className="input"
            value={form.description}
            placeholder="Descripción"
            onChange={(e) =>
              setForm({
                ...form,
                description: e.target.value,
              })
            }
          />

          <button
            className="button-primary"
            onClick={() => createTransactionMutation.mutate()}
            disabled={!canSave || createTransactionMutation.isPending}
          >
            Guardar
          </button>
        </div>

        <table className="table">
          <thead>
            <tr>
              <th>Fecha real</th>
              <th>Fecha presupuesto</th>
              <th>Tipo</th>
              <th>Cuenta</th>
              <th>Categoría</th>
              <th>Monto</th>
              <th>Estado</th>
              <th>Descripción</th>
              <th></th>
            </tr>
          </thead>

          <tbody>
            {transactionsQuery.data?.map((transaction: MoneyTransaction) => (
              <tr key={transaction.id}>
                <td>{transaction.realDate}</td>
                <td>{transaction.budgetDate}</td>

                <td>
                  {labelOrValue(
                    movementTypeLabels,
                    transaction.movementType,
                  )}
                </td>

                <td>
                  {nameById<Account>(
                    accountsQuery.data,
                    transaction.accountId,
                  )}
                </td>

                <td>
                  {nameById<Category>(
                    categoriesQuery.data,
                    transaction.categoryId,
                  )}
                </td>

                <td>{formatMoney(transaction.amount, transaction.currency)}</td>

                <td>
                  {labelOrValue(
                    transactionStatusLabels,
                    transaction.status,
                  )}
                </td>

                <td>{transaction.description}</td>

                <td>
                  <button
                    onClick={() =>
                      updateTransactionMutation.mutate(transaction)
                    }
                  >
                    {transaction.status === 'CONFIRMED'
                      ? 'Pasar a pendiente'
                      : 'Confirmar'}
                  </button>

                  <button
                    className="button-danger"
                    onClick={() =>
                      window.confirm('¿Eliminar movimiento?') &&
                      deleteTransactionMutation.mutate(transaction.id)
                    }
                  >
                    Eliminar
                  </button>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </AppLayout>
  );
}