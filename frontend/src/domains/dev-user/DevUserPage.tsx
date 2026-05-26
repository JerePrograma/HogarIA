import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { createDevUser, listDevUsers } from '../../api/devUsersApi';
import { getApiErrorMessage } from '../../api/http';
import { routePaths } from '../../app/router/routePaths';
import { queryKeys } from '../../domain/queryKeys';
import { EmptyState } from '../../shared/ui/EmptyState';
import { ErrorState } from '../../shared/ui/ErrorState';
import { MetricCard } from '../../shared/ui/MetricCard';

type DevUser = {
  id: string;
  fullName: string;
  email: string;
};

type DevUserForm = {
  fullName: string;
  email: string;
  password: string;
};

const initialForm: DevUserForm = {
  fullName: '',
  email: '',
  password: '',
};

export function DevUserPage() {
  const navigate = useNavigate();
  const queryClient = useQueryClient();

  const [form, setForm] = useState<DevUserForm>(initialForm);

  const devUsersQuery = useQuery<DevUser[]>({
    queryKey: queryKeys.devUsers,
    queryFn: listDevUsers,
  });

  const devUsers = devUsersQuery.data ?? [];

  const createMutation = useMutation({
    mutationFn: () => createDevUser(form),
    onSuccess: (user: { id: string }) => {
      queryClient.invalidateQueries({ queryKey: queryKeys.devUsers });
      localStorage.setItem('devUserId', user.id);
      navigate(routePaths.profiles);
    },
  });

  const canCreate =
    form.fullName.trim().length > 0 &&
    form.email.trim().length > 0 &&
    form.password.trim().length > 0 &&
    !createMutation.isPending;

  const selectUser = (userId: string) => {
    localStorage.setItem('devUserId', userId);
    navigate(routePaths.profiles);
  };

  return (
    <main className="min-h-screen px-4 py-6 md:px-8">
      <div className="mx-auto flex max-w-6xl flex-col gap-4">
        <section className="page-header">
          <div>
            <p className="eyebrow">Modo desarrollo</p>
            <h1>Dev users</h1>
            <p className="muted">
              Seleccioná o creá un usuario de desarrollo para operar perfiles y datos locales.
            </p>
          </div>
        </section>

        <section className="metric-grid">
          <MetricCard
            title="Usuarios creados"
            value={devUsers.length}
            helper="Disponibles para seleccionar."
            tone="info"
          />
        </section>

        <section className="panel">
          <div className="section-title">
            <div>
              <p className="eyebrow">Alta rápida</p>
              <h2>Crear usuario de desarrollo</h2>
            </div>
          </div>

          {devUsersQuery.isError ? (
            <ErrorState message={getApiErrorMessage(devUsersQuery.error)} />
          ) : null}

          <div className="form-grid">
            <label>
              Nombre completo
              <input
                className="input-ui"
                placeholder="Nombre completo"
                value={form.fullName}
                onChange={(event) => setForm({ ...form, fullName: event.target.value })}
              />
            </label>

            <label>
              Email
              <input
                className="input-ui"
                type="email"
                placeholder="Email"
                value={form.email}
                onChange={(event) => setForm({ ...form, email: event.target.value })}
              />
            </label>

            <label>
              Password
              <input
                className="input-ui"
                type="password"
                placeholder="Password"
                value={form.password}
                onChange={(event) => setForm({ ...form, password: event.target.value })}
              />
            </label>
          </div>

          <div className="form-actions">
            <button
              type="button"
              className="boton-principal"
              onClick={() => createMutation.mutate()}
              disabled={!canCreate}
            >
              {createMutation.isPending ? 'Creando...' : 'Crear y seleccionar'}
            </button>

            {!canCreate ? (
              <span className="muted">Completá nombre, email y password.</span>
            ) : null}
          </div>

          {createMutation.isError ? (
            <p className="mensaje-error">{getApiErrorMessage(createMutation.error)}</p>
          ) : null}
        </section>

        <section className="panel">
          <div className="section-title">
            <div>
              <p className="eyebrow">Selección</p>
              <h2>Usuarios disponibles</h2>
            </div>

            <span className="badge-count">{devUsers.length}</span>
          </div>

          {devUsersQuery.isLoading ? (
            <EmptyState title="Cargando usuarios" message="Estamos consultando usuarios de desarrollo." />
          ) : null}

          {!devUsersQuery.isLoading && !devUsersQuery.isError && devUsers.length === 0 ? (
            <EmptyState title="Sin usuarios" message="No hay usuarios creados." />
          ) : null}

          {devUsers.length > 0 ? (
            <div className="tabla-ui">
              <table className="table-compact">
                <thead>
                  <tr>
                    <th>Nombre</th>
                    <th>Email</th>
                    <th>Acciones</th>
                  </tr>
                </thead>

                <tbody>
                  {devUsers.map((user) => (
                    <tr key={user.id}>
                      <td>
                        <strong>{user.fullName}</strong>
                        <p className="compact-muted">ID: {user.id.slice(0, 8)}</p>
                      </td>

                      <td>{user.email}</td>

                      <td>
                        <button
                          type="button"
                          className="boton-principal"
                          onClick={() => selectUser(user.id)}
                        >
                          Seleccionar
                        </button>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          ) : null}
        </section>
      </div>
    </main>
  );
}
