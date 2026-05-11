package com.hogaria.integration.cjprestamos.remote;

import java.math.BigDecimal;

public record CjPrestamosDashboardRemoteResponse(
    BigDecimal montoInvertido,
    BigDecimal montoGanado,
    BigDecimal montoPorGanar,
    BigDecimal deudaTotal,
    Long prestamosActivos
) {}
