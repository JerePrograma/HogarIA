package com.hogaria.security;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;
import static org.mockito.Mockito.when;

import org.springframework.core.env.Environment;
import org.mockito.Mockito;
import org.springframework.test.util.ReflectionTestUtils;

class SecurityConfigStartupValidatorTest {

  @Test
  void localProfileAllowsInsecureSecret() {
    var validator = new SecurityConfigStartupValidator(environmentWithProfiles("local"));
    ReflectionTestUtils.setField(validator, "jwtSecret", "change_me");

    assertDoesNotThrow(() -> validator.run(null));
  }

  @Test
  void testProfileAllowsInsecureSecret() {
    var validator = new SecurityConfigStartupValidator(environmentWithProfiles("test"));
    ReflectionTestUtils.setField(validator, "jwtSecret", "change_me");

    assertDoesNotThrow(() -> validator.run(null));
  }

  @Test
  void nonLocalProfileBlocksInsecureSecret() {
    var validator = new SecurityConfigStartupValidator(environmentWithProfiles("prod"));
    ReflectionTestUtils.setField(validator, "jwtSecret", "change_me");

    assertThrows(IllegalStateException.class, () -> validator.run(null));
  }

  private Environment environmentWithProfiles(String... profiles) {
    var environment = Mockito.mock(Environment.class);
    when(environment.getActiveProfiles()).thenReturn(profiles);
    return environment;
  }
}
