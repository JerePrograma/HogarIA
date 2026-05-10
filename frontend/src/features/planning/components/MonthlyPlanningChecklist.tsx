import type { MonthlyPlanItem, MonthlyPlanSummary } from '../../../domain/types';

type FilterKey = 'UNPRICED' | 'MISSING_CLASSIFICATION' | 'READY_TO_CONVERT' | 'DUE_NEXT_7_DAYS' | 'ALL';

type Props = { summary?: MonthlyPlanSummary; items: MonthlyPlanItem[]; onApply: (key: FilterKey) => void };

export function MonthlyPlanningChecklist({ summary, items, onApply }: Props) {
  const missingClassification = items.filter((item) => !item.transactionId && item.status !== 'CANCELLED' && (item.accountId == null || item.categoryId == null)).length;
  const readyToConvert = items.filter((item) => !item.transactionId && item.status !== 'CANCELLED' && item.amount != null && item.accountId && item.categoryId).length;

  const cards = [
    { title: 'Sin cotizar', value: summary?.unpricedCount ?? 0, action: 'Ver items', onClick: () => onApply('UNPRICED') },
    { title: 'Sin cuenta/categoría', value: missingClassification, action: 'Usá preparar conversión', onClick: () => onApply('MISSING_CLASSIFICATION') },
    { title: 'Listos para convertir', value: readyToConvert, action: 'Filtrar', onClick: () => onApply('READY_TO_CONVERT') },
    { title: 'Próximos 7 días', value: summary?.dueNext7DaysCount ?? 0, action: 'Ordenar por fecha', onClick: () => onApply('DUE_NEXT_7_DAYS') },
    { title: 'Pendiente de cobro', value: summary?.pendingIncome ?? 0, action: 'Revisar', onClick: () => onApply('ALL') },
    { title: 'Pendiente de pago', value: summary?.pendingExpense ?? 0, action: 'Revisar', onClick: () => onApply('ALL') }
  ];

  return <section className='card'><h3 className='section-title'>Checklist operativo del mes</h3><div className='checklist-grid'>{cards.map((card) => <div key={card.title} className='checklist-item'><strong>{card.title}</strong><div className='kpi-value'>{card.value}</div><button className='button-secondary' onClick={card.onClick}>{card.action}</button></div>)}</div></section>;
}

export type { FilterKey };
