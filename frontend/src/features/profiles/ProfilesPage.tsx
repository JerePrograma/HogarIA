import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { createProfile, deleteProfile, listProfiles, updateProfile } from '../../api/profilesApi';
import { getApiErrorMessage } from '../../api/http';
import { AppLayout } from '../../components/layout/AppLayout';
import { labelOrMissing, profileTypeLabels } from '../../domain/financeLabels';
import { profileTypeOptions } from '../../domain/financeOptions';
import type { Profile, ProfileType } from '../../domain/types';

export function ProfilesPage() {
  const nav = useNavigate();
  const qc = useQueryClient();

  const [form, setForm] = useState({
    name: '',
    type: 'PERSONAL' as ProfileType,
    baseCurrency: 'ARS',
    activeYear: new Date().getFullYear(),
  });

  const profilesQuery = useQuery({
    queryKey: ['profiles'],
    queryFn: listProfiles,
  });

  const createProfileMutation = useMutation({
    mutationFn: () => createProfile(form),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ['profiles'] });
      setForm({ ...form, name: '' });
    },
  });

  const updateProfileMutation = useMutation({
    mutationFn: (profile: Profile) =>
      updateProfile(profile.id, {
        name: profile.name,
        type: profile.type,
        baseCurrency: profile.baseCurrency,
        activeYear: profile.activeYear,
        active: !profile.active,
      }),
    onSuccess: () => qc.invalidateQueries({ queryKey: ['profiles'] }),
  });

  const deleteProfileMutation = useMutation({
    mutationFn: (id: string) => deleteProfile(id),
    onSuccess: () => qc.invalidateQueries({ queryKey: ['profiles'] }),
  });

  if (profilesQuery.isLoading) {
    return (
      <AppLayout>
        <p className="muted">Cargando perfiles...</p>
      </AppLayout>
    );
  }

  if (profilesQuery.isError) {
    return (
      <AppLayout>
        <div className="alert danger">{getApiErrorMessage(profilesQuery.error)}</div>
      </AppLayout>
    );
  }

  return (
    <AppLayout>
      <div className="page-header">
        <div>
          <p className="eyebrow">Configuración</p>
          <h1>Perfiles financieros</h1>
          <p className="muted">Separá tus finanzas personales, familiares o de negocio.</p>
        </div>
      </div>

      <section className="card">
        <h2>Crear perfil</h2>

        <div className="form-grid">
          <label className="field">
            <span>Nombre del perfil</span>
            <input
              value={form.name}
              placeholder="Ej: Hogar, Personal, Negocio"
              onChange={(event) => setForm({ ...form, name: event.target.value })}
            />
          </label>

          <label className="field">
            <span>Tipo</span>
            <select
              value={form.type}
              onChange={(event) => setForm({ ...form, type: event.target.value as ProfileType })}
            >
              {profileTypeOptions.map((option) => (
                <option key={option.value} value={option.value}>
                  {option.label}
                </option>
              ))}
            </select>
          </label>

          <label className="field">
            <span>Moneda base</span>
            <input
              value={form.baseCurrency}
              maxLength={3}
              onChange={(event) => setForm({ ...form, baseCurrency: event.target.value.toUpperCase() })}
            />
          </label>

          <label className="field">
            <span>Año activo</span>
            <input
              type="number"
              value={form.activeYear}
              onChange={(event) => setForm({ ...form, activeYear: Number(event.target.value) })}
            />
          </label>
        </div>

        <div className="actions">
          <button
            type="button"
            className="button primary"
            onClick={() => createProfileMutation.mutate()}
            disabled={!form.name.trim() || createProfileMutation.isPending}
          >
            Crear perfil
          </button>
        </div>
      </section>

      <section className="card">
        <h2>Perfiles disponibles</h2>

        {!profilesQuery.data?.length ? (
          <p className="muted">No hay perfiles.</p>
        ) : (
          <div className="table-wrap">
            <table className="table">
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
                {profilesQuery.data.map((profile: Profile) => (
                  <tr key={profile.id}>
                    <td>{profile.name}</td>
                    <td>{labelOrMissing(profileTypeLabels, profile.type)}</td>
                    <td>{profile.baseCurrency}</td>
                    <td>{profile.activeYear}</td>
                    <td>
                      <span className={`badge ${profile.active ? 'good' : 'muted'}`}>
                        {profile.active ? 'Activo' : 'Inactivo'}
                      </span>
                    </td>
                    <td>
                      <div className="actions compact">
                        <button
                          type="button"
                          className="button secondary"
                          onClick={() => {
                            localStorage.setItem('selectedProfileId', profile.id);
                            nav(`/profiles/${profile.id}/dashboard`);
                          }}
                        >
                          Entrar
                        </button>

                        <button
                          type="button"
                          className="button ghost"
                          onClick={() => updateProfileMutation.mutate(profile)}
                        >
                          {profile.active ? 'Desactivar' : 'Activar'}
                        </button>

                        <button
                          type="button"
                          className="button danger"
                          onClick={() => window.confirm('¿Eliminar perfil?') && deleteProfileMutation.mutate(profile.id)}
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