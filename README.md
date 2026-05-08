# HogarIA

Aplicación financiera full-stack (Spring Boot + React/TypeScript) para gestión de perfiles, presupuesto, movimientos, objetivos, hábitos, inflación e importación guiada de Excel.

## Comandos
- `docker compose up -d`
- `cd backend && mvn test`
- `cd backend && mvn spring-boot:run`
- `cd frontend && npm install`
- `cd frontend && npm run build`
- `cd frontend && npm run dev`

## URLs
- Frontend: http://localhost:5173
- Backend: http://localhost:8080
- Swagger: http://localhost:8080/swagger-ui/index.html

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
