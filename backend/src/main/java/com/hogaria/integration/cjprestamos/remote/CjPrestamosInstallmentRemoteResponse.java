package com.hogaria.integration.cjprestamos.remote;

import java.math.BigDecimal;
import java.time.LocalDate;

public record CjPrestamosInstallmentRemoteResponse(
    Long id,
    Long prestamoId,
    Integer numeroCuota,
    LocalDate fechaVencimiento,
    BigDecimal montoProgramado,
    BigDecimal montoPagado,
    BigDecimal saldoPendiente,
    String estado
) {}
