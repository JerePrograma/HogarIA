# Contrato temporal de bridge HogarIA ↔ cjprestamos

## 1) Objetivo del bridge
Definir un contrato técnico **temporal, de solo lectura y backend-to-backend** para que HogarIA consulte información operativa de préstamos en cjprestamos mientras el bounded context interno `loans` aún no está implementado en HogarIA.

Este bridge permite:
- visualizar datos de cartera y seguimiento de préstamos en HogarIA,
- mantener la trazabilidad de integración sin acoplar frontend a un sistema externo,
- preparar la futura migración hacia un módulo `loans` interno.

## 2) Por qué el frontend NO debe consumir cjprestamos directo
El frontend de HogarIA no debe llamar cjprestamos directamente por razones de seguridad, gobernanza y arquitectura:
- **Seguridad de credenciales**: secretos de integración no pueden exponerse en browser.
- **Aislamiento de contratos**: el backend actúa como anti-corruption layer frente a cambios de cjprestamos.
- **Control de autorización**: HogarIA debe seguir centralizando validaciones por `profileId` + `userId`.
- **Manejo uniforme de errores**: timeouts, 401/403, 5xx y datos inválidos se gestionan de forma consistente en backend.
- **Evolución futura**: facilita reemplazar la fuente externa por módulo interno `loans` sin romper UI.

## 3) Datos mínimos a consultar desde cjprestamos
HogarIA necesita como mínimo:
- préstamos activos,
- resumen de cartera,
- control de caja,
- cuotas próximas,
- pagos registrados,
- mora,
- ganancia realizada,
- ganancia proyectada.

## 4) Endpoints esperados del lado cjprestamos
Contrato esperado para integración de lectura:
- `GET /api/integration/hogaria/loans/active`
- `GET /api/integration/hogaria/dashboard`
- `GET /api/integration/hogaria/control-caja`
- `GET /api/integration/hogaria/loans/{loanId}/installments`
- `GET /api/integration/hogaria/loans/{loanId}/payments`

Notas:
- Estos endpoints se consideran **temporales** para bridge.
- HogarIA consume estos recursos únicamente desde backend.

## 5) DTOs propuestos en HogarIA
Se definen DTOs mínimos de integración (records Java):
- `ExternalLoanSummaryResponse`
- `ExternalLoanResponse`
- `ExternalLoanInstallmentResponse`
- `ExternalLoanPaymentResponse`
- `ExternalLoanCashControlResponse`

## 6) Mapeo conceptual
Relación de conceptos de negocio de cjprestamos hacia contratos HogarIA:
- `Prestamo` → `ExternalLoanResponse`
- `Cuota` → `ExternalLoanInstallmentResponse`
- `Pago` → `ExternalLoanPaymentResponse`
- `DashboardService` (métricas agregadas) → `ExternalLoanSummaryResponse` / `ExternalLoanCashControlResponse`

## 7) Qué NO debe hacer este bridge
Alcance explícitamente excluido en esta fase:
- **No** persistir datos de cjprestamos en base HogarIA.
- **No** crear `MoneyTransaction` automáticamente.
- **No** mezclar capital recuperado como ingreso común.
- **No** modificar datos en cjprestamos (sin operaciones write).

## 8) Seguridad
Requisitos mínimos de seguridad para el bridge temporal:
- usar **Basic Auth** solo para comunicación backend-to-backend,
- cargar credenciales por variables de entorno,
- no hardcodear secretos en código,
- no exponer credenciales al frontend.

## 9) Manejo de errores
El cliente de integración debe contemplar y estandarizar:
- integración deshabilitada,
- timeout de red,
- respuestas `401/403`,
- respuestas `5xx`,
- respuesta inválida o no parseable.

Recomendación: encapsular estos casos en errores de integración controlados para no filtrar detalles sensibles al cliente web.

## 10) Plan posterior
Este bridge debe ser reemplazado gradualmente por el bounded context interno `loans` de HogarIA:
1. implementar dominio interno (`Loan`, cuotas, pagos, mora),
2. sincronizar/replicar semántica de métricas relevantes,
3. migrar consumidores internos a fuente local,
4. retirar dependencia del bridge externo.
