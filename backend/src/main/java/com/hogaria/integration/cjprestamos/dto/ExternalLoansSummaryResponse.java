package com.hogaria.integration.cjprestamos.dto;

import java.util.List;

public record ExternalLoansSummaryResponse(
    String status,
    String message,
    boolean readOnly,
    ExternalLoanSummaryResponse dashboard,
    ExternalLoanCashControlResponse cashControl,
    List<ExternalLoanResponse> activeLoans
) {
  public static ExternalLoansSummaryResponse disabled() {
    return new ExternalLoansSummaryResponse("DISABLED", "Integración cjprestamos deshabilitada", true, null, null, List.of());
  }

  public static ExternalLoansSummaryResponse enabled(
      ExternalLoanSummaryResponse dashboard,
      ExternalLoanCashControlResponse cashControl,
      List<ExternalLoanResponse> activeLoans,
      boolean readOnly) {
    return new ExternalLoansSummaryResponse("ENABLED", "OK", readOnly, dashboard, cashControl, activeLoans);
  }
}
