import { describe, expect, it } from "vitest";
import {
  countBlockingMissingCategoryRows,
  countImportRows,
  countImportableRows,
  countTechnicalNeutralRows,
} from "./importCalculations";
import type { TransactionImportRow } from "./types";

function buildRow(overrides: Partial<TransactionImportRow> = {}): TransactionImportRow {
  return {
    rowNumber: 1,
    realDate: "2026-05-01",
    budgetDate: "2026-05-01",
    normalizedDescription: "varios",
    rawDescription: "Varios",
    amount: 1000,
    currency: "ARS",
    movementType: "INCOME",
    paymentChannel: "MERCADO_PAGO",
    balanceImpact: "UNKNOWN",
    classificationStatus: "REVIEW",
    classificationReason: "RULE_MP_GENERIC_INFLOW_REVIEW",
    suggestedCategoryId: null,
    suggestedCategoryName: null,
    confidence: "LOW",
    status: "REVIEW",
    ...overrides,
  };
}

describe("transaction import calculations", () => {
  it("does not block confirmation for REVIEW rows without category", () => {
    const rows = [buildRow()];

    expect(countBlockingMissingCategoryRows(rows, false)).toBe(0);
    expect(countImportableRows(rows, false)).toBe(1);
  });

  it("blocks NEEDS_CATEGORY rows without category when fallback is disabled", () => {
    const rows = [
      buildRow({
        status: "NEEDS_CATEGORY",
        classificationStatus: "NEEDS_CATEGORY",
        classificationReason: "NO_IMPORT_RULE",
      }),
    ];

    expect(countBlockingMissingCategoryRows(rows, false)).toBe(1);
    expect(countImportableRows(rows, false)).toBe(0);
  });

  it("unblocks NEEDS_CATEGORY rows when fallback is enabled", () => {
    const rows = [
      buildRow({
        status: "NEEDS_CATEGORY",
        classificationStatus: "NEEDS_CATEGORY",
        classificationReason: "NO_IMPORT_RULE",
      }),
    ];

    expect(countBlockingMissingCategoryRows(rows, true)).toBe(0);
    expect(countImportableRows(rows, true)).toBe(1);
  });

  it("keeps review, needs-category and technical-neutral counters separate", () => {
    const rows = [
      buildRow(),
      buildRow({
        rowNumber: 2,
        status: "NEEDS_CATEGORY",
        classificationStatus: "NEEDS_CATEGORY",
        classificationReason: "NO_IMPORT_RULE",
      }),
      buildRow({
        rowNumber: 3,
        movementType: "TRANSFER",
        balanceImpact: "INTERNAL_TRANSFER",
        classificationStatus: "TECHNICAL",
        classificationReason: "RULE_MP_FUNDING_TRANSFER",
      }),
    ];

    const counters = countImportRows(rows);

    expect(counters.REVIEW).toBe(2);
    expect(counters.NEEDS_CATEGORY).toBe(1);
    expect(countTechnicalNeutralRows(rows)).toBe(1);
  });
});
