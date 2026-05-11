package com.hogaria.integration.cjprestamos.mapper;

import com.hogaria.integration.cjprestamos.dto.*;
import com.hogaria.integration.cjprestamos.remote.*;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class CjPrestamosBridgeMapper {

  public ExternalLoanResponse toExternalLoan(CjPrestamosLoanActiveRemoteResponse remote) {
    return new ExternalLoanResponse(
        remote.id(),
        remote.personaId(),
        remote.personaNombre(),
        remote.montoInicial(),
        remote.totalCobrado(),
        remote.totalPendiente(),
        remote.gananciaRealizada(),
        remote.gananciaProyectada(),
        remote.estado());
  }

  public List<ExternalLoanResponse> toExternalLoans(List<CjPrestamosLoanActiveRemoteResponse> remote) {
    return remote.stream().map(this::toExternalLoan).toList();
  }

  public ExternalLoanSummaryResponse toExternalDashboard(CjPrestamosDashboardRemoteResponse remote) {
    return new ExternalLoanSummaryResponse(
        remote.montoInvertido(),
        remote.montoGanado(),
        remote.montoPorGanar(),
        remote.deudaTotal(),
        remote.prestamosActivos());
  }

  public ExternalLoanCashControlResponse toExternalCashControl(CjPrestamosCashControlRemoteResponse remote) {
    return new ExternalLoanCashControlResponse(
        remote.cajaDisponible(), remote.inversionActiva(), remote.capitalRecuperado(), remote.capitalPendiente(),
        remote.gananciaRealizada(), remote.gananciaProyectada(), remote.ingresosMesActual(), remote.egresosMesActual(),
        remote.balanceMesActual(), remote.proyeccionCobro30Dias(), remote.proyeccionCobro60Dias(), remote.proyeccionCobro90Dias(),
        remote.carteraEnMora(), remote.cuotasPendientes(), remote.cuotasVencenProximos7Dias(), remote.recuperoCapitalPorcentaje(),
        remote.rendimientoEsperadoPorcentaje());
  }

  public ExternalLoanInstallmentResponse toExternalInstallment(CjPrestamosInstallmentRemoteResponse remote) {
    return new ExternalLoanInstallmentResponse(
        remote.id(), remote.prestamoId(), remote.numeroCuota(), remote.fechaVencimiento(), remote.montoProgramado(), remote.montoPagado(), remote.saldoPendiente(), remote.estado());
  }

  public List<ExternalLoanInstallmentResponse> toExternalInstallments(List<CjPrestamosInstallmentRemoteResponse> remote) {
    return remote.stream().map(this::toExternalInstallment).toList();
  }

  public ExternalLoanPaymentResponse toExternalPayment(CjPrestamosPaymentRemoteResponse remote) {
    return new ExternalLoanPaymentResponse(
        remote.id(), remote.prestamoId(), remote.fechaPago(), remote.monto(), remote.principalRecovered(), remote.interestCollected(), remote.referenciaManual(), remote.observaciones(), remote.estado());
  }

  public List<ExternalLoanPaymentResponse> toExternalPayments(List<CjPrestamosPaymentRemoteResponse> remote) {
    return remote.stream().map(this::toExternalPayment).toList();
  }
}
