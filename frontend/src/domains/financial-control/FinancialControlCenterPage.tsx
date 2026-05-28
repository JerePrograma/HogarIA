import { useMemo } from "react";
import type { ReactNode } from "react";
import { Link, useParams } from "react-router-dom";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { AppLayout } from "../../app/shell/AppShell";
import { routePaths } from "../../app/router/routePaths";
import { listAccounts } from "../../api/accountsApi";
import { listCategories } from "../../api/categoriesApi";
import {
  linkInternalTransfer,
  listTransactions,
  previewDuplicateTransactions,
  previewInternalTransfers,
  resolveDuplicateTransactions,
} from "../../api/transactionsApi";
import { formatMoney } from "../../domain/formatters";
import { queryKeys } from "../../domain/queryKeys";
import type {
  Account,
  Category,
  DuplicateGroup,
  InternalTransferCandidate,
  MoneyTransaction,
} from "../../domain/types";
import {
  getDefaultClassificationStatus,
  shouldCountTransactionInOperationalBalance,
} from "../../domain/transactionRules";
import { Alert } from "../../shared/ui/Alert";
import { EmptyState } from "../../shared/ui/EmptyState";
import { ErrorState } from "../../shared/ui/ErrorState";
import { LoadingState } from "../../shared/ui/LoadingState";
import { useTransactionPeriod } from "../transactions/hooks/useTransactionPeriod";

type QueueTone = "ok" | "watch" | "risk" | "neutral";

type QueueCard = {
  title: string;
  count: number;
  amount?: number;
  tone: QueueTone;
  filterHint: string;
  actionLabel: string;
  actionTo: string;
};

export function FinancialControlCenterPage() {
  const { profileId = "" } = useParams();
  const queryClient = useQueryClient();
  const { year, month, handleShiftPeriod, handleCurrentPeriod } =
    useTransactionPeriod();

  const transactionsQuery = useQuery({
    queryKey: queryKeys.transactions(profileId, year, month),
    queryFn: () => listTransactions(profileId, year, month),
    enabled: Boolean(profileId),
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

  const duplicatesQuery = useQuery({
    queryKey: [
      ...queryKeys.transactionDataQuality(profileId, year, month),
      "duplicates",
    ],
    queryFn: () => previewDuplicateTransactions(profileId, year, month),
    enabled: Boolean(profileId),
  });

  const internalTransfersQuery = useQuery({
    queryKey: [
      ...queryKeys.transactionDataQuality(profileId, year, month),
      "internal-transfers",
    ],
    queryFn: () => previewInternalTransfers(profileId, year, month),
    enabled: Boolean(profileId),
  });

  const invalidateQuality = async () => {
    await Promise.all([
      queryClient.invalidateQueries({
        queryKey: queryKeys.transactions(profileId, year, month),
      }),
      queryClient.invalidateQueries({
        queryKey: queryKeys.transactionDataQuality(profileId, year, month),
      }),
      queryClient.invalidateQueries({
        queryKey: queryKeys.dashboard(profileId),
      }),
    ]);
  };

  const linkTransferMutation = useMutation({
    mutationFn: (candidate: InternalTransferCandidate) =>
      linkInternalTransfer(profileId, {
        debitTransactionId: candidate.debitLeg.id,
        creditTransactionId: candidate.creditLeg.id,
        toleranceAmount: Number(candidate.amountDifference ?? 0),
        toleranceDays: Math.max(2, Number(candidate.dayDistance ?? 0)),
      }),
    onSuccess: invalidateQuality,
  });

  const resolveDuplicateMutation = useMutation({
    mutationFn: (group: DuplicateGroup) =>
      resolveDuplicateTransactions(profileId, {
        keepTransactionId: group.transactions[0].id,
        duplicateTransactionIds: group.transactions.slice(1).map((tx) => tx.id),
        note: "Resuelto desde Centro de control",
      }),
    onSuccess: invalidateQuality,
  });

  const transactions = transactionsQuery.data ?? [];
  const accounts = accountsQuery.data ?? [];
  const categories = categoriesQuery.data ?? [];
  const duplicateGroups = duplicatesQuery.data?.groups ?? [];
  const internalCandidates = internalTransfersQuery.data?.candidates ?? [];

  const quality = useMemo(
    () => buildQuality(profileId, transactions, accounts, categories, duplicateGroups, internalCandidates),
    [accounts, categories, duplicateGroups, internalCandidates, profileId, transactions],
  );

  const loading =
    transactionsQuery.isLoading ||
    accountsQuery.isLoading ||
    categoriesQuery.isLoading ||
    duplicatesQuery.isLoading ||
    internalTransfersQuery.isLoading;

  const hasError =
    transactionsQuery.isError ||
    accountsQuery.isError ||
    categoriesQuery.isError ||
    duplicatesQuery.isError ||
    internalTransfersQuery.isError;

  return (
    <AppLayout>
      <div className="page-stack">
        <section className="panel">
          <div className="section-title">
            <div>
              <p className="eyebrow">Revisión inteligente</p>
              <h1>Centro de control financiero</h1>
              <p className="muted">
                {year}-{String(month).padStart(2, "0")} · perfil activo{" "}
                {profileId.slice(0, 8)}
              </p>
            </div>

            <div className="row-actions">
              <button
                type="button"
                className="boton-secundario"
                onClick={() => handleShiftPeriod(-1)}
              >
                Mes anterior
              </button>
              <button
                type="button"
                className="boton-secundario"
                onClick={() => handleCurrentPeriod()}
              >
                Mes actual
              </button>
              <button
                type="button"
                className="boton-secundario"
                onClick={() => handleShiftPeriod(1)}
              >
                Mes siguiente
              </button>
            </div>
          </div>
        </section>

        {loading ? <LoadingState message="Revisando calidad de datos." /> : null}
        {hasError ? (
          <ErrorState message="No se pudo cargar alguna cola de revisión." />
        ) : null}

        {!loading && !hasError ? (
          <>
            <section className="metrics-grid">
              {quality.cards.map((card) => (
                <QualityCard key={card.title} card={card} />
              ))}
            </section>

            <section className="two-column-layout">
              <ReviewPanel
                title="Movimientos repetidos"
                emptyTitle="Sin duplicados activos"
                emptyMessage="No hay movimientos repetidos pendientes para este período."
              >
                {duplicateGroups.slice(0, 6).map((group) => (
                  <div className="surface-inset" key={`${group.groupType}-${group.key}`}>
                    <p className="label-ui">Ya cargaste esto</p>
                    <strong>{formatMoney(group.amount, group.currency)}</strong>
                    <p className="compact-muted">
                      {group.transactions.length} movimientos muy parecidos.
                    </p>
                    <div className="row-actions mt-3">
                      <Link
                        className="boton-secundario"
                        to={`${routePaths.transactions(profileId)}?onlyDuplicates=true`}
                      >
                        Resolver duplicados
                      </Link>
                      {group.transactions.length > 1 ? (
                        <button
                          type="button"
                          className="boton-danger"
                          disabled={resolveDuplicateMutation.isPending}
                          onClick={() => resolveDuplicateMutation.mutate(group)}
                        >
                          Ignorar duplicados
                        </button>
                      ) : null}
                    </div>
                  </div>
                ))}
              </ReviewPanel>

              <ReviewPanel
                title="Transferencias internas posibles"
                emptyTitle="Sin transferencias pendientes"
                emptyMessage="No hay pares con mismo monto, cuentas distintas y fecha cercana."
              >
                {internalCandidates.slice(0, 6).map((candidate) => (
                  <div
                    className="surface-inset"
                    key={`${candidate.debitLeg.id}-${candidate.creditLeg.id}`}
                  >
                    <p className="label-ui">Candidato</p>
                    <strong>
                      {formatMoney(candidate.debitLeg.amount, candidate.debitLeg.currency)}
                    </strong>
                    <p className="compact-muted">
                      {candidate.debitLeg.description || "Sin descripción"} ↔{" "}
                      {candidate.creditLeg.description || "Sin descripción"}
                    </p>
                    <p className="compact-muted">{candidate.reason}</p>
                    <div className="row-actions mt-3">
                      <button
                        type="button"
                        className="boton-principal"
                        disabled={linkTransferMutation.isPending}
                        onClick={() => linkTransferMutation.mutate(candidate)}
                      >
                        Vincular
                      </button>
                      <Link
                        className="boton-secundario"
                        to={`${routePaths.transactions(profileId)}?onlyInternalTransfers=true`}
                      >
                        Revisar pares
                      </Link>
                    </div>
                  </div>
                ))}
              </ReviewPanel>
            </section>

            <section className="panel">
              <div className="section-title">
                <div>
                  <p className="eyebrow">Lectura rápida</p>
                  <h2>Impacto financiero del período</h2>
                </div>
              </div>

              <div className="transactions-filter-grid">
                <ImpactLine label="Impactan balance" value={quality.impacting.length} />
                <ImpactLine label="Visibles neutrales" value={quality.neutral.length} />
                <ImpactLine label="Sin categoría" value={quality.withoutCategory.length} />
                <ImpactLine label="Pendientes" value={quality.pending.length} />
                <ImpactLine label="Importados a revisar" value={quality.importReview.length} />
                <ImpactLine label="Inconsistencias catálogo" value={quality.catalogIssues.length} />
              </div>

              {quality.catalogIssues.length > 0 ? (
                <Alert tone="warning">
                  Hay claves equivalentes en cuentas o categorías. Conviene desactivar o
                  fusionar explícitamente antes de seguir importando.
                </Alert>
              ) : null}
            </section>
          </>
        ) : null}
      </div>
    </AppLayout>
  );
}

function QualityCard({ card }: { card: QueueCard }) {
  const toneClass =
    card.tone === "risk"
      ? "metric-card-danger"
      : card.tone === "watch"
        ? "metric-card-warning"
        : card.tone === "neutral"
          ? "metric-card-info"
          : "metric-card-success";

  return (
    <article className={`metric-card ${toneClass}`.trim()}>
      <p className="label-ui">{card.filterHint}</p>
      <h3>{card.title}</h3>
      <strong>{card.count}</strong>
      {card.amount != null ? <p className="compact-muted">{card.amount}</p> : null}
      <Link className="boton-fantasma mt-3" to={card.actionTo}>
        {card.actionLabel}
      </Link>
    </article>
  );
}

function ReviewPanel({
  title,
  emptyTitle,
  emptyMessage,
  children,
}: {
  title: string;
  emptyTitle: string;
  emptyMessage: string;
  children: ReactNode;
}) {
  const items = Array.isArray(children) ? children.filter(Boolean) : children;
  const empty = Array.isArray(items) ? items.length === 0 : !items;

  return (
    <section className="panel">
      <div className="section-title">
        <div>
          <p className="eyebrow">Cola</p>
          <h2>{title}</h2>
        </div>
      </div>
      <div className="grid gap-3">
        {empty ? <EmptyState title={emptyTitle} message={emptyMessage} /> : items}
      </div>
    </section>
  );
}

function ImpactLine({ label, value }: { label: string; value: number }) {
  return (
    <div className="surface-inset">
      <p className="label-ui">{label}</p>
      <strong>{value}</strong>
    </div>
  );
}

function buildQuality(
  profileId: string,
  transactions: MoneyTransaction[],
  accounts: Account[],
  categories: Category[],
  duplicateGroups: DuplicateGroup[],
  internalCandidates: InternalTransferCandidate[],
) {
  const transactionsPath = routePaths.transactions(profileId);
  const withoutCategory = transactions.filter(
    (tx) => !tx.categoryId || getDefaultClassificationStatus(tx) === "NEEDS_CATEGORY",
  );
  const pending = transactions.filter((tx) => tx.status === "PENDING");
  const ignoredTechnical = transactions.filter(
    (tx) =>
      tx.status === "IGNORED" ||
      getDefaultClassificationStatus(tx) === "TECHNICAL" ||
      getDefaultClassificationStatus(tx) === "IGNORED_BY_RULE",
  );
  const importReview = transactions.filter(
    (tx) =>
      tx.origin === "IMPORT" &&
      (getDefaultClassificationStatus(tx) === "REVIEW" ||
        getDefaultClassificationStatus(tx) === "NEEDS_CATEGORY"),
  );
  const impacting = transactions.filter((tx) =>
    shouldCountTransactionInOperationalBalance(tx),
  );
  const neutral = transactions.filter(
    (tx) => !shouldCountTransactionInOperationalBalance(tx),
  );
  const imported = transactions.filter((tx) => tx.origin === "IMPORT");
  const internalTransfers = transactions.filter(
    (tx) => tx.movementType === "TRANSFER" || Boolean(tx.internalTransferGroupId),
  );
  const catalogIssues = [
    ...findDuplicateKeys(accounts, (account) => account.accountKey ?? normalizeKey(account.name)),
    ...findDuplicateKeys(
      categories.filter((category) => category.active),
      (category) => `${category.profileId ?? "GLOBAL"}:${category.type}:${category.categoryKey ?? normalizeKey(category.name)}`,
    ),
  ];

  const cards: QueueCard[] = [
    {
      title: "Duplicados exactos",
      count: duplicateGroups.filter((group) => group.groupType === "EXACT_DUPLICATE").length,
      tone: duplicateGroups.some((group) => group.groupType === "EXACT_DUPLICATE") ? "risk" : "ok",
      filterHint: "Ya cargaste esto",
      actionLabel: "Resolver duplicados",
      actionTo: `${transactionsPath}?onlyDuplicates=true`,
    },
    {
      title: "Repetidos posibles",
      count: duplicateGroups.filter((group) => group.groupType === "POSSIBLE_CROSS_SOURCE_DUPLICATE").length,
      tone: "watch",
      filterHint: "Puede venir de dos lugares",
      actionLabel: "Revisar repetidos",
      actionTo: `${transactionsPath}?onlyDuplicates=true`,
    },
    {
      title: "Transferencias internas",
      count: internalCandidates.length,
      tone: internalCandidates.length > 0 ? "watch" : "ok",
      filterHint: "Plata entre tus cuentas",
      actionLabel: "Revisar pares",
      actionTo: `${transactionsPath}?onlyInternalTransfers=true`,
    },
    {
      title: "Sin categoría",
      count: withoutCategory.length,
      tone: withoutCategory.length > 0 ? "watch" : "ok",
      filterHint: "Necesita categoría",
      actionLabel: "Categorizar ahora",
      actionTo: `${transactionsPath}?onlyWithoutCategory=true`,
    },
    {
      title: "Pendientes",
      count: pending.length,
      tone: pending.length > 0 ? "watch" : "ok",
      filterHint: "Necesita confirmación",
      actionLabel: "Confirmar pendientes",
      actionTo: `${transactionsPath}?status=PENDING`,
    },
    {
      title: "No afectan el mes",
      count: ignoredTechnical.length,
      tone: "neutral",
      filterHint: "Visibles pero neutrales",
      actionLabel: "Auditar neutrales",
      actionTo: `${transactionsPath}?impact=NEUTRAL`,
    },
    {
      title: "Importaciones a revisar",
      count: importReview.length,
      tone: importReview.length > 0 ? "watch" : "ok",
      filterHint: `${imported.length} vinieron de archivo`,
      actionLabel: "Continuar importación",
      actionTo: `${transactionsPath}?origin=IMPORT`,
    },
    {
      title: "Catálogos",
      count: catalogIssues.length,
      tone: catalogIssues.length > 0 ? "risk" : "ok",
      filterHint: "Cuentas/categorías",
      actionLabel: "Limpiar categorías",
      actionTo: `/profiles/${profileId}/categories#limpiar`,
    },
    {
      title: "Impactan balance",
      count: impacting.length,
      tone: "ok",
      filterHint: "Ingresos y gastos reales",
      actionLabel: "Ver movimientos",
      actionTo: transactionsPath,
    },
    {
      title: "Visibles neutrales",
      count: neutral.length + internalTransfers.length,
      tone: "neutral",
      filterHint: "No inflan el resultado",
      actionLabel: "Auditar neutrales",
      actionTo: `${transactionsPath}?impact=NEUTRAL`,
    },
  ];

  return {
    cards,
    withoutCategory,
    pending,
    ignoredTechnical,
    importReview,
    impacting,
    neutral,
    catalogIssues,
  };
}

function normalizeKey(value: string) {
  return value
    .normalize("NFD")
    .replace(/\p{Diacritic}/gu, "")
    .toLowerCase()
    .replace(/[^a-z0-9]+/g, "");
}

function findDuplicateKeys<T>(items: T[], keyFn: (item: T) => string) {
  const countByKey = new Map<string, number>();
  for (const item of items) {
    const key = keyFn(item);
    countByKey.set(key, (countByKey.get(key) ?? 0) + 1);
  }

  return [...countByKey.entries()]
    .filter(([, count]) => count > 1)
    .map(([key]) => key);
}
