import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useState } from 'react';
import { useParams } from 'react-router-dom';
import { createAccount, deleteAccount, listAccounts, updateAccount } from '../../api/accountsApi';
import { AppLayout } from '../../components/layout/AppLayout';
import { EmptyState } from '../../components/ui/EmptyState';
import { ErrorState } from '../../components/ui/ErrorState';
import { MetricCard } from '../../components/ui/MetricCard';
import { StatusBadge } from '../../components/ui/StatusBadge';
import type { Account } from '../../domain/types';

export function AccountsPage() {
  const { profileId = '' } = useParams();
  const queryClient = useQueryClient();
  const [name, setName] = useState('');

  const accountsQuery = useQuery<Account[]>({
    queryKey: ['accounts', profileId],
    queryFn: () => listAccounts(profileId),
    enabled: Boolean(profileId),
  });

  const accounts = accountsQuery.data ?? [];
  const activeAccounts = accounts.filter((account) => account.active);
  const inactiveAccounts = accounts.filter((account) => !account.active);

  const createMutation = useMutation({
    mutationFn: () =>
      createAccount(profileId, {
        name: name.trim(),
        accountType: 'CASH',
        currency: 'ARS',
      }),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['accounts', profileId] });
      setName('');
    },
  });

  const toggleMutation = useMutation({
    mutationFn: (account: Account) =>
      updateAccount(account.id, {
        name: account.name,
        accountType: account.accountType,
        currency: account.currency,
        creditLimit: account.creditLimit,
        statementCloseDay: account.statementCloseDay,
        dueDay: account.dueDay,
        active: !account.active,
      }),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['accounts', profileId] }),
  });

  const deleteMutation = useMutation({
    mutationFn: (id: string) => deleteAccount(id),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['accounts', profileId] }),
  });

  const canCreate = name.trim().length > 0 && !createMutation.isPending;

  return (
    <AppLayout>
      <div className="page-stack">
        <section className="page-header">
          <div>
            <p className="eyebrow">Organización financiera</p>
            <h1>Cuentas</h1>
            <p className="muted">
              Administrá las cuentas donde se registran ingresos, gastos, pagos, ahorros y tarjetas.
            </p>
          </div>
        </section>

        <section className="metric-grid">
          <MetricCard title="Total cuentas" value={accounts.length} helper="Cuentas cargadas." tone="info" />
          <MetricCard title="Activas" value={activeAccounts.length} helper="Disponibles para operar." tone="success" />
          <MetricCard title="Inactivas" value={inactiveAccounts.length} helper="Fuera del flujo operativo." tone={inactiveAccounts.length > 0 ? 'warning' : 'neutral'} />
        </section>

        <section className="panel">
          <div className="section-title">
            <div>
              <p className="eyebrow">Alta rápida</p>
              <h2>Crear cuenta</h2>
              <p className="secondary-text">
                Por ahora se crea como efectivo en ARS. Después se puede extender el formulario para tarjetas/bancos.
              </p>
            </div>
          </div>

          <div className="form-row">
            <label>
              Nombre
              <input
                className="input-ui"
                value={name}
                onChange={(event) => setName(event.target.value)}
                placeholder="Ej: Efectivo, Mercado Pago, Banco"
              />
            </label>

            <button
              type="button"
              className="boton-principal"
              onClick={() => createMutation.mutate()}
              disabled={!canCreate}
            >
              {createMutation.isPending ? 'Creando...' : 'Crear cuenta'}
            </button>
          </div>

          {createMutation.isError ? (
            <p className="mensaje-error">No se pudo crear la cuenta.</p>
          ) : null}
        </section>

        <section className="panel">
          <div className="section-title">
            <div>
              <p className="eyebrow">Listado</p>
              <h2>Cuentas cargadas</h2>
            </div>

            <span className="badge-count">{accounts.length}</span>
          </div>

          {accountsQuery.isLoading ? (
            <EmptyState title="Cargando cuentas" message="Estamos consultando tus cuentas." />
          ) : null}

          {accountsQuery.isError ? (
            <ErrorState message="No se pudieron cargar las cuentas." />
          ) : null}

          {!accountsQuery.isLoading && !accountsQuery.isError && accounts.length === 0 ? (
            <EmptyState title="Sin cuentas" message="Todavía no tenés cuentas cargadas." />
          ) : null}

          {accounts.length > 0 ? (
            <div className="tabla-ui">
              <table className="table-compact">
                <thead>
                  <tr>
                    <th>Nombre</th>
                    <th>Tipo</th>
                    <th>Moneda</th>
                    <th>Límite</th>
                    <th>Cierre/Venc.</th>
                    <th>Estado</th>
                    <th>Acciones</th>
                  </tr>
                </thead>

                <tbody>
                  {accounts.map((account) => (
                    <tr key={account.id}>
                      <td>
                        <strong>{account.name}</strong>
                      </td>
                      <td>{account.accountType}</td>
                      <td>{account.currency}</td>
                      <td>{account.creditLimit ?? '-'}</td>
                      <td>
                        {account.statementCloseDay ?? '-'} / {account.dueDay ?? '-'}
                      </td>
                      <td>
                        <StatusBadge
                          tone={account.active ? 'ok' : 'watch'}
                          label={account.active ? 'Activa' : 'Inactiva'}
                        />
                      </td>
                      <td>
                        <div className="row-actions">
                          <button
                            type="button"
                            className="boton-secundario"
                            onClick={() => toggleMutation.mutate(account)}
                            disabled={toggleMutation.isPending}
                          >
                            {account.active ? 'Desactivar' : 'Activar'}
                          </button>

                          <button
                            type="button"
                            className="boton-danger"
                            onClick={() =>
                              window.confirm('¿Desactivar cuenta?') &&
                              deleteMutation.mutate(account.id)
                            }
                            disabled={deleteMutation.isPending}
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
          ) : null}
        </section>
      </div>
    </AppLayout>
  );
}