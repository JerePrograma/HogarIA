package com.hogaria.integration.cjprestamos;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.integrations.cjprestamos")
public record CjPrestamosProperties(
    boolean enabled,
    boolean syncEnabled,
    String baseUrl,
    String apiPrefix,
    String username,
    String password,
    int connectTimeoutMs,
    int readTimeoutMs
) {
  public boolean hasCompleteCredentials() {
    return baseUrl != null && !baseUrl.isBlank()
        && username != null && !username.isBlank()
        && password != null && !password.isBlank();
  }

  public String missingRequiredFields() {
    java.util.List<String> missing = new java.util.ArrayList<>();
    if (baseUrl == null || baseUrl.isBlank()) missing.add("CJP_BASE_URL");
    if (username == null || username.isBlank()) missing.add("CJP_USERNAME");
    if (password == null || password.isBlank()) missing.add("CJP_PASSWORD");
    return String.join(", ", missing);
  }

  public String resolvedApiPrefix() {
    if (apiPrefix == null || apiPrefix.isBlank()) return "/api/integration/hogaria";
    return apiPrefix.startsWith("/") ? apiPrefix : "/" + apiPrefix;
  }
}
