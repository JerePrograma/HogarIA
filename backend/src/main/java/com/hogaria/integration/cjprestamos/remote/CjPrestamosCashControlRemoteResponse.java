package com.hogaria.integration.cjprestamos.remote;

import java.math.BigDecimal;

public record CjPrestamosCashControlRemoteResponse(
    BigDecimal cajaDisponible,
    BigDecimal inversionActiva,
    BigDecimal capitalRecuperado,
    BigDecimal capitalPendiente,
    BigDecimal gananciaRealizada,
    BigDecimal gananciaProyectada,
    BigDecimal ingresosMesActual,
    BigDecimal egresosMesActual,
    BigDecimal balanceMesActual,
    BigDecimal proyeccionCobro30Dias,
    BigDecimal proyeccionCobro60Dias,
    BigDecimal proyeccionCobro90Dias,
    BigDecimal carteraEnMora,
    Long cuotasPendientes,
    Long cuotasVencenProximos7Dias,
    BigDecimal recuperoCapitalPorcentaje,
    BigDecimal rendimientoEsperadoPorcentaje
) {}
