import type { MonthlyPlanSummary } from '../../../domain/types';

type Props = { summary?: MonthlyPlanSummary };

export function MonthlyPlanningGuide({ summary }: Props) {
  const items = summary?.items ?? [];
  const hasItems = items.length > 0;
  const hasMissingAccountOrCategory = items.some((item) => !item.transactionId && item.status !== 'CANCELLED' && (item.accountId == null || item.categoryId == null));
  const hasConvertibleOrPending = items.some((item) => item.status !== 'CANCELLED' && !item.transactionId && item.amount != null && item.accountId && item.categoryId) || (summary?.pendingExpense ?? 0) > 0 || (summary?.pendingIncome ?? 0) > 0;
  const hasUnpriced = (summary?.unpricedCount ?? 0) > 0;

  let activeStep = 1;
  let doneLabel: string | null = null;
  if (!hasItems) activeStep = 1;
  else if (hasUnpriced) activeStep = 2;
  else if (hasMissingAccountOrCategory) activeStep = 3;
  else if (hasConvertibleOrPending) activeStep = 4;
  else doneLabel = 'Mes ordenado';

  const steps = [
    { title: 'Cargar', text: 'Anotá ingresos, gastos o pendientes con una frase simple.' },
    { title: 'Revisar', text: 'Confirmá fecha, monto, estado y prioridad.' },
    { title: 'Completar', text: 'Agregá cuenta/categoría o usá sugerencias.' },
    { title: 'Confirmar', text: 'Marcá pagado/cobrado o convertí a movimiento real.' }
  ];

  return <section className='card'>
    <h3 className='section-title'>Paso a paso del mes</h3>
    <div className='guide-steps'>
      {steps.map((step, idx) => {
        const stepNum = idx + 1;
        const className = `guide-step ${!doneLabel && activeStep === stepNum ? 'guide-step-active' : ''}`;
        return <div key={step.title} className={className}><strong>{stepNum}. {step.title}</strong><p className='secondary-text'>{step.text}</p></div>;
      })}
    </div>
    {doneLabel ? <p className='success-box'>{doneLabel}</p> : null}
  </section>;
}
