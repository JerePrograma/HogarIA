import type { Category, CategoryType } from "../../../domain/types";
import {
  categoryTypeLabels,
  labelOrValue,
} from "../../../domain/financeLabels";
import { BudgetAmountInput } from "./BudgetAmountInput";

type Props = {
  categories: Category[];
  amountByCategoryId: Map<string, number>;
  canEdit: boolean;
  savingCategoryId?: string | null;
  onSaveAmount: (categoryId: string, amount: number) => void;
};

export function BudgetEditorTable({
  categories,
  amountByCategoryId,
  canEdit,
  savingCategoryId,
  onSaveAmount,
}: Props) {
  return (
    <div className="budget-editor-responsive">
      <div className="tabla-ui budget-editor-table">
        <table>
          <thead>
            <tr>
              <th>Categoría</th>
              <th>Tipo</th>
              <th className="amount-cell">Presupuesto</th>
            </tr>
          </thead>

          <tbody>
            {categories.map((category) => {
              const currentAmount = amountByCategoryId.get(category.id) ?? 0;
              const saving = savingCategoryId === category.id;

              return (
                <tr key={category.id}>
                  <td>
                    <strong>{category.name}</strong>
                  </td>

                  <td>
                    {labelOrValue(
                      categoryTypeLabels,
                      category.type as CategoryType,
                    )}
                  </td>

                  <td className="amount-cell">
                    <BudgetAmountInput
                      value={currentAmount}
                      disabled={!canEdit || saving}
                      saving={saving}
                      onCommit={(amount) => onSaveAmount(category.id, amount)}
                    />
                  </td>
                </tr>
              );
            })}
          </tbody>
        </table>
      </div>

      <div className="budget-editor-mobile-list">
        {categories.map((category) => {
          const currentAmount = amountByCategoryId.get(category.id) ?? 0;
          const saving = savingCategoryId === category.id;

          return (
            <article key={category.id} className="budget-mobile-card">
              <div>
                <strong>{category.name}</strong>
                <span>
                  {labelOrValue(categoryTypeLabels, category.type as CategoryType)}
                </span>
              </div>

              <BudgetAmountInput
                value={currentAmount}
                disabled={!canEdit || saving}
                saving={saving}
                onCommit={(amount) => onSaveAmount(category.id, amount)}
              />
            </article>
          );
        })}
      </div>
    </div>
  );
}
