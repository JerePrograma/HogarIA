# HogarIA

## API quickstart

### 1) Crear dev user
`POST /api/dev/users`
```bash
curl -X POST http://localhost:8080/api/dev/users -H 'Content-Type: application/json' -d '{"email":"dev@hogaria.com","password":"12345678","fullName":"Dev User"}'
```

### 2) Crear profile
```bash
curl -X POST http://localhost:8080/api/profiles -H 'X-User-Id: <USER_ID>' -H 'Content-Type: application/json' -d '{"name":"Casa","type":"PERSONAL","baseCurrency":"ARS","activeYear":2026}'
```

### 3) Crear account
```bash
curl -X POST http://localhost:8080/api/accounts -H 'X-User-Id: <USER_ID>' -H 'Content-Type: application/json' -d '{"profileId":"<PROFILE_ID>","name":"Cuenta banco","accountType":"BANK","currency":"ARS"}'
```

### 4) Crear category
```bash
curl -X POST http://localhost:8080/api/categories -H 'X-User-Id: <USER_ID>' -H 'Content-Type: application/json' -d '{"profileId":"<PROFILE_ID>","name":"Supermercado","type":"VARIABLE_EXPENSE"}'
```

### 5) Crear transaction
```bash
curl -X POST http://localhost:8080/api/transactions -H 'X-User-Id: <USER_ID>' -H 'Content-Type: application/json' -d '{"profileId":"<PROFILE_ID>","accountId":"<ACCOUNT_ID>","categoryId":"<CATEGORY_ID>","movementType":"EXPENSE","realDate":"2026-05-01","budgetDate":"2026-05-01","amount":1000,"currency":"ARS"}'
```

### 6) Crear budget year
```bash
curl -X POST http://localhost:8080/api/profiles/<PROFILE_ID>/budgets -H 'X-User-Id: <USER_ID>' -H 'Content-Type: application/json' -d '{"year":2026,"targetIncome":1000000,"targetSaving":200000,"notes":"Plan 2026"}'
```

### 7) Crear budget month
```bash
curl -X POST http://localhost:8080/api/profiles/<PROFILE_ID>/budgets/2026/months -H 'X-User-Id: <USER_ID>' -H 'Content-Type: application/json' -d '{"month":5,"notes":"Mayo"}'
```

### 8) Upsert budget item
```bash
curl -X PUT http://localhost:8080/api/budget-months/<BUDGET_MONTH_ID>/items -H 'X-User-Id: <USER_ID>' -H 'Content-Type: application/json' -d '{"categoryId":"<CATEGORY_ID>","budgetAmount":150000}'
```

### 9) Obtener comparison
```bash
curl http://localhost:8080/api/profiles/<PROFILE_ID>/budgets/2026/months/5/comparison -H 'X-User-Id: <USER_ID>'
```

### 10) Obtener dashboard mensual
```bash
curl "http://localhost:8080/api/profiles/<PROFILE_ID>/dashboard/monthly?year=2026&month=5" -H 'X-User-Id: <USER_ID>'
```
