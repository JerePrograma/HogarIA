import { StatusBadge } from '../../../shared/ui/StatusBadge';
import type { MonthlyPlanSummary } from '../../../domain/types';
import {
  canConvertPlanItem,
  isDueNextDays,
  isMissingClassificationPlanItem,
  isUnpricedPlanItem,
} from '../planningUtils';

type Props = {
  summary?: MonthlyPlanSummary;
};

type GuideStep = {
  title: string;
  text: string;
};

const steps: GuideStep[] = [
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

export function MonthlyPlanningGuide({ summary }: Props) {
  const model = buildGuideModel(summary);

  return (
    <section className="panel">
      <div className="section-title">
        <div>
          <p className="eyebrow">Flujo sugerido</p>
          <h2>Paso a paso del mes</h2>
          <p className="muted">
            Usá esta guía como control rápido para cerrar el período sin huecos operativos.
          </p>
        </div>

        {model.doneLabel ? <StatusBadge tone="ok" label={model.doneLabel} /> : null}
      </div>

      <div className="guide-steps">
        {steps.map((step, index) => {
          const stepNumber = index + 1;
          const isActive = !model.doneLabel && model.activeStep === stepNumber;

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

function buildGuideModel(summary?: MonthlyPlanSummary): {
  activeStep: number;
  doneLabel: string | null;
} {
  const items = summary?.items ?? [];

  if (items.length === 0) {
    return { activeStep: 1, doneLabel: null };
  }

  if ((summary?.unpricedCount ?? 0) > 0 || items.some(isUnpricedPlanItem)) {
    return { activeStep: 2, doneLabel: null };
  }

  if (items.some(isMissingClassificationPlanItem)) {
    return { activeStep: 3, doneLabel: null };
  }

  const hasConvertible = items.some(canConvertPlanItem);
  const hasDueSoon = items.some((item) => isDueNextDays(item.expectedDate, 7));

  if (
    hasConvertible ||
    hasDueSoon ||
    (summary?.pendingExpense ?? 0) > 0 ||
    (summary?.pendingIncome ?? 0) > 0
  ) {
    return { activeStep: 4, doneLabel: null };
  }

  return { activeStep: 4, doneLabel: 'Mes ordenado' };
}