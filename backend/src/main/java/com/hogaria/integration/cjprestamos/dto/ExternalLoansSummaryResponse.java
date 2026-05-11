package com.hogaria.integration.cjprestamos.dto;

import java.util.List;

public record ExternalLoansSummaryResponse(
    String status,
    String message,
    ExternalLoanSummaryResponse dashboard,
    ExternalLoanCashControlResponse cashControl,
    List<ExternalLoanResponse> activeLoans
) {
  public static ExternalLoansSummaryResponse disabled() {
    return new ExternalLoansSummaryResponse("DISABLED", "Integración cjprestamos deshabilitada", null, null, List.of());
  }

  public static ExternalLoansSummaryResponse enabled(
      ExternalLoanSummaryResponse dashboard,
      ExternalLoanCashControlResponse cashControl,
      List<ExternalLoanResponse> activeLoans) {
    return new ExternalLoansSummaryResponse("ENABLED", "OK", dashboard, cashControl, activeLoans);
  }
}
