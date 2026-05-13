# HogarIA

Aplicación financiera full-stack (Spring Boot + React/TypeScript) para gestión de perfiles, presupuesto, movimientos, objetivos, hábitos, inflación, importación guiada de Excel e integración externa de préstamos (cjprestamos).


## Integración HogarIA ↔ cjprestamos (estado actual)
- HogarIA incluye el módulo `external-loans` para consultar información de préstamos desde cjprestamos.
- Endpoints expuestos en HogarIA: `summary`, `sync-config` y `sync` bajo `/api/profiles/{profileId}/external-loans`.
- La integración debe tratarse como **read-only por defecto**; `sync` existe en fase 2 y requiere habilitación explícita (`CJP_SYNC_ENABLED=true`).
- Documentación detallada: [docs/integracion-hogaria-cjprestamos.md](docs/integracion-hogaria-cjprestamos.md).

## Comandos
- `docker compose up -d`
- `cd backend && mvn test`
- `cd backend && mvn spring-boot:run`
- `cd frontend && npm install`
- `cd frontend && npm run build`
- `cd frontend && npm run dev`

## URLs
- Frontend: http://localhost:5174
- Backend: http://localhost:8080
- Swagger: http://localhost:8080/swagger-ui/index.html


## Runbook local paralelo (HogarIA + cjprestamos)

| App | Frontend | Backend | Auth local | Variables clave | Arranque |
|---|---|---|---|---|---|
| HogarIA | http://localhost:5174 | http://localhost:8080 | JWT Bearer (`/api/auth/login`) + fallback `X-User-Id` opcional en dev | `VITE_API_BASE_URL=/api`, `VITE_ALLOW_DEV_X_USER_ID=true`, `CORS_ALLOWED_ORIGINS=http://localhost:5173,http://127.0.0.1:5173,http://localhost:5174,http://127.0.0.1:5174` | `cd backend && mvn spring-boot:run` + `cd frontend && npm run dev` |
| cjprestamos | http://localhost:5173 | http://localhost:8081 | Basic Auth (`admin/admin` local inicial) | `VITE_API_BASE_URL=/api`, `CORS_ALLOWED_ORIGINS=http://localhost:5173,http://127.0.0.1:5173,http://localhost:5174,http://127.0.0.1:5174` | `cd ../cjprestamos/backend && mvn spring-boot:run` + `cd ../cjprestamos/frontend && npm run dev` |

Notas:
- Ambos frontends deben usar `VITE_API_BASE_URL=/api` para que Vite proxy enrute al backend correcto sin CORS cruzado entre 5173/5174.
- Si HogarIA backend está apagado, el frontend mostrará `No se pudo conectar con el backend...` (equivalente al `ERR_CONNECTION_REFUSED` del navegador).
- Un `404` en legajo (cjprestamos) o budgets (HogarIA) puede ser funcional si no existen datos cargados para el recurso solicitado.

## Capacidades actuales
- Dashboard mensual con KPIs financieros y Budget vs Real.
- Presupuesto anual/mensual por categorías.
- Objetivos financieros por perfil (incluye fondo de emergencia).
- Hábitos financieros por perfil con check-ins.
- Índices de inflación y acumulado por rango.
- Importador guiado de Excel por perfil (preview + commit por batch).

## Endpoints principales
- `GET/POST /api/profiles/{profileId}/goals`
- `DELETE /api/profiles/{profileId}/goals/{goalId}`
- `POST /api/profiles/{profileId}/goals/emergency-fund`
- `GET/POST /api/profiles/{profileId}/habits`
- `PUT /api/profiles/{profileId}/habits/{habitId}/checkins/{date}`
- `GET/POST /api/inflation`
- `GET /api/inflation/accumulated`
- `POST /api/profiles/{profileId}/imports/budget-excel/preview`
- `POST /api/profiles/{profileId}/imports/budget-excel/{batchId}/commit`
- `GET /api/profiles/{profileId}/imports`
- `GET /api/profiles/{profileId}/imports/{batchId}`

## Smoke test recomendado
1. Crear dev user.
2. Crear perfil.
3. Ejecutar seed financiero (si aplica).
4. Importar Excel desde “Carga guiada”.
5. Revisar Dashboard.
6. Revisar Presupuesto vs Real.
7. Revisar Objetivos.
8. Revisar Hábitos.
9. Revisar Inflación.

## Seguridad mínima (JWT)
- Endpoints financieros (`/api/profiles/**`, `/api/transactions/**`, `/api/budgets/**`, `/api/accounts/**`, `/api/categories/**`) requieren `Authorization: Bearer <token>`.
- Login: `POST /api/auth/login` con `{ "email": "...", "password": "..." }`.
- Usuario actual: `GET /api/auth/me`.
- `X-User-Id` solo se usa como fallback si `app.security.allow-x-user-id-fallback=true` (desactivado por defecto).
- Para desarrollo local podés crear usuarios en `POST /api/dev/users` y luego autenticarte con `/api/auth/login`.

### Variables relevantes
- `JWT_SECRET` (default inseguro solo local: `change_me`).
- `app.jwt.expiration-seconds` (default `43200`).
- `app.security.allow-x-user-id-fallback` (default `false`).
