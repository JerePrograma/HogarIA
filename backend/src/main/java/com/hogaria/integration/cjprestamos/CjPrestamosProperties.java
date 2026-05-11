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
  public String resolvedApiPrefix() {
    if (apiPrefix == null || apiPrefix.isBlank()) return "/api/integration/hogaria";
    return apiPrefix.startsWith("/") ? apiPrefix : "/" + apiPrefix;
  }
}
