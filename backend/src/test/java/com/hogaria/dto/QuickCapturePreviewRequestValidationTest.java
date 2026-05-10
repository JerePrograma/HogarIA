package com.hogaria.dto;

import com.hogaria.dto.QuickCaptureDtos.QuickCapturePreviewRequest;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;

class QuickCapturePreviewRequestValidationTest {
  private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

  @Test void rawTextBlankFallaValidacion(){
    var violations = validator.validate(new QuickCapturePreviewRequest("   ", 2026, 6, "ARS"));
    assertFalse(violations.isEmpty());
  }

  @Test void defaultMonthInvalidoFallaValidacion(){
    var violations = validator.validate(new QuickCapturePreviewRequest("texto", 2026, 13, "ARS"));
    assertFalse(violations.isEmpty());
  }
}
