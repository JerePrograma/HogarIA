import { memo, useMemo } from "react";
import { formatMoney } from "../../../domain/formatters";
import type {
  MonthlyPlanItem,
  MonthlyPlanReconciliationSummary,
  MonthlyPlanSummary,
} from "../../../domain/types";
import { canConvertPlanItem } from "../planningUtils";

export type FilterKey =
  | "UNPRICED"
  | "MISSING_CLASSIFICATION"
  | "READY_TO_CONVERT"
  | "DUE_NEXT_7_DAYS"
  | "PARTIALLY_EXECUTED"
  | "OVER_EXECUTED"
  | "NOT_EXECUTED"
  | "UNPLANNED_MOVEMENTS"
  | "SUGGESTED_MATCHES"
  | "ALL";

type Props = {
  summary?: MonthlyPlanSummary;
  reconciliation?: MonthlyPlanReconciliationSummary;
  items: MonthlyPlanItem[];
  onApply: (key: FilterKey) => void;
};

type ChecklistTone = "info" | "success" | "warning" | "danger" | "neutral";
type ChecklistPriority = "ALTA" | "MEDIA" | "BAJA" | "INFO";

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
  reconciliationIssueCount: number;
  actionableCount: number;
  pendingIncome: number;
  pendingExpense: number;
  projectedPendingNet: number;
  matchedTotal: number;
  remainingTotal: number;
  unplannedTransactionsTotal: number;
  healthLabel: string;
  healthDescription: string;
  healthTone: ChecklistTone;
  metrics: MetricModel[];
  actions: ActionModel[];
  primaryAction: ActionModel | null;
};

export function MonthlyPlanningChecklist({
  summary,
  reconciliation,
  items,
  onApply,
}: Props) {
  const model = useMemo(
    () => buildChecklistModel(summary, reconciliation, items),
    [summary, reconciliation, items],
  );

  return (
    <section className="panel mpc-panel">
      <header className="section-title mpc-header">
        <div>
          <p className="eyebrow">Control operativo</p>
          <h2>Qué falta resolver</h2>
          <p className="muted">
            Priorizá bloqueos, movimientos reales sin planificación y desvíos
            antes de cerrar el período.
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
            onClick={() => onApply("ALL")}
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
          label="Pendiente de cobro"
          value={formatMoney(model.pendingIncome)}
          tone="info"
        />

        <CashItem
          label="Pendiente de pago"
          value={formatMoney(model.pendingExpense)}
          tone={model.pendingExpense > 0 ? "warning" : "neutral"}
        />

        <CashItem
          label="Neto pendiente"
          value={formatMoney(model.projectedPendingNet)}
          tone={model.projectedPendingNet < 0 ? "danger" : "success"}
        />

        <CashItem
          label="Conciliado"
          value={formatMoney(model.matchedTotal)}
          tone={model.matchedTotal > 0 ? "success" : "neutral"}
        />

        <CashItem
          label="Saldo sin cubrir"
          value={formatMoney(model.remainingTotal)}
          tone={model.remainingTotal > 0 ? "warning" : "success"}
        />

        <CashItem
          label="No planificado"
          value={formatMoney(model.unplannedTransactionsTotal)}
          tone={model.unplannedTransactionsTotal > 0 ? "danger" : "neutral"}
        />
      </div>

      <div className="mpc-action-list">
        <div className="mpc-list-header">
          <div>
            <span className="label-ui">Cola de trabajo</span>
            <strong>Acciones recomendadas</strong>
          </div>

          <span className="badge-ui badge-muted">
            {model.actionableCount} pendiente
            {model.actionableCount === 1 ? "" : "s"}
          </span>
        </div>

        {model.actions.length > 0 ? (
          model.actions.map((action) => (
            <ChecklistActionRow
              key={action.key}
              action={action}
              onClick={() => onApply(action.key)}
            />
          ))
        ) : (
          <div className="mpc-empty-state">
            <strong>No hay acciones críticas pendientes.</strong>
            <p>
              La planificación no tiene bloqueos visibles ni desvíos de
              conciliación detectados. Podés revisar todos los ítems o seguir
              cargando movimientos estimados.
            </p>
            <button
              type="button"
              className="boton-secundario"
              onClick={() => onApply("ALL")}
            >
              Ver todos los ítems
            </button>
          </div>
        )}
      </div>
    </section>
  );
}

const ChecklistMetric = memo(function ChecklistMetric({
  metric,
}: {
  metric: MetricModel;
}) {
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
  reconciliation: MonthlyPlanReconciliationSummary | undefined,
  items: MonthlyPlanItem[],
): ChecklistModel {
  const activeItems = items.filter(isActiveItem);
  const pendingItems = activeItems.filter((item) => !item.transactionId);

  const computedUnpricedCount = pendingItems.filter(isUnpricedItem).length;
  const computedDueNext7DaysCount = pendingItems.filter(isDueNext7Days).length;
  const missingClassification = pendingItems.filter(
    isMissingClassification,
  ).length;
  const readyToConvert = items.filter(canConvertPlanItem).length;

  const partialItems = reconciliation?.partialItemsCount ?? 0;
  const overExecutedItems = reconciliation?.overExecutedItemsCount ?? 0;
  const unplannedTransactions = reconciliation?.unplannedTransactionsCount ?? 0;
  const suggestedMatches = reconciliation?.suggestedMatches.length ?? 0;
  const notExecutedItems =
    reconciliation?.items.filter(
      (item) => item.executionStatus === "NOT_EXECUTED",
    ).length ?? 0;

  const totalItems = items.length;
  const convertedItems = items.filter((item) =>
    Boolean(item.transactionId),
  ).length;

  const unpricedCount = summary?.unpricedCount ?? computedUnpricedCount;
  const dueNext7DaysCount =
    summary?.dueNext7DaysCount ?? computedDueNext7DaysCount;

  const pendingIncome =
    summary?.pendingIncome ?? sumPendingByKind(pendingItems, "INCOME");
  const pendingExpense =
    summary?.pendingExpense ?? sumPendingByKind(pendingItems, "EXPENSE");
  const projectedPendingNet = pendingIncome - pendingExpense;

  const blockerCount = unpricedCount + missingClassification;
  const reconciliationIssueCount =
    partialItems + overExecutedItems + unplannedTransactions + suggestedMatches;

  const actions = buildActionQueue({
    unpricedCount,
    missingClassification,
    readyToConvert,
    dueNext7DaysCount,
    partialItems,
    overExecutedItems,
    notExecutedItems,
    unplannedTransactions,
    suggestedMatches,
  });

  const actionableCount = actions.reduce(
    (total, action) => total + action.numericValue,
    0,
  );

  const primaryAction = findPrimaryAction(actions);

  const { healthLabel, healthDescription, healthTone } = resolveChecklistHealth(
    {
      blockerCount,
      reconciliationIssueCount,
      actionableCount,
      projectedPendingNet,
      readyToConvert,
      dueNext7DaysCount,
      unplannedTransactions,
      suggestedMatches,
      partialItems,
      overExecutedItems,
    },
  );

  const matchedTotal = reconciliation?.matchedTotal ?? 0;
  const remainingTotal = reconciliation?.remainingTotal ?? 0;
  const unplannedTransactionsTotal =
    reconciliation?.unplannedTransactionsTotal ?? 0;

  const metrics: MetricModel[] = [
    {
      label: "Planificados",
      value: totalItems,
      helper: `${activeItems.length} activo${activeItems.length === 1 ? "" : "s"}`,
      tone: "neutral",
    },
    {
      label: "Conciliados",
      value: reconciliation?.matchedItemsCount ?? convertedItems,
      helper: "Tienen movimientos reales vinculados",
      tone: (reconciliation?.matchedItemsCount ?? convertedItems) > 0 ? "success" : "neutral",
    },
    {
      label: "Bloqueantes",
      value: blockerCount,
      helper:
        blockerCount > 0
          ? "Impiden convertir o conciliar con seguridad"
          : "Sin bloqueos críticos",
      tone: blockerCount > 0 ? "warning" : "success",
    },
    {
      label: "No planificados",
      value: unplannedTransactions,
      helper:
        unplannedTransactions > 0
          ? "Movimientos reales sin vínculo"
          : "Sin movimientos sueltos",
      tone: unplannedTransactions > 0 ? "danger" : "success",
    },
    {
      label: "Sugerencias",
      value: suggestedMatches,
      helper:
        suggestedMatches > 0
          ? "Matches candidatos para revisar"
          : "Sin sugerencias pendientes",
      tone: suggestedMatches > 0 ? "info" : "neutral",
    },
    {
      label: "Excedidos",
      value: overExecutedItems,
      helper:
        overExecutedItems > 0
          ? "Movimientos superan lo planificado"
          : "Sin excesos detectados",
      tone: overExecutedItems > 0 ? "danger" : "success",
    },
  ];

  return {
    totalItems,
    activeItems: activeItems.length,
    convertedItems,
    blockerCount,
    reconciliationIssueCount,
    actionableCount,
    pendingIncome,
    pendingExpense,
    projectedPendingNet,
    matchedTotal,
    remainingTotal,
    unplannedTransactionsTotal,
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
  partialItems,
  overExecutedItems,
  notExecutedItems,
  unplannedTransactions,
  suggestedMatches,
}: {
  unpricedCount: number;
  missingClassification: number;
  readyToConvert: number;
  dueNext7DaysCount: number;
  partialItems: number;
  overExecutedItems: number;
  notExecutedItems: number;
  unplannedTransactions: number;
  suggestedMatches: number;
}): ActionModel[] {
  const actions: ActionModel[] = [];

  if (unplannedTransactions > 0) {
    actions.push({
      key: "UNPLANNED_MOVEMENTS",
      title: "Movimientos sin planificación",
      description:
        "Hay movimientos reales que todavía no están vinculados a ningún ítem planificado.",
      action: "Revisar",
      value: unplannedTransactions,
      numericValue: unplannedTransactions,
      tone: "danger",
      priority: "ALTA",
    });
  }

  if (suggestedMatches > 0) {
    actions.push({
      key: "SUGGESTED_MATCHES",
      title: "Sugerencias de conciliación",
      description:
        "El sistema detectó posibles vínculos entre planificación y movimientos reales.",
      action: "Conciliar",
      value: suggestedMatches,
      numericValue: suggestedMatches,
      tone: "info",
      priority: "ALTA",
    });
  }

  if (unpricedCount > 0) {
    actions.push({
      key: "UNPRICED",
      title: "Completar montos sin cotizar",
      description:
        "Hay ítems sin monto ni rango. La proyección queda poco confiable.",
      action: "Ver ítems",
      value: unpricedCount,
      numericValue: unpricedCount,
      tone: "warning",
      priority: "ALTA",
    });
  }

  if (missingClassification > 0) {
    actions.push({
      key: "MISSING_CLASSIFICATION",
      title: "Completar cuenta y categoría",
      description:
        "No se pueden convertir ni conciliar movimientos sin clasificación completa.",
      action: "Preparar",
      value: missingClassification,
      numericValue: missingClassification,
      tone: "warning",
      priority: "ALTA",
    });
  }

  if (overExecutedItems > 0) {
    actions.push({
      key: "OVER_EXECUTED",
      title: "Ítems excedidos",
      description: "Hay movimientos conciliados por encima de lo planificado.",
      action: "Ver excedidos",
      value: overExecutedItems,
      numericValue: overExecutedItems,
      tone: "danger",
      priority: "MEDIA",
    });
  }

  if (partialItems > 0) {
    actions.push({
      key: "PARTIALLY_EXECUTED",
      title: "Ítems parcialmente ejecutados",
      description:
        "Hay planificación cubierta sólo en parte por movimientos reales.",
      action: "Ver parciales",
      value: partialItems,
      numericValue: partialItems,
      tone: "warning",
      priority: "MEDIA",
    });
  }

  if (notExecutedItems > 0) {
    actions.push({
      key: "NOT_EXECUTED",
      title: "Ítems sin ejecutar",
      description:
        "Hay ítems planificados que todavía no tienen movimientos reales vinculados.",
      action: "Ver pendientes",
      value: notExecutedItems,
      numericValue: notExecutedItems,
      tone: "info",
      priority: "MEDIA",
    });
  }

  if (dueNext7DaysCount > 0) {
    actions.push({
      key: "DUE_NEXT_7_DAYS",
      title: "Revisar próximos vencimientos",
      description:
        "Hay ítems cercanos. Conviene confirmar fecha, estado y monto.",
      action: "Ver próximos",
      value: dueNext7DaysCount,
      numericValue: dueNext7DaysCount,
      tone: "info",
      priority: "MEDIA",
    });
  }

  if (readyToConvert > 0) {
    actions.push({
      key: "READY_TO_CONVERT",
      title: "Convertir movimientos listos",
      description:
        "Ya tienen monto, cuenta y categoría. Falta confirmar impacto real.",
      action: "Convertir",
      value: readyToConvert,
      numericValue: readyToConvert,
      tone: "success",
      priority: "MEDIA",
    });
  }

  return actions;
}

function findPrimaryAction(actions: ActionModel[]): ActionModel | null {
  return (
    actions.find(
      (action) => action.priority === "ALTA" && action.numericValue > 0,
    ) ??
    actions.find(
      (action) => action.priority === "MEDIA" && action.numericValue > 0,
    ) ??
    actions.find((action) => action.numericValue > 0) ??
    null
  );
}

function resolveChecklistHealth({
  blockerCount,
  reconciliationIssueCount,
  actionableCount,
  projectedPendingNet,
  readyToConvert,
  dueNext7DaysCount,
  unplannedTransactions,
  suggestedMatches,
  partialItems,
  overExecutedItems,
}: {
  blockerCount: number;
  reconciliationIssueCount: number;
  actionableCount: number;
  projectedPendingNet: number;
  readyToConvert: number;
  dueNext7DaysCount: number;
  unplannedTransactions: number;
  suggestedMatches: number;
  partialItems: number;
  overExecutedItems: number;
}): {
  healthLabel: string;
  healthDescription: string;
  healthTone: ChecklistTone;
} {
  if (unplannedTransactions > 0) {
    return {
      healthLabel: `${unplannedTransactions} movimiento${unplannedTransactions === 1 ? "" : "s"} sin plan`,
      healthDescription:
        "Primero vinculá o clasificá los movimientos reales que quedaron fuera de planificación.",
      healthTone: "danger",
    };
  }

  if (suggestedMatches > 0) {
    return {
      healthLabel: `${suggestedMatches} sugerencia${suggestedMatches === 1 ? "" : "s"} de match`,
      healthDescription:
        "Hay vínculos probables entre planificación y movimientos reales. Conviene confirmarlos.",
      healthTone: "info",
    };
  }

  if (blockerCount > 0) {
    return {
      healthLabel: `${blockerCount} bloqueo${blockerCount === 1 ? "" : "s"}`,
      healthDescription:
        "Primero resolvé montos, cuentas o categorías faltantes.",
      healthTone: "warning",
    };
  }

  if (overExecutedItems > 0) {
    return {
      healthLabel: `${overExecutedItems} excedido${overExecutedItems === 1 ? "" : "s"}`,
      healthDescription:
        "Hay ítems donde lo ejecutado supera lo planificado. Revisá si fue real o mala asignación.",
      healthTone: "danger",
    };
  }

  if (partialItems > 0) {
    return {
      healthLabel: `${partialItems} parcial${partialItems === 1 ? "" : "es"}`,
      healthDescription:
        "Hay ítems cubiertos sólo parcialmente por movimientos reales.",
      healthTone: "warning",
    };
  }

  if (projectedPendingNet < 0) {
    return {
      healthLabel: "Neto pendiente negativo",
      healthDescription:
        "El saldo pendiente proyectado queda por debajo de cero.",
      healthTone: "danger",
    };
  }

  if (readyToConvert > 0) {
    return {
      healthLabel: `${readyToConvert} listo${readyToConvert === 1 ? "" : "s"} para convertir`,
      healthDescription:
        "Ya podés confirmar movimientos reales sin completar datos extra.",
      healthTone: "success",
    };
  }

  if (dueNext7DaysCount > 0) {
    return {
      healthLabel: `${dueNext7DaysCount} vencimiento${dueNext7DaysCount === 1 ? "" : "s"} cerca`,
      healthDescription:
        "Conviene revisar los próximos compromisos del período.",
      healthTone: "info",
    };
  }

  if (reconciliationIssueCount > 0 || actionableCount > 0) {
    return {
      healthLabel: `${actionableCount} acción${actionableCount === 1 ? "" : "es"} pendiente${actionableCount === 1 ? "" : "s"}`,
      healthDescription: "Hay ítems para revisar antes de cerrar el mes.",
      healthTone: "info",
    };
  }

  return {
    healthLabel: "Checklist limpio",
    healthDescription:
      "No hay bloqueos, movimientos sueltos ni desvíos críticos detectados.",
    healthTone: "success",
  };
}

function isActiveItem(item: MonthlyPlanItem): boolean {
  return item.status !== "CANCELLED";
}

function isUnpricedItem(item: MonthlyPlanItem): boolean {
  return (
    isActiveItem(item) &&
    !item.transactionId &&
    item.amount == null &&
    item.minAmount == null &&
    item.maxAmount == null
  );
}

function isMissingClassification(item: MonthlyPlanItem): boolean {
  return (
    isActiveItem(item) &&
    !item.transactionId &&
    (!item.accountId || !item.categoryId)
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
  kind: "INCOME" | "EXPENSE",
): number {
  return items
    .filter((item) => {
      if (
        item.type === "TODO" ||
        item.status === "CANCELLED" ||
        item.transactionId
      ) {
        return false;
      }

      const incomeLike = item.type === "INCOME" || item.type === "RECOVERY";

      return kind === "INCOME" ? incomeLike : !incomeLike;
    })
    .reduce((total, item) => total + getExpectedAmount(item), 0);
}

function getExpectedAmount(item: MonthlyPlanItem): number {
  const numericValue =
    item.amount ??
    item.maxAmount ??
    item.minAmount ??
    item.grossMax ??
    item.grossMin ??
    item.netMax ??
    item.netMin ??
    0;

  const parsed = Number(numericValue);

  if (!Number.isFinite(parsed)) {
    return 0;
  }

  return Math.abs(parsed);
}

function parseLocalDate(value: string): Date | null {
  const match = /^(\d{4})-(\d{2})-(\d{2})$/.exec(value);

  if (!match) {
    return null;
  }

  const [, rawYear, rawMonth, rawDay] = match;
  const year = Number(rawYear);
  const month = Number(rawMonth);
  const day = Number(rawDay);
  const parsed = startOfDay(new Date(year, month - 1, day));

  if (
    parsed.getFullYear() !== year ||
    parsed.getMonth() !== month - 1 ||
    parsed.getDate() !== day
  ) {
    return null;
  }

  return parsed;
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
  if (tone === "success") return "badge-success";
  if (tone === "warning") return "badge-warning";
  if (tone === "danger") return "badge-danger";
  if (tone === "info") return "badge-info";
  return "badge-muted";
}

function getToneClass(tone: ChecklistTone): string {
  if (tone === "success") return "mpc-tone-success";
  if (tone === "warning") return "mpc-tone-warning";
  if (tone === "danger") return "mpc-tone-danger";
  if (tone === "info") return "mpc-tone-info";
  return "mpc-tone-neutral";
}

function getPriorityBadgeClass(priority: ChecklistPriority): string {
  if (priority === "ALTA") return "badge-warning";
  if (priority === "MEDIA") return "badge-info";
  if (priority === "BAJA") return "badge-success";
  return "badge-muted";
}