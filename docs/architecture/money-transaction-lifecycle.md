# MoneyTransaction lifecycle

`MoneyTransaction` no debe eliminarse directamente desde servicios de negocio.
Toda remoción operativa debe pasar por `TransactionLifecycleService`.

Semántica:

- Eliminar: borra físicamente la fila cuando no hay trazabilidad externa que preservar.
- Ignorar: conserva la fila con `status=IGNORED` y `classificationStatus=IGNORED_BY_RULE`.
- Desvincular: separa el movimiento real de planificación mensual (`monthly_plan_item.transaction_id`
  y `monthly_plan_transaction_match`) para que el plan vuelva a quedar pendiente.
- Sincronizar/importar: preserva idempotencia y trazabilidad; por eso se resuelve como soft-ignore.

Política actual:

- `MANUAL` sin trazabilidad externa: `PHYSICAL_DELETE + UNLINK_MONTHLY_PLAN`.
- `SYSTEM` sin trazabilidad externa: `PHYSICAL_DELETE + UNLINK_MONTHLY_PLAN`.
- `IMPORT`: `SOFT_IGNORE + UNLINK_MONTHLY_PLAN`.
- `ExternalSyncMapping`: `SOFT_IGNORE + UNLINK_MONTHLY_PLAN`.
- `source=CJPRESTAMOS`: `SOFT_IGNORE + UNLINK_MONTHLY_PLAN`.

El contrato REST de `DELETE /api/transactions/{id}` devuelve siempre el resultado efectivo para evitar
que una fila conservada como `IGNORED` sea interpretada por el frontend como desaparecida.
