# HogarIA MVP Iteración 3

Flujo MVP (dev):
1. `docker compose up -d` (PostgreSQL)
2. `cd backend && mvn spring-boot:run`
3. `cd frontend && npm install && npm run dev`
4. Abrir `http://localhost:5173`
5. Crear usuario dev (temporal)
6. Crear perfil
7. Crear cuenta
8. Crear categorías
9. Crear movimientos
10. Ver dashboard mensual

## Endpoints principales
- `POST /api/dev/users`
- `GET /api/dev/users`
- `POST /api/transactions`
- `GET /api/profiles/{profileId}/transactions?year=2026&month=5`
- `PUT /api/transactions/{id}`
- `DELETE /api/transactions/{id}`

> Header temporal: `X-User-Id: <uuid>` (se reemplazará por JWT en próxima iteración).

## cURL ejemplo
```bash
curl -X POST http://localhost:8080/api/dev/users \
  -H 'Content-Type: application/json' \
  -d '{"email":"dev@hogaria.local","fullName":"Dev User","password":"dev123"}'
```
