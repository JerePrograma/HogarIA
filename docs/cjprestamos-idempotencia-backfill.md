# CJPrestamos: idempotencia y backfill

Este cambio refuerza que la sync de cjprestamos no duplique movimientos y habilita backfill seguro de `external_sync_mapping` para movimientos históricos.

## Capas de idempotencia
1. `external_sync_mapping` por evento externo.
2. `money_transaction` por `source + sourceOperationId`.
3. `money_transaction` por `sourceHash` determinístico.

## Sync y dry-run
- Sync crea movimientos de desembolso/recupero/interés con trazabilidad (`source`, `sourceOperationId`, `sourceHash`, `origin=SYSTEM`, `classificationReason`).
- Dry-run lista movimientos planificados y omisiones por duplicado.

## Backfill
- `POST /external-loans/backfill/dry-run` detecta candidatos desde `sourceOperationId` o `description`.
- `POST /external-loans/backfill/apply` crea mappings (sin modificar transacciones).
- Por defecto debe enviarse `includeLowConfidence=false`. Los candidatos `LOW` requieren revisión manual antes de cualquier aplicación explícita.

## Diagnóstico operativo
- `GET /external-loans/idempotency/diagnostics` resume candidatos CJ, mappings existentes, recomendación de backfill y duplicados que bloquearían los índices estrictos del baseline `V1__baseline_schema_and_seed.sql`.
- El endpoint es solo lectura y reutiliza el dry-run de backfill para clasificar candidatos.
- Los duplicados se reportan por grupos de `(profile_id, source, source_operation_id)` y `(profile_id, source_hash)`, con muestras limitadas para revisión humana.

## Flujo operativo recomendado
1. Ejecutar health.
2. Ejecutar diagnóstico de idempotencia.
3. Ejecutar sync dry-run.
4. Si `backfillRecommended=true`, ejecutar backfill dry-run.
5. Aplicar backfill seguro con `includeLowConfidence=false`.
6. Ejecutar sync dry-run nuevamente.
7. Ejecutar sync.

## Conceptos clave
- `skippedDuplicates`: eventos externos que la sync no crearía porque ya detectó duplicado por mapping, `sourceOperationId` o `sourceHash`.
- `detectedExistingWithoutMapping`: subconjunto de duplicados donde existe una `money_transaction` compatible pero falta el registro en `external_sync_mapping`; normalmente habilita backfill.
- `backfillRecommended`: hay candidatos CJ sin mapping que podrían recibir `external_sync_mapping` sin tocar la transacción original.
- `hasIndexBlockingDuplicates`: existen filas actuales de `money_transaction` que violarían los índices únicos estrictos del baseline y deben resolverse antes de sincronizar.

## Qué NO modifica
- No altera `MoneyTransaction` existentes.
- No hace sync destructivo.
- El diagnóstico no escribe en base de datos.
- El backfill seguro no auto-aplica candidatos `LOW`.

## Validación manual
1. Ejecutar sync una vez y validar movimientos creados.
2. Reejecutar sync/dry-run y verificar duplicados omitidos.
3. Ejecutar backfill dry-run y luego apply en perfil con históricos CJ.

## SQL preflight manual
El script read-only está en:

`backend/src/main/resources/db/diagnostics/cjprestamos_idempotency_preflight.sql`

Sirve para revisar duplicados bloqueantes, transacciones CJ sin mapping y mappings que apuntan a transacciones inexistentes antes o después de recrear una base con el baseline. No debe registrarse como migración.
