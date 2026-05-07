# HogarIA

Base estabilizada MVP (iteración 2).

## Requisitos
- Java 21
- Maven
- Node 20+
- Docker

## Ejecución local
```bash
docker compose up -d
cd backend && mvn spring-boot:run
cd frontend && npm install && npm run dev
```

## URLs
- Backend: http://localhost:8080
- Swagger: http://localhost:8080/swagger-ui/index.html
- Frontend: http://localhost:5173

## Flujo mínimo
1. Crear perfil
2. Crear cuenta
3. Crear categoría
4. Crear movimiento
5. Ver dashboard (pendiente próxima iteración)

## Ejemplo curl
```bash
USER_ID=11111111-1111-1111-1111-111111111111
curl -X POST http://localhost:8080/api/transactions \
  -H "Content-Type: application/json" \
  -H "X-User-Id: ${USER_ID}" \
  -d '{
    "profileId":"00000000-0000-0000-0000-000000000001",
    "accountId":"00000000-0000-0000-0000-000000000002",
    "categoryId":"00000000-0000-0000-0000-000000000003",
    "movementType":"EXPENSE",
    "realDate":"2026-05-01",
    "budgetDate":"2026-05-01",
    "amount":1000.00,
    "currency":"ARS",
    "description":"Supermercado"
  }'
```
