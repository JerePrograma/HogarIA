# Contrato temporal de bridge HogarIA ↔ cjprestamos

## 1) Objetivo del bridge
Definir un contrato técnico **temporal, de solo lectura y backend-to-backend** para que HogarIA consulte información operativa de préstamos en cjprestamos mientras el bounded context interno `loans` aún no está implementado en HogarIA.

## 2) Contrato de IDs
- El bridge usa IDs legacy numéricos de cjprestamos (`Long`) bajo nombres `external*`.
- `externalLoanId`, `externalBorrowerId`, `externalInstallmentId` y `externalPaymentId` representan identificadores externos, no IDs de dominio HogarIA.
- Los UUID quedan reservados para el futuro módulo interno `loans` de HogarIA.

## 3) Endpoints read-only de cjprestamos
- `GET /api/integration/hogaria/loans/active`
- `GET /api/integration/hogaria/dashboard`
- `GET /api/integration/hogaria/control-caja`
- `GET /api/integration/hogaria/loans/{loanId}/installments`
- `GET /api/integration/hogaria/loans/{loanId}/payments`

## 4) Anti-corruption layer
HogarIA separa DTO remoto y DTO público:
- DTO remoto (`com.hogaria.integration.cjprestamos.remote`) **calca** el JSON real de cjprestamos (`personaNombre`, `montoInicial`, `monto`, etc.).
- DTO público (`com.hogaria.integration.cjprestamos.dto`) expone nombres estables del bridge para consumo interno/backend (`borrowerName`, `principalAmount`, `totalCollected`, etc.).
- `CjPrestamosBridgeMapper` transforma remoto → público.

## 5) Mapeo remoto → público
### Préstamo activo
- `id` → `externalLoanId`
- `personaId` → `externalBorrowerId`
- `personaNombre` → `borrowerName`
- `montoInicial` → `principalAmount`
- `totalCobrado` → `totalCollected`
- `totalPendiente` → `totalPending`
- `gananciaRealizada` → `realizedProfit`
- `gananciaProyectada` → `projectedProfit`
- `estado` → `status`

### Dashboard
- `montoInvertido` → `investedAmount`
- `montoGanado` → `earnedAmount`
- `montoPorGanar` → `amountToEarn`
- `deudaTotal` → `totalDebt`
- `prestamosActivos` → `activeLoans`

## 6) Restricción de campos
No se inventan campos no entregados por cjprestamos (por ejemplo `annualRate`, `nextDueDate`, `principalDue`, `interestDue`, `lateFee`, `paymentMethod`) salvo cálculo explícito y documentado.
