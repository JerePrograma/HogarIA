import { MetricCard } from '../../../components/ui/MetricCard';
import { formatMoney } from '../../../domain/formatters';
import type { MonthlyPlanItem, MonthlyPlanSummary } from '../../../domain/types';
import { canConvertPlanItem } from '../planningUtils';

type FilterKey =
  | 'UNPRICED'
  | 'MISSING_CLASSIFICATION'
  | 'READY_TO_CONVERT'
  | 'DUE_NEXT_7_DAYS'
  | 'ALL';

type Props = {
  summary?: MonthlyPlanSummary;
  items: MonthlyPlanItem[];
  onApply: (key: FilterKey) => void;
};

export function MonthlyPlanningChecklist({ summary, items, onApply }: Props) {
  const missingClassification = items.filter(
    (item) =>
      !item.transactionId &&
      item.status !== 'CANCELLED' &&
      (item.accountId == null || item.categoryId == null),
  ).length;

  const readyToConvert = items.filter(canConvertPlanItem).length;

  const cards = [
    {
      title: 'Sin cotizar',
      value: summary?.unpricedCount ?? 0,
      helper: 'Necesitan monto o rango para mejorar la proyección.',
      action: 'Ver ítems',
      tone: (summary?.unpricedCount ?? 0) > 0 ? 'warning' : 'success',
      onClick: () => onApply('UNPRICED'),
    },
    {
      title: 'Sin cuenta/categoría',
      value: missingClassification,
      helper: 'No se podrán convertir hasta completar clasificación.',
      action: 'Preparar conversión',
      tone: missingClassification > 0 ? 'warning' : 'success',
      onClick: () => onApply('MISSING_CLASSIFICATION'),
    },
    {
      title: 'Listos para convertir',
      value: readyToConvert,
      helper: 'Tienen monto, cuenta y categoría.',
      action: 'Filtrar',
      tone: readyToConvert > 0 ? 'info' : 'neutral',
      onClick: () => onApply('READY_TO_CONVERT'),
    },
    {
      title: 'Próximos 7 días',
      value: summary?.dueNext7DaysCount ?? 0,
      helper: 'Requieren atención por cercanía.',
      action: 'Ordenar por fecha',
      tone: (summary?.dueNext7DaysCount ?? 0) > 0 ? 'info' : 'neutral',
      onClick: () => onApply('DUE_NEXT_7_DAYS'),
    },
    {
      title: 'Pendiente de cobro',
      value: formatMoney(summary?.pendingIncome ?? 0),
      helper: 'Ingresos esperados no confirmados.',
      action: 'Revisar',
      tone: 'info',
      onClick: () => onApply('ALL'),
    },
    {
      title: 'Pendiente de pago',
      value: formatMoney(summary?.pendingExpense ?? 0),
      helper: 'Pagos esperados no confirmados.',
      action: 'Revisar',
      tone: (summary?.pendingExpense ?? 0) > 0 ? 'warning' : 'neutral',
      onClick: () => onApply('ALL'),
    },
  ] as const;

  return (
    <section className="panel">
      <div className="section-title">
        <div>
          <p className="eyebrow">Control operativo</p>
          <h2>Checklist del mes</h2>
        </div>
      </div>

      <div className="checklist-grid">
        {cards.map((card) => (
          <article key={card.title} className="checklist-item">
            <MetricCard
              title={card.title}
              value={card.value}
              helper={card.helper}
              tone={card.tone}
            />

            <button type="button" className="boton-secundario" onClick={card.onClick}>
              {card.action}
            </button>
          </article>
        ))}
      </div>
    </section>
  );
}

export type { FilterKey };