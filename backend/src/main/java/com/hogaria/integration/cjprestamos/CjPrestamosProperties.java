package com.hogaria.integration.cjprestamos;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "integration.cjprestamos")
public record CjPrestamosProperties(
    boolean enabled,
    String baseUrl,
    String username,
    String password,
    int connectTimeoutMillis,
    int readTimeoutMillis
) {}
