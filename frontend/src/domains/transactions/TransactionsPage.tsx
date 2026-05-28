import { useMemo, useState, type FormEvent } from "react";
import { useMutation, useQueryClient } from "@tanstack/react-query";
import { useParams, useSearchParams } from "react-router-dom";
import { getDomainErrorMessage } from "../../api/http";
import {
  linkInternalTransfer,
  previewCreateTransaction,
} from "../../api/transactionsApi";
import { AppLayout } from "../../app/shell/AppShell";
import { queryKeys } from "../../domain/queryKeys";
import { getCompatibleCategories } from "../../domain/transactionRules";
import { formatPeriodLabel, getDefaultDate } from "./utils/transactionUtils";
import { ALL, WITHOUT_CATEGORY, type TransactionFilters, type TransactionForm } from "./types";
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
import { TransactionCreatePreviewPanel } from "./components/TransactionCreatePreviewPanel";
import { TransactionSideSummary } from "./components/TransactionSideSummary";
import { TransactionListPanel } from "./components/TransactionListPanel";
import { Alert } from "../../shared/ui/Alert";
import type { InternalTransferCandidate, TransactionCreatePreview } from "../../domain/types";

export function TransactionsPage() {
  const { profileId = "" } = useParams();
  const [searchParams] = useSearchParams();
  const queryClient = useQueryClient();

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

  const [filters, setFilters] = useState<TransactionFilters>(() =>
    filtersFromSearchParams(searchParams),
  );
  const [createPreview, setCreatePreview] =
    useState<TransactionCreatePreview | null>(null);
  const [createErrorMessage, setCreateErrorMessage] = useState<string | null>(null);
  const [deletionMessage, setDeletionMessage] = useState<string | null>(null);
  const [selectedTransactionIds, setSelectedTransactionIds] = useState<string[]>([]);

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
    bulkCategorizeMutation,
    bulkStatusMutation,
    bulkIgnoreMutation,
  } = useTransactionMutations(profileId, year, month);

  const previewCreateMutation = useMutation({
    mutationFn: (nextForm: TransactionForm) =>
      previewCreateTransaction(profileId, {
        ...nextForm,
        profileId,
        categoryId: nextForm.categoryId || null,
        amount: Number(nextForm.amount),
        origin: "MANUAL",
      }),
    onSuccess: (preview, nextForm) => {
      setCreateErrorMessage(null);

      if (preview.riskLevel === "OK" && preview.canCreateDirectly) {
        createTransactionMutation.mutate(nextForm, {
          onSuccess: resetAfterCreate,
          onError: (error) => setCreateErrorMessage(getDomainErrorMessage(error)),
        });
        return;
      }

      setCreatePreview(preview);
    },
    onError: (error) => {
      setCreateErrorMessage(getDomainErrorMessage(error));
    },
  });

  const linkTransferMutation = useMutation({
    mutationFn: async ({
      createdTransactionId,
      candidate,
    }: {
      createdTransactionId: string;
      candidate: InternalTransferCandidate;
    }) => {
      const draftIsDebit = candidate.debitLeg.id == null;
      const draftIsCredit = candidate.creditLeg.id == null;

      return linkInternalTransfer(profileId, {
        debitTransactionId: draftIsDebit
          ? createdTransactionId
          : candidate.debitLeg.id,
        creditTransactionId: draftIsCredit
          ? createdTransactionId
          : candidate.creditLeg.id,
        toleranceAmount: Number(candidate.amountDifference ?? 0),
        toleranceDays: Math.max(2, Number(candidate.dayDistance ?? 0)),
      });
    },
    onSuccess: async () => {
      await queryClient.invalidateQueries({
        queryKey: queryKeys.transactions(profileId, year, month),
      });
      await queryClient.invalidateQueries({
        queryKey: queryKeys.dashboard(profileId),
      });
      resetAfterCreate();
    },
    onError: (error) => setCreateErrorMessage(getDomainErrorMessage(error)),
  });

  const compatibleCategories = useMemo(
    () =>
      getCompatibleCategories(categories, form.movementType, {
        includeTechnical: false,
      }),
    [categories, form.movementType],
  );

  const canSave =
    Boolean(form.accountId) &&
    form.amount > 0 &&
    Boolean(form.realDate) &&
    Boolean(form.budgetDate) &&
    !createTransactionMutation.isPending &&
    !previewCreateMutation.isPending;

  const periodLabel = formatPeriodLabel(year, month);

  const hasAccounts = accounts.length > 0;
  const hasCategories = categories.length > 0;

  const updateForm = (patch: Partial<TransactionForm>) => {
    setForm((current) => ({
      ...current,
      ...patch,
    }));
    setCreatePreview(null);
    setCreateErrorMessage(null);
  };

  const resetFilters = () => {
    setFilters(emptyFilters());
  };

  const resetAfterCreate = () => {
    setForm((current) => ({
      ...current,
      amount: 0,
      description: "",
      status: "CONFIRMED",
    }));
    setCreatePreview(null);
    setCreateErrorMessage(null);
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

    previewCreateMutation.mutate(form);
  };

  const createFromPreview = (nextForm: TransactionForm) => {
    createTransactionMutation.mutate(nextForm, {
      onSuccess: resetAfterCreate,
      onError: (error) => setCreateErrorMessage(getDomainErrorMessage(error)),
    });
  };

  const handleCreateAnyway = () => createFromPreview(form);

  const handleMarkPending = () =>
    createFromPreview({
      ...form,
      status: "PENDING",
    });

  const handleLinkTransfer = async (candidate: InternalTransferCandidate) => {
    try {
      const created = await createTransactionMutation.mutateAsync({
        ...form,
        status: form.categoryId ? form.status || "CONFIRMED" : "PENDING",
      });

      linkTransferMutation.mutate({
        createdTransactionId: created.id,
        candidate,
      });
    } catch (error) {
      setCreateErrorMessage(getDomainErrorMessage(error));
    }
  };

  const contextualBanner = getContextualBanner(filters);

  const deletionSuccessMessage = (mode: "PHYSICAL_DELETE" | "SOFT_IGNORE") =>
    mode === "PHYSICAL_DELETE"
      ? "Movimiento eliminado."
      : "Movimiento ignorado para preservar trazabilidad.";

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

        {deletionMessage ? (
          <Alert tone="success">{deletionMessage}</Alert>
        ) : null}

        {contextualBanner ? (
          <Alert tone="info">
            {contextualBanner}{" "}
            <button type="button" className="boton-fantasma" onClick={resetFilters}>
              Limpiar vista
            </button>
          </Alert>
        ) : null}

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
            pending={
              createTransactionMutation.isPending ||
              linkTransferMutation.isPending
            }
            previewPending={previewCreateMutation.isPending}
            isError={Boolean(createErrorMessage) || createTransactionMutation.isError}
            errorMessage={
              createErrorMessage ??
              (createTransactionMutation.isError
                ? getDomainErrorMessage(createTransactionMutation.error)
                : null)
            }
            onFormChange={updateForm}
            onSubmit={handleSubmit}
          />

          <TransactionSideSummary
            visibleCount={filteredTransactions.length}
            filteredTotal={filteredTotal}
            totals={totals}
          />
        </section>

        {createPreview ? (
          <TransactionCreatePreviewPanel
            profileId={profileId}
            preview={createPreview}
            form={form}
            categoriesById={categoriesById}
            saving={
              createTransactionMutation.isPending ||
              linkTransferMutation.isPending
            }
            onCreateAnyway={handleCreateAnyway}
            onMarkPending={handleMarkPending}
            onLinkTransfer={handleLinkTransfer}
            onChangeCategory={(categoryId) => updateForm({ categoryId })}
            onCancel={() => setCreatePreview(null)}
          />
        ) : null}

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
          deletingTransactionId={deleteTransactionMutation.variables?.id}
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
          onDelete={(transaction) =>
            deleteTransactionMutation.mutate(transaction, {
              onSuccess: (response) =>
                setDeletionMessage(deletionSuccessMessage(response.mode)),
            })
          }
          selectedTransactionIds={selectedTransactionIds}
          onSelectionChange={setSelectedTransactionIds}
          bulkPending={
            bulkCategorizeMutation.isPending ||
            bulkStatusMutation.isPending ||
            bulkIgnoreMutation.isPending
          }
          onBulkCategorize={(categoryId) =>
            bulkCategorizeMutation.mutate(
              { transactionIds: selectedTransactionIds, categoryId },
              { onSuccess: () => setSelectedTransactionIds([]) },
            )
          }
          onBulkStatus={(status) =>
            bulkStatusMutation.mutate(
              {
                transactionIds: selectedTransactionIds,
                status,
                reason: "Actualizado desde movimientos",
              },
              { onSuccess: () => setSelectedTransactionIds([]) },
            )
          }
          onBulkIgnore={() =>
            bulkIgnoreMutation.mutate(
              {
                transactionIds: selectedTransactionIds,
                reason: "Ignorado desde movimientos",
              },
              { onSuccess: () => setSelectedTransactionIds([]) },
            )
          }
          onBulkLinkTransfer={() => {
            const selected = transactions.filter((transaction) =>
              selectedTransactionIds.includes(transaction.id),
            );
            const debit =
              selected.find((transaction) =>
                ["EXPENSE", "SAVING", "TRANSFER"].includes(transaction.movementType),
              ) ?? selected[0];
            const credit =
              selected.find((transaction) => transaction.id !== debit?.id) ??
              selected[1];

            if (!debit || !credit) return;

            linkInternalTransfer(profileId, {
              debitTransactionId: debit.id,
              creditTransactionId: credit.id,
              toleranceAmount: 0,
              toleranceDays: 2,
            }).then(() => {
              setSelectedTransactionIds([]);
              queryClient.invalidateQueries({
                queryKey: queryKeys.transactions(profileId, year, month),
              });
            });
          }}
        />
      </div>
    </AppLayout>
  );
}

function emptyFilters(): TransactionFilters {
  return {
    search: "",
    accountId: ALL,
    categoryId: ALL,
    movementType: ALL,
    status: ALL,
    classificationStatus: ALL,
    origin: ALL,
    paymentChannel: ALL,
    source: "",
    dateFrom: "",
    dateTo: "",
    exactAmount: "",
    onlyDuplicates: false,
    onlyInternalTransfers: false,
    onlyWithoutCategory: false,
    onlyImported: false,
    impactKind: ALL,
  };
}

function filtersFromSearchParams(params: URLSearchParams): TransactionFilters {
  const next = emptyFilters();

  next.search = params.get("search") ?? "";
  next.accountId = params.get("accountId") ?? ALL;
  next.categoryId = params.get("categoryId") ?? ALL;
  next.movementType = (params.get("movementType") as TransactionFilters["movementType"]) ?? ALL;
  next.status = (params.get("status") as TransactionFilters["status"]) ?? ALL;
  next.classificationStatus =
    (params.get("classificationStatus") as TransactionFilters["classificationStatus"]) ?? ALL;
  next.origin = (params.get("origin") as TransactionFilters["origin"]) ?? ALL;
  next.paymentChannel =
    (params.get("paymentChannel") as TransactionFilters["paymentChannel"]) ?? ALL;
  next.source = params.get("source") ?? "";
  next.dateFrom = params.get("dateFrom") ?? "";
  next.dateTo = params.get("dateTo") ?? "";
  next.exactAmount = params.get("exactAmount") ?? "";
  next.onlyDuplicates = params.get("onlyDuplicates") === "true";
  next.onlyInternalTransfers = params.get("onlyInternalTransfers") === "true";
  next.onlyWithoutCategory = params.get("onlyWithoutCategory") === "true";
  next.onlyImported =
    params.get("onlyImported") === "true" || params.get("origin") === "IMPORT";
  next.impactKind =
    (params.get("impact") as TransactionFilters["impactKind"]) ?? ALL;

  if (next.onlyWithoutCategory) {
    next.categoryId = WITHOUT_CATEGORY;
  }

  return next;
}

function getContextualBanner(filters: TransactionFilters) {
  if (filters.onlyWithoutCategory || filters.categoryId === WITHOUT_CATEGORY) {
    return "Estás viendo movimientos sin categoría.";
  }

  if (filters.onlyInternalTransfers) {
    return "Estás revisando posibles transferencias internas.";
  }

  if (filters.onlyDuplicates) {
    return "Estás resolviendo movimientos repetidos.";
  }

  if (filters.status === "PENDING") {
    return "Estás confirmando movimientos pendientes.";
  }

  if (filters.origin === "IMPORT" || filters.onlyImported) {
    return "Estás revisando movimientos que vinieron de una importación.";
  }

  if (filters.impactKind === "NEUTRAL") {
    return "Estás auditando movimientos que se ven en cuentas pero no cambian el resultado del mes.";
  }

  return null;
}
