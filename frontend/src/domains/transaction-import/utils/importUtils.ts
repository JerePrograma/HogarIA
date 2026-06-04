import {
  getCategoryDisplayName,
  getCompatibleCategories,
} from "../../../domain/transactionRules";
import { Category } from "../../../domain/types";
import {
  TransactionImportBalanceImpact,
  TransactionImportClassificationStatus,
  TransactionImportConfidence,
  TransactionImportMovementType,
  TransactionImportPaymentChannel,
  TransactionImportRowStatus,
  TransactionImportRow,
} from "../types";

export const importMovementLabels: Record<
  TransactionImportMovementType,
  string
> = {
  INCOME: "Ingreso",
  EXPENSE: "Gasto",
  SAVING: "Ahorro / inversión",
  TRANSFER: "Transferencia",
  ADJUSTMENT: "Ajuste",
};

export const importRowStatusLabels: Record<TransactionImportRowStatus, string> =
  {
    READY: "Lista",
    NEEDS_CATEGORY: "Requiere categoría",
    DUPLICATE: "Duplicada",
    DUPLICATE_EXACT: "Duplicado exacto",
    POSSIBLE_INTERNAL_TRANSFER: "Posible transferencia interna",
    INTERNAL_TRANSFER_MATCHED: "Transferencia interna",
    POSSIBLE_CROSS_SOURCE_DUPLICATE: "Posible duplicado cross-source",
    REVIEW: "Revisión",
    SKIPPED: "Omitida",
    ERROR: "Error",
  };

export const importPaymentChannelLabels: Record<
  TransactionImportPaymentChannel,
  string
> = {
  UNKNOWN: "Sin canal",
  CASH: "Efectivo",
  BANK_TRANSFER: "Transferencia bancaria",
  DEBIN: "DEBIN",
  CUENTA_DNI: "Cuenta DNI",
  DEBIT_CARD: "Tarjeta de débito",
  CREDIT_CARD: "Tarjeta de crédito",
  DIRECT_DEBIT: "Débito directo",
  POS_TRANSFER: "Transferencia POS",
  ATM: "Cajero / punto efectivo",
  MONEY_MARKET_YIELD: "Rendimiento diario",
  TRANSPORT_CARD: "Tarjeta transporte",
  QR_PAYMENT: "Pago QR",
  CARD_FOREIGN_CURRENCY: "Tarjeta moneda extranjera",
  MERCADO_PAGO: "Mercado Pago",
  MERCADO_CREDITO: "Mercado Crédito",
  INTERNAL_TRANSFER: "Transferencia interna",
  OTHER: "Otro",
};

export const importBalanceImpactLabels: Record<
  TransactionImportBalanceImpact,
  string
> = {
  OPERATING_INCOME: "Ingreso operativo",
  CONSUMPTION_EXPENSE: "Consumo",
  SAVING_OUTFLOW: "Ahorro",
  INVESTMENT_OUTFLOW: "Inversión",
  DEBT_OUTFLOW: "Pago de deuda",
  RECOVERABLE_OUTFLOW: "Egreso recuperable",
  PRINCIPAL_RECOVERY: "Recupero de capital",
  INTEREST_INCOME: "Interés ganado",
  REFUND_OR_REIMBURSEMENT: "Reintegro",
  INTERNAL_TRANSFER: "Transferencia interna",
  EXTERNAL_TRANSFER: "Transferencia externa",
  NEUTRAL_ADJUSTMENT: "Ajuste neutro",
  IGNORED: "Ignorado",
  TECHNICAL: "Técnico",
  UNKNOWN: "Sin impacto",
};

export const importClassificationStatusLabels: Record<
  TransactionImportClassificationStatus,
  string
> = {
  CLASSIFIED: "Clasificada",
  NEEDS_CATEGORY: "Sin categoría",
  REVIEW: "Revisión",
  TECHNICAL: "Técnica",
  IGNORED_BY_RULE: "Ignorada por regla",
};

export const importConfidenceLabels: Record<
  TransactionImportConfidence,
  string
> = {
  HIGH: "Alta",
  MEDIUM: "Media",
  LOW: "Baja",
  NONE: "Sin confianza",
};

export function getImportRowStatusTone(
  status: TransactionImportRowStatus,
): "ok" | "watch" | "critical" | "neutral" {
  if (status === "READY") return "ok";
  if (status === "NEEDS_CATEGORY" || status === "REVIEW") return "watch";
  if (status === "ERROR") return "critical";

  return "neutral";
}

export function getImportRowIssueMessage(
  row: TransactionImportRow,
  createMissingFallbackCategory: boolean,
) {
  if (row.status === "DUPLICATE" || row.status === "DUPLICATE_EXACT") {
    return row.suggestedCategoryName
      ? `Movimiento ya existente. Categoría sugerida para revisar: ${row.suggestedCategoryName}.`
      : "Movimiento ya existente. Se omitirá al confirmar.";
  }

  if (
    row.status === "NEEDS_CATEGORY" &&
    !row.suggestedCategoryId &&
    createMissingFallbackCategory
  ) {
    return row.suggestedCategoryName
      ? `Sugerida para revisar: ${row.suggestedCategoryName}. Se usará una categoría temporal compatible al confirmar.`
      : "Se usará una categoría temporal compatible al confirmar.";
  }

  if (
    row.status === "NEEDS_CATEGORY" &&
    !row.suggestedCategoryId &&
    !createMissingFallbackCategory
  ) {
    return "Necesita una categoría para poder importarse.";
  }

  if (row.status === "REVIEW") {
    return (
      row.skipReason ||
      row.classificationReason ||
      "Requiere revisión manual antes de confirmar."
    );
  }
  if (
    row.status === "POSSIBLE_INTERNAL_TRANSFER" ||
    row.status === "INTERNAL_TRANSFER_MATCHED"
  ) {
    return (
      row.skipReason ||
      "Posible transferencia interna: mismo monto, fecha cercana y cuenta distinta. Se omite para no inflar ingresos/gastos."
    );
  }

  if (row.status === "POSSIBLE_CROSS_SOURCE_DUPLICATE") {
    return (
      row.skipReason ||
      "Posible duplicado entre Banco Provincia y Mercado Pago. Revisar antes de contar ambos como consumo."
    );
  }

  if (row.status === "SKIPPED" && row.skipReason) {
    return `Motivo de omisión: ${row.skipReason}`;
  }

  if (row.status === "ERROR") {
    return row.warning ?? "La fila tiene un error y no se importará.";
  }

  return "";
}

export function getSuggestedCategoryName(
  row: TransactionImportRow,
  categoriesById: Map<string, Category>,
) {
  const category = categoriesById.get(row.suggestedCategoryId ?? "");

  return (
    (category ? getCategoryDisplayName(category) : null) ??
    row.suggestedCategoryName ??
    null
  );
}

export function getClassificationExplanationSummary(row: TransactionImportRow) {
  if (row.classificationMatchedField && row.classificationMatchedValue) {
    return `${row.classificationMatchedField}: ${row.classificationMatchedValue}`;
  }

  if (!row.classificationExplanationJson) {
    return null;
  }

  try {
    const parsed = JSON.parse(row.classificationExplanationJson) as {
      matchedField?: string;
      matchedValue?: string;
      warning?: string;
    };
    const matched = [parsed.matchedField, parsed.matchedValue]
      .filter(Boolean)
      .join(": ");

    return matched || parsed.warning || null;
  } catch {
    return row.classificationExplanationJson;
  }
}

export function getSelectableCategoriesForImportRow(
  row: TransactionImportRow,
  categories: Category[],
) {
  return getCompatibleCategories(categories, row.movementType, {
    includeTechnical: true,
  });
}
