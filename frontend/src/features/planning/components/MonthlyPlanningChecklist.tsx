import { memo, useMemo } from 'react';
import type {
  RealConfirmedSummary,
  RealVsPlannedSummary,
} from '../../../domain/financialSemantics';
import { formatMoney } from '../../../domain/formatters';
import type { MonthlyPlanItem, MonthlyPlanSummary } from '../../../domain/types';
import { canConvertPlanItem } from '../planningUtils';

export type FilterKey =
  | 'UNPRICED'
  | 'MISSING_CLASSIFICATION'
  | 'READY_TO_CONVERT'
  | 'DUE_NEXT_7_DAYS'
  | 'ALL';

type Props = {
  summary?: MonthlyPlanSummary;
  items: MonthlyPlanItem[];
  realSummary?: RealConfirmedSummary;
  realVsPlanned?: RealVsPlannedSummary;
  onApply: (key: FilterKey) => void;
};

type ChecklistTone = 'info' | 'success' | 'warning' | 'danger' | 'neutral';
type ChecklistPriority = 'ALTA' | 'MEDIA' | 'BAJA' | 'INFO';

type MetricModel = {
  label: string;
  value: string | number;
  helper: string;
  tone: ChecklistTone;
};

type ActionModel = {
  key: FilterKey;
  title: string;
  description: string;
  action: string;
  value: string | number;
  numericValue: number;
  tone: ChecklistTone;
  priority: ChecklistPriority;
  disabled?: boolean;
};

type ChecklistModel = {
  totalItems: number;
  activeItems: number;
  convertedItems: number;
  blockerCount: number;
  actionableCount: number;
  pendingIncome: number;
  pendingExpense: number;
  projectedPendingNet: number;
  healthLabel: string;
  healthDescription: string;
  healthTone: ChecklistTone;
  metrics: MetricModel[];
  actions: ActionModel[];
  primaryAction: ActionModel | null;
};

export function MonthlyPlanningChecklist({
  summary,
  items,
  realSummary,
  realVsPlanned,
  onApply,
}: Props) {
  const model = useMemo(() => buildChecklistModel(summary, items), [summary, items]);

  return (
    <section className="panel mpc-panel">
      <header className="section-title mpc-header">
        <div>
          <p className="eyebrow">Control operativo</p>
          <h2>Qué falta resolver</h2>
          <p className="muted">
            Priorizá bloqueos, conversiones y desvíos contra movimientos confirmados.
          </p>
        </div>

        <span className={`badge-ui ${getBadgeClass(model.healthTone)}`}>
          {model.healthLabel}
        </span>
      </header>

      <div className={`mpc-health-strip ${getToneClass(model.healthTone)}`}>
        <div className="mpc-health-content">
          <span className="label-ui">Estado del mes</span>
          <strong>{model.healthLabel}</strong>
          <p>{model.healthDescription}</p>
        </div>

        {model.primaryAction ? (
          <button
            type="button"
            className="boton-principal mpc-health-action"
            onClick={() => onApply(model.primaryAction!.key)}
            disabled={model.primaryAction.disabled}
          >
            {model.primaryAction.action}
          </button>
        ) : (
          <button
            type="button"
            className="boton-secundario mpc-health-action"
            onClick={() => onApply('ALL')}
          >
            Ver planificación
          </button>
        )}
      </div>

      <div className="mpc-metrics-row" aria-label="Resumen operativo mensual">
        {model.metrics.map((metric) => (
          <ChecklistMetric key={metric.label} metric={metric} />
        ))}
      </div>

      <div className="mpc-cash-row">
        <CashItem
          label="Real confirmado"
          value={formatMoney(realSummary?.operationalBalance ?? 0)}
          tone={(realSummary?.operationalBalance ?? 0) < 0 ? 'danger' : 'success'}
        />

        <CashItem
          label="Pendiente de cobro"
          value={formatMoney(model.pendingIncome)}
          tone="info"
        />

        <CashItem
          label="Pendiente de pago"
          value={formatMoney(model.pendingExpense)}
          tone={model.pendingExpense > 0 ? 'warning' : 'neutral'}
        />

        <CashItem
          label="Neto pendiente"
          value={formatMoney(model.projectedPendingNet)}
          tone={model.projectedPendingNet < 0 ? 'danger' : 'success'}
        />

        <CashItem
          label="Real no planificado"
          value={formatMoney(realVsPlanned?.realUnplannedAmount ?? 0)}
          tone={(realVsPlanned?.realUnplannedAmount ?? 0) > 0 ? 'warning' : 'neutral'}
        />
      </div>

      <div className="mpc-action-list">
        <div className="mpc-list-header">
          <div>
            <span className="label-ui">Cola de trabajo</span>
            <strong>Acciones recomendadas</strong>
          </div>

          <span className="badge-ui badge-muted">
            {model.actionableCount} pendiente{model.actionableCount === 1 ? '' : 's'}
          </span>
        </div>

        {model.actions.length > 0 ? (
          model.actions.map((action) => (
            <ChecklistActionRow
              key={action.title}
              action={action}
              onClick={() => onApply(action.key)}
            />
          ))
        ) : (
          <div className="mpc-empty-state">
            <strong>No hay acciones críticas pendientes.</strong>
            <p>
              La planificación no tiene bloqueos visibles. Podés revisar todos los ítems o seguir
              cargando movimientos estimados.
            </p>
            <button type="button" className="boton-secundario" onClick={() => onApply('ALL')}>
              Ver todos los ítems
            </button>
          </div>
        )}
      </div>
    </section>
  );
}

const ChecklistMetric = memo(function ChecklistMetric({ metric }: { metric: MetricModel }) {
  return (
    <article className={`mpc-metric ${getToneClass(metric.tone)}`}>
      <span>{metric.label}</span>
      <strong>{metric.value}</strong>
      <p>{metric.helper}</p>
    </article>
  );
});

const CashItem = memo(function CashItem({
  label,
  value,
  tone,
}: {
  label: string;
  value: string;
  tone: ChecklistTone;
}) {
  return (
    <article className={`mpc-cash-item ${getToneClass(tone)}`}>
      <span>{label}</span>
      <strong>{value}</strong>
    </article>
  );
});

const ChecklistActionRow = memo(function ChecklistActionRow({
  action,
  onClick,
}: {
  action: ActionModel;
  onClick: () => void;
}) {
  return (
    <article className={`mpc-action-row ${getToneClass(action.tone)}`}>
      <div className="mpc-action-priority">
        <span className={`badge-ui ${getPriorityBadgeClass(action.priority)}`}>
          {action.priority}
        </span>
      </div>

      <div className="mpc-action-body">
        <div>
          <strong>{action.title}</strong>
          <p>{action.description}</p>
        </div>

        <span className="mpc-action-value">{action.value}</span>
      </div>

      <button
        type="button"
        className="boton-secundario mpc-action-button"
        onClick={onClick}
        disabled={action.disabled}
      >
        {action.action}
      </button>
    </article>
  );
});

function buildChecklistModel(
  summary: MonthlyPlanSummary | undefined,
  items: MonthlyPlanItem[],
): ChecklistModel {
  const activeItems = items.filter(isActiveItem);
  const pendingItems = activeItems.filter((item) => !item.transactionId);

  const computedUnpricedCount = pendingItems.filter(isUnpricedItem).length;
  const computedDueNext7DaysCount = pendingItems.filter(isDueNext7Days).length;
  const missingClassification = pendingItems.filter(isMissingClassification).length;
  const readyToConvert = items.filter(canConvertPlanItem).length;

  const totalItems = items.length;
  const convertedItems = items.filter((item) => Boolean(item.transactionId)).length;

  const unpricedCount = summary?.unpricedCount ?? computedUnpricedCount;
  const dueNext7DaysCount = summary?.dueNext7DaysCount ?? computedDueNext7DaysCount;

  const pendingIncome = summary?.pendingIncome ?? sumPendingByKind(pendingItems, 'INCOME');
  const pendingExpense = summary?.pendingExpense ?? sumPendingByKind(pendingItems, 'EXPENSE');
  const projectedPendingNet = pendingIncome - pendingExpense;

  const blockerCount = unpricedCount + missingClassification;
  const actionableCount = blockerCount + readyToConvert + dueNext7DaysCount;

  const actions = buildActionQueue({
    unpricedCount,
    missingClassification,
    readyToConvert,
    dueNext7DaysCount,
  });

  const primaryAction = findPrimaryAction(actions);

  const { healthLabel, healthDescription, healthTone } = resolveChecklistHealth({
    blockerCount,
    actionableCount,
    projectedPendingNet,
    readyToConvert,
    dueNext7DaysCount,
  });

  const metrics: MetricModel[] = [
    {
      label: 'Planificados',
      value: totalItems,
      helper: `${activeItems.length} activo${activeItems.length === 1 ? '' : 's'}`,
      tone: 'neutral',
    },
    {
      label: 'Convertidos',
      value: convertedItems,
      helper: 'Ya impactaron como movimiento real',
      tone: convertedItems > 0 ? 'success' : 'neutral',
    },
    {
      label: 'Bloqueantes',
      value: blockerCount,
      helper: blockerCount > 0 ? 'Impiden convertir con seguridad' : 'Sin bloqueos críticos',
      tone: blockerCount > 0 ? 'warning' : 'success',
    },
    {
      label: 'Listos',
      value: readyToConvert,
      helper: 'Pueden pasar a movimiento real',
      tone: readyToConvert > 0 ? 'info' : 'neutral',
    },
  ];

  return {
    totalItems,
    activeItems: activeItems.length,
    convertedItems,
    blockerCount,
    actionableCount,
    pendingIncome,
    pendingExpense,
    projectedPendingNet,
    healthLabel,
    healthDescription,
    healthTone,
    metrics,
    actions,
    primaryAction,
  };
}

function buildActionQueue({
  unpricedCount,
  missingClassification,
  readyToConvert,
  dueNext7DaysCount,
}: {
  unpricedCount: number;
  missingClassification: number;
  readyToConvert: number;
  dueNext7DaysCount: number;
}): ActionModel[] {
  const actions: ActionModel[] = [];

  if (unpricedCount > 0) {
    actions.push({
      key: 'UNPRICED',
      title: 'Completar montos sin cotizar',
      description: 'Hay ítems sin monto ni rango. La proyección queda poco confiable.',
      action: 'Ver ítems',
      value: unpricedCount,
      numericValue: unpricedCount,
      tone: 'warning',
      priority: 'ALTA',
    });
  }

  if (missingClassification > 0) {
    actions.push({
      key: 'MISSING_CLASSIFICATION',
      title: 'Completar cuenta y categoría',
      description: 'No se pueden convertir movimientos sin clasificación completa.',
      action: 'Preparar conversión',
      value: missingClassification,
      numericValue: missingClassification,
      tone: 'warning',
      priority: 'ALTA',
    });
  }

  if (dueNext7DaysCount > 0) {
    actions.push({
      key: 'DUE_NEXT_7_DAYS',
      title: 'Revisar próximos vencimientos',
      description: 'Hay ítems cercanos. Conviene confirmar fecha, estado y monto.',
      action: 'Ver próximos',
      value: dueNext7DaysCount,
      numericValue: dueNext7DaysCount,
      tone: 'info',
      priority: 'MEDIA',
    });
  }

  if (readyToConvert > 0) {
    actions.push({
      key: 'READY_TO_CONVERT',
      title: 'Convertir movimientos listos',
      description: 'Ya tienen monto, cuenta y categoría. Falta confirmar impacto real.',
      action: 'Convertir',
      value: readyToConvert,
      numericValue: readyToConvert,
      tone: 'success',
      priority: 'MEDIA',
    });
  }

  return actions;
}

function findPrimaryAction(actions: ActionModel[]): ActionModel | null {
  return (
    actions.find((action) => action.priority === 'ALTA' && action.numericValue > 0)
    ?? actions.find((action) => action.priority === 'MEDIA' && action.numericValue > 0)
    ?? actions.find((action) => action.numericValue > 0)
    ?? null
  );
}

function resolveChecklistHealth({
  blockerCount,
  actionableCount,
  projectedPendingNet,
  readyToConvert,
  dueNext7DaysCount,
}: {
  blockerCount: number;
  actionableCount: number;
  projectedPendingNet: number;
  readyToConvert: number;
  dueNext7DaysCount: number;
}): {
  healthLabel: string;
  healthDescription: string;
  healthTone: ChecklistTone;
} {
  if (blockerCount > 0) {
    return {
      healthLabel: `${blockerCount} bloqueo${blockerCount === 1 ? '' : 's'}`,
      healthDescription: 'Primero resolvé montos, cuentas o categorías faltantes.',
      healthTone: 'warning',
    };
  }

  if (projectedPendingNet < 0) {
    return {
      healthLabel: 'Neto pendiente negativo',
      healthDescription: 'El saldo pendiente proyectado queda por debajo de cero.',
      healthTone: 'danger',
    };
  }

  if (readyToConvert > 0) {
    return {
      healthLabel: `${readyToConvert} listo${readyToConvert === 1 ? '' : 's'} para convertir`,
      healthDescription: 'Ya podés confirmar movimientos reales sin completar datos extra.',
      healthTone: 'success',
    };
  }

  if (dueNext7DaysCount > 0) {
    return {
      healthLabel: `${dueNext7DaysCount} vencimiento${dueNext7DaysCount === 1 ? '' : 's'} cerca`,
      healthDescription: 'Conviene revisar los próximos compromisos del período.',
      healthTone: 'info',
    };
  }

  if (actionableCount > 0) {
    return {
      healthLabel: `${actionableCount} acción${actionableCount === 1 ? '' : 'es'} pendiente${actionableCount === 1 ? '' : 's'}`,
      healthDescription: 'Hay ítems para revisar antes de cerrar el mes.',
      healthTone: 'info',
    };
  }

  return {
    healthLabel: 'Checklist limpio',
    healthDescription: 'No hay bloqueos ni acciones críticas detectadas.',
    healthTone: 'success',
  };
}

function isActiveItem(item: MonthlyPlanItem): boolean {
  return item.status !== 'CANCELLED';
}

function isUnpricedItem(item: MonthlyPlanItem): boolean {
  return (
    isActiveItem(item)
    && !item.transactionId
    && item.amount == null
    && item.minAmount == null
    && item.maxAmount == null
  );
}

function isMissingClassification(item: MonthlyPlanItem): boolean {
  return (
    isActiveItem(item)
    && !item.transactionId
    && (!item.accountId || !item.categoryId)
  );
}

function isDueNext7Days(item: MonthlyPlanItem): boolean {
  if (!isActiveItem(item) || item.transactionId || !item.expectedDate) {
    return false;
  }

  const expectedDate = parseLocalDate(item.expectedDate);

  if (!expectedDate) {
    return false;
  }

  const today = startOfDay(new Date());
  const limit = addDays(today, 7);

  return expectedDate >= today && expectedDate <= limit;
}

function sumPendingByKind(
  items: MonthlyPlanItem[],
  kind: 'INCOME' | 'EXPENSE',
): number {
  return items
    .filter((item) => {
      if (item.type === 'TODO' || item.status === 'CANCELLED' || item.transactionId) {
        return false;
      }

      const incomeLike = item.type === 'INCOME' || item.type === 'RECOVERY';

      return kind === 'INCOME' ? incomeLike : !incomeLike;
    })
    .reduce((total, item) => total + getExpectedAmount(item), 0);
}

function getExpectedAmount(item: MonthlyPlanItem): number {
  const numericValue =
    item.amount
    ?? item.maxAmount
    ?? item.minAmount
    ?? item.grossMax
    ?? item.grossMin
    ?? item.netMax
    ?? item.netMin
    ?? 0;

  const parsed = Number(numericValue);

  if (!Number.isFinite(parsed)) {
    return 0;
  }

  return Math.abs(parsed);
}

function parseLocalDate(value: string): Date | null {
  const [year, month, day] = value.split('-').map(Number);

  if (!year || !month || !day) {
    return null;
  }

  return startOfDay(new Date(year, month - 1, day));
}

function startOfDay(date: Date): Date {
  return new Date(date.getFullYear(), date.getMonth(), date.getDate());
}

function addDays(date: Date, days: number): Date {
  const result = new Date(date);
  result.setDate(result.getDate() + days);
  return result;
}

function getBadgeClass(tone: ChecklistTone): string {
  if (tone === 'success') return 'badge-success';
  if (tone === 'warning') return 'badge-warning';
  if (tone === 'danger') return 'badge-danger';
  if (tone === 'info') return 'badge-info';
  return 'badge-muted';
}

function getToneClass(tone: ChecklistTone): string {
  if (tone === 'success') return 'mpc-tone-success';
  if (tone === 'warning') return 'mpc-tone-warning';
  if (tone === 'danger') return 'mpc-tone-danger';
  if (tone === 'info') return 'mpc-tone-info';
  return 'mpc-tone-neutral';
}

function getPriorityBadgeClass(priority: ChecklistPriority): string {
  if (priority === 'ALTA') return 'badge-warning';
  if (priority === 'MEDIA') return 'badge-info';
  if (priority === 'BAJA') return 'badge-success';
  return 'badge-muted';
}
