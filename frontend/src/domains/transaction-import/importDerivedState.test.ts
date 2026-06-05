import { describe, expect, it } from "vitest";
import {
  buildImportDerivedState,
  canImportWithoutCategory,
  isImportableRow,
} from "./importDerivedState";
import type { TransactionImportRow } from "./types";

function row(overrides: Partial<TransactionImportRow> = {}): TransactionImportRow {
  return {
    rowNumber: 1,
    realDate: "2026-05-01",
    normalizedDescription: "movimiento",
    rawSignedAmount: -100,
    amount: 100,
    movementType: "EXPENSE",
    balanceImpact: "CONSUMPTION_EXPENSE",
    classificationStatus: "NEEDS_CATEGORY",
    suggestedCategoryId: null,
    status: "NEEDS_CATEGORY",
    ...overrides,
  };
}

describe("importDerivedState", () => {
  it("counts only real duplicates as duplicateRows", () => {
    const derived = buildImportDerivedState(
      [
        row({ rowNumber: 1, status: "DUPLICATE" }),
        row({ rowNumber: 2, status: "DUPLICATE_EXACT" }),
        row({ rowNumber: 3, status: "POSSIBLE_INTERNAL_TRANSFER" }),
        row({ rowNumber: 4, status: "INTERNAL_TRANSFER_MATCHED" }),
        row({ rowNumber: 5, status: "POSSIBLE_CROSS_SOURCE_DUPLICATE" }),
      ],
      false,
      new Set(),
      false,
    );

    expect(derived.duplicateRows).toBe(2);
    expect(derived.exactDuplicateRows).toBe(1);
    expect(derived.crossSourceRiskRows).toBe(1);
  });

  it("counts internal transfers by status or balance impact once per row", () => {
    const derived = buildImportDerivedState(
      [
        row({ rowNumber: 1, status: "POSSIBLE_INTERNAL_TRANSFER" }),
        row({
          rowNumber: 2,
          status: "REVIEW",
          balanceImpact: "INTERNAL_TRANSFER",
        }),
        row({
          rowNumber: 3,
          status: "INTERNAL_TRANSFER_MATCHED",
          balanceImpact: "INTERNAL_TRANSFER",
        }),
      ],
      false,
      new Set(),
      false,
    );

    expect(derived.internalTransferRows).toBe(3);
  });

  it("matches backend category-free importability", () => {
    const operationalReview = row({
      status: "REVIEW",
      classificationStatus: "REVIEW",
      balanceImpact: "UNKNOWN",
    });
    const technicalReview = row({
      rowNumber: 2,
      status: "REVIEW",
      movementType: "TRANSFER",
      classificationStatus: "TECHNICAL",
      balanceImpact: "INTERNAL_TRANSFER",
    });

    expect(canImportWithoutCategory(operationalReview)).toBe(false);
    expect(isImportableRow(operationalReview, false)).toBe(false);
    expect(canImportWithoutCategory(technicalReview)).toBe(true);
    expect(isImportableRow(technicalReview, false)).toBe(true);
  });

  it("counts unique blocked rows and requires a duplicate decision", () => {
    const rows = [
      row({ rowNumber: 1, status: "NEEDS_CATEGORY" }),
      row({ rowNumber: 2, status: "ERROR" }),
      row({ rowNumber: 3, status: "SKIPPED" }),
      row({
        rowNumber: 4,
        status: "READY",
        suggestedCategoryId: "incompatible",
      }),
      row({ rowNumber: 5, status: "DUPLICATE_EXACT" }),
    ];

    const derived = buildImportDerivedState(
      rows,
      false,
      new Set([4]),
      false,
    );

    expect(derived.blockedRows).toBe(5);
    expect(derived.hasBlockingIssues).toBe(true);
  });

  it("counts technical and neutral impacts", () => {
    const derived = buildImportDerivedState(
      [
        row({ rowNumber: 1, classificationStatus: "TECHNICAL" }),
        row({ rowNumber: 2, balanceImpact: "NEUTRAL_ADJUSTMENT" }),
        row({ rowNumber: 3, balanceImpact: "TECHNICAL" }),
      ],
      false,
      new Set(),
      false,
    );

    expect(derived.technicalNeutralRows).toBe(3);
  });

  it("fallback releases NEEDS_CATEGORY without a suggestion", () => {
    const rows = [row()];
    const blocked = buildImportDerivedState(rows, false, new Set(), false);
    const released = buildImportDerivedState(rows, true, new Set(), false);

    expect(blocked.blockingMissingCategoryRows).toBe(1);
    expect(blocked.importableRows).toBe(0);
    expect(released.blockingMissingCategoryRows).toBe(0);
    expect(released.fallbackCoveredRows).toBe(1);
    expect(released.importableRows).toBe(1);
  });
});
