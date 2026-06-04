import { Fragment, useMemo, useState } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useParams } from 'react-router-dom';
import {
  createCategory,
  deleteCategory,
  listCategories,
  updateCategory,
} from '../../api/categoriesApi';
import { AppLayout } from '../../app/shell/AppShell';
import { EmptyState } from '../../shared/ui/EmptyState';
import { ErrorState } from '../../shared/ui/ErrorState';
import { MetricCard } from '../../shared/ui/MetricCard';
import { StatusBadge } from '../../shared/ui/StatusBadge';
import {
  categoryScopeLabels,
  categoryTypeLabels,
  labelOrValue,
} from '../../domain/financeLabels';
import {
  categoryScopeOptions as allCategoryScopeOptions,
  categoryTypeOptions,
} from '../../domain/financeOptions';
import type { Category, CategoryScope, CategoryType } from '../../domain/types';
import { queryKeys } from '../../domain/queryKeys';
import { getCategoryDisplayName } from '../../domain/transactionRules';

type EditableScope = Exclude<CategoryScope, 'GLOBAL'>;

type EditForm = {
  id: string;
  parentId: string | null;
  name: string;
  type: CategoryType;
  scope: EditableScope;
};

const ALL_GROUPS = [
  'Ingresos',
  'Gastos fijos',
  'Gastos variables',
  'Deudas',
  'Ahorro/Inversión',
  'Técnicas',
] as const;

type CategoryGroup = (typeof ALL_GROUPS)[number];

const categoryScopeOptions = allCategoryScopeOptions.filter(
  (option): option is { value: EditableScope; label: string } => option.value !== 'GLOBAL',
);

const isEditableScope = (scope: CategoryScope): scope is EditableScope => scope !== 'GLOBAL';

export function CategoriesPage() {
  const { profileId = '' } = useParams();
  const queryClient = useQueryClient();

  const [includeGlobal, setIncludeGlobal] = useState(true);
  const [search, setSearch] = useState('');
  const [parentId, setParentId] = useState('');
  const [name, setName] = useState('');
  const [type, setType] = useState<CategoryType>('VARIABLE_EXPENSE');
  const [scope, setScope] = useState<EditableScope>('PERSONAL');
  const [editForm, setEditForm] = useState<EditForm | null>(null);

  const categoriesQuery = useQuery<Category[]>({
    queryKey: queryKeys.categories(profileId, includeGlobal),
    queryFn: () => listCategories(profileId, includeGlobal),
    enabled: Boolean(profileId),
  });

  const categories = useMemo(
    () => sortCategories(categoriesQuery.data ?? []),
    [categoriesQuery.data],
  );
  const activeCategories = categories.filter((category) => category.active);
  const globalCategories = categories.filter((category) => category.scope === 'GLOBAL');
  const duplicateCategoryGroups = buildDuplicateCategoryGroups(activeCategories);
  const parentOptions = categories.filter((category) => category.active);
  const filteredCategories = useMemo(
    () => categories.filter((category) => matchesCategorySearch(category, search)),
    [categories, search],
  );
  const groupedCategories = useMemo(
    () => groupCategories(filteredCategories),
    [filteredCategories],
  );

  const invalidateCategories = () =>
    queryClient.invalidateQueries({ queryKey: queryKeys.categories(profileId) });

  const createMutation = useMutation({
    mutationFn: () =>
      createCategory(profileId, {
        parentId: parentId || null,
        name: name.trim(),
        type,
        scope,
      }),
    onSuccess: () => {
      invalidateCategories();
      setParentId('');
      setName('');
      setType('VARIABLE_EXPENSE');
      setScope('PERSONAL');
    },
  });

  const updateMutation = useMutation({
    mutationFn: (payload: EditForm) =>
      updateCategory(payload.id, {
        parentId: payload.parentId,
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
      parentId: category.parentId ?? null,
      name: category.name,
      type: category.type,
      scope: isEditableScope(category.scope) ? category.scope : 'PERSONAL',
    });
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
              Mantené un catálogo jerárquico para que importación, presupuestos y reportes hablen el mismo idioma.
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
            helper="Disponibles en formularios y reglas."
            tone="success"
          />
          <MetricCard
            title="Globales"
            value={globalCategories.length}
            helper="Catálogo compartido de HogarIA."
            tone="neutral"
          />
        </section>

        <section className="panel">
          <div className="section-title">
            <div>
              <p className="eyebrow">Alta rápida</p>
              <h2>Crear categoría o subcategoría</h2>
            </div>
          </div>

          <div className="form-grid mt-4">
            <label>
              Nombre
              <input
                className="input-ui"
                value={name}
                onChange={(event) => setName(event.target.value)}
                placeholder="Ej: Supermercado"
              />
            </label>

            <label>
              Padre
              <ParentSelect
                value={parentId}
                categories={parentOptions}
                onChange={setParentId}
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
            {!canCreate ? <span className="muted">Completá el nombre para crearla.</span> : null}
          </div>

          {createMutation.isError ? (
            <p className="mensaje-error">No se pudo crear la categoría.</p>
          ) : null}
        </section>

        <section className="panel">
          <div className="section-title">
            <div>
              <p className="eyebrow">Listado</p>
              <h2>Árbol de categorías</h2>
              <p className="muted">
                Las subcategorías se muestran indentadas y con su ruta completa.
              </p>
            </div>
            <span className="badge-count">{filteredCategories.length}</span>
          </div>

          <div className="transaction-import-toolbar">
            <label className="transaction-import-search">
              Buscar
              <input
                className="input-ui"
                value={search}
                placeholder="Nombre, ruta o categoryKey"
                onChange={(event) => setSearch(event.target.value)}
              />
            </label>

            <label className="surface-inset cluster-ui">
              <input
                type="checkbox"
                checked={includeGlobal}
                onChange={(event) => setIncludeGlobal(event.target.checked)}
              />
              <span>Incluir categorías globales</span>
            </label>
          </div>

          {categoriesQuery.isLoading ? (
            <EmptyState title="Cargando categorías" message="Estamos consultando el catálogo." />
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

          {filteredCategories.length > 0 ? (
            <div className="tabla-ui">
              <table className="table-compact">
                <thead>
                  <tr>
                    <th>Ruta</th>
                    <th>CategoryKey</th>
                    <th>Tipo</th>
                    <th>Alcance</th>
                    <th>Estado</th>
                    <th>Acciones</th>
                  </tr>
                </thead>

                <tbody>
                  {ALL_GROUPS.map((group) => {
                    const groupItems = groupedCategories.get(group) ?? [];
                    if (groupItems.length === 0) return null;

                    return (
                      <Fragment key={group}>
                        <tr>
                          <td colSpan={6}>
                            <strong>{group}</strong>
                            <span className="muted"> · {groupItems.length}</span>
                          </td>
                        </tr>
                        {groupItems.map((category) => {
                          const activeEditForm = editForm?.id === category.id ? editForm : null;
                          const isEditing = Boolean(activeEditForm);
                          const editableParentOptions = parentOptions.filter(
                            (option) =>
                              option.id !== category.id &&
                              !buildDescendantIds(categories, category.id).has(option.id),
                          );

                          return (
                            <tr key={category.id}>
                              <td>
                                {isEditing ? (
                                  <div className="form-grid">
                                    <label>
                                      Nombre
                                      <input
                                        className="input-ui"
                                        value={activeEditForm?.name ?? ''}
                                        onChange={(event) =>
                                          setEditForm((current) =>
                                            current ? { ...current, name: event.target.value } : current,
                                          )
                                        }
                                      />
                                    </label>
                                    <label>
                                      Padre
                                      <ParentSelect
                                        value={activeEditForm?.parentId ?? ''}
                                        categories={editableParentOptions}
                                        onChange={(nextParentId) =>
                                          setEditForm((current) =>
                                            current
                                              ? { ...current, parentId: nextParentId || null }
                                              : current,
                                          )
                                        }
                                      />
                                    </label>
                                  </div>
                                ) : (
                                  <div
                                    style={{
                                      paddingLeft: `${Math.max(0, category.depth ?? 0) * 1.25}rem`,
                                    }}
                                  >
                                    <strong>{getCategoryDisplayName(category)}</strong>
                                    {category.parentId ? (
                                      <p className="compact-muted">Subcategoría de {parentName(categories, category.parentId)}</p>
                                    ) : null}
                                  </div>
                                )}
                              </td>

                              <td>
                                <span className="compact-muted">{category.categoryKey ?? '-'}</span>
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
                                  labelOrValue(categoryTypeLabels, category.type)
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
                                  labelOrValue(categoryScopeLabels, category.scope)
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
                                      onClick={() => setEditForm(null)}
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
                      </Fragment>
                    );
                  })}
                </tbody>
              </table>
            </div>
          ) : null}
        </section>

        <section className="panel" id="limpiar">
          <div className="section-title">
            <div>
              <p className="eyebrow">Limpieza</p>
              <h2>Posibles duplicadas</h2>
              <p className="muted">
                Se agrupan por `categoryKey` y tipo para detectar variantes que conviene desactivar.
              </p>
            </div>
            <span className="badge-count">{duplicateCategoryGroups.length}</span>
          </div>

          {duplicateCategoryGroups.length === 0 ? (
            <EmptyState
              title="Sin categorías repetidas"
              message="No encontramos categorías activas con el mismo categoryKey y tipo."
            />
          ) : (
            <div className="grid gap-3">
              {duplicateCategoryGroups.map((group) => (
                <article key={group.key} className="surface-inset">
                  <strong>{group.label}</strong>
                  <p className="muted">
                    {group.items.length} categorías comparten identidad normalizada.
                  </p>
                  <div className="row-actions mt-3">
                    {group.items.map((category) => (
                      <button
                        key={category.id}
                        type="button"
                        className="boton-secundario"
                        disabled={toggleMutation.isPending || category.scope === 'GLOBAL'}
                        onClick={() => toggleMutation.mutate(category)}
                      >
                        {category.scope === 'GLOBAL'
                          ? `${getCategoryDisplayName(category)} · global`
                          : `Desactivar ${getCategoryDisplayName(category)}`}
                      </button>
                    ))}
                  </div>
                </article>
              ))}
            </div>
          )}
        </section>
      </div>
    </AppLayout>
  );
}

function ParentSelect({
  value,
  categories,
  onChange,
}: {
  value: string;
  categories: Category[];
  onChange: (value: string) => void;
}) {
  return (
    <select
      className="input-ui"
      value={value}
      onChange={(event) => onChange(event.target.value)}
    >
      <option value="">Sin padre</option>
      {categories.map((category) => (
        <option key={category.id} value={category.id}>
          {getCategoryDisplayName(category)}
        </option>
      ))}
    </select>
  );
}

function sortCategories(categories: Category[]) {
  return [...categories].sort((left, right) => {
    if (left.active !== right.active) return left.active ? -1 : 1;
    if (left.technical !== right.technical) return left.technical ? 1 : -1;
    return getCategoryDisplayName(left).localeCompare(getCategoryDisplayName(right), 'es', {
      sensitivity: 'base',
    });
  });
}

function matchesCategorySearch(category: Category, search: string) {
  const normalizedSearch = normalize(search);
  if (!normalizedSearch) return true;

  return (
    normalize(category.name).includes(normalizedSearch) ||
    normalize(category.displayPath).includes(normalizedSearch) ||
    normalize(category.categoryKey).includes(normalizedSearch)
  );
}

function groupCategories(categories: Category[]) {
  const groups = new Map<CategoryGroup, Category[]>();

  for (const group of ALL_GROUPS) {
    groups.set(group, []);
  }

  categories.forEach((category) => {
    groups.get(categoryGroup(category))?.push(category);
  });

  return groups;
}

function categoryGroup(category: Category): CategoryGroup {
  if (category.technical) return 'Técnicas';
  if (category.type === 'INCOME') return 'Ingresos';
  if (category.type === 'FIXED_EXPENSE') return 'Gastos fijos';
  if (category.type === 'VARIABLE_EXPENSE') return 'Gastos variables';
  if (category.type === 'DEBT') return 'Deudas';
  return 'Ahorro/Inversión';
}

function buildDescendantIds(categories: Category[], categoryId: string) {
  const descendants = new Set<string>();
  let changed = true;

  while (changed) {
    changed = false;
    categories.forEach((category) => {
      if (
        category.parentId &&
        (category.parentId === categoryId || descendants.has(category.parentId)) &&
        !descendants.has(category.id)
      ) {
        descendants.add(category.id);
        changed = true;
      }
    });
  }

  return descendants;
}

function parentName(categories: Category[], parentId: string) {
  const parent = categories.find((category) => category.id === parentId);
  return parent ? getCategoryDisplayName(parent) : 'otra categoría';
}

function buildDuplicateCategoryGroups(categories: Category[]) {
  const groups = new Map<string, Category[]>();

  categories.forEach((category) => {
    const key = `${category.type}:${category.categoryKey ?? normalizeCategoryName(category.name)}`;
    groups.set(key, [...(groups.get(key) ?? []), category]);
  });

  return [...groups.entries()]
    .filter(([, items]) => items.length > 1)
    .map(([key, items]) => ({
      key,
      label: items.map((item) => getCategoryDisplayName(item)).join(' / '),
      items,
    }));
}

function normalizeCategoryName(value: string) {
  return value
    .normalize('NFD')
    .replace(/\p{Diacritic}/gu, '')
    .toLowerCase()
    .replace(/[^a-z0-9]+/g, '');
}

function normalize(value: string | null | undefined) {
  return (value ?? '')
    .normalize('NFD')
    .replace(/\p{Diacritic}/gu, '')
    .toLowerCase()
    .trim();
}
