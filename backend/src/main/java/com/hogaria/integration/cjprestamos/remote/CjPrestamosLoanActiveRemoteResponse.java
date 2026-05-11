package com.hogaria.integration.cjprestamos.remote;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record CjPrestamosLoanActiveRemoteResponse(
    Long id,
    Long personaId,
    String personaNombre,
    BigDecimal montoInicial,
    Integer cantidadCuotas,
    String frecuenciaTipo,
    String estado,
    BigDecimal totalCobrado,
    BigDecimal totalPendiente,
    BigDecimal gananciaRealizada,
    BigDecimal gananciaProyectada,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {}
