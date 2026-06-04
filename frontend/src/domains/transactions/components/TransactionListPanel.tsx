import type {
  Account,
  Category,
  MoneyTransaction,
  MovementType,
  PaymentChannel,
  TransactionClassificationStatus,
  TransactionStatus,
} from "../../../domain/types";
import {
  classificationStatusOptions,
  movementTypeOptions,
  paymentChannelOptions,
  transactionOriginOptions,
  transactionStatusOptions,
} from "../../../domain/financeOptions";
import { getCategoryDisplayName } from "../../../domain/transactionRules";
import { EmptyState } from "../../../shared/ui/EmptyState";
import { ErrorState } from "../../../shared/ui/ErrorState";
import { ALL, WITHOUT_CATEGORY, type TransactionFilters } from "../types";
import { TransactionTable } from "./TransactionTable";
import { TransactionMobileList } from "./TransactionMobileList";
import { useState } from "react";

type ViewMode = "SIMPLE" | "AUDIT";

interface Props {
  transactions: MoneyTransaction[];
  filteredTransactions: MoneyTransaction[];
  filters: TransactionFilters;
  accounts: Account[];
  categories: Category[];
  accountsById: Map<string, Account>;
  categoriesById: Map<string, Category>;
  activeFilterCount: number;
  loading: boolean;
  isError: boolean;
  updatePending: boolean;
  deletePending: boolean;
  updatingTransactionId?: string;
  deletingTransactionId?: string;
  onFiltersChange: (patch: Partial<TransactionFilters>) => void;
  onResetFilters: () => void;
  onToggleStatus: (transaction: MoneyTransaction) => void;
  onDelete: (transaction: MoneyTransaction) => void;
  selectedTransactionIds: string[];
  onSelectionChange: (ids: string[]) => void;
  bulkPending: boolean;
  onBulkCategorize: (categoryId: string) => void;
  onBulkStatus: (status: "CONFIRMED" | "PENDING") => void;
  onBulkIgnore: () => void;
  onBulkLinkTransfer: () => void;
}

export function TransactionListPanel({
  transactions,
  filteredTransactions,
  filters,
  accounts,
  categories,
  accountsById,
  categoriesById,
  activeFilterCount,
  loading,
  isError,
  updatePending,
  deletePending,
  updatingTransactionId,
  deletingTransactionId,
  onFiltersChange,
  onResetFilters,
  onToggleStatus,
  onDelete,
  selectedTransactionIds,
  onSelectionChange,
  bulkPending,
  onBulkCategorize,
  onBulkStatus,
  onBulkIgnore,
  onBulkLinkTransfer,
}: Props) {
  const [viewMode, setViewMode] = useState<ViewMode>("SIMPLE");
  const [bulkCategoryId, setBulkCategoryId] = useState("");
  const selectedCount = selectedTransactionIds.length;

  const toggleSelection = (id: string) => {
    onSelectionChange(
      selectedTransactionIds.includes(id)
        ? selectedTransactionIds.filter((current) => current !== id)
        : [...selectedTransactionIds, id],
    );
  };

  const toggleVisibleSelection = () => {
    const visibleIds = filteredTransactions.map((transaction) => transaction.id);
    const allVisibleSelected = visibleIds.every((id) =>
      selectedTransactionIds.includes(id),
    );

    onSelectionChange(
      allVisibleSelected
        ? selectedTransactionIds.filter((id) => !visibleIds.includes(id))
        : [...new Set([...selectedTransactionIds, ...visibleIds])],
    );
  };

  return (
    <section className="panel transactions-list-panel">
      <div className="section-title">
        <div>
          <p className="eyebrow">Listado</p>
          <h2>Movimientos del período</h2>
          <p className="muted">
            {filteredTransactions.length} visible
            {filteredTransactions.length === 1 ? "" : "s"} de{" "}
            {transactions.length} total
            {transactions.length === 1 ? "" : "es"}.
          </p>
        </div>

        {activeFilterCount > 0 ? (
          <button
            type="button"
            className="boton-fantasma"
            onClick={onResetFilters}
          >
            Limpiar filtros ({activeFilterCount})
          </button>
        ) : null}
      </div>

      <div className="transactions-view-switch">
        <button
          type="button"
          className={viewMode === "SIMPLE" ? "boton-principal" : "boton-secundario"}
          onClick={() => setViewMode("SIMPLE")}
        >
          Vista simple
        </button>
        <button
          type="button"
          className={viewMode === "AUDIT" ? "boton-principal" : "boton-secundario"}
          onClick={() => setViewMode("AUDIT")}
        >
          Auditoría
        </button>
      </div>

      <div className="transactions-toolbar">
        <label className="transactions-search">
          Buscar
          <input
            className="input-ui"
            value={filters.search}
            placeholder="Descripción, cuenta, categoría, canal o clasificación"
            onChange={(event) =>
              onFiltersChange({ search: event.target.value })
            }
          />
        </label>

        <div className="transactions-filter-grid">
          <label>
            Cuenta
            <select
              className="input-ui"
              value={filters.accountId}
              onChange={(event) =>
                onFiltersChange({
                  accountId: event.target.value,
                })
              }
            >
              <option value={ALL}>Todas</option>
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
              value={filters.categoryId}
              onChange={(event) =>
                onFiltersChange({
                  categoryId: event.target.value,
                })
              }
            >
              <option value={ALL}>Todas</option>
              <option value={WITHOUT_CATEGORY}>Sin categoría</option>
              {categories.map((category) => (
                <option key={category.id} value={category.id}>
                  {getCategoryDisplayName(category)}
                </option>
              ))}
            </select>
          </label>

          <label>
            Tipo
            <select
              className="input-ui"
              value={filters.movementType}
              onChange={(event) =>
                onFiltersChange({
                  movementType: event.target.value as MovementType | typeof ALL,
                })
              }
            >
              <option value={ALL}>Todos</option>
              {movementTypeOptions.map((option) => (
                <option key={option.value} value={option.value}>
                  {option.label}
                </option>
              ))}
            </select>
          </label>

          <label>
            Estado
            <select
              className="input-ui"
              value={filters.status}
              onChange={(event) =>
                onFiltersChange({
                  status: event.target.value as TransactionStatus | typeof ALL,
                })
              }
            >
              <option value={ALL}>Todos</option>
              {transactionStatusOptions.map((option) => (
                <option key={option.value} value={option.value}>
                  {option.label}
                </option>
              ))}
            </select>
          </label>

          <label>
            Origen
            <select
              className="input-ui"
              value={filters.origin}
              onChange={(event) =>
                onFiltersChange({
                  origin: event.target.value as TransactionFilters["origin"],
                })
              }
            >
              <option value={ALL}>Todos</option>
              {transactionOriginOptions.map((option) => (
                <option key={option.value} value={option.value}>
                  {option.label}
                </option>
              ))}
            </select>
          </label>

          <label>
            Clasificación
            <select
              className="input-ui"
              value={filters.classificationStatus}
              onChange={(event) =>
                onFiltersChange({
                  classificationStatus: event.target.value as
                    | TransactionClassificationStatus
                    | typeof ALL,
                })
              }
            >
              <option value={ALL}>Todas</option>
              {classificationStatusOptions.map((option) => (
                <option key={option.value} value={option.value}>
                  {option.label}
                </option>
              ))}
            </select>
          </label>

          <label>
            Canal
            <select
              className="input-ui"
              value={filters.paymentChannel}
              onChange={(event) =>
                onFiltersChange({
                  paymentChannel: event.target.value as
                    | PaymentChannel
                    | typeof ALL,
                })
              }
            >
              <option value={ALL}>Todos</option>
              {paymentChannelOptions.map((option) => (
                <option key={option.value} value={option.value}>
                  {option.label}
                </option>
              ))}
            </select>
          </label>

          <label>
            Source
            <input
              className="input-ui"
              value={filters.source}
              placeholder="BANCO_PROVINCIA, MERCADO_PAGO"
              onChange={(event) => onFiltersChange({ source: event.target.value })}
            />
          </label>

          <label>
            Desde
            <input
              className="input-ui"
              type="date"
              value={filters.dateFrom}
              onChange={(event) => onFiltersChange({ dateFrom: event.target.value })}
            />
          </label>

          <label>
            Hasta
            <input
              className="input-ui"
              type="date"
              value={filters.dateTo}
              onChange={(event) => onFiltersChange({ dateTo: event.target.value })}
            />
          </label>

          <label>
            Monto exacto
            <input
              className="input-ui"
              type="number"
              step="0.01"
              value={filters.exactAmount}
              onChange={(event) =>
                onFiltersChange({ exactAmount: event.target.value })
              }
            />
          </label>

          <label>
            Impacto
            <select
              className="input-ui"
              value={filters.impactKind}
              onChange={(event) =>
                onFiltersChange({
                  impactKind: event.target.value as TransactionFilters["impactKind"],
                })
              }
            >
              <option value={ALL}>Todos</option>
              <option value="NEUTRAL">No cambia el resultado</option>
              <option value="OPERATING_INCOME">Ingreso real</option>
              <option value="CONSUMPTION_EXPENSE">Gasto real</option>
              <option value="INTERNAL_TRANSFER">Transferencia entre cuentas</option>
              <option value="IGNORED">Ignorado</option>
            </select>
          </label>
        </div>

        <div className="transactions-filter-grid">
          <label>
            <input
              type="checkbox"
              checked={filters.onlyDuplicates}
              onChange={(event) =>
                onFiltersChange({ onlyDuplicates: event.target.checked })
              }
            />{" "}
            Solo duplicados
          </label>

          <label>
            <input
              type="checkbox"
              checked={filters.onlyInternalTransfers}
              onChange={(event) =>
                onFiltersChange({
                  onlyInternalTransfers: event.target.checked,
                })
              }
            />{" "}
            Solo transferencias internas
          </label>

          <label>
            <input
              type="checkbox"
              checked={filters.onlyImported}
              onChange={(event) =>
                onFiltersChange({ onlyImported: event.target.checked })
              }
            />{" "}
            Solo importados
          </label>

          <label>
            <input
              type="checkbox"
              checked={filters.onlyWithoutCategory}
              onChange={(event) =>
                onFiltersChange({ onlyWithoutCategory: event.target.checked })
              }
            />{" "}
            Solo sin categoría
          </label>
        </div>
      </div>

      {selectedCount > 0 ? (
        <section className="transactions-bulk-panel">
          <div>
            <strong>
              {selectedCount} seleccionado{selectedCount === 1 ? "" : "s"}
            </strong>
            <p className="muted">
              Aplicá una acción clara a todos los movimientos elegidos.
            </p>
          </div>

          <select
            className="input-ui"
            value={bulkCategoryId}
            onChange={(event) => setBulkCategoryId(event.target.value)}
          >
            <option value="">Elegir categoría</option>
            {categories.map((category) => (
              <option key={category.id} value={category.id}>
                {getCategoryDisplayName(category)}
              </option>
            ))}
          </select>

          <button
            type="button"
            className="boton-secundario"
            disabled={!bulkCategoryId || bulkPending}
            onClick={() => onBulkCategorize(bulkCategoryId)}
          >
            Categorizar
          </button>
          <button
            type="button"
            className="boton-secundario"
            disabled={bulkPending}
            onClick={() => onBulkStatus("CONFIRMED")}
          >
            Confirmar
          </button>
          <button
            type="button"
            className="boton-secundario"
            disabled={bulkPending}
            onClick={() => onBulkStatus("PENDING")}
          >
            Marcar pendiente
          </button>
          <button
            type="button"
            className="boton-secundario"
            disabled={bulkPending}
            onClick={onBulkIgnore}
          >
            Ignorar
          </button>
          <button
            type="button"
            className="boton-secundario"
            disabled={bulkPending || selectedCount !== 2}
            onClick={onBulkLinkTransfer}
          >
            Vincular transferencia
          </button>
          <button
            type="button"
            className="boton-fantasma"
            disabled={bulkPending}
            onClick={() => onSelectionChange([])}
          >
            Limpiar selección
          </button>
        </section>
      ) : null}

      {loading ? (
        <EmptyState
          title="Cargando movimientos"
          message="Estamos consultando los movimientos del período."
        />
      ) : null}

      {isError ? (
        <ErrorState message="No se pudieron cargar los movimientos del período." />
      ) : null}

      {!loading && !isError && transactions.length === 0 ? (
        <EmptyState
          title="Sin movimientos"
          message="Todavía no hay movimientos cargados para este mes."
        />
      ) : null}

      {!loading &&
      !isError &&
      transactions.length > 0 &&
      filteredTransactions.length === 0 ? (
        <EmptyState
          title="Sin resultados"
          message="No hay movimientos que coincidan con los filtros actuales."
        />
      ) : null}

      {filteredTransactions.length > 0 ? (
        <>
          <TransactionTable
            transactions={filteredTransactions}
            accountsById={accountsById}
            categoriesById={categoriesById}
            viewMode={viewMode}
            selectedTransactionIds={selectedTransactionIds}
            onToggleSelection={toggleSelection}
            onToggleVisibleSelection={toggleVisibleSelection}
            updatePending={updatePending}
            deletePending={deletePending}
            updatingTransactionId={updatingTransactionId}
            deletingTransactionId={deletingTransactionId}
            onToggleStatus={onToggleStatus}
            onDelete={onDelete}
          />

          <TransactionMobileList
            transactions={filteredTransactions}
            accountsById={accountsById}
            categoriesById={categoriesById}
            viewMode={viewMode}
            selectedTransactionIds={selectedTransactionIds}
            onToggleSelection={toggleSelection}
            updatePending={updatePending}
            deletePending={deletePending}
            deletingTransactionId={deletingTransactionId}
            onToggleStatus={onToggleStatus}
            onDelete={onDelete}
          />
        </>
      ) : null}
    </section>
  );
}
