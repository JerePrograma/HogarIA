# CONTRATO_INTEGRACION_CJPRESTAMOS

## 1. Propósito del contrato

Este documento define el **contrato técnico-funcional de integración** entre **HogarIA** y **cjprestamos**, tomando como base un análisis profundo del repositorio `JerePrograma/cjprestamos` (código, documentación y estructura funcional), y aterrizándolo a decisiones compatibles con la arquitectura y reglas de HogarIA.

Objetivo principal:
- Integrar el bounded context de préstamos (`loans`) en HogarIA **sin colapsar dominios**, preservando separación contable entre:
  - capital prestado,
  - capital recuperado,
  - interés ganado,
  - estado de cartera.

---

## 2. Alcance

### 2.1 Incluye
- Contrato de consumo **read-only** de información desde cjprestamos hacia HogarIA.
- Contrato de sincronización (fase 2, condicionada) de eventos económicos.
- Reglas de idempotencia, trazabilidad, seguridad y observabilidad.
- Mapeo conceptual `cjprestamos -> HogarIA`.
- Criterios de consistencia contable y reconciliación.

### 2.2 Excluye
- Fusión de bases de datos.
- Reescritura del dominio interno de cjprestamos.
- Exposición pública de endpoints legacy sin endurecimiento de seguridad.

---

## 3. Supuestos de dominio (obligatorios)

1. **HogarIA** mantiene ownership de `profile`, `account`, `monthly planning`, `budgets`, `movements`, `KPIs`.
2. **cjprestamos** mantiene ownership de `persona`, `prestamo`, `cuota`, `pago`, `imputacion`, `legajo`.
3. `Persona` en cjprestamos **NO** equivale a `User` de HogarIA.
4. La integración debe operar con principio de menor privilegio y aislamiento por tenant.
5. Toda materialización contable en HogarIA debe poder reconstruir:
   - origen remoto,
   - timestamp de origen,
   - corrida de sincronización,
   - regla de imputación aplicada.

---

## 4. Estado real identificado en cjprestamos

Del análisis del repositorio:
- Existe documentación explícita de integración HogarIA↔cjprestamos.
- Existen endpoints temporales bajo `/api/integration/hogaria` orientados a lectura.
- El backend opera con Basic Auth (adecuado para contexto interno MVP, insuficiente para producción multi-tenant).
- El dominio contable de préstamos está suficientemente maduro para servir como fuente externa.
- Ya se explicita riesgo de:
  - spoofing de identidad entre servicios,
  - duplicación en sync sin idempotencia fuerte,
  - degradación por caída del sistema remoto.

Conclusión de arquitectura:
- **Modo recomendado actual: read-only**.
- **Sync productivo: fase 2 condicionada** a seguridad + idempotencia + reconciliación.

---

## 5. Modelo de integración objetivo

## 5.1 Patrón
- **Bounded Context externo**: `loans-external` (provider: cjprestamos).
- **Módulo HogarIA consumidor**: `external-loans`.
- **Fachada interna de HogarIA**: endpoints propios por `profileId`.
- **Anti-Corruption Layer (ACL)** en HogarIA para traducir contratos legacy a modelo canónico.

## 5.2 Principios
1. Contratos explícitos versionados (`/v1`).
2. Diseño tolerante a fallos remotos.
3. Consistencia eventual para sync.
4. Idempotencia obligatoria en cualquier escritura derivada.
5. Sin dependencia de IDs secuenciales como identidad pública.

---

## 6. Contratos API mínimos

## 6.1 Endpoints remotos consumidos (cjprestamos)
Base sugerida: `/api/integration/hogaria`

- `GET /health`
- `GET /loans/active`
- `GET /dashboard`
- `GET /control-caja`
- `GET /loans/{loanId}/installments`
- `GET /loans/{loanId}/payments`

### 6.1.1 Requisitos de contrato remoto
- Formato JSON estable.
- Campos monetarios normalizados (aclarar redondeo en contrato).
- Timezone explícita en timestamps (ISO-8601).
- Semántica de estado documentada (`ACTIVE`, `CLOSED`, etc.).

## 6.2 Endpoints de fachada HogarIA
- `GET /api/profiles/{profileId}/external-loans/summary`
- `GET /api/profiles/{profileId}/external-loans/sync-config`
- `PUT /api/profiles/{profileId}/external-loans/sync-config`
- `POST /api/profiles/{profileId}/external-loans/sync`

### 6.2.1 Semántica
- `summary`: agregación read-only de estado remoto traducido a KPIs de HogarIA.
- `sync-config`: habilitación/deshabilitación por perfil + parámetros de ejecución.
- `sync`: ejecución explícita/manual o programada controlada; debe devolver traza de corrida.

---

## 7. Contrato de datos canónico en HogarIA

## 7.1 Entidades de proyección (sugeridas)
1. `ExternalLoan`
2. `ExternalLoanInstallment`
3. `ExternalLoanPayment`
4. `ExternalLoanSyncRun`
5. `ExternalLoanMapping` (relación `profileId/accountId` con origin IDs)

## 7.2 Identidad
- `originSystem`: `CJPRESTAMOS`
- `originLoanId`: string (aunque origen sea Long)
- `originPaymentId`: string
- `originInstallmentId`: string
- IDs internos HogarIA: UUID.

## 7.3 Campos contables obligatorios
- `principalDisbursed`
- `principalRecovered`
- `interestCollected`
- `outstandingPrincipal`
- `projectedInterest`
- `arrearsAmount`

---

## 8. Reglas contables de interoperabilidad

1. El capital recuperado **no** puede sumarse como interés.
2. El interés realizado solo existe sobre excedente cobrado respecto de principal.
3. Cada pago debe poder descomponerse en:
   - `principal_component`
   - `interest_component`
4. Toda métrica agregada de dashboard debe derivar de componentes atómicos auditables.
5. Se debe preservar correlación entre pago e imputaciones a cuotas cuando exista en origen.

---

## 9. Seguridad (mínimo para habilitar fase 2)

## 9.1 Obligatorio
- Eliminar confianza en headers spoofeables para identidad de usuario final.
- Autenticación servicio-a-servicio robusta (mTLS o JWT firmado con rotación).
- Autorización en HogarIA por `profileId` + `userId` en endpoints de integración.
- Secrets fuera de repositorio y rotación periódica.

## 9.2 Recomendado
- Firmar requests internas de sync.
- Allowlist por entorno para IP/service account.
- Rate limiting por perfil y por integración.

---

## 10. Idempotencia y anti-duplicación

## 10.1 Claves idempotentes
Clave recomendada por movimiento derivado:
`hash(originSystem, originLoanId, originPaymentId, profileId, accountingRuleVersion)`

## 10.2 Estrategia de upsert
- Si existe clave idempotente: actualizar metadata sin duplicar movimiento.
- Si cambia payload remoto relevante: marcar `requires_reconciliation` y no sobrescribir silenciosamente.

## 10.3 Reintentos
- Exponential backoff.
- Reintentos solo para errores transitorios (`429`, `5xx`, timeout).
- Correr reconciliación post-reintento si estado queda incierto.

---

## 11. Observabilidad y auditoría

Cada corrida de sync debe registrar:
- `runId` UUID,
- `profileId`,
- ventana temporal consultada,
- conteos (leídos, insertados, actualizados, duplicados, omitidos, con error),
- checksum/huella del lote,
- duración,
- estado final.

Logs estructurados mínimos:
- `integration=cjprestamos`
- `originLoanId`
- `originPaymentId`
- `idempotencyKey`
- `syncRunId`
- `traceId`

---

## 12. Mapeo funcional cjprestamos -> HogarIA

| cjprestamos | HogarIA (propuesto) | Prioridad | Observación |
|---|---|---|---|
| Persona | BorrowerContact externo | Media | No mapear a User IAM |
| Prestamo | LoanExternalReference / LoanSnapshot | Alta | Núcleo de integración |
| Cuota | InstallmentSnapshot | Alta | Base para RECOVERY futuro |
| Pago | PaymentSnapshot | Alta | Fuente de movimientos |
| ImputacionPago | PaymentAllocationSnapshot | Alta | Clave para trazabilidad |
| EventoPrestamo | ExternalLoanEvent | Media | Auditoría y diagnósticos |
| Legajo | DossierExternalRef | Baja | Fase posterior |

---

## 13. Integración con planificación mensual de HogarIA

Regla del dominio HogarIA (obligatoria):
- cuotas futuras sincronizadas deben alimentar `MonthlyPlanItem` de tipo `RECOVERY`.

Implementación recomendada:
1. Detectar cuotas pendientes por horizonte (30/60/90 días o mes objetivo).
2. Proyectar `expected_recovery_date` y `expected_recovery_amount`.
3. Crear/actualizar `MonthlyPlanItem(RECOVERY)` idempotente por `originInstallmentId + month`.
4. Reconciliar al registrar pagos reales.

---

## 14. Plan de adopción por fases

## Fase 0 — Contrato y endurecimiento
- Cerrar contrato de campos y estados.
- Acordar rounding policy compartida.
- Definir matriz de errores y fallback UX.

## Fase 1 — Read-only productivo controlado
- Habilitar summary por perfil.
- Dashboards informativos sin escritura contable automática.
- Alertas de disponibilidad de proveedor.

## Fase 2 — Sync manual supervisado
- `sync` bajo operador responsable.
- Idempotencia obligatoria activa.
- Reconciliación diaria.

## Fase 3 — Sync semiautomático
- Ventanas programadas.
- Circuit breaker + colas.
- Indicadores de calidad de datos (SLO/SLA).

---

## 15. Matriz de riesgos y mitigaciones

| Riesgo | Impacto | Probabilidad | Mitigación |
|---|---|---|---|
| Duplicación de movimientos | Alto | Media | Idempotency key + upsert estricto |
| Mapeo contable incorrecto capital/interés | Alto | Media | Tests de paridad contable + rule versioning |
| Caída de cjprestamos | Medio/Alto | Alta | Timeouts, cache corta, degradación controlada |
| Fuga de credenciales integración | Alto | Media | Secret manager + rotación + scopes mínimos |
| Acceso cruzado entre perfiles | Alto | Media | Autorización estricta por `profileId/userId` |

---

## 16. Pruebas de aceptación del contrato

## 16.1 Contract tests
- Validar schema JSON de todos los endpoints remotos.
- Validar tipos, nulabilidad y compatibilidad backward.

## 16.2 Tests contables
Casos mínimos:
1. Pago parcial bajo principal.
2. Pago exacto que completa principal.
3. Pago con excedente (interés).
4. Múltiples pagos desordenados por fecha de registro.
5. Reintento del mismo pago (idempotencia).

## 16.3 Tests de seguridad
- Acceso denegado por `profileId` ajeno.
- Usuario autenticado sin permisos adecuados.
- Intento de spoofing de headers.

---

## 17. Criterios de entrada y salida para habilitar sync

## 17.1 Go/No-Go de entrada (fase 2)
- ✅ Auth servicio-a-servicio robusta.
- ✅ Idempotencia validada por tests automáticos.
- ✅ Paridad contable validada con dataset de referencia.
- ✅ Trazabilidad de run + observabilidad.

## 17.2 Go/No-Go de salida (operación estable)
- ✅ 0 duplicados en N corridas consecutivas.
- ✅ Diferencia contable neta <= umbral definido.
- ✅ Error rate remoto dentro de SLO aceptado.

---

## 18. Variables de configuración recomendadas (HogarIA)

- `CJP_INTEGRATION_ENABLED`
- `CJP_BASE_URL`
- `CJP_USERNAME`
- `CJP_PASSWORD`
- `CJP_CONNECT_TIMEOUT_MS`
- `CJP_READ_TIMEOUT_MS`
- `CJP_SYNC_ENABLED` (si aplica en implementación real)
- `CJP_SYNC_DRY_RUN_DEFAULT`
- `CJP_SYNC_MAX_RETRIES`
- `CJP_SYNC_BACKOFF_MS`

Nota: mantener naming final alineado al código real para evitar drift documental.

---

## 19. Definiciones de compatibilidad

Este contrato se considera compatible mientras:
1. No cambien nombres/tipos de campos obligatorios sin versionado.
2. No cambie semántica de cálculo contable sin incremento de `accountingRuleVersion`.
3. No cambien códigos de estado/respuesta sin publicación de changelog.

---

## 20. Decisiones cerradas

1. `loans` se integra como bounded context separado.
2. No se modela préstamo como gasto/ingreso plano.
3. Separación capital/recupero/interés es obligatoria.
4. Read-only es baseline actual recomendado.
5. Sync productivo se habilita solo con condiciones técnicas verificadas.

---

## 21. Anexos operativos

## 21.1 Checklist de corrida manual de sync
1. Verificar `health` remoto.
2. Ejecutar `summary` por perfil.
3. Ejecutar `sync` en dry-run.
4. Revisar diff contable esperado vs observado.
5. Ejecutar sync real si no hay desvíos.
6. Guardar evidencia de `runId` y reconciliación.

## 21.2 Evidencia mínima por incidente
- `syncRunId`
- payload resumido de error
- correlación `originLoanId/originPaymentId`
- decisión tomada (retry/rollback/manual fix)

---

## 22. Resultado esperado para HogarIA

Con este contrato aplicado, HogarIA obtiene:
- visibilidad robusta de cartera de préstamos externa,
- preservación de consistencia contable,
- base segura para evolucionar de lectura a sincronización,
- trazabilidad suficiente para auditoría y soporte operativo.

