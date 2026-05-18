import type { Account, Category, MonthlyPlanItem } from '../../../domain/types';

type Props = {
  items: MonthlyPlanItem[];
  accounts: Account[];
  categories: Category[];
};

export function PlanningMappingsAuditPanel({ items, accounts, categories }: Props) {
  const accountIds = new Set(accounts.map((account) => account.id));
  const categoryIds = new Set(categories.map((category) => category.id));

  const missingBoth = items.filter((item) => !item.accountId && !item.categoryId);
  const missingAccount = items.filter((item) => !item.accountId && item.categoryId);
  const missingCategory = items.filter((item) => item.accountId && !item.categoryId);
  const brokenRefs = items.filter(
    (item) =>
      (item.accountId && !accountIds.has(item.accountId)) ||
      (item.categoryId && !categoryIds.has(item.categoryId)),
  );

  return (
    <section className="panel planning-items-panel" id="planning-mappings-audit">
      <h2 className="planning-items-title">Auditoría de mapeos</h2>
      <div className="planning-items-kpis">
        <span>Sin cuenta y categoría: {missingBoth.length}</span>
        <span>Sin cuenta: {missingAccount.length}</span>
        <span>Sin categoría: {missingCategory.length}</span>
        <span>Referencias inválidas: {brokenRefs.length}</span>
      </div>

      {missingBoth.length === 0 && missingAccount.length === 0 && missingCategory.length === 0 && brokenRefs.length === 0 ? (
        <p className="secondary-text">Excelente: no detectamos problemas de mapeo en este período.</p>
      ) : (
        <ul>
          {[...missingBoth, ...missingAccount, ...missingCategory, ...brokenRefs].slice(0, 12).map((item) => (
            <li key={item.id}>
              <strong>{item.title}</strong> · {item.type} · {item.expectedDate ?? 'sin fecha'}
            </li>
          ))}
        </ul>
      )}
    </section>
  );
}
