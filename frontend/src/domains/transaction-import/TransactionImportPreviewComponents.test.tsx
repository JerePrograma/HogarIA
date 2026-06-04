import "@testing-library/jest-dom/vitest";

import { cleanup, fireEvent, render, screen, within } from "@testing-library/react";
import { afterEach, describe, expect, it, vi } from "vitest";
import type { Category } from "../../domain/types";
import { ImportCommitPanel } from "./ImportCommitPanel";
import { ImportPreviewSummary } from "./ImportPreviewSummary";
import { ImportRowsTable } from "./ImportRowsTable";
import {
  countBlockingMissingCategoryRows,
  countImportableRows,
} from "./importCalculations";
import type { TransactionImportRow } from "./types";

const categories: Category[] = [
  {
    id: "cat-uber",
    profileId: null,
    parentId: null,
    name: "Taxi y apps",
    categoryKey: "taxiyapps",
    type: "VARIABLE_EXPENSE",
    scope: "GLOBAL",
    defaultMovementType: "EXPENSE",
    budgetable: true,
    technical: false,
    active: true,
    createdAt: "2026-01-01T00:00:00Z",
    updatedAt: "2026-01-01T00:00:00Z",
  },
  {
    id: "cat-sueldo",
    profileId: null,
    parentId: null,
    name: "Sueldo",
    categoryKey: "sueldo",
    type: "INCOME",
    scope: "GLOBAL",
    defaultMovementType: "INCOME",
    budgetable: true,
    technical: false,
    active: true,
    createdAt: "2026-01-01T00:00:00Z",
    updatedAt: "2026-01-01T00:00:00Z",
  },
];

function buildRow(overrides: Partial<TransactionImportRow> = {}): TransactionImportRow {
  return {
    rowNumber: 3,
    realDate: "2026-01-30",
    budgetDate: "2026-01-30",
    normalizedDescription: "payu ar uber",
    rawDescription: "PAGO CON TARJETA DEBITO",
    amount: 5378,
    currency: "ARS",
    movementType: "EXPENSE",
    paymentChannel: "DEBIT_CARD",
    balanceImpact: "CONSUMPTION_EXPENSE",
    classificationStatus: "CLASSIFIED",
    classificationReason: "RULE_UBER",
    suggestedCategoryId: null,
    suggestedCategoryName: "Taxi y apps",
    confidence: "HIGH",
    status: "READY",
    merchantName: "PAYU*AR*UBER",
    ...overrides,
  };
}

describe("transaction import preview components", () => {
  afterEach(() => {
    cleanup();
  });

  it("renders preview totals including suggested and review rows", () => {
    render(
      <ImportPreviewSummary
        totalRows={12}
        importableRows={8}
        duplicateRows={2}
        invalidRows={1}
        ignoredRows={1}
        suggestedCategoryRows={6}
        reviewRows={3}
        needsCategoryRows={2}
        technicalNeutralRows={4}
      />,
    );

    expect(screen.getByText("Sugeridas")).toBeInTheDocument();
    expect(screen.getByText("Review")).toBeInTheDocument();
    expect(screen.getByText("Necesitan categoría")).toBeInTheDocument();
    expect(screen.getByText("Técnicas/neutras")).toBeInTheDocument();
    expect(screen.getByText("6")).toBeInTheDocument();
    expect(screen.getByText("3")).toBeInTheDocument();
    expect(screen.getAllByText("2").length).toBeGreaterThan(0);
    expect(screen.getByText("4")).toBeInTheDocument();
  });

  it("shows duplicate rows and the skip reason before import", () => {
    render(
      <ImportRowsTable
        rows={[
          buildRow({
            status: "DUPLICATE_EXACT",
            matchReason: "source_hash existente",
            matchedTransactionId: "tx-1",
          }),
        ]}
        categories={categories}
        createMissingFallbackCategory={false}
        onRowsChange={vi.fn()}
      />,
    );

    expect(screen.getAllByText("Duplicado exacto").length).toBeGreaterThan(0);
    expect(
      screen.getByText(/No se creará al confirmar: ya existe un movimiento compatible/i),
    ).toBeInTheDocument();
    expect(screen.getByText("Match: source_hash existente")).toBeInTheDocument();
  });

  it("allows changing a suggested category before confirmation", () => {
    const onRowsChange = vi.fn();
    render(
      <ImportRowsTable
        rows={[buildRow()]}
        categories={categories}
        createMissingFallbackCategory={false}
        onRowsChange={onRowsChange}
      />,
    );

    const categorySelect = screen
      .getAllByRole("combobox")
      .find((select) =>
        within(select).queryByRole("option", { name: "Taxi y apps" }),
      );

    expect(categorySelect).toBeDefined();
    fireEvent.change(categorySelect!, { target: { value: "cat-uber" } });

    expect(onRowsChange).toHaveBeenCalledWith([
      expect.objectContaining({
        rowNumber: 3,
        suggestedCategoryId: "cat-uber",
      }),
    ]);
  });

  it("calls the final import confirmation action", () => {
    const onCommit = vi.fn();
    render(
      <ImportCommitPanel
        canCommit
        pending={false}
        hasMissingCategory={false}
        onCommit={onCommit}
      />,
    );

    fireEvent.click(screen.getByRole("button", { name: "Confirmar importación" }));

    expect(onCommit).toHaveBeenCalledTimes(1);
  });

  it("does not disable confirmation for REVIEW rows without category", () => {
    const rows = [
      buildRow({
        status: "REVIEW",
        movementType: "INCOME",
        balanceImpact: "UNKNOWN",
        classificationStatus: "REVIEW",
        classificationReason: "RULE_MP_GENERIC_INFLOW_REVIEW",
        suggestedCategoryId: null,
        suggestedCategoryName: null,
      }),
    ];
    const canCommit =
      countImportableRows(rows, false) > 0 &&
      countBlockingMissingCategoryRows(rows, false) === 0;

    render(
      <ImportCommitPanel
        canCommit={canCommit}
        pending={false}
        hasMissingCategory={false}
        onCommit={vi.fn()}
      />,
    );

    expect(screen.getByRole("button", { name: "Confirmar importación" })).toBeEnabled();
  });

  it("disables confirmation for NEEDS_CATEGORY rows until fallback is enabled", () => {
    const rows = [
      buildRow({
        status: "NEEDS_CATEGORY",
        classificationStatus: "NEEDS_CATEGORY",
        classificationReason: "NO_IMPORT_RULE",
        suggestedCategoryId: null,
      }),
    ];
    const canCommitWithoutFallback =
      countImportableRows(rows, false) > 0 &&
      countBlockingMissingCategoryRows(rows, false) === 0;
    const canCommitWithFallback =
      countImportableRows(rows, true) > 0 &&
      countBlockingMissingCategoryRows(rows, true) === 0;

    const { rerender } = render(
      <ImportCommitPanel
        canCommit={canCommitWithoutFallback}
        pending={false}
        hasMissingCategory
        onCommit={vi.fn()}
      />,
    );

    expect(screen.getByRole("button", { name: "Confirmar importación" })).toBeDisabled();

    rerender(
      <ImportCommitPanel
        canCommit={canCommitWithFallback}
        pending={false}
        hasMissingCategory={false}
        onCommit={vi.fn()}
      />,
    );

    expect(screen.getByRole("button", { name: "Confirmar importación" })).toBeEnabled();
  });
});
