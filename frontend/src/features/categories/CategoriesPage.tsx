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
import {
  categoryScopeLabels,
  categoryTypeLabels,
  labelOrValue,
} from '../../domain/financeLabels';
import type { Category, CategoryScope, CategoryType } from '../../domain/types';

export function CategoriesPage() {
  const { profileId = '' } = useParams();
  const qc = useQueryClient();

  const [includeGlobal, setIncludeGlobal] = useState(true);
  const [name, setName] = useState('');

  const q = useQuery({
    queryKey: ['categories', profileId, includeGlobal],
    queryFn: () => listCategories(profileId, includeGlobal),
    enabled: Boolean(profileId),
  });

  const c = useMutation({
    mutationFn: () =>
      createCategory(profileId, {
        name,
        type: 'VARIABLE_EXPENSE',
        scope: 'PERSONAL',
      }),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ['categories', profileId] });
      setName('');
    },
  });

  const u = useMutation({
    mutationFn: (cat: Category) =>
      updateCategory(cat.id, {
        name: cat.name,
        type: cat.type,
        scope: cat.scope,
        parentId: cat.parentId ?? undefined,
        active: !cat.active,
      }),
    onSuccess: () => qc.invalidateQueries({ queryKey: ['categories', profileId] }),
  });

  const d = useMutation({
    mutationFn: (id: string) => deleteCategory(id),
    onSuccess: () => qc.invalidateQueries({ queryKey: ['categories', profileId] }),
  });

  return (
    <AppLayout>
      <div className="card">
        <h1>Categorías</h1>

        <label>
          <input
            type="checkbox"
            checked={includeGlobal}
            onChange={(e) => setIncludeGlobal(e.target.checked)}
          />{' '}
          Incluir categorías globales
        </label>

        <div className="form-row">
          <input
            className="input"
            value={name}
            onChange={(e) => setName(e.target.value)}
            placeholder="Nombre de la categoría"
          />

          <button
            className="button-primary"
            onClick={() => c.mutate()}
            disabled={!name.trim() || c.isPending}
          >
            Crear
          </button>
        </div>

        {q.isLoading ? (
          <p className="empty-state">Cargando categorías...</p>
        ) : q.isError ? (
          <p className="empty-state">No se pudieron cargar las categorías.</p>
        ) : !q.data?.length ? (
          <p className="empty-state">Sin categorías.</p>
        ) : (
          <table className="table">
            <thead>
              <tr>
                <th>Nombre</th>
                <th>Tipo</th>
                <th>Alcance</th>
                <th>Activa</th>
                <th></th>
              </tr>
            </thead>

            <tbody>
              {q.data.map((cat: Category) => (
                <tr key={cat.id}>
                  <td>{cat.name}</td>
                  <td>{labelOrValue(categoryTypeLabels, cat.type as CategoryType)}</td>
                  <td>{labelOrValue(categoryScopeLabels, cat.scope as CategoryScope)}</td>
                  <td>{cat.active ? 'Sí' : 'No'}</td>
                  <td>
                    {cat.scope === 'GLOBAL' ? (
                      '-'
                    ) : (
                      <>
                        <button onClick={() => u.mutate(cat)}>
                          {cat.active ? 'Desactivar' : 'Activar'}
                        </button>

                        <button
                          className="button-danger"
                          onClick={() =>
                            window.confirm('¿Desactivar categoría?') && d.mutate(cat.id)
                          }
                        >
                          Eliminar
                        </button>
                      </>
                    )}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        )}
      </div>
    </AppLayout>
  );
}