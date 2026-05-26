import { Link } from 'react-router-dom';
import type { Account, Category, MonthlyPlanItem } from '../../../domain/types';
import { planningRoutes } from '../planningRoutes';

type Props = {
  profileId?: string;
  items: MonthlyPlanItem[];
  accounts: Account[];
  categories: Category[];
  periodParams?: URLSearchParams;
};

function toLink(base: string, params: URLSearchParams): string {
  const q = params.toString();
  return q ? `${base}?${q}` : base;
}

export function PlanningMappingsAuditPanel({ profileId = '', items, accounts, categories, periodParams = new URLSearchParams() }: Props) {
  const accountIds = new Set(accounts.map((account) => account.id));
  const categoryIds = new Set(categories.map((category) => category.id));

  const missingBoth = items.filter((item) => !item.accountId && !item.categoryId);
  const missingAccount = items.filter((item) => !item.accountId && item.categoryId);
  const missingCategory = items.filter((item) => item.accountId && !item.categoryId);
  const brokenRefs = items.filter((item) => (item.accountId && !accountIds.has(item.accountId)) || (item.categoryId && !categoryIds.has(item.categoryId)));

  const renderGroup = (title: string, group: MonthlyPlanItem[]) => (
    <article><h4>{title} ({group.length})</h4><ul>{group.slice(0, 12).map((item) => { const params = new URLSearchParams(periodParams); params.set('mode', 'classification'); return <li key={item.id}><strong>{item.title}</strong> <Link to={toLink(planningRoutes.itemEdit(profileId, item.id), params)}>Corregir</Link></li>; })}</ul>{group.length > 12 ? <p>Mostrando 12 de {group.length}. <Link to={toLink(planningRoutes.items(profileId), periodParams)}>Ver todos en ítems</Link></p> : null}</article>
  );

  return <section className="panel planning-items-panel" id="planning-mappings-audit"><h2 className="planning-items-title">Auditoría de mapeos</h2>{renderGroup('Sin cuenta y sin categoría', missingBoth)}{renderGroup('Sin cuenta', missingAccount)}{renderGroup('Sin categoría', missingCategory)}{renderGroup('Referencias inválidas', brokenRefs)}</section>;
}
