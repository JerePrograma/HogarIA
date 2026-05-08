import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { createDevUser, listDevUsers } from '../../api/devUsersApi';
import { getApiErrorMessage } from '../../api/http';

interface DevUserRow {
  id: string;
  fullName: string;
  email: string;
}

export function DevUserPage() {
  const nav = useNavigate();
  const qc = useQueryClient();

  const [form, setForm] = useState({
    fullName: '',
    email: '',
    password: '',
  });

  const usersQuery = useQuery({
    queryKey: ['dev-users'],
    queryFn: listDevUsers,
  });

  const createUserMutation = useMutation({
    mutationFn: () => createDevUser(form),
    onSuccess: (user: { id: string }) => {
      qc.invalidateQueries({ queryKey: ['dev-users'] });
      localStorage.setItem('devUserId', user.id);
      nav('/profiles');
    },
  });

  const canCreate = Boolean(form.fullName.trim()) && Boolean(form.email.trim()) && Boolean(form.password);

  return (
    <main className="auth-page">
      <section className="auth-card">
        <div className="page-header">
          <div>
            <p className="eyebrow">Entorno de desarrollo</p>
            <h1>Usuarios de desarrollo</h1>
            <p className="muted">Seleccioná un usuario existente o creá uno nuevo para continuar.</p>
          </div>
        </div>

        {usersQuery.isError && <div className="alert danger">{getApiErrorMessage(usersQuery.error)}</div>}

        <div className="card">
          <h2>Crear usuario</h2>

          <div className="form-grid">
            <label className="field">
              <span>Nombre completo</span>
              <input
                value={form.fullName}
                placeholder="Ej: Jeremías Rivelli"
                onChange={(event) => setForm({ ...form, fullName: event.target.value })}
              />
            </label>

            <label className="field">
              <span>Correo electrónico</span>
              <input
                type="email"
                value={form.email}
                placeholder="usuario@correo.com"
                onChange={(event) => setForm({ ...form, email: event.target.value })}
              />
            </label>

            <label className="field">
              <span>Contraseña</span>
              <input
                type="password"
                value={form.password}
                placeholder="Contraseña"
                onChange={(event) => setForm({ ...form, password: event.target.value })}
              />
            </label>
          </div>

          <div className="actions">
            <button
              type="button"
              className="button primary"
              onClick={() => createUserMutation.mutate()}
              disabled={!canCreate || createUserMutation.isPending}
            >
              Crear y seleccionar
            </button>
          </div>
        </div>

        <div className="card">
          <h2>Usuarios disponibles</h2>

          {usersQuery.isLoading ? (
            <p className="muted">Cargando usuarios...</p>
          ) : !usersQuery.data?.length ? (
            <p className="muted">No hay usuarios creados.</p>
          ) : (
            <div className="table-wrap">
              <table className="table">
                <thead>
                  <tr>
                    <th>Nombre</th>
                    <th>Correo electrónico</th>
                    <th>Acciones</th>
                  </tr>
                </thead>
                <tbody>
                  {usersQuery.data.map((user: DevUserRow) => (
                    <tr key={user.id}>
                      <td>{user.fullName}</td>
                      <td>{user.email}</td>
                      <td>
                        <button
                          type="button"
                          className="button secondary"
                          onClick={() => {
                            localStorage.setItem('devUserId', user.id);
                            nav('/profiles');
                          }}
                        >
                          Seleccionar
                        </button>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}
        </div>
      </section>
    </main>
  );
}