import { EmptyState } from "../../../components/ui/EmptyState";
import { ErrorState } from "../../../components/ui/ErrorState";
import type { Category } from "../../../domain/types";
import { getApiErrorMessage } from "../budgetErrors";
import { BudgetEditorTable } from "./BudgetEditorTable";

type Props = {
  categories: Category[];
  loading: boolean;
  error: boolean;
  amountByCategoryId: Map<string, number>;
  canEdit: boolean;
  savingCategoryId?: string | null;
  saveError: unknown;
  onSaveAmount: (categoryId: string, amount: number) => void;
};

export function BudgetEditorPanel({
  categories,
  loading,
  error,
  amountByCategoryId,
  canEdit,
  savingCategoryId,
  saveError,
  onSaveAmount,
}: Props) {
  return (
    <section className="panel">
      <div className="section-title">
        <div>
          <p className="eyebrow">Plan</p>
          <h2>Importes presupuestados</h2>
          <p className="secondary-text">
            Cargá el importe esperado por categoría. Los cambios se guardan al
            salir del campo.
          </p>
        </div>
      </div>

      {loading ? (
        <p className="secondary-text">Cargando categorías...</p>
      ) : null}

      {error ? (
        <ErrorState message="No se pudieron cargar las categorías." />
      ) : null}

      {!loading && !error && categories.length === 0 ? (
        <EmptyState
          title="Sin categorías presupuestables"
          message="No hay categorías activas de gasto, ahorro, deuda o inversión para presupuestar."
        />
      ) : null}

      {categories.length > 0 ? (
        <BudgetEditorTable
          categories={categories}
          amountByCategoryId={amountByCategoryId}
          canEdit={canEdit}
          savingCategoryId={savingCategoryId}
          onSaveAmount={onSaveAmount}
        />
      ) : null}

      {saveError ? (
        <p className="mensaje-error mt-4">{getApiErrorMessage(saveError)}</p>
      ) : null}
    </section>
  );
}
