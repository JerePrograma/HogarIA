package com.hogaria.integration.cjprestamos;

import com.hogaria.integration.cjprestamos.dto.ExternalLoanCashControlResponse;
import com.hogaria.integration.cjprestamos.dto.ExternalLoanInstallmentResponse;
import com.hogaria.integration.cjprestamos.dto.ExternalLoanPaymentResponse;
import com.hogaria.integration.cjprestamos.dto.ExternalLoanResponse;
import com.hogaria.integration.cjprestamos.dto.ExternalLoanSummaryResponse;
import java.util.List;
import java.util.UUID;

public interface CjPrestamosClient {

  List<ExternalLoanResponse> getActiveLoans(UUID profileId, UUID userId);

  ExternalLoanSummaryResponse getDashboardSummary(UUID profileId, UUID userId);

  ExternalLoanCashControlResponse getCashControl(UUID profileId, UUID userId);

  List<ExternalLoanInstallmentResponse> getLoanInstallments(UUID profileId, UUID userId, UUID loanId);

  List<ExternalLoanPaymentResponse> getLoanPayments(UUID profileId, UUID userId, UUID loanId);
}
