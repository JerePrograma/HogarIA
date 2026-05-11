package com.hogaria.integration.cjprestamos;

import com.hogaria.exception.BadRequestException;
import org.springframework.stereotype.Component;

@Component
public class CjPrestamosIntegrationConfigValidator {
  public void validateEnabledIntegration(CjPrestamosProperties properties) {
    if (!properties.enabled()) {
      return;
    }
    if (!properties.hasCompleteCredentials()) {
      throw new BadRequestException("Integración cjprestamos habilitada con configuración incompleta. Faltan: " + properties.missingRequiredFields());
    }
  }
}
