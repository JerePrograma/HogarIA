package com.hogaria.security;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.hogaria.config.SecurityConfig;
import com.hogaria.controller.ProfileController;
import com.hogaria.dto.ProfileDtos.ProfileResponse;
import com.hogaria.service.ProfileService;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = ProfileController.class)
@Import({SecurityConfig.class, CurrentUserResolver.class, JwtAuthenticationFilter.class, JwtService.class})
@TestPropertySource(properties = {"app.jwt.secret=test-secret", "app.security.allow-x-user-id-fallback=false"})
class SecurityHardeningWebMvcTest {
  @Autowired private MockMvc mockMvc;
  @Autowired private JwtService jwtService;
  @MockBean private ProfileService profileService;

  @Test
  void financialEndpointWithoutAuthReturns401() throws Exception {
    mockMvc.perform(get("/api/profiles")).andExpect(status().isUnauthorized());
  }

  @Test
  void financialEndpointWithInvalidTokenReturns401() throws Exception {
    mockMvc.perform(get("/api/profiles").header("Authorization", "Bearer invalid.token.here")).andExpect(status().isUnauthorized());
  }

  @Test
  void financialEndpointWithValidTokenReturns200AndIgnoresXUserId() throws Exception {
    UUID tokenUser = UUID.randomUUID();
    when(profileService.list(any())).thenReturn(List.of(new ProfileResponse(UUID.randomUUID(), "home", null, "ARS", 2026, true, null, null)));
    String token = jwtService.generateToken(tokenUser, "a@a.com");

    mockMvc.perform(get("/api/profiles")
            .header("Authorization", "Bearer " + token)
            .header("X-User-Id", UUID.randomUUID().toString()))
        .andExpect(status().isOk());

    verify(profileService).list(tokenUser);
  }
}
