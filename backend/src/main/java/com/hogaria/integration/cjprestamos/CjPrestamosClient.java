package com.hogaria.integration.cjprestamos;

import com.hogaria.integration.cjprestamos.remote.CjPrestamosCashControlRemoteResponse;
import com.hogaria.integration.cjprestamos.remote.CjPrestamosDashboardRemoteResponse;
import com.hogaria.integration.cjprestamos.remote.CjPrestamosInstallmentRemoteResponse;
import com.hogaria.integration.cjprestamos.remote.CjPrestamosLoanActiveRemoteResponse;
import com.hogaria.integration.cjprestamos.remote.CjPrestamosPaymentRemoteResponse;
import java.util.List;
import java.util.UUID;

public interface CjPrestamosClient {

  List<CjPrestamosLoanActiveRemoteResponse> getActiveLoans(UUID profileId, UUID userId);

  CjPrestamosDashboardRemoteResponse getDashboardSummary(UUID profileId, UUID userId);

  CjPrestamosCashControlRemoteResponse getCashControl(UUID profileId, UUID userId);

  List<CjPrestamosInstallmentRemoteResponse> getLoanInstallments(UUID profileId, UUID userId, Long externalLoanId);

  List<CjPrestamosPaymentRemoteResponse> getLoanPayments(UUID profileId, UUID userId, Long externalLoanId);
}
