# AGENTS.md — Guía para agentes Codex en HogarIA

## 1) Descripción funcional del proyecto
HogarIA es una aplicación de gestión financiera familiar enfocada en:
- control de gastos e ingresos,
- planificación mensual,
- presupuestos,
- movimientos,
- perfiles financieros,
- visualización de KPIs y dashboard.

El objetivo del producto es ayudar a personas y familias a organizar su economía con trazabilidad por perfil y decisiones basadas en datos.

## 2) Estructura backend/frontend
- `backend/`: API REST y lógica de negocio.
  - Stack principal: Java 21, Spring Boot 3.x, PostgreSQL, Flyway, JPA, Spring Security, MapStruct, Apache POI.
- `frontend/`: cliente web SPA.
  - Stack principal: React 18, TypeScript, Vite, Axios, TanStack Query, React Router, React Hook Form, Zod, Recharts, Tailwind.

## 3) Comandos esperados (instalar, compilar y testear)
### Backend
- Tests: `cd backend && mvn test`
- Run local: `cd backend && mvn spring-boot:run`

### Frontend
- Instalar dependencias: `cd frontend && npm install`
- Build: `cd frontend && npm run build`
- Desarrollo: `cd frontend && npm run dev`

## 4) Reglas de arquitectura (obligatorias)
1. No crear archivos binarios en commits de código.
2. No subir secretos (tokens, contraseñas, credenciales, `.env` sensibles, llaves privadas).
3. No romper ni reescribir migraciones Flyway existentes; nuevas necesidades van en migraciones nuevas y ordenadas.
4. Mantener Java 21 como versión objetivo.
5. Mantener Spring Boot en línea 3.x.
6. Usar UUID para nuevas entidades del dominio HogarIA.
7. Toda nueva funcionalidad sensible debe validar autorización por `profileId` y `userId`.
8. No usar `X-User-Id` como solución final de seguridad (solo podría existir temporalmente en desarrollo controlado).

## 5) Reglas de integración futura con cjprestamos
La integración de préstamos debe respetar estas decisiones de dominio:
1. Préstamos entra como bounded context independiente llamado `loans`.
2. No representar préstamos solo como gastos/ingresos simples.
3. Separar explícitamente:
   - capital prestado,
   - capital recuperado,
   - interés ganado.
4. Sincronizar cuotas futuras con `MonthlyPlanItem` de tipo `RECOVERY`.

## 6) Reglas de estilo y calidad
1. Escribir código claro, explícito y mantenible.
2. Evitar hacks de demo o atajos frágiles.
3. Agregar/ajustar tests cuando se toque lógica de negocio.
4. Mantener cohesión con la arquitectura existente antes de introducir patrones nuevos.

## 7) Definition of Done (DoD) para cualquier PR
Una PR se considera terminada cuando:
1. Compila y pasa tests relevantes del área afectada.
2. No introduce secretos, binarios innecesarios ni cambios accidentales de formato masivo.
3. Respeta reglas de seguridad por `profileId` + `userId` en funcionalidades sensibles.
4. Si toca persistencia, incluye migración Flyway nueva (sin editar históricas).
5. Si toca lógica de negocio, incluye tests actualizados o nuevos.
6. Documenta decisiones relevantes (README o notas técnicas) cuando aporta comportamiento nuevo.
7. Mantiene compatibilidad con Java 21 y Spring Boot 3.x.
