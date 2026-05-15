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
import { queryKeys } from '../../domain/queryKeys';

const categoryTypeOptions: Array<{ value: CategoryType; label: string }> = [
  { value: 'INCOME', label: 'Ingreso' },
  { value: 'FIXED_EXPENSE', label: 'Gasto fijo' },
  { value: 'VARIABLE_EXPENSE', label: 'Gasto variable' },
  { value: 'SAVING', label: 'Ahorro' },
  { value: 'DEBT', label: 'Deuda' },
  { value: 'INVESTMENT', label: 'Inversión' },
];

const categoryScopeOptions: Array<{ value: Exclude<CategoryScope, 'GLOBAL'>; label: string }> = [
  { value: 'PERSONAL', label: 'Personal' },
  { value: 'FAMILY', label: 'Familiar' },
  { value: 'BUSINESS', label: 'Negocio' },
];

type EditableScope = Exclude<CategoryScope, 'GLOBAL'>;

type EditForm = {
  id: string;
  name: string;
  type: CategoryType;
  scope: EditableScope;
};

const isEditableScope = (scope: CategoryScope): scope is EditableScope => scope !== 'GLOBAL';

export function CategoriesPage() {
  const { profileId = '' } = useParams();
  const queryClient = useQueryClient();

  const [includeGlobal, setIncludeGlobal] = useState(true);
  const [name, setName] = useState('');
  const [type, setType] = useState<CategoryType>('VARIABLE_EXPENSE');
  const [scope, setScope] = useState<EditableScope>('PERSONAL');

  const [editForm, setEditForm] = useState<EditForm | null>(null);

  const categoriesQuery = useQuery<Category[]>({
    queryKey: queryKeys.categories(profileId, includeGlobal),
    queryFn: () => listCategories(profileId, includeGlobal),
    enabled: Boolean(profileId),
  });

  const categories = categoriesQuery.data ?? [];
  const activeCategories = categories.filter((category) => category.active);
  const globalCategories = categories.filter((category) => category.scope === 'GLOBAL');

  const invalidateCategories = () =>
    queryClient.invalidateQueries({ queryKey: queryKeys.categories(profileId) });

  const createMutation = useMutation({
    mutationFn: () =>
      createCategory(profileId, {
        name: name.trim(),
        type,
        scope,
      }),
    onSuccess: () => {
      invalidateCategories();
      setName('');
      setType('VARIABLE_EXPENSE');
      setScope('PERSONAL');
    },
  });

  const updateMutation = useMutation({
    mutationFn: (payload: EditForm) =>
      updateCategory(payload.id, {
        name: payload.name.trim(),
        type: payload.type,
        scope: payload.scope,
      }),
    onSuccess: () => {
      invalidateCategories();
      setEditForm(null);
    },
  });

  const toggleMutation = useMutation({
    mutationFn: (category: Category) =>
      updateCategory(category.id, {
        name: category.name,
        type: category.type,
        scope: isEditableScope(category.scope) ? category.scope : 'PERSONAL',
        parentId: category.parentId ?? undefined,
        active: !category.active,
      }),
    onSuccess: invalidateCategories,
  });

  const deleteMutation = useMutation({
    mutationFn: (id: string) => deleteCategory(id),
    onSuccess: invalidateCategories,
  });

  const canCreate = Boolean(profileId) && name.trim().length > 0 && !createMutation.isPending;

  const canSaveEdit = (editForm?.name.trim().length ?? 0) > 0 && !updateMutation.isPending;

  const startEdit = (category: Category) => {
    if (category.scope === 'GLOBAL') return;

    setEditForm({
      id: category.id,
      name: category.name,
      type: category.type as CategoryType,
      scope: isEditableScope(category.scope) ? category.scope : 'PERSONAL',
    });
  };

  const cancelEdit = () => {
    setEditForm(null);
  };

  const saveEdit = () => {
    if (!editForm || !canSaveEdit) return;
    updateMutation.mutate(editForm);
  };

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
          <MetricCard
            title="Total categorías"
            value={categories.length}
            helper="Incluye personales y globales visibles."
            tone="info"
          />

          <MetricCard
            title="Activas"
            value={activeCategories.length}
            helper="Disponibles para operar."
            tone="success"
          />

          <MetricCard
            title="Globales"
            value={globalCategories.length}
            helper="Catálogo compartido."
            tone="neutral"
          />
        </section>

        <section className="panel">
          <div className="section-title">
            <div>
              <p className="eyebrow">Alta rápida</p>
              <h2>Crear categoría</h2>
              <p className="secondary-text">
                Definí el tipo contable de la categoría para ingresos, gastos, ahorro, deuda o inversión.
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

          <div className="form-grid mt-4">
            <label>
              Nombre
              <input
                className="input-ui"
                value={name}
                onChange={(event) => setName(event.target.value)}
                placeholder="Ej: CJ - Interés cobrado"
              />
            </label>

            <label>
              Tipo
              <select
                className="input-ui"
                value={type}
                onChange={(event) => setType(event.target.value as CategoryType)}
              >
                {categoryTypeOptions.map((option) => (
                  <option key={option.value} value={option.value}>
                    {option.label}
                  </option>
                ))}
              </select>
            </label>

            <label>
              Alcance
              <select
                className="input-ui"
                value={scope}
                onChange={(event) => setScope(event.target.value as EditableScope)}
              >
                {categoryScopeOptions.map((option) => (
                  <option key={option.value} value={option.value}>
                    {option.label}
                  </option>
                ))}
              </select>
            </label>
          </div>

          <div className="form-actions">
            <button
              type="button"
              className="boton-principal"
              onClick={() => createMutation.mutate()}
              disabled={!canCreate}
            >
              {createMutation.isPending ? 'Creando...' : 'Crear categoría'}
            </button>

            {!canCreate ? (
              <span className="muted">Completá el nombre para crear la categoría.</span>
            ) : null}
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

          {updateMutation.isError ? (
            <p className="mensaje-error">No se pudo actualizar la categoría.</p>
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
                  {categories.map((category) => {
                    const activeEditForm = editForm?.id === category.id ? editForm : null;
                    const isEditing = Boolean(activeEditForm);

                    return (
                      <tr key={category.id}>
                        <td>
                          {isEditing ? (
                            <input
                              className="input-ui"
                              value={activeEditForm?.name ?? ''}
                              onChange={(event) =>
                                setEditForm((current) =>
                                  current ? { ...current, name: event.target.value } : current,
                                )
                              }
                            />
                          ) : (
                            <strong>{category.name}</strong>
                          )}
                        </td>

                        <td>
                          {isEditing ? (
                            <select
                              className="input-ui"
                              value={activeEditForm?.type ?? 'VARIABLE_EXPENSE'}
                              onChange={(event) =>
                                setEditForm((current) =>
                                  current
                                    ? { ...current, type: event.target.value as CategoryType }
                                    : current,
                                )
                              }
                            >
                              {categoryTypeOptions.map((option) => (
                                <option key={option.value} value={option.value}>
                                  {option.label}
                                </option>
                              ))}
                            </select>
                          ) : (
                            labelOrValue(categoryTypeLabels, category.type as CategoryType)
                          )}
                        </td>

                        <td>
                          {isEditing ? (
                            <select
                              className="input-ui"
                              value={activeEditForm?.scope ?? 'PERSONAL'}
                              onChange={(event) =>
                                setEditForm((current) =>
                                  current
                                    ? { ...current, scope: event.target.value as EditableScope }
                                    : current,
                                )
                              }
                            >
                              {categoryScopeOptions.map((option) => (
                                <option key={option.value} value={option.value}>
                                  {option.label}
                                </option>
                              ))}
                            </select>
                          ) : (
                            labelOrValue(categoryScopeLabels, category.scope as CategoryScope)
                          )}
                        </td>

                        <td>
                          <StatusBadge
                            tone={category.active ? 'ok' : 'watch'}
                            label={category.active ? 'Activa' : 'Inactiva'}
                          />
                        </td>

                        <td>
                          {category.scope === 'GLOBAL' ? (
                            <span className="compact-muted">Solo lectura</span>
                          ) : isEditing ? (
                            <div className="row-actions">
                              <button
                                type="button"
                                className="boton-principal"
                                onClick={saveEdit}
                                disabled={!canSaveEdit}
                              >
                                {updateMutation.isPending ? 'Guardando...' : 'Guardar'}
                              </button>

                              <button
                                type="button"
                                className="boton-secundario"
                                onClick={cancelEdit}
                                disabled={updateMutation.isPending}
                              >
                                Cancelar
                              </button>
                            </div>
                          ) : (
                            <div className="row-actions">
                              <button
                                type="button"
                                className="boton-secundario"
                                onClick={() => startEdit(category)}
                                disabled={Boolean(editForm)}
                              >
                                Editar
                              </button>

                              <button
                                type="button"
                                className="boton-secundario"
                                onClick={() => toggleMutation.mutate(category)}
                                disabled={toggleMutation.isPending || Boolean(editForm)}
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
                                disabled={deleteMutation.isPending || Boolean(editForm)}
                              >
                                Eliminar
                              </button>
                            </div>
                          )}
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