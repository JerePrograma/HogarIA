# HogarIA

## Cómo levantar
- `docker compose up -d`
- `cd backend && mvn spring-boot:run`
- `cd frontend && npm install && npm run dev`

## URLs
- Frontend: http://localhost:5173
- Backend: http://localhost:8080
- Swagger: http://localhost:8080/swagger-ui/index.html

## Flujo UI
1. Crear dev user.
2. Crear perfil.
3. Crear cuenta.
4. Crear categorías.
5. Crear movimientos.
6. Crear presupuesto.
7. Ver Budget vs Real.
8. Ver Dashboard.

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
- `X-User-Id` temporal.
- JWT pendiente.
- Objetivos financieros pendientes.
- Hábitos pendientes.
- Inflación pendiente.
