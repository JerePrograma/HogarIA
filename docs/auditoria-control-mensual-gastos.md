# Auditoría inicial: control mensual de gastos/caja

## Hallazgos clave previos

1. `DashboardService` mezclaba gasto económico y salidas de caja, porque `totalExpenses` solo sumaba `FIXED_EXPENSE + VARIABLE_EXPENSE + INVESTMENT`, dejando fuera `DEBT`, parte de `SAVING`, recuperos y transferencias.  
2. `DashboardService` no distinguía eventos de cjprestamos (`DISBURSEMENT`, `PAYMENT_PRINCIPAL_RECOVERY`, `PAYMENT_INTEREST_INCOME`) al calcular resumen mensual, por lo que recupero/interés podían quedar semánticamente ambiguos.  
3. `ExternalLoansService` en dry-run/sync reportaba conteos, pero no impacto monetario por tipo contable (capital prestado, recuperado, interés).  
4. `ExternalLoanSyncEventProcessor` usaba hashes simplificados (`payment-interest-{id}`, etc.), insuficientes para reconciliar cambios económicos retroactivos.  
5. `BudgetService` compara por `Category.Type`, sin una capa semántica compartida con dashboard/planning.

## Riesgo contable detectado

El sistema podía responder **inconsistente** a:
- “¿Cuánto dinero se fue?” (por subcontabilizar salidas).
- “¿Cuánto fue gasto real?” (por mezclar recuperos/intereses/ajustes).

## Línea de solución aplicada en esta iteración

- Introducción de clasificador financiero central (`FinancialCashFlowClassifier` + `CashFlowTreatment`).
- Extensión de `DashboardSummaryResponse` con bloque `monthlyCashFlowSummary` para desglose operativo sin romper campos existentes.
- Inclusión de clasificación que considera categoría, movementType y eventType de sync externa.

> Nota: quedan fases complementarias para completar reconciliación avanzada y alineación total en budget/planning/frontend con la misma semántica.
