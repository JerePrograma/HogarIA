# Auditoría técnica de módulos reales (backend/frontend)

Fecha de auditoría: 2026-05-16.

## Inventario backend (código real)

### Controllers
- AccountController
- AuthController
- BudgetController
- CategoryController
- DashboardController
- DevUserController
- ExternalLoansController
- MonthlyPlanController
- MonthlyPlanQuickCaptureController
- MonthlyPlanSuggestionController
- PlanningController
- ProfileController
- TransactionController

### Entidades JPA
- Account
- AppUser
- BudgetCategoryItem
- BudgetMonth
- BudgetYear
- Category
- ExcelImportBatch
- ExcelImportRow
- ExternalLoanSyncConfig
- ExternalSyncMapping
- FinancialGoal
- FinancialProfile
- Habit
- HabitCheckin
- InflationIndex
- MoneyTransaction
- MonthlyPlanItem

### Services
- AccountService
- AuthService
- BudgetService
- CategoryService
- DashboardService
- DevUserService
- ExternalLoanSyncConfigService
- ExternalLoanSyncEventProcessor
- ExternalLoansService
- ExternalSyncIdempotencyService
- FinancialGoalService
- HabitService
- InflationService
- MonthlyPlanQuickCaptureService
- MonthlyPlanService
- MonthlyPlanSuggestionService
- ProfileService
- TransactionService

## Inventario frontend (código real)

### Rutas
- /dev-user
- /profiles
- /profiles/:profileId/dashboard
- /profiles/:profileId/accounts
- /profiles/:profileId/categories
- /profiles/:profileId/transactions
- /profiles/:profileId/budgets
- /profiles/:profileId/goals
- /profiles/:profileId/habits
- /profiles/:profileId/inflation
- /profiles/:profileId/planning
- /profiles/:profileId/prestamos-externos

### APIs frontend (`frontend/src/api/*Api.ts`)
- accountsApi.ts
- budgetsApi.ts
- categoriesApi.ts
- dashboardApi.ts
- devUsersApi.ts
- externalLoansApi.ts
- goalsApi.ts
- habitsApi.ts
- importsApi.ts
- inflationApi.ts
- monthlyPlanQuickCaptureApi.ts
- monthlyPlanningApi.ts
- monthlyPlanSuggestionsApi.ts
- profilesApi.ts
- transactionsApi.ts

## Matriz módulo / lifecycle (estado real)

| Módulo | Entidad backend | Controller | Service | API frontend | Página frontend | CRUD/lifecycle | Tests | Estado real |
|---|---|---|---|---|---|---|---|---|
| Perfiles | FinancialProfile | ProfileController | ProfileService | profilesApi | ProfilesPage | create/list/get/update/delete | Parcial (seguridad MVC, faltan controller CRUD dedicados) | Implementado |
| Cuentas | Account | AccountController | AccountService | accountsApi | AccountsPage | create/list/get/update/deactivate | Service tests | Implementado |
| Categorías | Category | CategoryController | CategoryService | categoriesApi | CategoriesPage | create/list/get/update/deactivate | Service tests | Implementado |
| Movimientos | MoneyTransaction | TransactionController | TransactionService | transactionsApi | TransactionsPage | create/list/get/update/delete | Service tests | Implementado |
| Presupuesto | BudgetYear/BudgetMonth/BudgetCategoryItem | BudgetController | BudgetService | budgetsApi | BudgetPage | ciclo anual + mensual + items | Service tests | Implementado |
| Dashboard | agregados | DashboardController | DashboardService | dashboardApi | DashboardPage | consulta KPIs | Service tests | Implementado |
| Planificación mensual | MonthlyPlanItem | MonthlyPlanController/QuickCapture/Suggestion/PlanningController | MonthlyPlan* | monthlyPlanningApi/monthlyPlanQuickCaptureApi/monthlyPlanSuggestionsApi | MonthlyPlanningPage | create/list/update/delete + quick capture | Service tests | Implementado |
| Préstamos externos | ExternalLoanSyncConfig/ExternalSyncMapping | ExternalLoansController | ExternalLoansService + sync services | externalLoansApi | ExternalLoansPage | summary/config/dry-run/sync/health | Service + IT | Implementado |
| Dev user/Auth | AppUser | DevUserController/AuthController | DevUserService/AuthService | devUsersApi | DevUserPage | login dev + token | Cobertura indirecta | Implementado |
| Goals | FinancialGoal | PlanningController (`/api/profiles/{profileId}/goals*`) | FinancialGoalService | goalsApi | GoalsPage | list/create/delete + emergency-fund | Service tests agregados | **CERRADO** |
| Habits | Habit/HabitCheckin | PlanningController (`/api/profiles/{profileId}/habits*`) | HabitService | habitsApi | HabitsPage | list/create/checkins | Service tests agregados + validaciones de fecha/frecuencia | **CERRADO** |
| Inflation | InflationIndex | PlanningController (`/api/inflation*`) | InflationService | inflationApi | InflationPage | list/create/accumulated | Service tests agregados + validaciones de rango y payload | **CERRADO** |
| Importador Excel | ExcelImportBatch/Row | **Sin controller detectado** | (sin service dedicado visible) | importsApi | BudgetExcelImportPage | preview/commit/rollback esperado | Sin tests | **Ruta frontend retirada del router activo** |

## Hallazgos críticos

1. `goals`, `habits` e `inflation` sí exponen backend real vía `PlanningController`; `imports` permanece fuera del router activo.
2. `ALLOW_X_USER_ID_FALLBACK` estaba habilitado por defecto global; corregido a `false` por defecto.
3. Perfil local habilitaba `CJP_SYNC_ENABLED=true` por defecto; corregido a `false` (read-only por defecto).

## Cambios aplicados en este cierre

- Endurecimiento de seguridad de configuración:
  - Fallback `X-User-Id` desactivado por defecto global.
  - Sync real de cjprestamos desactivado por defecto en `local`.
  - Validación de arranque para bloquear `JWT_SECRET=change_me` fuera de perfiles `local/test`.
