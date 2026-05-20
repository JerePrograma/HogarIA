import type {
  Account,
  Category,
  MoneyTransaction,
  MovementType,
  TransactionClassificationStatus,
  TransactionStatus,
} from "../../../domain/types";
import {
  classificationStatusOptions,
  movementTypeOptions,
  transactionStatusOptions,
} from "../../../domain/financeOptions";
import { EmptyState } from "../../../components/ui/EmptyState";
import { ErrorState } from "../../../components/ui/ErrorState";
import { ALL, WITHOUT_CATEGORY, type TransactionFilters } from "../types";
import { TransactionTable } from "./TransactionTable";
import { TransactionMobileList } from "./TransactionMobileList";

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
  onDelete: (transactionId: string) => void;
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
}: Props) {
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
                  {category.technical
                    ? `${category.name} · técnica`
                    : category.name}
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
        </div>
      </div>

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
            updatePending={updatePending}
            deletePending={deletePending}
            onToggleStatus={onToggleStatus}
            onDelete={onDelete}
          />
        </>
      ) : null}
    </section>
  );
}
