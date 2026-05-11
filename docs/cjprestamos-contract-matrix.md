# Matriz de contrato HogarIA ↔ cjprestamos (estado actual)

## Alcance y limitación real de esta verificación
- Se inspeccionó el cliente real de HogarIA (`HttpCjPrestamosClient`) y sus DTOs remotos.
- No se pudo inspeccionar el servidor `cjprestamos` porque la ruta indicada `/workspace/cjprestamos` no existe en este entorno.
- Por ese motivo, la compatibilidad servidor-cliente queda en estado **no verificable** para todos los endpoints.

## Matriz endpoint por endpoint

| Endpoint HogarIA consume | Endpoint cjprestamos expone | DTO HogarIA | DTO cjprestamos | Compatible | Riesgo | Acción |
|---|---|---|---|---|---|---|
| `GET /api/integration/hogaria/loans/active` + headers `X-Profile-Id`, `X-User-Id` | No verificable (repo no disponible) | `CjPrestamosLoanActiveRemoteResponse[]` | No verificable | No verificable | Alto (rotura total de deserialización o 4xx/5xx si cambia contrato/ruta/headers) | No avanzar hasta contrastar controller/dto reales de cjprestamos |
| `GET /api/integration/hogaria/dashboard` + headers `X-Profile-Id`, `X-User-Id` | No verificable (repo no disponible) | `CjPrestamosDashboardRemoteResponse` | No verificable | No verificable | Alto (dashboard inconsistente o error de parseo) | No avanzar hasta contrastar controller/dto reales de cjprestamos |
| `GET /api/integration/hogaria/control-caja` + headers `X-Profile-Id`, `X-User-Id` | No verificable (repo no disponible) | `CjPrestamosCashControlRemoteResponse` | No verificable | No verificable | Alto (errores de negocio en KPIs/caja) | No avanzar hasta contrastar controller/dto reales de cjprestamos |
| `GET /api/integration/hogaria/loans/{externalLoanId}/installments` + headers `X-Profile-Id`, `X-User-Id` | No verificable (repo no disponible) | `CjPrestamosInstallmentRemoteResponse[]` | No verificable | No verificable | Alto (cuotas incompletas/incorrectas) | No avanzar hasta contrastar controller/dto reales de cjprestamos |
| `GET /api/integration/hogaria/loans/{externalLoanId}/payments` + headers `X-Profile-Id`, `X-User-Id` | No verificable (repo no disponible) | `CjPrestamosPaymentRemoteResponse[]` | No verificable | No verificable | Alto (split capital/interés inconsistente) | No avanzar hasta contrastar controller/dto reales de cjprestamos |

## Request headers observados en el cliente HogarIA
- `X-Profile-Id` (UUID serializado)
- `X-User-Id` (UUID serializado)
- Auth básica HTTP configurada desde propiedades (username/password)

## Campo JSON esperado por endpoint (lado HogarIA)
> Nota: al usar Java `record` sin anotaciones `@JsonProperty`, el nombre esperado del JSON coincide con el nombre del componente del record.

### loans/active (`CjPrestamosLoanActiveRemoteResponse`)
- `id: Long`
- `personaId: Long`
- `personaNombre: String`
- `montoInicial: BigDecimal`
- `cantidadCuotas: Integer`
- `frecuenciaTipo: String`
- `estado: String`
- `totalCobrado: BigDecimal`
- `totalPendiente: BigDecimal`
- `gananciaRealizada: BigDecimal`
- `gananciaProyectada: BigDecimal`
- `createdAt: LocalDateTime`
- `updatedAt: LocalDateTime`

### dashboard (`CjPrestamosDashboardRemoteResponse`)
- `montoInvertido: BigDecimal`
- `montoGanado: BigDecimal`
- `montoPorGanar: BigDecimal`
- `deudaTotal: BigDecimal`
- `prestamosActivos: Long`

### control-caja (`CjPrestamosCashControlRemoteResponse`)
- `cajaDisponible: BigDecimal`
- `inversionActiva: BigDecimal`
- `capitalRecuperado: BigDecimal`
- `capitalPendiente: BigDecimal`
- `gananciaRealizada: BigDecimal`
- `gananciaProyectada: BigDecimal`
- `ingresosMesActual: BigDecimal`
- `egresosMesActual: BigDecimal`
- `balanceMesActual: BigDecimal`
- `proyeccionCobro30Dias: BigDecimal`
- `proyeccionCobro60Dias: BigDecimal`
- `proyeccionCobro90Dias: BigDecimal`
- `carteraEnMora: BigDecimal`
- `cuotasPendientes: Long`
- `cuotasVencenProximos7Dias: Long`
- `recuperoCapitalPorcentaje: BigDecimal`
- `rendimientoEsperadoPorcentaje: BigDecimal`

### loans/{id}/installments (`CjPrestamosInstallmentRemoteResponse`)
- `id: Long`
- `prestamoId: Long`
- `numeroCuota: Integer`
- `fechaVencimiento: LocalDate`
- `montoProgramado: BigDecimal`
- `montoPagado: BigDecimal`
- `saldoPendiente: BigDecimal`
- `estado: String`

### loans/{id}/payments (`CjPrestamosPaymentRemoteResponse`)
- `id: Long`
- `prestamoId: Long`
- `fechaPago: LocalDate`
- `monto: BigDecimal`
- `principalRecovered: BigDecimal`
- `interestCollected: BigDecimal`
- `referenciaManual: String`
- `observaciones: String`
- `estado: String`

## Nullable / no nullable (cliente HogarIA)
- En records de Java, a nivel tipo todos los campos son referencias (incluyendo `Long`, `BigDecimal`, etc.) y por tanto **potencialmente null** en runtime.
- No hay anotaciones de validación (`@NotNull`) ni defaults en los DTO remotos.
- Conclusión: la nulabilidad real depende 100% del servidor cjprestamos; sin su código no se puede certificar no-nullables.

## Semántica funcional inferida (desde el cliente)
- Integración read-only por HTTP GET.
- Dependencia de auth básica + headers de contexto (`profileId`, `userId`).
- Cualquier cambio de nombre de campo JSON, tipo, ruta, formato de fecha o política de headers puede romper el bridge.

## Compatibilidades confirmadas
- Solo se confirma coherencia interna de HogarIA entre rutas consumidas y DTO destino.
- No hay confirmación cruzada con cjprestamos en este entorno.

## Diferencias bloqueantes
- Bloqueante operativo: falta el repositorio/ruta `/workspace/cjprestamos` para validar contrato real contra servidor.

## Recomendación
- **No avanzar** al siguiente PR de lógica hasta montar/acceder al código real de `cjprestamos` y completar la comparación endpoint a endpoint solicitada.
