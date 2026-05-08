import { useState } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useParams } from 'react-router-dom';
import { createAccount, deleteAccount, listAccounts, updateAccount } from '../../api/accountsApi';
import { AppLayout } from '../../components/layout/AppLayout';
import { accountTypeLabels, labelOrMissing } from '../../domain/financeLabels';
import { accountTypeOptions } from '../../domain/financeOptions';
import type { Account, AccountType } from '../../domain/types';
import { formatMoney } from '../../domain/formatters';

export function AccountsPage() {
  const { profileId = '' } = useParams();
  const qc = useQueryClient();

  const [form, setForm] = useState({
    name: '',
    accountType: 'CASH' as AccountType,
    currency: 'ARS',
  });

  const accountsQuery = useQuery({
    queryKey: ['accounts', profileId],
    queryFn: () => listAccounts(profileId),
    enabled: Boolean(profileId),
  });

  const createAccountMutation = useMutation({
    mutationFn: () => createAccount(profileId, form),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ['accounts', profileId] });
      setForm({ ...form, name: '' });
    },
  });

  const updateAccountMutation = useMutation({
    mutationFn: (account: Account) =>
      updateAccount(String(account.id), {
        ...account,
        active: !account.active,
      }),
    onSuccess: () => qc.invalidateQueries({ queryKey: ['accounts', profileId] }),
  });

  const deleteAccountMutation = useMutation({
    mutationFn: (id: string) => deleteAccount(id),
    onSuccess: () => qc.invalidateQueries({ queryKey: ['accounts', profileId] }),
  });

  return (
    <AppLayout>
      <div className="page-header">
        <div>
          <p className="eyebrow">Estructura financiera</p>
          <h1>Cuentas</h1>
          <p className="muted">Registrá efectivo, bancos, tarjetas y billeteras virtuales.</p>
        </div>
      </div>

      <section className="card">
        <h2>Crear cuenta</h2>

        <div className="form-grid">
          <label className="field">
            <span>Nombre de la cuenta</span>
            <input
              value={form.name}
              placeholder="Ej: Cuenta sueldo, Mercado Pago, Efectivo"
              onChange={(event) => setForm({ ...form, name: event.target.value })}
            />
          </label>

          <label className="field">
            <span>Tipo de cuenta</span>
            <select
              value={form.accountType}
              onChange={(event) => setForm({ ...form, accountType: event.target.value as AccountType })}
            >
              {accountTypeOptions.map((option) => (
                <option key={option.value} value={option.value}>
                  {option.label}
                </option>
              ))}
            </select>
          </label>

          <label className="field">
            <span>Moneda</span>
            <input
              value={form.currency}
              maxLength={3}
              onChange={(event) => setForm({ ...form, currency: event.target.value.toUpperCase() })}
            />
          </label>
        </div>

        <div className="actions">
          <button
            type="button"
            className="button primary"
            onClick={() => createAccountMutation.mutate()}
            disabled={!form.name.trim() || createAccountMutation.isPending}
          >
            Crear cuenta
          </button>
        </div>
      </section>

      <section className="card">
        <h2>Cuentas registradas</h2>

        {accountsQuery.isLoading ? (
          <p className="muted">Cargando cuentas...</p>
        ) : !accountsQuery.data?.length ? (
          <p className="muted">Sin cuentas.</p>
        ) : (
          <div className="table-wrap">
            <table className="table">
              <thead>
                <tr>
                  <th>Nombre</th>
                  <th>Tipo</th>
                  <th>Moneda</th>
                  <th className="amount">Límite</th>
                  <th>Cierre/Vencimiento</th>
                  <th>Estado</th>
                  <th>Acciones</th>
                </tr>
              </thead>
              <tbody>
                {accountsQuery.data.map((account: Account) => (
                  <tr key={account.id}>
                    <td>{account.name}</td>
                    <td>{labelOrMissing(accountTypeLabels, account.accountType)}</td>
                    <td>{account.currency}</td>
                    <td className="amount">
                      {account.creditLimit ? formatMoney(account.creditLimit, account.currency) : '-'}
                    </td>
                    <td>
                      {account.statementCloseDay || account.dueDay
                        ? `${account.statementCloseDay ?? '-'} / ${account.dueDay ?? '-'}`
                        : '-'}
                    </td>
                    <td>
                      <span className={`badge ${account.active ? 'good' : 'muted'}`}>
                        {account.active ? 'Activa' : 'Inactiva'}
                      </span>
                    </td>
                    <td>
                      <div className="actions compact">
                        <button
                          type="button"
                          className="button ghost"
                          onClick={() => updateAccountMutation.mutate(account)}
                        >
                          {account.active ? 'Desactivar' : 'Activar'}
                        </button>

                        <button
                          type="button"
                          className="button danger"
                          onClick={() => window.confirm('¿Eliminar cuenta?') && deleteAccountMutation.mutate(account.id)}
                        >
                          Eliminar
                        </button>
                      </div>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </section>
    </AppLayout>
  );
}