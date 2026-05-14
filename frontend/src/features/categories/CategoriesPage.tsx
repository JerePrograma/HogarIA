import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useState } from 'react';
import { useParams } from 'react-router-dom';
import {
  createCategory,
  deleteCategory,
  listCategories,
  updateCategory,
} from '../../api/categoriesApi';
import { AppLayout } from '../../components/layout/AppLayout';
import { EmptyState } from '../../components/ui/EmptyState';
import { ErrorState } from '../../components/ui/ErrorState';
import { MetricCard } from '../../components/ui/MetricCard';
import { StatusBadge } from '../../components/ui/StatusBadge';
import {
  categoryScopeLabels,
  categoryTypeLabels,
  labelOrValue,
} from '../../domain/financeLabels';
import type { Category, CategoryScope, CategoryType } from '../../domain/types';

export function CategoriesPage() {
  const { profileId = '' } = useParams();
  const queryClient = useQueryClient();

  const [includeGlobal, setIncludeGlobal] = useState(true);
  const [name, setName] = useState('');

  const categoriesQuery = useQuery<Category[]>({
    queryKey: ['categories', profileId, includeGlobal],
    queryFn: () => listCategories(profileId, includeGlobal),
    enabled: Boolean(profileId),
  });

  const categories = categoriesQuery.data ?? [];
  const activeCategories = categories.filter((category) => category.active);
  const globalCategories = categories.filter((category) => category.scope === 'GLOBAL');

  const createMutation = useMutation({
    mutationFn: () =>
      createCategory(profileId, {
        name: name.trim(),
        type: 'VARIABLE_EXPENSE',
        scope: 'PERSONAL',
      }),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['categories', profileId] });
      setName('');
    },
  });

  const toggleMutation = useMutation({
    mutationFn: (category: Category) =>
      updateCategory(category.id, {
        name: category.name,
        type: category.type,
        scope: category.scope,
        parentId: category.parentId ?? undefined,
        active: !category.active,
      }),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['categories', profileId] }),
  });

  const deleteMutation = useMutation({
    mutationFn: (id: string) => deleteCategory(id),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['categories', profileId] }),
  });

  const canCreate = name.trim().length > 0 && !createMutation.isPending;

  return (
    <AppLayout>
      <div className="page-stack">
        <section className="page-header">
          <div>
            <p className="eyebrow">Clasificación</p>
            <h1>Categorías</h1>
            <p className="muted">
              Organizá movimientos, presupuestos, hábitos y planificación mediante categorías.
            </p>
          </div>
        </section>

        <section className="metric-grid">
          <MetricCard title="Total categorías" value={categories.length} helper="Incluye personales y globales visibles." tone="info" />
          <MetricCard title="Activas" value={activeCategories.length} helper="Disponibles para operar." tone="success" />
          <MetricCard title="Globales" value={globalCategories.length} helper="Catálogo compartido." tone="neutral" />
        </section>

        <section className="panel">
          <div className="section-title">
            <div>
              <p className="eyebrow">Alta rápida</p>
              <h2>Crear categoría</h2>
              <p className="secondary-text">
                Se crea como gasto variable personal. Después puede ampliarse con selector de tipo y alcance.
              </p>
            </div>
          </div>

          <label className="surface-inset cluster-ui">
            <input
              type="checkbox"
              checked={includeGlobal}
              onChange={(event) => setIncludeGlobal(event.target.checked)}
            />
            <span>Incluir categorías globales</span>
          </label>

          <div className="form-row mt-4">
            <label>
              Nombre
              <input
                className="input-ui"
                value={name}
                onChange={(event) => setName(event.target.value)}
                placeholder="Nombre de la categoría"
              />
            </label>

            <button
              type="button"
              className="boton-principal"
              onClick={() => createMutation.mutate()}
              disabled={!canCreate}
            >
              {createMutation.isPending ? 'Creando...' : 'Crear categoría'}
            </button>
          </div>

          {createMutation.isError ? (
            <p className="mensaje-error">No se pudo crear la categoría.</p>
          ) : null}
        </section>

        <section className="panel">
          <div className="section-title">
            <div>
              <p className="eyebrow">Listado</p>
              <h2>Categorías cargadas</h2>
            </div>

            <span className="badge-count">{categories.length}</span>
          </div>

          {categoriesQuery.isLoading ? (
            <EmptyState title="Cargando categorías" message="Estamos consultando las categorías." />
          ) : null}

          {categoriesQuery.isError ? (
            <ErrorState message="No se pudieron cargar las categorías." />
          ) : null}

          {!categoriesQuery.isLoading && !categoriesQuery.isError && categories.length === 0 ? (
            <EmptyState title="Sin categorías" message="Todavía no hay categorías cargadas." />
          ) : null}

          {categories.length > 0 ? (
            <div className="tabla-ui">
              <table className="table-compact">
                <thead>
                  <tr>
                    <th>Nombre</th>
                    <th>Tipo</th>
                    <th>Alcance</th>
                    <th>Estado</th>
                    <th>Acciones</th>
                  </tr>
                </thead>

                <tbody>
                  {categories.map((category) => (
                    <tr key={category.id}>
                      <td>
                        <strong>{category.name}</strong>
                      </td>
                      <td>{labelOrValue(categoryTypeLabels, category.type as CategoryType)}</td>
                      <td>{labelOrValue(categoryScopeLabels, category.scope as CategoryScope)}</td>
                      <td>
                        <StatusBadge
                          tone={category.active ? 'ok' : 'watch'}
                          label={category.active ? 'Activa' : 'Inactiva'}
                        />
                      </td>
                      <td>
                        {category.scope === 'GLOBAL' ? (
                          <span className="compact-muted">Solo lectura</span>
                        ) : (
                          <div className="row-actions">
                            <button
                              type="button"
                              className="boton-secundario"
                              onClick={() => toggleMutation.mutate(category)}
                              disabled={toggleMutation.isPending}
                            >
                              {category.active ? 'Desactivar' : 'Activar'}
                            </button>

                            <button
                              type="button"
                              className="boton-danger"
                              onClick={() =>
                                window.confirm('¿Desactivar categoría?') &&
                                deleteMutation.mutate(category.id)
                              }
                              disabled={deleteMutation.isPending}
                            >
                              Eliminar
                            </button>
                          </div>
                        )}
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