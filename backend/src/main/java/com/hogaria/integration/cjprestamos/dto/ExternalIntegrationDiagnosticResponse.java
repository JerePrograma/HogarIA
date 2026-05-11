package com.hogaria.integration.cjprestamos.dto;

public record ExternalIntegrationDiagnosticResponse(String status, String message) {
  public static ExternalIntegrationDiagnosticResponse ok(String message) {
    return new ExternalIntegrationDiagnosticResponse("OK", message);
  }

  public static ExternalIntegrationDiagnosticResponse unavailable(String message) {
    return new ExternalIntegrationDiagnosticResponse("UNAVAILABLE", message);
  }

  public static ExternalIntegrationDiagnosticResponse unauthorized(String message) {
    return new ExternalIntegrationDiagnosticResponse("UNAUTHORIZED", message);
  }

  public static ExternalIntegrationDiagnosticResponse misconfigured(String message) {
    return new ExternalIntegrationDiagnosticResponse("MISCONFIGURED", message);
  }
}
