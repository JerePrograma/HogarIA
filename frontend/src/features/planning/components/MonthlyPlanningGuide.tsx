import { StatusBadge } from '../../../components/ui/StatusBadge';
import type { MonthlyPlanSummary } from '../../../domain/types';

type Props = {
  summary?: MonthlyPlanSummary;
};

export function MonthlyPlanningGuide({ summary }: Props) {
  const items = summary?.items ?? [];
  const hasItems = items.length > 0;

  const hasMissingAccountOrCategory = items.some(
    (item) =>
      !item.transactionId &&
      item.status !== 'CANCELLED' &&
      (item.accountId == null || item.categoryId == null),
  );

  const hasConvertibleOrPending =
    items.some(
      (item) =>
        item.status !== 'CANCELLED' &&
        !item.transactionId &&
        item.amount != null &&
        item.accountId &&
        item.categoryId,
    ) ||
    (summary?.pendingExpense ?? 0) > 0 ||
    (summary?.pendingIncome ?? 0) > 0;

  const hasUnpriced = (summary?.unpricedCount ?? 0) > 0;

  let activeStep = 1;
  let doneLabel: string | null = null;

  if (!hasItems) activeStep = 1;
  else if (hasUnpriced) activeStep = 2;
  else if (hasMissingAccountOrCategory) activeStep = 3;
  else if (hasConvertibleOrPending) activeStep = 4;
  else doneLabel = 'Mes ordenado';

  const steps = [
    {
      title: 'Cargar',
      text: 'Anotá ingresos, gastos o pendientes con una frase simple.',
    },
    {
      title: 'Revisar',
      text: 'Confirmá fecha, monto, estado y prioridad.',
    },
    {
      title: 'Completar',
      text: 'Agregá cuenta/categoría o usá sugerencias.',
    },
    {
      title: 'Confirmar',
      text: 'Marcá pagado/cobrado o convertí a movimiento real.',
    },
  ];

  return (
    <section className="panel">
      <div className="section-title">
        <div>
          <p className="eyebrow">Flujo sugerido</p>
          <h2>Paso a paso del mes</h2>
        </div>

        {doneLabel ? <StatusBadge tone="ok" label={doneLabel} /> : null}
      </div>

      <div className="guide-steps">
        {steps.map((step, index) => {
          const stepNumber = index + 1;
          const isActive = !doneLabel && activeStep === stepNumber;

          return (
            <article
              key={step.title}
              className={`guide-step ${isActive ? 'guide-step-active' : ''}`.trim()}
            >
              <span className="badge-count">{stepNumber}</span>
              <h3 className="mb-1 mt-3 text-lg font-semibold">{step.title}</h3>
              <p className="secondary-text">{step.text}</p>
            </article>
          );
        })}
      </div>
    </section>
  );
}