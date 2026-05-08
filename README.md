# HogarIA

## Cómo levantar
- `docker compose up -d`
- `cd backend && mvn spring-boot:run`
- `cd frontend && npm install && npm run dev`

## URLs
- Frontend: http://localhost:5173
- Backend: http://localhost:8080
- Swagger: http://localhost:8080/swagger-ui/index.html

## Flujo UI completo
1. Crear o seleccionar dev user.
2. Crear, editar, desactivar y entrar a perfiles.
3. Crear, listar y desactivar cuentas.
4. Crear, listar y desactivar categorías (con includeGlobal).
5. Crear, listar, editar estado y eliminar movimientos por año/mes.
6. Crear/obtener budget anual y mensual.
7. Cargar budget por categoría y ver Budget vs Real.
8. Ver dashboard mensual con resumen, 50/30/20 y desglose por categoría.

## Smoke test funcional UI
1. Crear dev user.
2. Crear perfil Casa.
3. Crear cuenta Banco.
4. Crear categorías: Sueldo `INCOME`, Alquiler `FIXED_EXPENSE`, Supermercado `VARIABLE_EXPENSE`, Ahorro `SAVING`.
5. Crear movimientos: Sueldo 1.000.000, Alquiler 300.000, Supermercado 180.000, Ahorro 200.000.
6. Crear budget: Alquiler 300.000, Supermercado 150.000, Ahorro 200.000.
7. Verificar dashboard: income 1.000.000, fixed 300.000, variable 180.000, saving 200.000, balance 320.000, health EXCELLENT.
8. Verificar Budget vs Real: totalBudget 650.000, totalReal 680.000, Supermercado EXCEEDED, Sueldo no aparece como EXCEEDED.

## Limitaciones
- `X-User-Id` temporal (sin JWT real).
- JWT pendiente.
- Objetivos financieros pendientes.
- Hábitos pendientes.
- Inflación pendiente.

## Nuevas capacidades (v4)
- Objetivos financieros por perfil con cálculo de progreso y creación asistida de fondo de emergencia.
- Hábitos financieros por perfil con check-ins diarios/semanales/mensuales.
- Índices de inflación con cálculo de inflación acumulada por rango.
- Migración Flyway `V4__financial_planning_extensions.sql` con tablas: `financial_goal`, `habit`, `habit_checkin`, `inflation_index`.

### Endpoints nuevos
- `GET/POST /api/profiles/{profileId}/goals`
- `DELETE /api/profiles/{profileId}/goals/{goalId}`
- `POST /api/profiles/{profileId}/goals/emergency-fund`
- `GET/POST /api/profiles/{profileId}/habits`
- `PUT /api/profiles/{profileId}/habits/{habitId}/checkins/{date}`
- `GET/POST /api/inflation`
- `GET /api/inflation/accumulated`
