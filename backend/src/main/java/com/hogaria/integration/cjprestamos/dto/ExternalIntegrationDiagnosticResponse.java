package com.hogaria.integration.cjprestamos.dto;

import java.util.List;

public record ExternalIntegrationDiagnosticResponse(
    String status,
    String message,
    boolean integrationEnabled,
    boolean syncEnabled,
    String baseUrl,
    String apiPrefix,
    boolean hasUsername,
    boolean hasPassword,
    int connectTimeoutMs,
    int readTimeoutMs,
    boolean remoteCheckExecuted,
    List<String> missingFields
) {
  public static ExternalIntegrationDiagnosticResponse of(
      String status,
      String message,
      boolean integrationEnabled,
      boolean syncEnabled,
      String baseUrl,
      String apiPrefix,
      boolean hasUsername,
      boolean hasPassword,
      int connectTimeoutMs,
      int readTimeoutMs,
      boolean remoteCheckExecuted,
      List<String> missingFields) {
    return new ExternalIntegrationDiagnosticResponse(status, message, integrationEnabled, syncEnabled, baseUrl, apiPrefix, hasUsername, hasPassword, connectTimeoutMs, readTimeoutMs, remoteCheckExecuted, missingFields);
  }
}
