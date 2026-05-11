package com.hogaria.integration.cjprestamos.remote;

import java.math.BigDecimal;
import java.time.LocalDate;

public record CjPrestamosPaymentRemoteResponse(
    Long id,
    Long prestamoId,
    LocalDate fechaPago,
    BigDecimal monto,
    String referenciaManual,
    String observaciones,
    String estado
) {}
