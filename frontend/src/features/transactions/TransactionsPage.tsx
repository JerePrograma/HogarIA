import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { useMemo, useState } from "react";
import { Link, useParams } from "react-router-dom";
import { listAccounts } from "../../api/accountsApi";
import { listCategories } from "../../api/categoriesApi";
import {
  createTransaction,
  deleteTransaction,
  listTransactions,
  updateTransaction,
} from "../../api/transactionsApi";
import { AppLayout } from "../../components/layout/AppLayout";
import { EmptyState } from "../../components/ui/EmptyState";
import { ErrorState } from "../../components/ui/ErrorState";
import { MetricCard } from "../../components/ui/MetricCard";
import { MonthSelector } from "../../components/ui/MonthSelector";
import { StatusBadge } from "../../components/ui/StatusBadge";
import {
  labelOrValue,
  movementTypeLabels,
  transactionStatusLabels,
} from "../../domain/financeLabels";
import {
  movementTypeOptions,
  transactionStatusOptions,
} from "../../domain/financeOptions";
import { formatMoney } from "../../domain/formatters";
import type {
  Account,
  Category,
  MoneyTransaction,
  MovementType,
  TransactionStatus,
} from "../../domain/types";

interface TransactionForm {
  accountId: string;
  categoryId: string;
  movementType: MovementType;
  realDate: string;
  budgetDate: string;
  amount: number;
  currency: string;
  description: string;
  status: TransactionStatus;
}

type AllOption = "ALL";

interface TransactionFilters {
  search: string;
  accountId: string | AllOption;
  categoryId: string | AllOption;
  movementType: MovementType | AllOption;
  status: TransactionStatus | AllOption;
}

const ALL: AllOption = "ALL";

const currencyOptions = ["ARS", "USD"];

function getDefaultDate(year: number, month: number) {
  return `${year}-${String(month).padStart(2, "0")}-01`;
}

function getPeriodDate(year: number, month: number) {
  return new Date(year, month - 1, 1);
}

function shiftPeriod(year: number, month: number, delta: number) {
  const date = new Date(year, month - 1 + delta, 1);

  return {
    year: date.getFullYear(),
    month: date.getMonth() + 1,
  };
}

function formatPeriodLabel(year: number, month: number) {
  return new Intl.DateTimeFormat("es-AR", {
    month: "long",
    year: "numeric",
  }).format(getPeriodDate(year, month));
}

function formatDate(value: string) {
  if (!value) return "-";

  return new Intl.DateTimeFormat("es-AR", {
    day: "2-digit",
    month: "2-digit",
    year: "numeric",
  }).format(new Date(`${value}T00:00:00`));
}

function getStatusTone(status: TransactionStatus): "ok" | "watch" | "critical" {
  if (status === "CONFIRMED") return "ok";
  if (status === "PENDING") return "watch";
  return "critical";
}

function getMovementTone(
  type: MovementType,
): "ok" | "watch" | "risk" | "critical" | "neutral" {
  if (type === "INCOME") return "ok";
  if (type === "SAVING") return "ok";
  if (type === "TRANSFER") return "neutral";
  if (type === "ADJUSTMENT") return "watch";
  return "critical";
}

function toTransactionUpdatePayload(transaction: MoneyTransaction) {
  return {
    accountId: transaction.accountId,
    categoryId: transaction.categoryId,
    movementType: transaction.movementType,
    realDate: transaction.realDate,
    budgetDate: transaction.budgetDate,
    amount: transaction.amount,
    currency: transaction.currency,
    description: transaction.description ?? "",
    status: transaction.status === "CONFIRMED" ? "PENDING" : "CONFIRMED",
  };
}

function normalizeSearch(value: string) {
  return value.trim().toLowerCase();
}

function FilterChip({
  active,
  children,
  onClick,
}: {
  active: boolean;
  children: React.ReactNode;
  onClick: () => void;
}) {
  return (
    <button
      type="button"
      className={`tx-filter-chip ${active ? "active" : ""}`}
      onClick={onClick}
    >
      {children}
    </button>
  );
}

export function TransactionsPage() {
  const { profileId = "" } = useParams();
  const queryClient = useQueryClient();

  const today = new Date();
  const initialYear = today.getFullYear();
  const initialMonth = today.getMonth() + 1;

  const [year, setYear] = useState(initialYear);
  const [month, setMonth] = useState(initialMonth);

  const [form, setForm] = useState<TransactionForm>({
    accountId: "",
    categoryId: "",
    movementType: "EXPENSE",
    realDate: getDefaultDate(initialYear, initialMonth),
    budgetDate: getDefaultDate(initialYear, initialMonth),
    amount: 0,
    currency: "ARS",
    description: "",
    status: "CONFIRMED",
  });

  const [filters, setFilters] = useState<TransactionFilters>({
    search: "",
    accountId: ALL,
    categoryId: ALL,
    movementType: ALL,
    status: ALL,
  });

  const accountsQuery = useQuery<Account[]>({
    queryKey: ["accounts", profileId],
    queryFn: () => listAccounts(profileId),
    enabled: Boolean(profileId),
  });

  const categoriesQuery = useQuery<Category[]>({
    queryKey: ["categories", profileId],
    queryFn: () => listCategories(profileId, true),
    enabled: Boolean(profileId),
  });

  const transactionsQuery = useQuery<MoneyTransaction[]>({
    queryKey: ["tx", profileId, year, month],
    queryFn: () => listTransactions(profileId, year, month),
    enabled: Boolean(profileId),
  });

  const accounts = accountsQuery.data ?? [];
  const categories = categoriesQuery.data ?? [];
  const transactions = transactionsQuery.data ?? [];

  const accountsById = useMemo(
    () => new Map(accounts.map((account) => [account.id, account])),
    [accounts],
  );

  const categoriesById = useMemo(
    () => new Map(categories.map((category) => [category.id, category])),
    [categories],
  );

  const totals = useMemo(
    () =>
      transactions.reduce(
        (acc, transaction) => {
          const amount = Number(transaction.amount ?? 0);

          if (transaction.status === "IGNORED") {
            acc.ignored += amount;
            acc.ignoredCount += 1;
            return acc;
          }

          if (transaction.status === "PENDING") {
            acc.pendingCount += 1;
          }

          if (transaction.status === "CONFIRMED") {
            acc.confirmedCount += 1;
          }

          if (transaction.movementType === "INCOME") {
            acc.income += amount;
            return acc;
          }

          if (transaction.movementType === "SAVING") {
            acc.saving += amount;
            return acc;
          }

          if (transaction.movementType === "EXPENSE") {
            acc.expenses += amount;
            return acc;
          }

          acc.other += amount;
          return acc;
        },
        {
          income: 0,
          expenses: 0,
          saving: 0,
          ignored: 0,
          other: 0,
          confirmedCount: 0,
          pendingCount: 0,
          ignoredCount: 0,
        },
      ),
    [transactions],
  );

  const filteredTransactions = useMemo(() => {
    const search = normalizeSearch(filters.search);

    return transactions
      .filter((transaction) => {
        const accountName = accountsById.get(transaction.accountId)?.name ?? "";
        const categoryName =
          categoriesById.get(transaction.categoryId)?.name ?? "";

        const matchesSearch =
          !search ||
          normalizeSearch(transaction.description ?? "").includes(search) ||
          normalizeSearch(accountName).includes(search) ||
          normalizeSearch(categoryName).includes(search) ||
          normalizeSearch(transaction.currency).includes(search);

        const matchesAccount =
          filters.accountId === ALL ||
          transaction.accountId === filters.accountId;

        const matchesCategory =
          filters.categoryId === ALL ||
          transaction.categoryId === filters.categoryId;

        const matchesMovement =
          filters.movementType === ALL ||
          transaction.movementType === filters.movementType;

        const matchesStatus =
          filters.status === ALL || transaction.status === filters.status;

        return (
          matchesSearch &&
          matchesAccount &&
          matchesCategory &&
          matchesMovement &&
          matchesStatus
        );
      })
      .sort((a, b) => {
        const byDate = b.realDate.localeCompare(a.realDate);
        if (byDate !== 0) return byDate;

        return (b.createdAt ?? "").localeCompare(a.createdAt ?? "");
      });
  }, [accountsById, categoriesById, filters, transactions]);

  const filteredTotal = useMemo(
    () =>
      filteredTransactions.reduce((acc, transaction) => {
        if (transaction.status === "IGNORED") return acc;

        if (transaction.movementType === "INCOME") {
          return acc + Number(transaction.amount ?? 0);
        }

        if (
          transaction.movementType === "EXPENSE" ||
          transaction.movementType === "SAVING"
        ) {
          return acc - Number(transaction.amount ?? 0);
        }

        return acc;
      }, 0),
    [filteredTransactions],
  );

  const createTransactionMutation = useMutation({
    mutationFn: () =>
      createTransaction({
        ...form,
        profileId,
        amount: Number(form.amount),
        origin: "MANUAL",
      }),
    onSuccess: () => {
      queryClient.invalidateQueries({
        queryKey: ["tx", profileId, year, month],
      });
      queryClient.invalidateQueries({
        queryKey: ["budget-comp", profileId, year, month],
      });

      setForm((current) => ({
        ...current,
        amount: 0,
        description: "",
      }));
    },
  });

  const updateTransactionMutation = useMutation({
    mutationFn: (transaction: MoneyTransaction) =>
      updateTransaction(
        transaction.id,
        toTransactionUpdatePayload(transaction),
      ),
    onSuccess: () => {
      queryClient.invalidateQueries({
        queryKey: ["tx", profileId, year, month],
      });
      queryClient.invalidateQueries({
        queryKey: ["budget-comp", profileId, year, month],
      });
    },
  });

  const deleteTransactionMutation = useMutation({
    mutationFn: (id: string) => deleteTransaction(id),
    onSuccess: () => {
      queryClient.invalidateQueries({
        queryKey: ["tx", profileId, year, month],
      });
      queryClient.invalidateQueries({
        queryKey: ["budget-comp", profileId, year, month],
      });
    },
  });

  const canSave =
    Boolean(form.accountId) &&
    Boolean(form.categoryId) &&
    form.amount > 0 &&
    Boolean(form.realDate) &&
    Boolean(form.budgetDate) &&
    !createTransactionMutation.isPending;

  const balance = totals.income - totals.expenses - totals.saving;

  const periodLabel = formatPeriodLabel(year, month);

  const activeFilterCount = [
    filters.search,
    filters.accountId !== ALL,
    filters.categoryId !== ALL,
    filters.movementType !== ALL,
    filters.status !== ALL,
  ].filter(Boolean).length;

  const hasAccounts = accounts.length > 0;
  const hasCategories = categories.length > 0;

  const resetFilters = () => {
    setFilters({
      search: "",
      accountId: ALL,
      categoryId: ALL,
      movementType: ALL,
      status: ALL,
    });
  };

  const handlePeriodChange = (nextYear: number, nextMonth: number) => {
    setYear(nextYear);
    setMonth(nextMonth);

    const nextDefaultDate = getDefaultDate(nextYear, nextMonth);

    setForm((current) => ({
      ...current,
      realDate: nextDefaultDate,
      budgetDate: nextDefaultDate,
    }));
  };

  const handleShiftPeriod = (delta: number) => {
    const next = shiftPeriod(year, month, delta);
    handlePeriodChange(next.year, next.month);
  };

  const handleCurrentPeriod = () => {
    handlePeriodChange(initialYear, initialMonth);
  };

  const handleSubmit = (event: React.FormEvent<HTMLFormElement>) => {
    event.preventDefault();

    if (!canSave) return;

    createTransactionMutation.mutate();
  };

  return (
    <AppLayout>
      <div className="page-stack transactions-page">
        <section className="page-header transactions-hero">
          <div className="transactions-hero-copy">
            <p className="eyebrow">Gestión diaria</p>
            <h1>Movimientos</h1>
            <p className="muted">
              Registrá ingresos, gastos, ahorros y ajustes. Esta información
              alimenta el presupuesto, el dashboard y la comparación mensual.
            </p>

            <div className="transactions-period-summary">
              <span className="badge badge-info">{periodLabel}</span>
              <span className="badge badge-muted">
                {transactions.length} movimiento
                {transactions.length === 1 ? "" : "s"}
              </span>
              <span className="badge badge-ok">
                {totals.confirmedCount} confirmado(s)
              </span>
              <span className="badge badge-warning">
                {totals.pendingCount} pendiente(s)
              </span>
              {totals.ignoredCount > 0 ? (
                <span className="badge badge-muted">
                  {totals.ignoredCount} ignorado(s)
                </span>
              ) : null}
            </div>
          </div>

          <div className="transactions-hero-controls">
            <div className="transactions-actions">
              <Link
                className="boton-secundario"
                to={`/profiles/${profileId}/transactions/import`}
              >
                Importar
              </Link>

              <Link
                className="boton-secundario"
                to={`/profiles/${profileId}/transactions/recategorize`}
              >
                Recategorizar
              </Link>
            </div>

            <div className="transactions-period-card">
              <div className="transactions-period-card-header">
                <span className="label-ui">Período operativo</span>
                <strong>{periodLabel}</strong>
              </div>

              <div className="transactions-period-buttons">
                <button
                  type="button"
                  className="boton-fantasma"
                  onClick={() => handleShiftPeriod(-1)}
                >
                  ← Anterior
                </button>

                <button
                  type="button"
                  className="boton-fantasma"
                  onClick={handleCurrentPeriod}
                >
                  Hoy
                </button>

                <button
                  type="button"
                  className="boton-fantasma"
                  onClick={() => handleShiftPeriod(1)}
                >
                  Siguiente →
                </button>
              </div>

              <MonthSelector
                year={year}
                month={month}
                onYearChange={(nextYear) => handlePeriodChange(nextYear, month)}
                onMonthChange={(nextMonth) =>
                  handlePeriodChange(year, nextMonth)
                }
              />
            </div>
          </div>
        </section>

        <section className="metric-grid">
          <MetricCard
            title="Ingresos"
            value={formatMoney(totals.income)}
            helper="Entradas no ignoradas del período."
            tone="success"
          />

          <MetricCard
            title="Gastos"
            value={formatMoney(totals.expenses)}
            helper="Egresos registrados para el mes."
            tone="danger"
          />

          <MetricCard
            title="Ahorro"
            value={formatMoney(totals.saving)}
            helper="Movimientos destinados a ahorro."
            tone="info"
          />

          <MetricCard
            title="Balance operativo"
            value={formatMoney(balance)}
            helper="Ingresos menos gastos y ahorro."
            tone={balance >= 0 ? "success" : "danger"}
          />
        </section>

        {!accountsQuery.isLoading && !hasAccounts ? (
          <section className="mensaje-warning transactions-setup-warning">
            <strong>No hay cuentas cargadas.</strong>
            <span>
              Necesitás al menos una cuenta para registrar movimientos.
            </span>
            <Link
              className="boton-secundario"
              to={`/profiles/${profileId}/accounts`}
            >
              Crear cuenta
            </Link>
          </section>
        ) : null}

        {!categoriesQuery.isLoading && !hasCategories ? (
          <section className="mensaje-warning transactions-setup-warning">
            <strong>No hay categorías cargadas.</strong>
            <span>
              Necesitás al menos una categoría para clasificar movimientos.
            </span>
            <Link
              className="boton-secundario"
              to={`/profiles/${profileId}/categories`}
            >
              Crear categoría
            </Link>
          </section>
        ) : null}

        <section className="transactions-main-grid">
          <form
            className="panel transactions-form-panel"
            onSubmit={handleSubmit}
          >
            <div className="section-title">
              <div>
                <p className="eyebrow">Alta rápida</p>
                <h2>Cargar movimiento</h2>
                <p className="muted">
                  Mantené cuenta, categoría y tipo para cargar varios
                  movimientos seguidos.
                </p>
              </div>
            </div>

            <div className="transactions-type-picker">
              {movementTypeOptions.map((option) => (
                <FilterChip
                  key={option.value}
                  active={form.movementType === option.value}
                  onClick={() =>
                    setForm({
                      ...form,
                      movementType: option.value as MovementType,
                    })
                  }
                >
                  {option.label}
                </FilterChip>
              ))}
            </div>

            <div className="form-grid">
              <label>
                Cuenta
                <select
                  className="input-ui"
                  value={form.accountId}
                  onChange={(event) =>
                    setForm({ ...form, accountId: event.target.value })
                  }
                >
                  <option value="">Seleccionar cuenta</option>
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
                  <option value="">Seleccionar categoría</option>
                  {categories.map((category) => (
                    <option key={category.id} value={category.id}>
                      {category.name}
                    </option>
                  ))}
                </select>
              </label>

              <label>
                Estado
                <select
                  className="input-ui"
                  value={form.status}
                  onChange={(event) =>
                    setForm({
                      ...form,
                      status: event.target.value as TransactionStatus,
                    })
                  }
                >
                  {transactionStatusOptions.map((option) => (
                    <option key={option.value} value={option.value}>
                      {option.label}
                    </option>
                  ))}
                </select>
              </label>

              <label>
                Moneda
                <select
                  className="input-ui"
                  value={form.currency}
                  onChange={(event) =>
                    setForm({ ...form, currency: event.target.value })
                  }
                >
                  {currencyOptions.map((currency) => (
                    <option key={currency} value={currency}>
                      {currency}
                    </option>
                  ))}
                </select>
              </label>

              <label>
                Fecha real
                <input
                  className="input-ui"
                  type="date"
                  value={form.realDate}
                  onChange={(event) =>
                    setForm({ ...form, realDate: event.target.value })
                  }
                />
              </label>

              <label>
                Fecha de presupuesto
                <input
                  className="input-ui"
                  type="date"
                  value={form.budgetDate}
                  onChange={(event) =>
                    setForm({ ...form, budgetDate: event.target.value })
                  }
                />
              </label>

              <label>
                Monto
                <input
                  className="input-ui"
                  type="number"
                  min={0}
                  step="0.01"
                  value={form.amount === 0 ? "" : form.amount}
                  placeholder="0,00"
                  onChange={(event) =>
                    setForm({
                      ...form,
                      amount: Number(event.target.value),
                    })
                  }
                />
              </label>

              <label className="form-field-wide">
                Descripción
                <input
                  className="input-ui"
                  value={form.description}
                  placeholder="Ej: supermercado, sueldo, alquiler"
                  onChange={(event) =>
                    setForm({ ...form, description: event.target.value })
                  }
                />
              </label>
            </div>

            <div className="transactions-form-preview">
              <span className="label-ui">Vista previa</span>
              <strong>
                {form.amount > 0
                  ? formatMoney(form.amount, form.currency)
                  : "$ 0,00"}
              </strong>
              <p className="muted">
                {labelOrValue(movementTypeLabels, form.movementType)} ·{" "}
                {labelOrValue(transactionStatusLabels, form.status)}
              </p>
            </div>

            <div className="form-actions">
              <button
                type="submit"
                className="boton-principal"
                disabled={!canSave}
              >
                {createTransactionMutation.isPending
                  ? "Guardando..."
                  : "Guardar movimiento"}
              </button>

              {!canSave ? (
                <span className="muted">
                  Completá cuenta, categoría, monto y fechas para guardar.
                </span>
              ) : null}
            </div>

            {createTransactionMutation.isError ? (
              <p className="mensaje-error">
                No se pudo guardar el movimiento. Revisá los datos ingresados.
              </p>
            ) : null}
          </form>

          <aside className="panel-soft transactions-side-panel">
            <p className="eyebrow">Lectura del período</p>
            <h3>Resumen operativo</h3>

            <div className="transactions-side-list">
              <div>
                <span>Movimientos visibles</span>
                <strong>{filteredTransactions.length}</strong>
              </div>

              <div>
                <span>Resultado filtrado</span>
                <strong>{formatMoney(filteredTotal)}</strong>
              </div>

              <div>
                <span>Ignorados</span>
                <strong>{formatMoney(totals.ignored)}</strong>
              </div>
            </div>

            <p className="muted">
              El balance principal usa todo el período. El resultado filtrado
              responde a la búsqueda y filtros activos.
            </p>
          </aside>
        </section>

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
                onClick={resetFilters}
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
                placeholder="Descripción, cuenta, categoría o moneda"
                onChange={(event) =>
                  setFilters({
                    ...filters,
                    search: event.target.value,
                  })
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
                    setFilters({
                      ...filters,
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
                    setFilters({
                      ...filters,
                      categoryId: event.target.value,
                    })
                  }
                >
                  <option value={ALL}>Todas</option>
                  {categories.map((category) => (
                    <option key={category.id} value={category.id}>
                      {category.name}
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
                    setFilters({
                      ...filters,
                      movementType: event.target.value as
                        | MovementType
                        | AllOption,
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
                    setFilters({
                      ...filters,
                      status: event.target.value as
                        | TransactionStatus
                        | AllOption,
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
            </div>
          </div>

          {transactionsQuery.isLoading ? (
            <EmptyState
              title="Cargando movimientos"
              message="Estamos consultando los movimientos del período."
            />
          ) : null}

          {transactionsQuery.isError ? (
            <ErrorState message="No se pudieron cargar los movimientos del período." />
          ) : null}

          {!transactionsQuery.isLoading &&
          !transactionsQuery.isError &&
          transactions.length === 0 ? (
            <EmptyState
              title="Sin movimientos"
              message="Todavía no hay movimientos cargados para este mes."
            />
          ) : null}

          {!transactionsQuery.isLoading &&
          !transactionsQuery.isError &&
          transactions.length > 0 &&
          filteredTransactions.length === 0 ? (
            <EmptyState
              title="Sin resultados"
              message="No hay movimientos que coincidan con los filtros actuales."
            />
          ) : null}

          {filteredTransactions.length > 0 ? (
            <>
              <div className="tabla-ui transactions-table">
                <table className="table-compact">
                  <thead>
                    <tr>
                      <th>Fecha</th>
                      <th>Movimiento</th>
                      <th>Cuenta</th>
                      <th>Categoría</th>
                      <th className="amount-cell">Monto</th>
                      <th>Estado</th>
                      <th>Acciones</th>
                    </tr>
                  </thead>

                  <tbody>
                    {filteredTransactions.map((transaction) => {
                      const accountName =
                        accountsById.get(transaction.accountId)?.name ??
                        "Cuenta no encontrada";

                      const categoryName =
                        categoriesById.get(transaction.categoryId)?.name ??
                        "Categoría no encontrada";

                      const isUpdating =
                        updateTransactionMutation.isPending &&
                        updateTransactionMutation.variables?.id ===
                          transaction.id;

                      const isDeleting =
                        deleteTransactionMutation.isPending &&
                        deleteTransactionMutation.variables === transaction.id;

                      return (
                        <tr key={transaction.id}>
                          <td>
                            <strong>{formatDate(transaction.realDate)}</strong>
                            <br />
                            <span className="muted">
                              Presupuesto: {formatDate(transaction.budgetDate)}
                            </span>
                          </td>

                          <td>
                            <StatusBadge
                              tone={getMovementTone(transaction.movementType)}
                              label={labelOrValue(
                                movementTypeLabels,
                                transaction.movementType,
                              )}
                            />

                            <p className="compact-muted">
                              {transaction.description || "Sin descripción"}
                            </p>
                          </td>

                          <td>{accountName}</td>

                          <td>{categoryName}</td>

                          <td className="amount-cell">
                            {formatMoney(
                              transaction.amount,
                              transaction.currency,
                            )}
                          </td>

                          <td>
                            <StatusBadge
                              tone={getStatusTone(transaction.status)}
                              label={labelOrValue(
                                transactionStatusLabels,
                                transaction.status,
                              )}
                            />
                          </td>

                          <td>
                            <div className="row-actions transactions-row-actions">
                              <button
                                type="button"
                                className="boton-secundario"
                                disabled={updateTransactionMutation.isPending}
                                onClick={() =>
                                  updateTransactionMutation.mutate(transaction)
                                }
                              >
                                {isUpdating
                                  ? "Actualizando..."
                                  : transaction.status === "CONFIRMED"
                                    ? "Pasar a pendiente"
                                    : "Confirmar"}
                              </button>

                              <button
                                type="button"
                                className="boton-danger"
                                disabled={deleteTransactionMutation.isPending}
                                onClick={() =>
                                  window.confirm(
                                    "¿Eliminar este movimiento?",
                                  ) &&
                                  deleteTransactionMutation.mutate(
                                    transaction.id,
                                  )
                                }
                              >
                                {isDeleting ? "Eliminando..." : "Eliminar"}
                              </button>
                            </div>
                          </td>
                        </tr>
                      );
                    })}
                  </tbody>
                </table>
              </div>

              <div className="transactions-mobile-list">
                {filteredTransactions.map((transaction) => {
                  const accountName =
                    accountsById.get(transaction.accountId)?.name ??
                    "Cuenta no encontrada";

                  const categoryName =
                    categoriesById.get(transaction.categoryId)?.name ??
                    "Categoría no encontrada";

                  return (
                    <article
                      key={transaction.id}
                      className="transactions-mobile-card"
                    >
                      <header>
                        <div>
                          <strong>
                            {transaction.description || "Sin descripción"}
                          </strong>
                          <p className="muted">
                            {formatDate(transaction.realDate)} · Presupuesto:{" "}
                            {formatDate(transaction.budgetDate)}
                          </p>
                        </div>

                        <span className="transactions-mobile-amount">
                          {formatMoney(
                            transaction.amount,
                            transaction.currency,
                          )}
                        </span>
                      </header>

                      <div className="transactions-mobile-badges">
                        <StatusBadge
                          tone={getMovementTone(transaction.movementType)}
                          label={labelOrValue(
                            movementTypeLabels,
                            transaction.movementType,
                          )}
                        />

                        <StatusBadge
                          tone={getStatusTone(transaction.status)}
                          label={labelOrValue(
                            transactionStatusLabels,
                            transaction.status,
                          )}
                        />
                      </div>

                      <dl className="transactions-mobile-details">
                        <div>
                          <dt>Cuenta</dt>
                          <dd>{accountName}</dd>
                        </div>

                        <div>
                          <dt>Categoría</dt>
                          <dd>{categoryName}</dd>
                        </div>
                      </dl>

                      <div className="row-actions">
                        <button
                          type="button"
                          className="boton-secundario"
                          disabled={updateTransactionMutation.isPending}
                          onClick={() =>
                            updateTransactionMutation.mutate(transaction)
                          }
                        >
                          {transaction.status === "CONFIRMED"
                            ? "Pasar a pendiente"
                            : "Confirmar"}
                        </button>

                        <button
                          type="button"
                          className="boton-danger"
                          disabled={deleteTransactionMutation.isPending}
                          onClick={() =>
                            window.confirm("¿Eliminar este movimiento?") &&
                            deleteTransactionMutation.mutate(transaction.id)
                          }
                        >
                          Eliminar
                        </button>
                      </div>
                    </article>
                  );
                })}
              </div>
            </>
          ) : null}
        </section>
      </div>
    </AppLayout>
  );
}
