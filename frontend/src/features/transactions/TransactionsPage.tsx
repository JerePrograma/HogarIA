import { useMemo, useState, type FormEvent } from "react";
import { useParams } from "react-router-dom";
import { AppLayout } from "../../components/layout/AppLayout";
import { getCompatibleCategories } from "../../domain/transactionRules";
import { formatPeriodLabel, getDefaultDate } from "./utils/transactionUtils";
import { ALL, type TransactionFilters, type TransactionForm } from "./types";
import { useTransactionPeriod } from "./hooks/useTransactionPeriod";
import { useTransactionsData } from "./hooks/useTransactionsData";
import { useTransactionTotals } from "./hooks/useTransactionTotals";
import { useTransactionFilters } from "./hooks/useTransactionFilters";
import { useTransactionMutations } from "./hooks/useTransactionMutations";
import { TransactionHero } from "./components/TransactionHero";
import { TransactionMetrics } from "./components/TransactionMetrics";
import { RealConfirmedPanel } from "./components/RealConfirmedPanel";
import { TransactionSetupWarnings } from "./components/TransactionSetupWarnings";
import { TransactionQuickForm } from "./components/TransactionQuickForm";
import { TransactionSideSummary } from "./components/TransactionSideSummary";
import { TransactionListPanel } from "./components/TransactionListPanel";

export function TransactionsPage() {
  const { profileId = "" } = useParams();

  const {
    year,
    month,
    initialYear,
    initialMonth,
    handlePeriodChange,
    handleShiftPeriod,
    handleCurrentPeriod,
  } = useTransactionPeriod();

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
    classificationStatus: ALL,
  });

  const {
    accountsQuery,
    categoriesQuery,
    transactionsQuery,
    accounts,
    categories,
    transactions,
    accountsById,
    categoriesById,
  } = useTransactionsData(profileId, year, month);

  const totals = useTransactionTotals(transactions);

  const { filteredTransactions, filteredTotal, activeFilterCount } =
    useTransactionFilters(transactions, filters, accountsById, categoriesById);

  const {
    createTransactionMutation,
    updateTransactionMutation,
    deleteTransactionMutation,
  } = useTransactionMutations(profileId, year, month);

  const compatibleCategories = useMemo(
    () =>
      getCompatibleCategories(categories, form.movementType, {
        includeTechnical: false,
      }),
    [categories, form.movementType],
  );

  const canSave =
    Boolean(form.accountId) &&
    Boolean(form.categoryId) &&
    form.amount > 0 &&
    Boolean(form.realDate) &&
    Boolean(form.budgetDate) &&
    !createTransactionMutation.isPending;

  const periodLabel = formatPeriodLabel(year, month);

  const hasAccounts = accounts.length > 0;
  const hasCategories = categories.length > 0;

  const updateForm = (patch: Partial<TransactionForm>) => {
    setForm((current) => ({
      ...current,
      ...patch,
    }));
  };

  const resetFilters = () => {
    setFilters({
      search: "",
      accountId: ALL,
      categoryId: ALL,
      movementType: ALL,
      status: ALL,
      classificationStatus: ALL,
    });
  };

  const updateDefaultDates = (nextDefaultDate: string) => {
    setForm((current) => ({
      ...current,
      realDate: nextDefaultDate,
      budgetDate: nextDefaultDate,
    }));
  };

  const onPeriodChange = (nextYear: number, nextMonth: number) => {
    handlePeriodChange(nextYear, nextMonth, updateDefaultDates);
  };

  const onShiftPeriod = (delta: number) => {
    handleShiftPeriod(delta, updateDefaultDates);
  };

  const onCurrentPeriod = () => {
    handleCurrentPeriod(updateDefaultDates);
  };

  const handleSubmit = (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();

    if (!canSave) return;

    createTransactionMutation.mutate(form, {
      onSuccess: () => {
        setForm((current) => ({
          ...current,
          amount: 0,
          description: "",
        }));
      },
    });
  };

  return (
    <AppLayout>
      <div className="page-stack transactions-page">
        <TransactionHero
          profileId={profileId}
          year={year}
          month={month}
          periodLabel={periodLabel}
          transactionsCount={transactions.length}
          totals={totals}
          onPeriodChange={onPeriodChange}
          onShiftPeriod={onShiftPeriod}
          onCurrentPeriod={onCurrentPeriod}
        />

        <RealConfirmedPanel totals={totals} />

        <TransactionMetrics totals={totals} />

        <TransactionSetupWarnings
          profileId={profileId}
          accountsLoading={accountsQuery.isLoading}
          categoriesLoading={categoriesQuery.isLoading}
          hasAccounts={hasAccounts}
          hasCategories={hasCategories}
        />

        <section className="transactions-main-grid">
          <TransactionQuickForm
            form={form}
            accounts={accounts}
            compatibleCategories={compatibleCategories}
            canSave={canSave}
            pending={createTransactionMutation.isPending}
            isError={createTransactionMutation.isError}
            onFormChange={updateForm}
            onSubmit={handleSubmit}
          />

          <TransactionSideSummary
            visibleCount={filteredTransactions.length}
            filteredTotal={filteredTotal}
            totals={totals}
          />
        </section>

        <TransactionListPanel
          transactions={transactions}
          filteredTransactions={filteredTransactions}
          filters={filters}
          accounts={accounts}
          categories={categories}
          accountsById={accountsById}
          categoriesById={categoriesById}
          activeFilterCount={activeFilterCount}
          loading={transactionsQuery.isLoading}
          isError={transactionsQuery.isError}
          updatePending={updateTransactionMutation.isPending}
          deletePending={deleteTransactionMutation.isPending}
          updatingTransactionId={updateTransactionMutation.variables?.id}
          deletingTransactionId={deleteTransactionMutation.variables}
          onFiltersChange={(patch) =>
            setFilters((current) => ({
              ...current,
              ...patch,
            }))
          }
          onResetFilters={resetFilters}
          onToggleStatus={(transaction) =>
            updateTransactionMutation.mutate(transaction)
          }
          onDelete={(transactionId) =>
            deleteTransactionMutation.mutate(transactionId)
          }
        />
      </div>
    </AppLayout>
  );
}
