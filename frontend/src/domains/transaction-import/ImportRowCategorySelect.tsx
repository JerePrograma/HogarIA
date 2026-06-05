import { Category } from "../../domain/types";
import { getSelectableCategoriesForImportRow } from "./utils/importUtils";
import { TransactionImportRow } from "./types";
import { getCategoryDisplayName } from "../../domain/transactionRules";

interface Props {
  row: TransactionImportRow;
  categories: Category[];
  onChange: (categoryId: string | null) => void;
  disabled?: boolean;
}

export function ImportRowCategorySelect({ row, categories, onChange, disabled = false }: Props) {
  const compatibleCategories = getSelectableCategoriesForImportRow(
    row,
    categories,
  );

  const currentCategoryStillVisible =
    !row.suggestedCategoryId ||
    compatibleCategories.some(
      (category) => category.id === row.suggestedCategoryId,
    );

  return (
    <select
      className="input-ui"
      value={row.suggestedCategoryId ?? ""}
      disabled={disabled}
      onChange={(event) => onChange(event.target.value || null)}
    >
      <option value="">Sin categoría asignada</option>

      {!currentCategoryStillVisible && row.suggestedCategoryId ? (
        <option value={row.suggestedCategoryId}>
          Categoría incompatible seleccionada
        </option>
      ) : null}

      {compatibleCategories.map((category) => (
        <option key={category.id} value={category.id}>
          {getCategoryDisplayName(category)}
        </option>
      ))}
    </select>
  );
}
