# HogarIA

Flujo MVP implementado: dev user -> profile -> accounts/categories/transactions -> dashboard mensual.

## URLs
- Backend: `http://localhost:8080`
- Frontend: `http://localhost:5173`

## Variables
- Frontend: `VITE_API_BASE_URL` (default `http://localhost:8080`)
- Header temporal: `X-User-Id`

## Comandos
- Backend tests: `cd backend && mvn test`
- Frontend install/build: `cd frontend && npm install && npm run build`
- Docker: `docker compose config`

## Flujo rápido con curl
1) Crear dev user
```bash
curl -X POST http://localhost:8080/api/dev-users -H 'Content-Type: application/json' -d '{"name":"Demo","email":"demo@test.com"}'
```
2) Crear profile
```bash
curl -X POST http://localhost:8080/api/profiles -H 'X-User-Id: <USER_ID>' -H 'Content-Type: application/json' -d '{"name":"Casa","scope":"PERSONAL"}'
```
3) Crear cuenta
```bash
curl -X POST http://localhost:8080/api/profiles/<PROFILE_ID>/accounts -H 'X-User-Id: <USER_ID>' -H 'Content-Type: application/json' -d '{"name":"Efectivo","accountType":"CASH","currency":"ARS"}'
```
4) Crear categoría
```bash
curl -X POST http://localhost:8080/api/profiles/<PROFILE_ID>/categories -H 'X-User-Id: <USER_ID>' -H 'Content-Type: application/json' -d '{"name":"Comida","type":"VARIABLE_EXPENSE","scope":"PERSONAL"}'
```
5) Crear movimiento
```bash
curl -X POST http://localhost:8080/api/transactions -H 'X-User-Id: <USER_ID>' -H 'Content-Type: application/json' -d '{"profileId":"<PROFILE_ID>","accountId":"<ACCOUNT_ID>","categoryId":"<CATEGORY_ID>","movementType":"EXPENSE","realDate":"2026-05-01","budgetDate":"2026-05-01","amount":1000,"currency":"ARS"}'
```
6) Ver dashboard
```bash
curl 'http://localhost:8080/api/profiles/<PROFILE_ID>/dashboard/monthly?year=2026&month=5' -H 'X-User-Id: <USER_ID>'
```
