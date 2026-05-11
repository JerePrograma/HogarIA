package com.hogaria.integration.cjprestamos.remote;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.fasterxml.jackson.databind.json.JsonMapper;
import org.junit.jupiter.api.Test;

class CjPrestamosRemoteDeserializationTest {
  private final JsonMapper mapper = JsonMapper.builder().findAndAddModules().build();

  @Test void deserializesActiveLoans() throws Exception {
    String json = """
        [{"id":1,"personaId":9,"personaNombre":"Ana","montoInicial":1000.50,"cantidadCuotas":10,"frecuenciaTipo":"MENSUAL","estado":"ACTIVO","totalCobrado":200,"totalPendiente":800.50,"gananciaRealizada":40,"gananciaProyectada":120,"createdAt":"2026-01-02T10:15:30","updatedAt":"2026-01-03T10:15:30"}]
        """;
    var value = mapper.readValue(json, CjPrestamosLoanActiveRemoteResponse[].class);
    assertEquals(1L, value[0].id());
    assertEquals("Ana", value[0].personaNombre());
  }

  @Test void deserializesDashboard() throws Exception {
    String json = "{" +
        "\"montoInvertido\":1000,\"montoGanado\":120,\"montoPorGanar\":80,\"deudaTotal\":500,\"prestamosActivos\":4}";
    var value = mapper.readValue(json, CjPrestamosDashboardRemoteResponse.class);
    assertEquals(4L, value.prestamosActivos());
  }

  @Test void deserializesCashControl() throws Exception {
    String json = "{" +
        "\"cajaDisponible\":1,\"inversionActiva\":2,\"capitalRecuperado\":3,\"capitalPendiente\":4,\"gananciaRealizada\":5,\"gananciaProyectada\":6," +
        "\"ingresosMesActual\":7,\"egresosMesActual\":8,\"balanceMesActual\":9,\"proyeccionCobro30Dias\":10,\"proyeccionCobro60Dias\":11,\"proyeccionCobro90Dias\":12," +
        "\"carteraEnMora\":13,\"cuotasPendientes\":14,\"cuotasVencenProximos7Dias\":15,\"recuperoCapitalPorcentaje\":16,\"rendimientoEsperadoPorcentaje\":17}";
    var value = mapper.readValue(json, CjPrestamosCashControlRemoteResponse.class);
    assertEquals(14L, value.cuotasPendientes());
  }

  @Test void deserializesInstallments() throws Exception {
    String json = "[{\"id\":2,\"prestamoId\":1,\"numeroCuota\":3,\"fechaVencimiento\":\"2026-06-01\",\"montoProgramado\":100,\"montoPagado\":50,\"saldoPendiente\":50,\"estado\":\"PENDIENTE\"}]";
    var value = mapper.readValue(json, CjPrestamosInstallmentRemoteResponse[].class);
    assertEquals(3, value[0].numeroCuota());
  }

  @Test void deserializesPayments() throws Exception {
    String json = "[{\"id\":9,\"prestamoId\":1,\"fechaPago\":\"2026-06-03\",\"monto\":88,\"principalRecovered\":50,\"interestCollected\":38,\"referenciaManual\":\"TRX-1\",\"observaciones\":\"ok\",\"estado\":\"CONFIRMADO\"}]";
    var value = mapper.readValue(json, CjPrestamosPaymentRemoteResponse[].class);
    assertEquals("TRX-1", value[0].referenciaManual());
    assertEquals(0, value[0].principalRecovered().compareTo(new java.math.BigDecimal("50")));
    assertEquals(0, value[0].interestCollected().compareTo(new java.math.BigDecimal("38")));
  }
}
