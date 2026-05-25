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

## Qué NO modifica
- No altera `MoneyTransaction` existentes.
- No hace sync destructivo.

## Validación manual
1. Ejecutar sync una vez y validar movimientos creados.
2. Reejecutar sync/dry-run y verificar duplicados omitidos.
3. Ejecutar backfill dry-run y luego apply en perfil con históricos CJ.
