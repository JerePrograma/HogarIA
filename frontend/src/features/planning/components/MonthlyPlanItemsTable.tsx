import { Fragment, memo, useEffect, useMemo, useState } from "react";
import { getApiErrorMessage } from "../../../api/http";
import { getMonthlyPlanSuggestions } from "../../../api/monthlyPlanSuggestionsApi";
import { StatusBadge } from "../../../components/ui/StatusBadge";
import {
  labelOrValue,
  monthlyPlanPriorityLabels,
  monthlyPlanStatusLabels,
  monthlyPlanTypeLabels,
} from "../../../domain/financeLabels";
import type {
  Account,
  Category,
  ConfirmPlanTransactionMatchPayload,
  MonthlyPlanItem,
  MonthlyPlanItemUpdatePayload,
  MonthlyPlanPriority,
  MonthlyPlanReconciliationSummary,
  MonthlyPlanStatus,
  PlanningSuggestionResponse,
} from "../../../domain/types";
import {
  canConvertPlanItem,
  formatPlanAmount,
  formatPlanNet,
  formatPlanRecovery,
  getPlanItemCompletionScore,
  getPlanItemMissingLabels,
  getPlanItemNextAction,
  isDueNextDays,
  matchesExternalFilter,
  matchesStatusFilter,
  sortPlanItems,
  type PlanItemSortKey,
  type StatusFilterKey,
  type TableFilterKey,
} from "../planningUtils";

export type { TableFilterKey } from "../planningUtils";

type Props = {
  items: MonthlyPlanItem[];
  reconciliation?: MonthlyPlanReconciliationSummary;
  accounts: Account[];
  categories: Category[];
  onConvert: (id: string) => void;
  onCancel: (id: string) => void;
  onDelete: (id: string) => void;
  onUpdate: (
    id: string,
    payload: MonthlyPlanItemUpdatePayload,
  ) => Promise<void>;
  onMarkPaid: (id: string) => void;
  onMarkCollected: (id: string) => void;
  onConfirmMatch: (payload: ConfirmPlanTransactionMatchPayload) => void;
  onDeleteMatch: (matchId: string) => void;
  pendingActionId?: string | null;
  pendingMatchId?: string | null;
  actionError?: string | null;
  profileId: string;
  externalFilterKey?: TableFilterKey;
  onExternalFilterChange?: (filter: TableFilterKey) => void;
};

type EditMode = "FULL" | "AMOUNT" | "CONVERT";

type EditForm = {
  title: string;
  expectedDate: string;
  amount: string;
  minAmount: string;
  maxAmount: string;
  expectedRecoveryAmount: string;
  expectedRecoveryPercent: string;
  counterparty: string;
  status: MonthlyPlanStatus;
  priority: MonthlyPlanPriority;
  accountId: string;
  categoryId: string;
  installmentNumber: string;
  installmentTotal: string;
};

type SuggestionState = {
  itemId: string;
  response: PlanningSuggestionResponse;
};

type Option = {
  value: string;
  label: string;
};

const typeOptions: Option[] = [
  { value: "ALL", label: "Todos" },
  { value: "INCOME", label: "Ingresos" },
  { value: "EXPENSE", label: "Egresos" },
  { value: "DEBT", label: "Deudas" },
  { value: "SAVING", label: "Ahorros" },
  { value: "TRANSFER", label: "Transferencias" },
  { value: "RECOVERY", label: "Recuperos" },
  { value: "TODO", label: "TODO" },
];

const statusOptions: Array<{ value: StatusFilterKey; label: string }> = [
  { value: "ALL", label: "Todos" },
  { value: "PENDING", label: "Pendientes" },
  { value: "DONE", label: "Pagados/Cobrados" },
  { value: "CANCELLED", label: "Cancelados" },
];

const sortOptions: Array<{ value: PlanItemSortKey; label: string }> = [
  { value: "DATE", label: "Fecha" },
  { value: "PRIORITY", label: "Prioridad" },
  { value: "AMOUNT", label: "Monto" },
];

const externalFilterLabels: Record<TableFilterKey, string> = {
  ALL: "Todos",
  UNPRICED: "Sin monto",
  MISSING_CLASSIFICATION: "Sin cuenta/categoría",
  READY_TO_CONVERT: "Listos para convertir",
  DUE_NEXT_7_DAYS: "Próximos 7 días",
  NOT_EXECUTED: "Sin ejecutar",
  PARTIALLY_EXECUTED: "Parciales",
  OVER_EXECUTED: "Excedidos",
  UNPLANNED_MOVEMENTS: "Movimientos sin plan",
  SUGGESTED_MATCHES: "Sugerencias",
};

const toForm = (item: MonthlyPlanItem): EditForm => ({
  title: item.title,
  expectedDate: item.expectedDate ?? "",
  amount: item.amount == null ? "" : String(item.amount),
  minAmount: item.minAmount == null ? "" : String(item.minAmount),
  maxAmount: item.maxAmount == null ? "" : String(item.maxAmount),
  expectedRecoveryAmount:
    item.expectedRecoveryAmount == null
      ? ""
      : String(item.expectedRecoveryAmount),
  expectedRecoveryPercent:
    item.expectedRecoveryPercent == null
      ? ""
      : String(item.expectedRecoveryPercent),
  counterparty: item.counterparty ?? "",
  status: item.status,
  priority: item.priority,
  accountId: item.accountId ?? "",
  categoryId: item.categoryId ?? "",
  installmentNumber:
    item.installmentNumber == null ? "" : String(item.installmentNumber),
  installmentTotal:
    item.installmentTotal == null ? "" : String(item.installmentTotal),
});

export function MonthlyPlanItemsTable({
  items,
  reconciliation,
  accounts,
  categories,
  onConvert,
  onCancel,
  onDelete,
  onUpdate,
  onMarkPaid,
  onMarkCollected,
  pendingActionId,
  actionError,
  profileId,
  externalFilterKey = "ALL",
  onExternalFilterChange,
}: Props) {
  const [statusFilter, setStatusFilter] = useState<StatusFilterKey>("ALL");
  const [typeFilter, setTypeFilter] = useState<string>("ALL");
  const [sortBy, setSortBy] = useState<PlanItemSortKey>("DATE");
  const [search, setSearch] = useState("");
  const [editingId, setEditingId] = useState<string | null>(null);
  const [editMode, setEditMode] = useState<EditMode>("FULL");
  const [form, setForm] = useState<EditForm | null>(null);
  const [editError, setEditError] = useState<string | null>(null);
  const [savingId, setSavingId] = useState<string | null>(null);
  const [suggestingId, setSuggestingId] = useState<string | null>(null);
  const [suggestion, setSuggestion] = useState<SuggestionState | null>(null);
  const [expandedId, setExpandedId] = useState<string | null>(null);

  const accountById = useMemo(() => createLookup(accounts), [accounts]);
  const categoryById = useMemo(() => createLookup(categories), [categories]);

  const reconciliationByItemId = useMemo(
    () =>
      new Map((reconciliation?.items ?? []).map((item) => [item.itemId, item])),
    [reconciliation],
  );

  const suggestedItemIds = useMemo(
    () =>
      new Set(
        (reconciliation?.suggestedMatches ?? []).map((match) => match.itemId),
      ),
    [reconciliation],
  );

  const filtered = useMemo(
    () =>
      items
        .filter((item) =>
          matchesExternalFilter(
            item,
            externalFilterKey,
            reconciliationByItemId,
            suggestedItemIds,
          ),
        )
        .filter((item) => matchesStatusFilter(item, statusFilter))
        .filter((item) => typeFilter === "ALL" || item.type === typeFilter)
        .filter((item) =>
          matchesSearch(item, search, accountById, categoryById),
        )
        .sort((a, b) => sortPlanItems(a, b, sortBy)),
    [
      items,
      externalFilterKey,
      reconciliationByItemId,
      suggestedItemIds,
      statusFilter,
      typeFilter,
      search,
      accountById,
      categoryById,
      sortBy,
    ],
  );

  const visibleStats = useMemo(() => getVisibleStats(filtered), [filtered]);

  useEffect(() => {
    if (editingId && !items.some((item) => item.id === editingId)) {
      closeEdit();
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [editingId, items]);

  const startEdit = (
    item: MonthlyPlanItem,
    mode: EditMode,
    initialForm = toForm(item),
  ) => {
    setEditingId(item.id);
    setExpandedId(item.id);
    setEditMode(mode);
    setForm(initialForm);
    setEditError(null);
    setSuggestion(null);
  };

  const closeEdit = () => {
    setEditingId(null);
    setForm(null);
    setEditError(null);
    setSuggestion(null);
  };

  const clearAllFilters = () => {
    setStatusFilter("ALL");
    setTypeFilter("ALL");
    setSortBy("DATE");
    setSearch("");
    onExternalFilterChange?.("ALL");
  };

  const loadSuggestion = async (
    item: MonthlyPlanItem,
    sourceForm = form ?? toForm(item),
  ) => {
    try {
      setSuggestingId(item.id);
      setEditError(null);

      const response = await getMonthlyPlanSuggestions(profileId, {
        type: item.type,
        title: sourceForm.title,
        counterparty: sourceForm.counterparty || null,
        amount: parseNullableNumber(sourceForm.amount),
        minAmount: parseNullableNumber(sourceForm.minAmount),
        maxAmount: parseNullableNumber(sourceForm.maxAmount),
        expectedRecoveryAmount: parseNullableNumber(
          sourceForm.expectedRecoveryAmount,
        ),
        expectedRecoveryPercent: parseNullableNumber(
          sourceForm.expectedRecoveryPercent,
        ),
      });

      setSuggestion({ itemId: item.id, response });
    } catch {
      setSuggestion(null);
      setEditError("No se pudo obtener sugerencias en este momento.");
    } finally {
      setSuggestingId(null);
    }
  };

  const prepareSuggestionEdit = (item: MonthlyPlanItem) => {
    const initialForm = toForm(item);
    startEdit(item, "CONVERT", initialForm);
    void loadSuggestion(item, initialForm);
  };

  const saveEdit = async (item: MonthlyPlanItem) => {
    if (!form) {
      return;
    }

    const validationError = validateEditForm(form);

    if (validationError) {
      setEditError(validationError);
      return;
    }

    const payload = buildUpdatePayload(item, form);

    try {
      setSavingId(item.id);
      setEditError(null);
      await onUpdate(item.id, payload);
      closeEdit();
    } catch (error) {
      setEditError(getApiErrorMessage(error));
    } finally {
      setSavingId(null);
    }
  };

  return (
    <section className="panel planning-items-panel">
      <div className="section-title planning-items-title">
        <div>
          <p className="eyebrow">Detalle</p>
          <h2>Ítems planificados</h2>
          <p className="secondary-text">
            Priorizá el próximo paso: completar datos, confirmar pago/cobro o
            convertir a movimiento.
          </p>
        </div>

        <div
          className="planning-items-kpis"
          aria-label="Resumen de ítems visibles"
        >
          <span className="badge-ui badge-info">
            {filtered.length} visibles
          </span>
          <span className="badge-ui badge-warning">
            {visibleStats.incomplete} incompletos
          </span>
          <span className="badge-ui badge-ok">
            {visibleStats.convertible} convertibles
          </span>
        </div>
      </div>

      <TableFilters
        statusFilter={statusFilter}
        setStatusFilter={setStatusFilter}
        typeFilter={typeFilter}
        setTypeFilter={setTypeFilter}
        sortBy={sortBy}
        setSortBy={setSortBy}
        search={search}
        setSearch={setSearch}
        externalFilterKey={externalFilterKey}
        onExternalFilterChange={onExternalFilterChange}
        onClearAll={clearAllFilters}
      />

      {actionError ? <p className="mensaje-error">{actionError}</p> : null}

      <div className="planning-items-desktop tabla-ui mt-4">
        <table>
          <thead>
            <tr>
              <th>Fecha</th>
              <th>Ítem</th>
              <th>Tipo</th>
              <th>Monto</th>
              <th>Neto</th>
              <th>Estado</th>
              <th>Falta</th>
              <th>Acciones</th>
            </tr>
          </thead>

          <tbody>
            {filtered.map((item) => {
              const isEditing = editingId === item.id && form;
              const isExpanded = expandedId === item.id;
              const isBusy =
                pendingActionId === item.id || savingId === item.id;
              const currentSuggestion =
                suggestion && suggestion.itemId === item.id
                  ? suggestion.response
                  : null;

              return (
                <Fragment key={item.id}>
                  <PlanItemRow
                    item={item}
                    accountName={accountById.get(item.accountId ?? "")?.name}
                    categoryName={categoryById.get(item.categoryId ?? "")?.name}
                    isBusy={isBusy}
                    isExpanded={isExpanded}
                    onEdit={(mode) => startEdit(item, mode)}
                    onConvert={() => onConvert(item.id)}
                    onMarkPaid={() => onMarkPaid(item.id)}
                    onMarkCollected={() => onMarkCollected(item.id)}
                    onToggleMore={() =>
                      setExpandedId(isExpanded ? null : item.id)
                    }
                  />

                  {isExpanded ? (
                    <ExpandedActionsRow
                      item={item}
                      isBusy={isBusy}
                      isSuggesting={suggestingId === item.id}
                      onCancel={() => onCancel(item.id)}
                      onDelete={() => onDelete(item.id)}
                      onSuggest={() => prepareSuggestionEdit(item)}
                      onEditFull={() => startEdit(item, "FULL")}
                    />
                  ) : null}

                  {isEditing && form ? (
                    <EditRow
                      item={item}
                      form={form}
                      setForm={setForm}
                      editMode={editMode}
                      accounts={accounts}
                      categories={categories}
                      editError={editError}
                      suggestion={currentSuggestion}
                      saving={savingId === item.id}
                      suggesting={suggestingId === item.id}
                      onSuggest={() => void loadSuggestion(item)}
                      onApplySuggestion={() => {
                        if (!currentSuggestion) return;

                        setForm({
                          ...form,
                          accountId:
                            currentSuggestion.accountSuggestion?.id ??
                            form.accountId,
                          categoryId:
                            currentSuggestion.categorySuggestion?.id ??
                            form.categoryId,
                        });
                      }}
                      onSave={() => void saveEdit(item)}
                      onClose={closeEdit}
                    />
                  ) : null}
                </Fragment>
              );
            })}
          </tbody>
        </table>
      </div>

      <div className="planning-items-mobile">
        {filtered.map((item) => {
          const isBusy = pendingActionId === item.id || savingId === item.id;
          const isEditing = editingId === item.id && form;
          const currentSuggestion =
            suggestion && suggestion.itemId === item.id
              ? suggestion.response
              : null;

          return (
            <PlanItemMobileCard
              key={item.id}
              item={item}
              accountName={accountById.get(item.accountId ?? "")?.name}
              categoryName={categoryById.get(item.categoryId ?? "")?.name}
              isBusy={isBusy}
              isEditing={Boolean(isEditing)}
              form={isEditing ? form : null}
              editMode={editMode}
              accounts={accounts}
              categories={categories}
              editError={editError}
              suggestion={currentSuggestion}
              saving={savingId === item.id}
              suggesting={suggestingId === item.id}
              onEdit={(mode) => startEdit(item, mode)}
              onConvert={() => onConvert(item.id)}
              onMarkPaid={() => onMarkPaid(item.id)}
              onMarkCollected={() => onMarkCollected(item.id)}
              onCancel={() => onCancel(item.id)}
              onDelete={() => onDelete(item.id)}
              onSuggest={() => prepareSuggestionEdit(item)}
              setForm={setForm}
              onSave={() => void saveEdit(item)}
              onClose={closeEdit}
              onApplySuggestion={() => {
                if (!form || !currentSuggestion) return;

                setForm({
                  ...form,
                  accountId:
                    currentSuggestion.accountSuggestion?.id ?? form.accountId,
                  categoryId:
                    currentSuggestion.categorySuggestion?.id ?? form.categoryId,
                });
              }}
              onRefreshSuggestion={() => void loadSuggestion(item)}
            />
          );
        })}
      </div>

      {filtered.length === 0 ? (
        <p className="mensaje-info mt-4">
          No hay ítems que coincidan con los filtros actuales. Limpiá filtros o
          cambiá el período.
        </p>
      ) : null}
    </section>
  );
}

const TableFilters = memo(function TableFilters({
  statusFilter,
  setStatusFilter,
  typeFilter,
  setTypeFilter,
  sortBy,
  setSortBy,
  search,
  setSearch,
  externalFilterKey,
  onExternalFilterChange,
  onClearAll,
}: {
  statusFilter: StatusFilterKey;
  setStatusFilter: (value: StatusFilterKey) => void;
  typeFilter: string;
  setTypeFilter: (value: string) => void;
  sortBy: PlanItemSortKey;
  setSortBy: (value: PlanItemSortKey) => void;
  search: string;
  setSearch: (value: string) => void;
  externalFilterKey: TableFilterKey;
  onExternalFilterChange?: (filter: TableFilterKey) => void;
  onClearAll: () => void;
}) {
  return (
    <div className="planning-items-toolbar">
      <label className="planning-search-field">
        Buscar
        <input
          className="input-ui"
          placeholder="Título, contraparte, cuenta o categoría"
          value={search}
          onChange={(event) => setSearch(event.target.value)}
        />
      </label>

      <div className="form-row planning-filter-row">
        <label>
          Estado
          <select
            className="input-ui"
            value={statusFilter}
            onChange={(event) =>
              setStatusFilter(event.target.value as StatusFilterKey)
            }
          >
            {statusOptions.map((option) => (
              <option key={option.value} value={option.value}>
                {option.label}
              </option>
            ))}
          </select>
        </label>

        <label>
          Tipo
          <select
            className="input-ui"
            value={typeFilter}
            onChange={(event) => setTypeFilter(event.target.value)}
          >
            {typeOptions.map((option) => (
              <option key={option.value} value={option.value}>
                {option.label}
              </option>
            ))}
          </select>
        </label>

        <label>
          Orden
          <select
            className="input-ui"
            value={sortBy}
            onChange={(event) =>
              setSortBy(event.target.value as PlanItemSortKey)
            }
          >
            {sortOptions.map((option) => (
              <option key={option.value} value={option.value}>
                {option.label}
              </option>
            ))}
          </select>
        </label>
      </div>

      <div className="planning-filter-chips" aria-label="Filtros rápidos">
        {(Object.keys(externalFilterLabels) as TableFilterKey[]).map(
          (filter) => (
            <button
              key={filter}
              type="button"
              className={
                filter === externalFilterKey
                  ? "planning-filter-chip active"
                  : "planning-filter-chip"
              }
              onClick={() => onExternalFilterChange?.(filter)}
            >
              {externalFilterLabels[filter]}
            </button>
          ),
        )}

        <button type="button" className="boton-fantasma" onClick={onClearAll}>
          Limpiar
        </button>
      </div>
    </div>
  );
});

function PlanItemRow({
  item,
  accountName,
  categoryName,
  isBusy,
  isExpanded,
  onEdit,
  onConvert,
  onMarkPaid,
  onMarkCollected,
  onToggleMore,
}: {
  item: MonthlyPlanItem;
  accountName?: string;
  categoryName?: string;
  isBusy: boolean;
  isExpanded: boolean;
  onEdit: (mode: EditMode) => void;
  onConvert: () => void;
  onMarkPaid: () => void;
  onMarkCollected: () => void;
  onToggleMore: () => void;
}) {
  return (
    <tr
      className={
        isDueNextDays(item.expectedDate, 7)
          ? "plan-item-row plan-item-row-due"
          : "plan-item-row"
      }
    >
      <td>{formatDateOrDash(item.expectedDate)}</td>

      <td>
        <PlanItemTitle
          item={item}
          accountName={accountName}
          categoryName={categoryName}
        />
      </td>

      <td>{labelOrValue(monthlyPlanTypeLabels, item.type)}</td>

      <td className="amount-cell">{formatPlanAmount(item)}</td>

      <td className="amount-cell">{formatPlanNet(item)}</td>

      <td>
        <div className="planning-status-stack">
          <StatusBadge
            label={labelOrValue(monthlyPlanStatusLabels, item.status)}
          />
          <StatusBadge
            label={labelOrValue(monthlyPlanPriorityLabels, item.priority)}
          />
        </div>
      </td>

      <td>
        <MissingLabels item={item} />
      </td>

      <td>
        <PlanItemActions
          item={item}
          isBusy={isBusy}
          isExpanded={isExpanded}
          onEdit={onEdit}
          onConvert={onConvert}
          onMarkPaid={onMarkPaid}
          onMarkCollected={onMarkCollected}
          onToggleMore={onToggleMore}
        />
      </td>
    </tr>
  );
}

function PlanItemMobileCard({
  item,
  accountName,
  categoryName,
  isBusy,
  isEditing,
  form,
  editMode,
  accounts,
  categories,
  editError,
  suggestion,
  saving,
  suggesting,
  onEdit,
  onConvert,
  onMarkPaid,
  onMarkCollected,
  onCancel,
  onDelete,
  onSuggest,
  setForm,
  onSave,
  onClose,
  onApplySuggestion,
  onRefreshSuggestion,
}: {
  item: MonthlyPlanItem;
  accountName?: string;
  categoryName?: string;
  isBusy: boolean;
  isEditing: boolean;
  form: EditForm | null;
  editMode: EditMode;
  accounts: Account[];
  categories: Category[];
  editError: string | null;
  suggestion: PlanningSuggestionResponse | null;
  saving: boolean;
  suggesting: boolean;
  onEdit: (mode: EditMode) => void;
  onConvert: () => void;
  onMarkPaid: () => void;
  onMarkCollected: () => void;
  onCancel: () => void;
  onDelete: () => void;
  onSuggest: () => void;
  setForm: (next: EditForm) => void;
  onSave: () => void;
  onClose: () => void;
  onApplySuggestion: () => void;
  onRefreshSuggestion: () => void;
}) {
  return (
    <article className="plan-item-card">
      <div className="plan-item-card-header">
        <PlanItemTitle
          item={item}
          accountName={accountName}
          categoryName={categoryName}
        />

        <span className="plan-item-score" title="Completitud operativa">
          {getPlanItemCompletionScore(item)}%
        </span>
      </div>

      <dl className="plan-item-card-grid">
        <div>
          <dt>Fecha</dt>
          <dd>{formatDateOrDash(item.expectedDate)}</dd>
        </div>

        <div>
          <dt>Tipo</dt>
          <dd>{labelOrValue(monthlyPlanTypeLabels, item.type)}</dd>
        </div>

        <div>
          <dt>Monto</dt>
          <dd>{formatPlanAmount(item)}</dd>
        </div>

        <div>
          <dt>Neto</dt>
          <dd>{formatPlanNet(item)}</dd>
        </div>

        <div>
          <dt>Recupero</dt>
          <dd>{formatPlanRecovery(item)}</dd>
        </div>
      </dl>

      <div className="planning-status-stack">
        <StatusBadge
          label={labelOrValue(monthlyPlanStatusLabels, item.status)}
        />
        <StatusBadge
          label={labelOrValue(monthlyPlanPriorityLabels, item.priority)}
        />
      </div>

      <MissingLabels item={item} />

      <PlanItemActions
        item={item}
        isBusy={isBusy}
        isExpanded={false}
        onEdit={onEdit}
        onConvert={onConvert}
        onMarkPaid={onMarkPaid}
        onMarkCollected={onMarkCollected}
      />

      <div className="row-expanded-panel">
        <button
          type="button"
          className="boton-secundario"
          onClick={onCancel}
          disabled={isBusy || item.status === "CANCELLED"}
        >
          Cancelar
        </button>

        <button
          type="button"
          className="boton-danger"
          onClick={onDelete}
          disabled={isBusy}
        >
          Eliminar
        </button>

        <button
          type="button"
          className="boton-secundario"
          onClick={onSuggest}
          disabled={isBusy}
        >
          {suggesting ? "Sugiriendo..." : "Sugerir cuenta/categoría"}
        </button>
      </div>

      {isEditing && form ? (
        <div className="plan-item-card-edit">
          <EditPanel
            item={item}
            form={form}
            setForm={setForm}
            editMode={editMode}
            accounts={accounts}
            categories={categories}
            editError={editError}
            suggestion={suggestion}
            saving={saving}
            suggesting={suggesting}
            onSuggest={onRefreshSuggestion}
            onApplySuggestion={onApplySuggestion}
            onSave={onSave}
            onClose={onClose}
          />
        </div>
      ) : null}
    </article>
  );
}

function PlanItemTitle({
  item,
  accountName,
  categoryName,
}: {
  item: MonthlyPlanItem;
  accountName?: string;
  categoryName?: string;
}) {
  return (
    <div className="plan-item-title-cell">
      <strong>{item.title}</strong>

      <span className="compact-muted">
        {[item.counterparty, accountName, categoryName]
          .filter(Boolean)
          .join(" · ") || "Sin detalle"}
      </span>

      {item.transactionId ? (
        <span className="badge-ui badge-ok">Convertido</span>
      ) : null}
    </div>
  );
}

function MissingLabels({ item }: { item: MonthlyPlanItem }) {
  return (
    <div className="missing-badge-list">
      {getPlanItemMissingLabels(item).map((label) => (
        <span key={label} className="missing-badge">
          {label}
        </span>
      ))}
    </div>
  );
}

function PlanItemActions({
  item,
  isBusy,
  isExpanded,
  onEdit,
  onConvert,
  onMarkPaid,
  onMarkCollected,
  onToggleMore,
}: {
  item: MonthlyPlanItem;
  isBusy: boolean;
  isExpanded: boolean;
  onEdit: (mode: EditMode) => void;
  onConvert: () => void;
  onMarkPaid: () => void;
  onMarkCollected: () => void;
  onToggleMore?: () => void;
}) {
  const nextAction = getPlanItemNextAction(item);

  return (
    <div className="action-row planning-row-actions">
      <button
        type="button"
        className={
          nextAction === "EDIT" ? "boton-principal" : "boton-secundario"
        }
        onClick={() => onEdit("FULL")}
        disabled={isBusy}
      >
        Editar
      </button>

      {nextAction === "COMPLETE_AMOUNT" ? (
        <button
          type="button"
          className="boton-principal"
          onClick={() => onEdit("AMOUNT")}
          disabled={isBusy}
        >
          Completar monto
        </button>
      ) : null}

      {nextAction === "PREPARE_CONVERSION" ? (
        <button
          type="button"
          className="boton-principal"
          onClick={() => onEdit("CONVERT")}
          disabled={isBusy}
        >
          Preparar conversión
        </button>
      ) : null}

      {nextAction === "CONVERT" ? (
        <button
          type="button"
          className="boton-principal"
          onClick={onConvert}
          disabled={isBusy}
        >
          Convertir
        </button>
      ) : null}

      {nextAction === "MARK_PAID" ? (
        <button
          type="button"
          className="boton-secundario"
          onClick={onMarkPaid}
          disabled={isBusy}
        >
          Marcar pagado
        </button>
      ) : null}

      {nextAction === "MARK_COLLECTED" ? (
        <button
          type="button"
          className="boton-secundario"
          onClick={onMarkCollected}
          disabled={isBusy}
        >
          Marcar cobrado
        </button>
      ) : null}

      {onToggleMore ? (
        <button
          type="button"
          className="boton-fantasma"
          onClick={onToggleMore}
          disabled={isBusy}
        >
          {isExpanded ? "Ocultar acciones" : "Más"}
        </button>
      ) : null}
    </div>
  );
}

function ExpandedActionsRow({
  item,
  isBusy,
  isSuggesting,
  onCancel,
  onDelete,
  onSuggest,
  onEditFull,
}: {
  item: MonthlyPlanItem;
  isBusy: boolean;
  isSuggesting: boolean;
  onCancel: () => void;
  onDelete: () => void;
  onSuggest: () => void;
  onEditFull: () => void;
}) {
  return (
    <tr>
      <td colSpan={8}>
        <div className="row-expanded-panel">
          <button
            type="button"
            className="boton-secundario"
            onClick={onCancel}
            disabled={isBusy || item.status === "CANCELLED"}
          >
            Cancelar
          </button>

          <button
            type="button"
            className="boton-danger"
            onClick={onDelete}
            disabled={isBusy}
          >
            Eliminar
          </button>

          <button
            type="button"
            className="boton-secundario"
            onClick={onSuggest}
            disabled={isBusy}
          >
            {isSuggesting ? "Sugiriendo..." : "Sugerir cuenta/categoría"}
          </button>

          <button
            type="button"
            className="boton-secundario"
            onClick={onEditFull}
            disabled={isBusy}
          >
            Editar completo
          </button>
        </div>
      </td>
    </tr>
  );
}

function EditRow(props: EditPanelProps) {
  return (
    <tr>
      <td colSpan={8}>
        <EditPanel {...props} />
      </td>
    </tr>
  );
}

type EditPanelProps = {
  item: MonthlyPlanItem;
  form: EditForm;
  setForm: (next: EditForm) => void;
  editMode: EditMode;
  accounts: Account[];
  categories: Category[];
  editError: string | null;
  suggestion: PlanningSuggestionResponse | null;
  saving: boolean;
  suggesting: boolean;
  onSuggest: () => void;
  onApplySuggestion: () => void;
  onSave: () => void;
  onClose: () => void;
};

function EditPanel({
  item,
  form,
  setForm,
  editMode,
  accounts,
  categories,
  editError,
  suggestion,
  saving,
  suggesting,
  onSuggest,
  onApplySuggestion,
  onSave,
  onClose,
}: EditPanelProps) {
  const title =
    editMode === "AMOUNT"
      ? "Completar monto"
      : editMode === "CONVERT"
        ? "Preparar conversión"
        : "Editar ítem";

  return (
    <div className="panel-muted planning-edit-panel">
      <div className="section-title">
        <div>
          <p className="eyebrow">Edición</p>
          <h3>{title}</h3>
          <p className="secondary-text">
            {editMode === "CONVERT"
              ? "Para convertir, necesitás monto exacto, cuenta y categoría."
              : "Actualizá sólo los datos necesarios. Los campos vacíos limpian valores existentes."}
          </p>
        </div>
      </div>

      {editMode !== "AMOUNT" ? (
        <label>
          Título
          <input
            className="input-ui"
            placeholder="Ej: Alquiler, sueldo, cuota del préstamo"
            value={form.title}
            onChange={(event) =>
              setForm({ ...form, title: event.target.value })
            }
          />
        </label>
      ) : null}

      <div className="form-row">
        {editMode !== "CONVERT" ? (
          <label>
            Fecha esperada
            <input
              className="input-ui"
              type="date"
              value={form.expectedDate}
              onChange={(event) =>
                setForm({ ...form, expectedDate: event.target.value })
              }
            />
          </label>
        ) : null}

        <label>
          Monto exacto
          <input
            className="input-ui"
            type="number"
            inputMode="decimal"
            min="0"
            placeholder="Monto exacto"
            value={form.amount}
            onChange={(event) =>
              setForm({ ...form, amount: event.target.value })
            }
          />
        </label>

        {editMode !== "CONVERT" ? (
          <>
            <label>
              Monto mínimo
              <input
                className="input-ui"
                type="number"
                inputMode="decimal"
                min="0"
                placeholder="Monto mínimo"
                value={form.minAmount}
                onChange={(event) =>
                  setForm({ ...form, minAmount: event.target.value })
                }
              />
            </label>

            <label>
              Monto máximo
              <input
                className="input-ui"
                type="number"
                inputMode="decimal"
                min="0"
                placeholder="Monto máximo"
                value={form.maxAmount}
                onChange={(event) =>
                  setForm({ ...form, maxAmount: event.target.value })
                }
              />
            </label>
          </>
        ) : null}
      </div>

      {editMode === "FULL" ? (
        <>
          <div className="form-row">
            <label>
              Recupero monto
              <input
                className="input-ui"
                type="number"
                inputMode="decimal"
                min="0"
                placeholder="Recupero monto"
                value={form.expectedRecoveryAmount}
                onChange={(event) =>
                  setForm({
                    ...form,
                    expectedRecoveryAmount: event.target.value,
                  })
                }
              />
            </label>

            <label>
              Recupero %
              <input
                className="input-ui"
                type="number"
                inputMode="decimal"
                min="0"
                max="100"
                placeholder="0 a 100"
                value={form.expectedRecoveryPercent}
                onChange={(event) =>
                  setForm({
                    ...form,
                    expectedRecoveryPercent: event.target.value,
                  })
                }
              />
            </label>

            <label>
              Contraparte
              <input
                className="input-ui"
                placeholder="Proveedor, cliente, persona, entidad"
                value={form.counterparty}
                onChange={(event) =>
                  setForm({ ...form, counterparty: event.target.value })
                }
              />
            </label>
          </div>

          <div className="form-row">
            <label>
              N° cuota
              <input
                className="input-ui"
                type="number"
                inputMode="numeric"
                min="1"
                placeholder="Ej: 2"
                value={form.installmentNumber}
                onChange={(event) =>
                  setForm({ ...form, installmentNumber: event.target.value })
                }
              />
            </label>

            <label>
              Total cuotas
              <input
                className="input-ui"
                type="number"
                inputMode="numeric"
                min="1"
                placeholder="Ej: 12"
                value={form.installmentTotal}
                onChange={(event) =>
                  setForm({ ...form, installmentTotal: event.target.value })
                }
              />
            </label>
          </div>
        </>
      ) : null}

      <div className="form-row">
        <label>
          Cuenta
          <select
            className="input-ui"
            value={form.accountId}
            onChange={(event) =>
              setForm({ ...form, accountId: event.target.value })
            }
          >
            <option value="">Sin cuenta</option>
            {accounts.map((account) => (
              <option key={account.id} value={account.id}>
                {account.name}
              </option>
            ))}
          </select>
        </label>

        <label>
          Categoría
          <select
            className="input-ui"
            value={form.categoryId}
            onChange={(event) =>
              setForm({ ...form, categoryId: event.target.value })
            }
          >
            <option value="">Sin categoría</option>
            {categories.map((category) => (
              <option key={category.id} value={category.id}>
                {category.name}
              </option>
            ))}
          </select>
        </label>

        {editMode === "FULL" ? (
          <>
            <label>
              Estado
              <select
                className="input-ui"
                value={form.status}
                onChange={(event) =>
                  setForm({
                    ...form,
                    status: event.target.value as MonthlyPlanStatus,
                  })
                }
              >
                {Object.entries(monthlyPlanStatusLabels).map(
                  ([value, label]) => (
                    <option key={value} value={value}>
                      {label}
                    </option>
                  ),
                )}
              </select>
            </label>

            <label>
              Prioridad
              <select
                className="input-ui"
                value={form.priority}
                onChange={(event) =>
                  setForm({
                    ...form,
                    priority: event.target.value as MonthlyPlanPriority,
                  })
                }
              >
                {Object.entries(monthlyPlanPriorityLabels).map(
                  ([value, label]) => (
                    <option key={value} value={value}>
                      {label}
                    </option>
                  ),
                )}
              </select>
            </label>
          </>
        ) : null}
      </div>

      {editError ? <p className="mensaje-error">{editError}</p> : null}

      {suggestion ? (
        <SuggestionBox
          suggestion={suggestion}
          onApplySuggestion={onApplySuggestion}
        />
      ) : null}

      <div className="table-actions">
        <button
          type="button"
          className="boton-secundario"
          onClick={onSuggest}
          disabled={saving || suggesting}
        >
          {suggesting ? "Sugiriendo..." : "Sugerir cuenta/categoría"}
        </button>

        <button
          type="button"
          className="boton-principal"
          onClick={onSave}
          disabled={saving}
        >
          {saving ? "Guardando..." : "Guardar"}
        </button>

        <button
          type="button"
          className="boton-secundario"
          onClick={onClose}
          disabled={saving}
        >
          Cancelar
        </button>
      </div>

      {item.type === "TODO" ? (
        <p className="mensaje-warning">
          Este ítem es TODO: puede planificarse, pero no debería convertirse a
          movimiento financiero.
        </p>
      ) : null}
    </div>
  );
}

function SuggestionBox({
  suggestion,
  onApplySuggestion,
}: {
  suggestion: PlanningSuggestionResponse;
  onApplySuggestion: () => void;
}) {
  const hasSuggestion = suggestion.confidence !== "NONE";

  return (
    <div className="surface-inset planning-suggestion-box">
      <div>
        <strong>
          {hasSuggestion
            ? "Sugerencias disponibles"
            : "Sin historial suficiente"}
        </strong>
        <p className="secondary-text">
          {hasSuggestion
            ? "Podés aplicar cuenta y categoría sugeridas según historial."
            : "No encontré datos confiables para inferir cuenta o categoría."}
        </p>
      </div>

      {hasSuggestion ? (
        <button
          type="button"
          className="boton-secundario"
          onClick={onApplySuggestion}
        >
          Aplicar sugerencia
        </button>
      ) : null}

      {suggestion.reasons.length > 0 ? (
        <ul className="planning-suggestion-reasons">
          {suggestion.reasons.map((reason, index) => (
            <li key={`${reason}-${index}`}>{reason}</li>
          ))}
        </ul>
      ) : null}
    </div>
  );
}

function validateEditForm(form: EditForm): string | null {
  const amount = parseNullableNumber(form.amount);
  const minAmount = parseNullableNumber(form.minAmount);
  const maxAmount = parseNullableNumber(form.maxAmount);
  const expectedRecoveryAmount = parseNullableNumber(
    form.expectedRecoveryAmount,
  );
  const expectedRecoveryPercent = parseNullableNumber(
    form.expectedRecoveryPercent,
  );
  const installmentNumber = parseNullableNumber(form.installmentNumber);
  const installmentTotal = parseNullableNumber(form.installmentTotal);

  if (!form.title.trim()) {
    return "El título no puede quedar vacío.";
  }

  if (amount != null && amount < 0) {
    return "El monto exacto no puede ser negativo.";
  }

  if (minAmount != null && minAmount < 0) {
    return "El monto mínimo no puede ser negativo.";
  }

  if (maxAmount != null && maxAmount < 0) {
    return "El monto máximo no puede ser negativo.";
  }

  if (minAmount != null && maxAmount != null && minAmount > maxAmount) {
    return "El monto mínimo no puede superar al máximo.";
  }

  if (expectedRecoveryAmount != null && expectedRecoveryAmount < 0) {
    return "El recupero por monto no puede ser negativo.";
  }

  if (
    expectedRecoveryPercent != null &&
    (expectedRecoveryPercent < 0 || expectedRecoveryPercent > 100)
  ) {
    return "El recupero porcentual debe estar entre 0 y 100.";
  }

  if (expectedRecoveryAmount != null && expectedRecoveryPercent != null) {
    return "Usá recupero por monto o por porcentaje, no ambos.";
  }

  if (installmentNumber != null && installmentNumber < 1) {
    return "El número de cuota debe ser mayor a cero.";
  }

  if (installmentTotal != null && installmentTotal < 1) {
    return "El total de cuotas debe ser mayor a cero.";
  }

  if (
    installmentNumber != null &&
    installmentTotal != null &&
    installmentNumber > installmentTotal
  ) {
    return "La cuota no puede superar el total de cuotas.";
  }

  return null;
}

function buildUpdatePayload(
  item: MonthlyPlanItem,
  form: EditForm,
): MonthlyPlanItemUpdatePayload {
  const amount = parseNullableNumber(form.amount);
  const minAmount = parseNullableNumber(form.minAmount);
  const maxAmount = parseNullableNumber(form.maxAmount);
  const expectedRecoveryAmount = parseNullableNumber(
    form.expectedRecoveryAmount,
  );
  const expectedRecoveryPercent = parseNullableNumber(
    form.expectedRecoveryPercent,
  );
  const installmentNumber = parseNullableNumber(form.installmentNumber);
  const installmentTotal = parseNullableNumber(form.installmentTotal);

  const hasExactAmount = amount != null;
  const hasRange = minAmount != null || maxAmount != null;

  const clearInstallment =
    form.installmentNumber.trim() === "" &&
    form.installmentTotal.trim() === "" &&
    (item.installmentNumber != null || item.installmentTotal != null);

  return {
    title: form.title.trim(),
    status: form.status,
    priority: form.priority,

    amount,
    minAmount: hasExactAmount ? null : minAmount,
    maxAmount: hasExactAmount ? null : maxAmount,
    expectedRecoveryAmount,
    expectedRecoveryPercent,

    expectedDate: form.expectedDate || undefined,
    counterparty: form.counterparty.trim() || undefined,
    accountId: form.accountId || undefined,
    categoryId: form.categoryId || undefined,

    installmentNumber,
    installmentTotal,

    clearAmount: !hasExactAmount && (item.amount != null || hasRange),
    clearRange:
      hasExactAmount ||
      (form.minAmount.trim() === "" &&
        form.maxAmount.trim() === "" &&
        (item.minAmount != null || item.maxAmount != null)),
    clearRecovery:
      form.expectedRecoveryAmount.trim() === "" &&
      form.expectedRecoveryPercent.trim() === "" &&
      (item.expectedRecoveryAmount != null ||
        item.expectedRecoveryPercent != null),
    clearExpectedDate:
      form.expectedDate.trim() === "" && Boolean(item.expectedDate),
    clearCounterparty:
      form.counterparty.trim() === "" && Boolean(item.counterparty),
    clearAccount: form.accountId.trim() === "" && Boolean(item.accountId),
    clearCategory: form.categoryId.trim() === "" && Boolean(item.categoryId),
    ...(clearInstallment ? { clearInstallment: true } : {}),
  };
}

function parseNullableNumber(value: string): number | null {
  if (!value.trim()) {
    return null;
  }

  const normalized = value.replace(",", ".");
  const parsed = Number(normalized);

  return Number.isFinite(parsed) ? parsed : null;
}

function matchesSearch(
  item: MonthlyPlanItem,
  rawSearch: string,
  accountById: Map<string, Account>,
  categoryById: Map<string, Category>,
): boolean {
  const search = normalizeSearch(rawSearch);

  if (!search) {
    return true;
  }

  const accountName = accountById.get(item.accountId ?? "")?.name ?? "";
  const categoryName = categoryById.get(item.categoryId ?? "")?.name ?? "";

  return normalizeSearch(
    [
      item.title,
      item.description,
      item.counterparty,
      item.expectedDate,
      item.status,
      item.type,
      accountName,
      categoryName,
    ]
      .filter(Boolean)
      .join(" "),
  ).includes(search);
}

function normalizeSearch(value: string): string {
  return value
    .trim()
    .toLocaleLowerCase("es-AR")
    .normalize("NFD")
    .replace(/\p{Diacritic}/gu, "");
}

function createLookup<T extends { id: string }>(items: T[]): Map<string, T> {
  return new Map(items.map((item) => [item.id, item]));
}

function getVisibleStats(items: MonthlyPlanItem[]): {
  incomplete: number;
  convertible: number;
} {
  return items.reduce(
    (acc, item) => ({
      incomplete:
        acc.incomplete +
        (getPlanItemMissingLabels(item).some((label) => label.startsWith("Sin"))
          ? 1
          : 0),
      convertible: acc.convertible + (canConvertPlanItem(item) ? 1 : 0),
    }),
    { incomplete: 0, convertible: 0 },
  );
}

function formatDateOrDash(value: string | null | undefined): string {
  if (!value) {
    return "-";
  }

  return value;
}
