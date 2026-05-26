import { useMemo, useState } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { useParams, useSearchParams } from "react-router-dom";
import { listAccounts } from "../../api/accountsApi";
import {
  applyBulkRecategorize,
  previewBulkRecategorize,
  type BulkRecategorizePreviewPayload,
} from "../../api/bulkRecategorizeApi";
import { createCategory, listCategories } from "../../api/categoriesApi";
import { getApiErrorMessage } from "../../api/http";
import { AppLayout } from "../../components/layout/AppLayout";
import { ErrorState } from "../../components/ui/ErrorState";
import { queryKeys } from "../../domain/queryKeys";
import type { CategoryType } from "../../domain/types";
import { RecategorizationCriteriaPanel } from "./components/RecategorizationCriteriaPanel";
import { RecategorizationHero } from "./components/RecategorizationHero";
import { RecategorizationPreviewPanel } from "./components/RecategorizationPreviewPanel";
import { RecategorizationResultPanel } from "./components/RecategorizationResultPanel";
import { RecategorizationSidePanel } from "./components/RecategorizationSidePanel";
import {
  ALL,
  buildReviewActionPatch,
  countByStatus,
  getRecategorizationStatusDescription,
  getRecategorizationStatusTitle,
  inferAutoTargetClassificationReason,
  inferAutoTargetClassificationStatus,
  matchesCandidateFilter,
  parseQueryBoolean,
  parseQueryList,
  type CandidateFilters,
  type ReviewAction,
} from "./recategorizationUtils";

function getInitialForm(
  search: URLSearchParams,
): BulkRecategorizePreviewPayload {
  return {
    accountId: search.get("accountId"),
    from: search.get("from"),
    to: search.get("to"),
    fromCategoryId: search.get("fromCategoryId"),
    onlyWithoutCategory: parseQueryBoolean(search.get("onlyWithoutCategory")),
    targetMode:
      (search.get(
        "targetMode",
      ) as BulkRecategorizePreviewPayload["targetMode"]) ?? "MANUAL",
    toCategoryId: search.get("toCategoryId"),
    movementType:
      (search.get(
        "movementType",
      ) as BulkRecategorizePreviewPayload["movementType"]) ?? null,
    descriptionContains: search.get("descriptionContains"),
    exactAmount: null,
    minAmount: null,
    maxAmount: null,
    onlyImported: parseQueryBoolean(search.get("onlyImported")),
    reviewFilter:
      (search.get(
        "reviewFilter",
      ) as BulkRecategorizePreviewPayload["reviewFilter"]) ?? null,
    targetMovementType:
      (search.get(
        "targetMovementType",
      ) as BulkRecategorizePreviewPayload["targetMovementType"]) ?? null,
    targetStatus:
      (search.get(
        "targetStatus",
      ) as BulkRecategorizePreviewPayload["targetStatus"]) ?? null,
    targetClassificationStatus:
      (search.get(
        "targetClassificationStatus",
      ) as BulkRecategorizePreviewPayload["targetClassificationStatus"]) ??
      null,
    targetClassificationReason: search.get("targetClassificationReason"),
    transactionIds: parseQueryList(search.get("transactionIds")),
    classificationStatus:
      (search.get(
        "classificationStatus",
      ) as BulkRecategorizePreviewPayload["classificationStatus"]) ?? null,
    paymentChannel:
      (search.get(
        "paymentChannel",
      ) as BulkRecategorizePreviewPayload["paymentChannel"]) ?? null,
    source: search.get("source"),
    counterpartyContains: search.get("counterpartyContains"),
  };
}

export function TransactionRecategorizationPage() {
  const { profileId = "" } = useParams();
  const [search] = useSearchParams();
  const qc = useQueryClient();

  const [form, setForm] = useState<BulkRecategorizePreviewPayload>(() =>
    getInitialForm(search),
  );

  const [reviewAction, setReviewAction] =
    useState<ReviewAction>("RECATEGORIZE");

  const [missingCategoryName, setMissingCategoryName] = useState(
    search.get("suggestedCategoryName") ?? "",
  );

  const [candidateFilters, setCandidateFilters] = useState<CandidateFilters>({
    search: "",
    status: ALL,
  });

  const accountsQuery = useQuery({
    queryKey: queryKeys.accounts(profileId),
    queryFn: () => listAccounts(profileId),
    enabled: Boolean(profileId),
  });

  const categoriesQuery = useQuery({
    queryKey: queryKeys.categories(profileId, true),
    queryFn: () => listCategories(profileId, true),
    enabled: Boolean(profileId),
  });

  const accounts = accountsQuery.data ?? [];
  const categories = categoriesQuery.data ?? [];

  const accountsById = useMemo(
    () => new Map(accounts.map((account) => [account.id, account])),
    [accounts],
  );

  const categoriesById = useMemo(
    () => new Map(categories.map((category) => [category.id, category])),
    [categories],
  );

  const selectedAccount = form.accountId
    ? accountsById.get(form.accountId)
    : null;

  const selectedTargetCategory = form.toCategoryId
    ? categoriesById.get(form.toCategoryId)
    : null;

  const isAutoTargetMode = form.targetMode === "AUTO_BY_IMPORT_RULES";

  const previewMutation = useMutation({
    mutationFn: () => previewBulkRecategorize(profileId, form),
    onSuccess: () => {
      setCandidateFilters({
        search: "",
        status: ALL,
      });
    },
  });

  const createCategoryMutation = useMutation({
    mutationFn: () => {
      const inferredType = previewMutation.data?.candidates.find(
        (candidate) =>
          candidate.targetCategoryName === missingCategoryName.trim() &&
          candidate.targetCategoryType,
      )?.targetCategoryType as CategoryType | undefined;

      if (!inferredType) {
        throw new Error("No se pudo inferir el tipo de la categoría sugerida.");
      }

      return createCategory(profileId, {
        name: missingCategoryName.trim(),
        type: inferredType,
        scope: "PERSONAL",
      });
    },
    onSuccess: async (created) => {
      setForm((current) => ({
        ...current,
        toCategoryId: created.id,
      }));

      await qc.invalidateQueries({
        queryKey: queryKeys.categories(profileId, true),
      });
    },
  });

  const candidates = previewMutation.data?.candidates ?? [];

  const readyCandidates = useMemo(
    () => candidates.filter((candidate) => candidate.previewStatus === "READY"),
    [candidates],
  );

  const readyIds = useMemo(
    () => readyCandidates.map((candidate) => candidate.transactionId),
    [readyCandidates],
  );

  const statusCounters = useMemo(() => countByStatus(candidates), [candidates]);

  const visibleCandidates = useMemo(
    () =>
      candidates.filter((candidate) =>
        matchesCandidateFilter(candidate, candidateFilters),
      ),
    [candidateFilters, candidates],
  );

  const applyMutation = useMutation({
    mutationFn: () =>
      applyBulkRecategorize(profileId, {
        targetMode: form.targetMode ?? "MANUAL",
        toCategoryId: isAutoTargetMode ? null : form.toCategoryId,
        targetMovementType: form.targetMovementType,
        targetStatus: form.targetStatus,
        targetClassificationStatus: form.targetClassificationStatus,
        targetClassificationReason: form.targetClassificationReason,
        transactionIds: isAutoTargetMode ? [] : readyIds,
        forceAmbiguous: false,
        updates: isAutoTargetMode
          ? readyCandidates
              .filter((candidate) => candidate.targetCategoryId)
              .map((candidate) => ({
                transactionId: candidate.transactionId,
                targetCategoryId: candidate.targetCategoryId,
                targetMovementType: candidate.targetMovementType,
                targetClassificationStatus:
                  inferAutoTargetClassificationStatus(candidate),
                targetClassificationReason:
                  inferAutoTargetClassificationReason(candidate),
              }))
          : [],
      }),
    onSuccess: async () => {
      await Promise.all([
        qc.invalidateQueries({ queryKey: queryKeys.transactions(profileId) }),
        qc.invalidateQueries({
          queryKey: queryKeys.categories(profileId, true),
        }),
        qc.invalidateQueries({ queryKey: queryKeys.dashboard(profileId) }),
        qc.invalidateQueries({
          queryKey: queryKeys.budgetComparison(profileId),
        }),
      ]);
    },
  });

  const hasAnySearchCriteria = Boolean(
    form.accountId ||
    form.from ||
    form.to ||
    form.fromCategoryId ||
    form.onlyWithoutCategory != null ||
    form.movementType ||
    form.descriptionContains ||
    form.exactAmount != null ||
    form.minAmount != null ||
    form.maxAmount != null ||
    form.onlyImported != null ||
    form.reviewFilter ||
    form.classificationStatus ||
    form.paymentChannel ||
    form.source ||
    form.counterpartyContains ||
    (form.transactionIds?.length ?? 0) > 0,
  );

  const activeCandidateFilterCount = [
    candidateFilters.search,
    candidateFilters.status !== ALL,
  ].filter(Boolean).length;

  const inferredCategoryType = previewMutation.data?.candidates.find(
    (candidate) =>
      candidate.targetCategoryName === missingCategoryName.trim() &&
      candidate.targetCategoryType,
  )?.targetCategoryType;

  const canCreateMissingCategory =
    Boolean(missingCategoryName.trim()) &&
    Boolean(inferredCategoryType) &&
    !createCategoryMutation.isPending;

  const hasNonCategoryTarget = Boolean(
    form.targetMovementType ||
    form.targetStatus ||
    form.targetClassificationStatus ||
    form.targetClassificationReason,
  );

  const canPreview =
    (isAutoTargetMode || Boolean(form.toCategoryId) || hasNonCategoryTarget) &&
    hasAnySearchCriteria &&
    !previewMutation.isPending;

  const canApply = readyIds.length > 0 && !applyMutation.isPending;

  const statusTitle = getRecategorizationStatusTitle({
    hasApplyResult: Boolean(applyMutation.data),
    hasPreview: Boolean(previewMutation.data),
    readyCount: readyIds.length,
    ambiguousCount: previewMutation.data?.ambiguousCount ?? 0,
    needsCategoryCount: statusCounters.NEEDS_CATEGORY ?? 0,
  });

  const statusDescription = getRecategorizationStatusDescription({
    hasAnySearchCriteria,
    readyCount: readyIds.length,
    ambiguousCount: previewMutation.data?.ambiguousCount ?? 0,
    needsCategoryCount: statusCounters.NEEDS_CATEGORY ?? 0,
  });

  const updateForm = (patch: Partial<BulkRecategorizePreviewPayload>) => {
    setForm((current) => ({
      ...current,
      ...patch,
    }));

    previewMutation.reset();
    applyMutation.reset();
  };

  const resetCandidateFilters = () => {
    setCandidateFilters({
      search: "",
      status: ALL,
    });
  };

  const clearCriteria = () => {
    updateForm({
      accountId: null,
      from: null,
      to: null,
      fromCategoryId: null,
      onlyWithoutCategory: null,
      movementType: null,
      descriptionContains: null,
      exactAmount: null,
      minAmount: null,
      maxAmount: null,
      onlyImported: null,
      reviewFilter: null,
      targetMovementType: null,
      targetStatus: null,
      targetClassificationStatus: null,
      targetClassificationReason: null,
      transactionIds: null,
      classificationStatus: null,
      paymentChannel: null,
      source: null,
      counterpartyContains: null,
    });
  };

  const handleReviewActionChange = (action: ReviewAction) => {
    setReviewAction(action);
    updateForm(buildReviewActionPatch(action));
  };

  const resetAfterApply = () => {
    previewMutation.reset();
    applyMutation.reset();
  };

  return (
    <AppLayout>
      <div className="page-stack recategorization-page">
        <RecategorizationHero
          profileId={profileId}
          selectedAccount={selectedAccount}
          selectedTargetCategory={selectedTargetCategory}
          isAutoTargetMode={isAutoTargetMode}
          descriptionContains={form.descriptionContains}
          statusTitle={statusTitle}
          statusDescription={statusDescription}
        />

        <section className="recategorization-layout">
          <div className="recategorization-main">
            <RecategorizationCriteriaPanel
              form={form}
              accounts={accounts}
              categories={categories}
              isAutoTargetMode={isAutoTargetMode}
              reviewAction={reviewAction}
              missingCategoryName={missingCategoryName}
              canCreateMissingCategory={canCreateMissingCategory}
              canPreview={canPreview}
              hasAnySearchCriteria={hasAnySearchCriteria}
              previewPending={previewMutation.isPending}
              applyPending={applyMutation.isPending}
              createCategoryPending={createCategoryMutation.isPending}
              previewErrorMessage={
                previewMutation.isError
                  ? getApiErrorMessage(previewMutation.error)
                  : null
              }
              createCategoryErrorMessage={
                createCategoryMutation.isError
                  ? getApiErrorMessage(createCategoryMutation.error)
                  : null
              }
              onFormChange={updateForm}
              onReviewActionChange={handleReviewActionChange}
              onMissingCategoryNameChange={setMissingCategoryName}
              onCreateCategory={() => createCategoryMutation.mutate()}
              onClearCriteria={clearCriteria}
              onPreview={() => previewMutation.mutate()}
            />

            {previewMutation.data ? (
              <RecategorizationPreviewPanel
                preview={previewMutation.data}
                candidates={candidates}
                visibleCandidates={visibleCandidates}
                candidateFilters={candidateFilters}
                statusCounters={statusCounters}
                activeCandidateFilterCount={activeCandidateFilterCount}
                readyCount={readyIds.length}
                canApply={canApply}
                applyPending={applyMutation.isPending}
                onCandidateFiltersChange={(patch) =>
                  setCandidateFilters((current) => ({
                    ...current,
                    ...patch,
                  }))
                }
                onResetFilters={resetCandidateFilters}
                onApply={() => applyMutation.mutate()}
              />
            ) : null}

            {applyMutation.isError ? (
              <ErrorState message={getApiErrorMessage(applyMutation.error)} />
            ) : null}

            {applyMutation.data ? (
              <RecategorizationResultPanel
                profileId={profileId}
                result={applyMutation.data}
                onReset={resetAfterApply}
              />
            ) : null}
          </div>

          <RecategorizationSidePanel
            selectedTargetCategory={selectedTargetCategory}
            isAutoTargetMode={isAutoTargetMode}
            form={form}
          />
        </section>
      </div>
    </AppLayout>
  );
}
