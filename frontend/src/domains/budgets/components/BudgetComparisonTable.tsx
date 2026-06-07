import { StatusBadge } from "../../../shared/ui/StatusBadge";
import {
  budgetComparisonStatusLabels,
  categoryTypeLabels,
  labelOrValue,
} from "../../../domain/financeLabels";
import { formatMoney, formatPercent } from "../../../domain/formatters";
import { sortBudgetComparisonsByRisk } from "../../../domain/sorting";
import type { BudgetComparisonItem, CategoryType } from "../../../domain/types";
import { getBudgetComparisonStatusTone } from "../budgetRules";

type Props = {
  items: BudgetComparisonItem[];
};

export function BudgetComparisonTable({ items }: Props) {
  const sortedItems = sortBudgetComparisonsByRisk(items);

  return (
    <div className="budget-comparison-responsive">
      <div className="tabla-ui budget-comparison-table">
        <table>
          <thead>
            <tr>
              <th>Categoría</th>
              <th>Tipo</th>
              <th className="amount-cell">Presupuesto</th>
              <th className="amount-cell">Real</th>
              <th className="amount-cell">Diferencia</th>
              <th>% usado</th>
              <th>Estado</th>
            </tr>
          </thead>

          <tbody>
            {sortedItems.map((item) => (
              <tr key={item.categoryId}>
                <td>
                  <strong>{item.categoryName}</strong>
                </td>

                <td>
                  {labelOrValue(
                    categoryTypeLabels,
                    item.categoryType as CategoryType,
                  )}
                </td>

                <td className="amount-cell">{formatMoney(item.budgetAmount)}</td>

                <td className="amount-cell">{formatMoney(item.realAmount)}</td>

                <td className="amount-cell">{formatMoney(item.difference)}</td>

                <td>{formatPercent(item.percentage)}</td>

                <td>
                  <StatusBadge
                    tone={getBudgetComparisonStatusTone(item.status)}
                    label={labelOrValue(
                      budgetComparisonStatusLabels,
                      item.status,
                    )}
                  />
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>

      <div className="budget-comparison-mobile-list">
        {sortedItems.map((item) => (
          <article key={item.categoryId} className="budget-mobile-card">
            <header>
              <div>
                <strong>{item.categoryName}</strong>
                <span>
                  {labelOrValue(categoryTypeLabels, item.categoryType as CategoryType)}
                </span>
              </div>

              <StatusBadge
                tone={getBudgetComparisonStatusTone(item.status)}
                label={labelOrValue(budgetComparisonStatusLabels, item.status)}
              />
            </header>

            <dl className="budget-mobile-kpis">
              <div>
                <dt>Presupuesto</dt>
                <dd>{formatMoney(item.budgetAmount)}</dd>
              </div>
              <div>
                <dt>Real</dt>
                <dd>{formatMoney(item.realAmount)}</dd>
              </div>
              <div>
                <dt>Desvío</dt>
                <dd>{formatMoney(item.difference)}</dd>
              </div>
              <div>
                <dt>Uso</dt>
                <dd>{formatPercent(item.percentage)}</dd>
              </div>
            </dl>
          </article>
        ))}
      </div>
    </div>
  );
}
