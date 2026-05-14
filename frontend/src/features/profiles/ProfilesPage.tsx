import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { getApiErrorMessage } from '../../api/http';
import {
  createProfile,
  deleteProfile,
  listProfiles,
  updateProfile,
} from '../../api/profilesApi';
import { AppLayout } from '../../components/layout/AppLayout';
import { EmptyState } from '../../components/ui/EmptyState';
import { ErrorState } from '../../components/ui/ErrorState';
import { MetricCard } from '../../components/ui/MetricCard';
import { StatusBadge } from '../../components/ui/StatusBadge';
import type { Profile, ProfileType } from '../../domain/types';

type ProfileForm = {
  name: string;
  type: ProfileType;
  baseCurrency: string;
  activeYear: number;
};

const profileTypeLabels: Record<ProfileType, string> = {
  PERSONAL: 'Personal',
  FAMILY: 'Familiar',
  BUSINESS: 'Negocio',
};

const profileTypeOptions: Array<{ value: ProfileType; label: string }> = [
  { value: 'PERSONAL', label: 'Personal' },
  { value: 'FAMILY', label: 'Familiar' },
  { value: 'BUSINESS', label: 'Negocio' },
];

const createInitialForm = (): ProfileForm => ({
  name: '',
  type: 'PERSONAL',
  baseCurrency: 'ARS',
  activeYear: new Date().getFullYear(),
});

export function ProfilesPage() {
  const navigate = useNavigate();
  const queryClient = useQueryClient();

  const [form, setForm] = useState<ProfileForm>(createInitialForm);

  const profilesQuery = useQuery<Profile[]>({
    queryKey: ['profiles'],
    queryFn: listProfiles,
  });

  const profiles = profilesQuery.data ?? [];
  const activeProfiles = profiles.filter((profile) => profile.active);
  const inactiveProfiles = profiles.filter((profile) => !profile.active);

  const createMutation = useMutation({
    mutationFn: () =>
      createProfile({
        ...form,
        name: form.name.trim(),
        baseCurrency: form.baseCurrency.trim().toUpperCase() || 'ARS',
      }),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['profiles'] });
      setForm((current) => ({ ...current, name: '' }));
    },
  });

  const toggleMutation = useMutation({
    mutationFn: ({
      id,
      payload,
    }: {
      id: string;
      payload: Parameters<typeof updateProfile>[1];
    }) => updateProfile(id, payload),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['profiles'] }),
  });

  const deleteMutation = useMutation({
    mutationFn: (id: string) => deleteProfile(id),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['profiles'] }),
  });

  const canCreate =
    form.name.trim().length > 0 &&
    form.baseCurrency.trim().length > 0 &&
    form.activeYear >= 2000 &&
    form.activeYear <= 2100 &&
    !createMutation.isPending;

  const enterProfile = (profileId: string) => {
    localStorage.setItem('selectedProfileId', profileId);
    navigate(`/profiles/${profileId}/dashboard`);
  };

  const toggleProfile = (profile: Profile) => {
    toggleMutation.mutate({
      id: profile.id,
      payload: {
        name: profile.name,
        type: profile.type,
        baseCurrency: profile.baseCurrency,
        activeYear: profile.activeYear,
        active: !profile.active,
      },
    });
  };

  return (
    <AppLayout>
      <div className="page-stack">
        <section className="page-header">
          <div>
            <p className="eyebrow">Espacio de trabajo</p>
            <h1>Perfiles</h1>
            <p className="muted">
              Elegí el contexto financiero sobre el que vas a operar: personal, familiar o negocio.
            </p>
          </div>
        </section>

        <section className="metric-grid">
          <MetricCard
            title="Total perfiles"
            value={profiles.length}
            helper="Perfiles disponibles."
            tone="info"
          />

          <MetricCard
            title="Activos"
            value={activeProfiles.length}
            helper="Listos para operar."
            tone="success"
          />

          <MetricCard
            title="Inactivos"
            value={inactiveProfiles.length}
            helper="Pausados o fuera de uso."
            tone={inactiveProfiles.length > 0 ? 'warning' : 'neutral'}
          />
        </section>

        <section className="panel">
          <div className="section-title">
            <div>
              <p className="eyebrow">Alta</p>
              <h2>Crear perfil</h2>
              <p className="secondary-text">
                Creá un perfil por unidad financiera real. Mezclar gastos personales, familiares y
                negocio en uno solo después sale caro.
              </p>
            </div>
          </div>

          <div className="form-grid">
            <label>
              Nombre
              <input
                className="input-ui"
                value={form.name}
                placeholder="Ej: Personal, Familia, Negocio"
                onChange={(event) => setForm({ ...form, name: event.target.value })}
              />
            </label>

            <label>
              Tipo
              <select
                className="input-ui"
                value={form.type}
                onChange={(event) =>
                  setForm({ ...form, type: event.target.value as ProfileType })
                }
              >
                {profileTypeOptions.map((option) => (
                  <option key={option.value} value={option.value}>
                    {option.label}
                  </option>
                ))}
              </select>
            </label>

            <label>
              Moneda base
              <input
                className="input-ui"
                value={form.baseCurrency}
                maxLength={3}
                onChange={(event) =>
                  setForm({ ...form, baseCurrency: event.target.value.toUpperCase() })
                }
              />
            </label>

            <label>
              Año activo
              <input
                className="input-ui"
                type="number"
                min={2000}
                max={2100}
                value={form.activeYear}
                onChange={(event) =>
                  setForm({ ...form, activeYear: Number(event.target.value) })
                }
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
              {createMutation.isPending ? 'Creando...' : 'Crear perfil'}
            </button>

            {!canCreate ? (
              <span className="muted">
                Completá nombre, moneda y un año válido para crear el perfil.
              </span>
            ) : null}
          </div>

          {createMutation.isError ? (
            <p className="mensaje-error">{getApiErrorMessage(createMutation.error)}</p>
          ) : null}
        </section>

        <section className="panel">
          <div className="section-title">
            <div>
              <p className="eyebrow">Listado</p>
              <h2>Perfiles disponibles</h2>
            </div>

            <span className="badge-count">{profiles.length}</span>
          </div>

          {profilesQuery.isLoading ? (
            <EmptyState title="Cargando perfiles" message="Estamos consultando tus perfiles." />
          ) : null}

          {profilesQuery.isError ? (
            <ErrorState message={getApiErrorMessage(profilesQuery.error)} />
          ) : null}

          {!profilesQuery.isLoading && !profilesQuery.isError && profiles.length === 0 ? (
            <EmptyState
              title="Sin perfiles"
              message="No hay perfiles creados. Creá uno para empezar a operar."
            />
          ) : null}

          {profiles.length > 0 ? (
            <div className="tabla-ui">
              <table className="table-compact">
                <thead>
                  <tr>
                    <th>Nombre</th>
                    <th>Tipo</th>
                    <th>Moneda</th>
                    <th>Año</th>
                    <th>Estado</th>
                    <th>Acciones</th>
                  </tr>
                </thead>

                <tbody>
                  {profiles.map((profile) => (
                    <tr key={profile.id}>
                      <td>
                        <strong>{profile.name}</strong>
                        <p className="compact-muted">ID: {profile.id.slice(0, 8)}</p>
                      </td>

                      <td>{profileTypeLabels[profile.type] ?? profile.type}</td>
                      <td>{profile.baseCurrency}</td>
                      <td>{profile.activeYear}</td>

                      <td>
                        <StatusBadge
                          tone={profile.active ? 'ok' : 'watch'}
                          label={profile.active ? 'Activo' : 'Inactivo'}
                        />
                      </td>

                      <td>
                        <div className="row-actions">
                          <button
                            type="button"
                            className="boton-principal"
                            onClick={() => enterProfile(profile.id)}
                            disabled={!profile.active}
                          >
                            Entrar
                          </button>

                          <button
                            type="button"
                            className="boton-secundario"
                            onClick={() => toggleProfile(profile)}
                            disabled={toggleMutation.isPending}
                          >
                            {profile.active ? 'Desactivar' : 'Activar'}
                          </button>

                          <button
                            type="button"
                            className="boton-danger"
                            onClick={() =>
                              window.confirm('¿Desactivar perfil?') &&
                              deleteMutation.mutate(profile.id)
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