package com.hogaria.security;

import java.util.Arrays;
import java.util.Locale;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

@Component
public class SecurityConfigStartupValidator implements ApplicationRunner {

  private static final String INSECURE_SECRET = "change_me";

  private final Environment environment;

  @Value("${app.jwt.secret:change_me}")
  private String jwtSecret;

  public SecurityConfigStartupValidator(Environment environment) {
    this.environment = environment;
  }

  @Override
  public void run(ApplicationArguments args) {
    boolean localOrTest = Arrays.stream(environment.getActiveProfiles())
        .map(p -> p.toLowerCase(Locale.ROOT))
        .anyMatch(p -> p.equals("local") || p.equals("test"));

    if (!localOrTest && (jwtSecret == null || jwtSecret.isBlank() || INSECURE_SECRET.equals(jwtSecret))) {
      throw new IllegalStateException("JWT secret inseguro: en perfiles no local/test debe definir app.jwt.secret con valor seguro.");
    }
  }
}
