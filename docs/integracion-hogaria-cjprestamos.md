# Integración HogarIA ↔ cjprestamos

## 1) Objetivo de la integración
La integración permite que HogarIA consuma información operativa de préstamos desde **cjprestamos** sin duplicar la lógica core de originación/cobranza. 

Estado actual en HogarIA:
- Existe el módulo `external-loans`.
- Existe cliente HTTP hacia cjprestamos.
- Existe endpoint de `summary` (consulta).
- Existe `sync-config` (configuración por perfil para eventual sincronización contable).
- Existe endpoint de `sync` manual, con guardas por feature flag y validaciones.

## 2) Dueño de cada dominio
**HogarIA (system of record):**
- perfiles,
- cuentas,
- categorías,
- movimientos,
- presupuesto.

**cjprestamos (system of record):**
- personas,
- préstamos,
- cuotas,
- pagos,
- imputaciones,
- legajos.

## 3) Por qué NO se fusionan bases
No se fusionan bases porque cada sistema resuelve un bounded context distinto y con reglas de negocio propias.

Razones prácticas:
- Evita acoplar modelos con ciclos de cambio diferentes.
- Mantiene límites de responsabilidad claros por dominio.
- Reduce riesgo operativo (un incidente en un sistema no obliga a cambios estructurales en el otro).
- Permite evolucionar integración por contrato API versionable.

## 4) Endpoints que HogarIA expone
Base: `/api/profiles/{profileId}/external-loans`

- `GET /api/profiles/{profileId}/external-loans/summary`
- `GET /api/profiles/{profileId}/external-loans/sync-config`
- `PUT /api/profiles/{profileId}/external-loans/sync-config`
- `POST /api/profiles/{profileId}/external-loans/sync`

## 5) Endpoints remotos de cjprestamos consumidos
- `GET /api/integration/hogaria/loans/active`
- `GET /api/integration/hogaria/dashboard`
- `GET /api/integration/hogaria/control-caja`
- `GET /api/integration/hogaria/loans/{loanId}/installments`
- `GET /api/integration/hogaria/loans/{loanId}/payments`

## 6) Variables de entorno
Configuración actual (backend):

- `CJP_INTEGRATION_ENABLED`: habilita/deshabilita la integración externa.
- `CJP_SYNC_ENABLED`: habilita/deshabilita la ejecución de sync contable manual.
- `CJP_BASE_URL`: URL base de cjprestamos.
- `CJP_USERNAME`: usuario para autenticación hacia cjprestamos.
- `CJP_PASSWORD`: contraseña para autenticación hacia cjprestamos.
- `CJP_CONNECT_TIMEOUT_MS`: timeout de conexión HTTP.
- `CJP_READ_TIMEOUT_MS`: timeout de lectura HTTP.

## 7) Modo read-only
La integración debe considerarse **read-only por defecto**:
- `summary` consulta datos remotos sin escribir en HogarIA.
- Cuando `CJP_SYNC_ENABLED=false`, el endpoint `sync` rechaza ejecución y devuelve error controlado indicando que la sincronización contable está deshabilitada.

## 8) Modo sync (fase 2)
`sync` existe como capacidad manual y condicionada, pero **no se recomienda como esquema de producción definitivo** mientras no estén resueltos seguridad end-to-end y lineamientos operativos de solo lectura vs escritura.

Uso esperado hoy:
- habilitar explícitamente con `CJP_SYNC_ENABLED=true`,
- configurar mapeos por perfil en `sync-config`,
- ejecutar manualmente bajo control operativo.

## 9) Riesgos conocidos
- **Spoofing de identidad**: `X-User-Id` es spoofeable si no hay auth real/end-to-end.
- **Duplicación de movimientos**: riesgo por reintentos o eventos externos repetidos; se mitiga con idempotencia, pero requiere monitoreo.
- **Split contable incorrecto**: diferencia entre capital recuperado e interés debe mantenerse explícita para evitar distorsión en KPIs.
- **Dependencia de disponibilidad**: caída o degradación de cjprestamos impacta summary/sync en HogarIA.

## 10) Comandos de validación
Como este cambio es documental, validación mínima sugerida:

```bash
# Verificar cambios pendientes
cd /workspace/HogarIA && git status --short

# Revisar README actualizado
cd /workspace/HogarIA && sed -n '1,260p' README.md

# Revisar documento de integración
cd /workspace/HogarIA && sed -n '1,260p' docs/integracion-hogaria-cjprestamos.md
```
