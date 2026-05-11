# Auditoría técnica backend — Preparación para módulo `loans`

## Alcance y método
Este documento releva el estado **actual** del backend de HogarIA para evaluar preparación de integración de un bounded context de préstamos (`loans`) sin asumir funcionalidades inexistentes.

Fuentes analizadas:
- Entidades JPA del dominio financiero base.
- Controladores REST y contratos de rutas.
- Servicios y validaciones de ownership (`profileId` + `userId`).
- Repositories y consultas disponibles.
- Migraciones Flyway (`V1..V5`).
- Capa de seguridad efectiva (Spring Security + resolver de usuario actual).

---

## 1) Entidades actuales (lectura crítica)

### 1.1 AppUser
- Existe como tabla/entidad de usuarios con `email`, `passwordHash`, `fullName`, `createdAt`.
- Hoy es el ancla de ownership indirecto: `FinancialProfile.userId` referencia a `AppUser.id`.
- No hay relación JPA explícita (`@ManyToOne`) entre `FinancialProfile` y `AppUser` (se usa UUID plano), lo cual simplifica pero deja validaciones en servicios.

### 1.2 FinancialProfile
- Es el **tenant funcional real** del sistema financiero (`profileId`) con columnas `userId`, `type`, `baseCurrency`, `activeYear`, `active`.
- Es la pieza correcta para aislar préstamos por perfil.
- El patrón actual depende de verificar ownership por servicio (`findByIdAndUserId`).

### 1.3 Account
- Cuenta financiera asociada a `profileId`.
- Soporta tipos útiles para préstamos (por ejemplo `BANK`, `CREDIT_CARD`) y metadatos de tarjeta (`creditLimit`, `statementCloseDay`, `dueDay`).
- No modela explícitamente “cuenta prestamista”, “cuenta prestatario”, ni cuenta técnica de cartera de préstamos.

### 1.4 Category
- Taxonomía por perfil/global con tipos `INCOME`, `FIXED_EXPENSE`, `VARIABLE_EXPENSE`, `SAVING`, `DEBT`, `INVESTMENT`.
- Incluye tipo `DEBT`, pero es clasificación contable, **no** representación de contrato de préstamo.
- Permite categorías globales (`profileId = null`) y perfil propio.

### 1.5 MoneyTransaction
- Registro contable de movimientos reales/presupuestarios con `movementType` (`INCOME`, `EXPENSE`, etc.), `accountId`, `categoryId`, `amount`, fechas y estados.
- Es excelente para reflejar impactos de caja de préstamos (desembolso, cobro de cuota, interés), pero no guarda semántica contractual (saldo pendiente, tasa pactada, calendario, mora).
- `amount > 0` por check de DB; el signo económico se infiere por tipo.

### 1.6 MonthlyPlanItem
- Planificación mensual con tipo ampliado (`DEBT`, `RECOVERY`, `TODO`, etc.), monto fijo/rango, recupero esperado, cuotas y vínculo opcional a `transactionId`.
- Esta entidad **sí ofrece puntos de acople muy útiles** para proyección de cuotas futuras de préstamos (`Type.RECOVERY`, `installmentNumber/Total`).
- Aun así, sigue siendo planificación; no reemplaza ledger de préstamo.

### 1.7 BudgetYear / BudgetMonth / BudgetCategoryItem
- Estructura anual/mensual de presupuesto por categoría.
- Útil para forecasting de ingresos por recupero y/o egresos por fondeo de préstamos.
- No distingue principal vs interés.

### 1.8 FinancialGoal
- Objetivos financieros por perfil (`goalType`, `targetAmount`, `currentAmount`, etc.).
- Puede coexistir con loans (ej. meta de capital a prestar), pero no modela préstamos en sí.

### Conclusión de entidades
El dominio actual está bien para **contabilidad y planificación**; no tiene aún un agregado explícito para préstamos (contrato, cuotas, eventos y estado de cartera). Eso implica que “modelar préstamos como gasto/ingreso” sería insuficiente y técnicamente frágil.

---

## 2) Controladores actuales y rutas REST

### Superficie REST existente
- Perfil, cuentas, categorías, transacciones, presupuesto, planificación mensual, sugerencias, quick-capture, dashboard, goals/habits/inflation.
- Patrón dominante: rutas bajo `/api` y scoping por `/profiles/{profileId}/...`.

### Hallazgos relevantes para loans
- `TransactionController#create` usa `/api/transactions` sin `profileId` en path (va en body), inconsistente con otros recursos perfil-scoped.
- Existen endpoints por id directo (`/accounts/{id}`, `/categories/{id}`, `/transactions/{id}`, `/budget-months/{id}`), con autorización delegada al servicio.
- La API de planificación ya contempla convertir ítems a transacción (`convert-to-transaction`), patrón reutilizable para cuotas de préstamos.

### Implicación
El módulo `loans` debería mantener el patrón dominante de scoping explícito:
- `/api/profiles/{profileId}/loans`
- `/api/profiles/{profileId}/loans/{loanId}/installments`
- evitar endpoints globales sin contexto de perfil para operaciones sensibles.

---

## 3) Servicios actuales y responsabilidades

### Fortalezas
- Varios servicios validan ownership con `findByIdAndUserId(profileId, userId)` antes de operar (`ProfileService`, `AccountService`, `CategoryService`, `TransactionService`, `MonthlyPlanService`, etc.).
- `TransactionService` valida coherencia `accountId` y `categoryId` contra `profileId`.
- `MonthlyPlanService` valida reglas de negocio ricas (rangos, recupero, cuotas, moneda).

### Deuda técnica observable
- Servicios en formato muy compacto (one-liners y alta densidad), lo que reduce legibilidad/mantenibilidad para crecer un contexto complejo como loans.
- Reglas de autorización dispersas (cada servicio implementa su `ensureProfile`), sin policy central reutilizable.
- `MonthlyPlanSuggestionService` ejecuta `itemRepo.findAll()` y luego filtra en memoria por perfil: patrón no escalable.
- Parte de mensajes/validaciones en español y otros en inglés; inconsistencia menor, pero relevante para contratos estables de API.

---

## 4) Repositories y queries relevantes

### Lo que ya existe
- Consultas por `profileId` en cuentas, categorías, transacciones, monthly plan, presupuestos y goals.
- Índices adecuados en tablas críticas (`money_transaction`, `monthly_plan_item`, presupuestos).

### Huecos para loans
- No hay repositorios para cronogramas de cuotas, estado de préstamo, eventos de pago parcial/mora, ni métricas de cartera.
- Hay consultas que podrían derivar en problemas de performance al escalar (`findAll` en sugerencias).
- No hay especificaciones/queries agregadas para separar principal/interés porque el modelo actual no lo distingue.

---

## 5) Migraciones Flyway actuales

- `V1`: núcleo usuarios/perfiles/cuentas/categorías/transacciones.
- `V2`: presupuestos.
- `V3`: corrección de tipo de `budget_month.month`.
- `V4`: goals, habits, inflación, importación Excel.
- `V5`: monthly plan con soporte de recupero/cuotas y vínculo a transacción.

### Lectura para loans
- Base sólida para introducir `V6+` de `loans` sin tocar históricas.
- No existen tablas ni constraints de dominio préstamo; será necesario crear nuevas tablas y claves/índices dedicados.

---

## 6) Seguridad actual

### SecurityConfig
- `csrf.disable()`, `anyRequest().permitAll()` y `/api/dev/**` permitido.
- Es decir: no hay autenticación/autorización Spring Security activa en runtime.

### CurrentUserResolver + `X-User-Id`
- El usuario actual se deriva de header `X-User-Id` parseado a UUID.
- Si el UUID es inválido, devuelve `BadRequest`.
- Este mecanismo depende de confianza en cliente/entorno y no constituye seguridad robusta para producción.

### Riesgo
Para un módulo sensible como préstamos (capital, interés, vencimientos, mora), este enfoque es insuficiente como solución final.

---

## 7) Piezas que YA sirven para préstamos

1. **Aislamiento por perfil (`profileId`)** ya extendido transversalmente.
2. **Ledger de caja (`MoneyTransaction`)** para impactos monetarios reales.
3. **Planificación (`MonthlyPlanItem`)** para cuotas futuras y recuperos esperados.
4. **Catálogo de cuentas (`Account`)** para origen/destino de fondos.
5. **Categorías (`Category`)** para reporting y presupuesto.
6. **Dashboard/planificación** como consumidores naturales de métricas loans agregadas.

---

## 8) Piezas que NO sirven (o no alcanzan) y requieren refactor

1. **Modelo transaccional actual no separa principal/interés**: imprescindible para préstamos.
2. **Ausencia de agregado préstamo** (contrato, tasa, cronograma, saldo, estado).
3. **Seguridad basada en header libre** (`X-User-Id`) y `permitAll` global.
4. **Autorización repetida por servicio** en lugar de componente de dominio reusable.
5. **Consultas no escalables en sugerencias** (`findAll` + filtro en memoria).
6. **Falta de idempotencia/event sourcing mínimo** para registrar pagos/reversiones de cuotas de forma auditable.

---

## 9) Cómo debería entrar el módulo `loans`

## 9.1 Entidades nuevas (bounded context independiente)
Mínimo recomendado:
- `Loan` (cabecera): `id`, `profileId`, `accountId` (cuenta de fondeo/cobro), `borrowerName|counterparty`, `currency`, `principalAmount`, `annualRate`, `termMonths`, `startDate`, `status`, `createdAt`, `updatedAt`.
- `LoanInstallment`: `id`, `loanId`, `installmentNumber`, `dueDate`, `principalDue`, `interestDue`, `totalDue`, `status`, `paidAt`.
- `LoanPaymentEvent`: `id`, `loanId`, opcional `installmentId`, `paymentDate`, `principalPaid`, `interestPaid`, `lateFeePaid`, `moneyTransactionId`, `notes`.

Opcional (si quieren trazabilidad fuerte):
- `LoanBalanceSnapshot` o cálculo derivado con query agregada consistente.

## 9.2 Servicios
- `LoanService`: alta/edición/cancelación lógica, cálculo cronograma, ownership checks.
- `LoanInstallmentService`: gestión de cuotas, recalculo y estado (DUE/PAID/OVERDUE/PARTIAL).
- `LoanPaymentService`: aplicación de pagos, prorrateo principal/interés, creación de `MoneyTransaction` asociada.
- `LoanProjectionService`: sincronización hacia `MonthlyPlanItem` tipo `RECOVERY`.

## 9.3 Endpoints sugeridos
- `POST /api/profiles/{profileId}/loans`
- `GET /api/profiles/{profileId}/loans`
- `GET /api/profiles/{profileId}/loans/{loanId}`
- `POST /api/profiles/{profileId}/loans/{loanId}/payments`
- `GET /api/profiles/{profileId}/loans/{loanId}/installments`
- `PATCH /api/profiles/{profileId}/loans/{loanId}/installments/{installmentId}`

## 9.4 Relación con `profileId`
- Obligatoria en todas las entidades loans.
- Validación centralizada de ownership por `profileId + userId` antes de toda operación.

## 9.5 Relación con `accountId`
- Requerida para mapear flujo real de fondos (desembolso/cobro).
- Validar que la cuenta pertenezca al mismo perfil.

## 9.6 Relación con `categoryId`
- No como núcleo del préstamo, sino como clasificación contable/reporting de transacciones generadas.
- Recomendación: categorías específicas (ej. `LOAN_PRINCIPAL_RECOVERY`, `LOAN_INTEREST_INCOME`) por perfil o globales controladas.

## 9.7 Relación con `MonthlyPlanItem`
- Cada cuota futura sincronizarse como `MonthlyPlanItem.Type.RECOVERY` (o `EXPENSE` para préstamos tomados, si ese caso entra luego).
- Guardar referencia cruzada (`loanId`, `installmentId`) en tabla puente o nuevas columnas para evitar matching heurístico por texto/fecha.

## 9.8 Relación con `MoneyTransaction`
- Todo pago/cobro confirmado debe materializar una `MoneyTransaction`.
- Necesario distinguir componentes principal/interés (por ejemplo mediante `LoanPaymentEvent` + una o más transacciones relacionadas).

---

## 10) Riesgos técnicos concretos

1. **Riesgo de seguridad**: con `permitAll` + `X-User-Id` spoofeable, exposición alta de datos financieros multi-perfil.
2. **Riesgo contable**: sin separar principal/interés, reporting y KPIs pueden ser incorrectos.
3. **Riesgo de integridad**: falta de constraints de dominio loans (ej. suma de pagos > total de cuota, estados inconsistentes).
4. **Riesgo de performance**: patrones de consulta en memoria no escalan (ya visible en sugerencias).
5. **Riesgo de acoplamiento**: intentar “encajar préstamos” sólo en `MonthlyPlanItem`/`MoneyTransaction` degradaría mantenibilidad.
6. **Riesgo operativo**: ausencia de estrategia de idempotencia para pagos (reintentos podrían duplicar cobros).

---

## 11) Plan de implementación por fases

### Fase 0 — Hardening mínimo previo
- Endurecer seguridad (autenticación real; eliminar dependencia final de `X-User-Id` en clientes).
- Centralizar policy de ownership `profileId + userId`.
- Definir convenciones de errores/API para módulo loans.

### Fase 1 — Modelo y persistencia loans (sin UI compleja)
- Agregar migraciones nuevas `V6__loans_core.sql` (y sucesivas).
- Crear entidades/repositories `Loan`, `LoanInstallment`, `LoanPaymentEvent` con índices por `profile_id`, `status`, `due_date`.
- Seed/control de categorías contables para principal/interés si corresponde.

### Fase 2 — Casos de uso core
- Alta de préstamo + generación de cronograma.
- Registro de pago con impacto en saldo y estado de cuota.
- Integración transaccional con `MoneyTransaction`.

### Fase 3 — Integración de planificación
- Proyección automática de cuotas a `MonthlyPlanItem` tipo `RECOVERY`.
- Sincronización en cambios de cronograma (reprogramación/cancelación).

### Fase 4 — Reporting y observabilidad
- KPIs de cartera: capital vigente, recuperado, interés ganado, mora.
- Alertas de cuotas vencidas y conciliación plan vs real.

### Fase 5 — Robustez
- Idempotencia en pagos.
- Tests de concurrencia y consistencia (double payment, reversas).
- Backfills/migraciones de datos si hay préstamos preexistentes modelados como movimientos sueltos.

---

## Decisión recomendada
**Recomendación:** incorporar `loans` como bounded context independiente, con persistencia y servicios propios, e integraciones explícitas con `MonthlyPlanItem` (proyección) y `MoneyTransaction` (impacto real de caja). No modelar préstamos únicamente como categorías o transacciones genéricas.

Además, antes de exponer operaciones financieras sensibles de préstamos, ejecutar un hardening de seguridad (authn/authz real) para reemplazar el esquema actual basado en `X-User-Id` como mecanismo final.
