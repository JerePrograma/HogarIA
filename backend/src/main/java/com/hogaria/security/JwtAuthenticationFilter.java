package com.hogaria.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {
  private final JwtService jwtService;

  @Value("${app.security.allow-x-user-id-fallback:false}")
  private boolean allowXUserIdFallback;

  public JwtAuthenticationFilter(JwtService jwtService) {
    this.jwtService = jwtService;
  }

  @Override
  protected void doFilterInternal(
          HttpServletRequest request,
          HttpServletResponse response,
          FilterChain filterChain
  ) throws ServletException, IOException {

    String auth = request.getHeader("Authorization");

    if (auth != null && auth.startsWith("Bearer ")) {
      UUID userId = jwtService.parseUserId(auth.substring(7));

      if (userId != null) {
        authenticate(userId);
      }
    }

    if (SecurityContextHolder.getContext().getAuthentication() == null && allowXUserIdFallback) {
      String xUserId = request.getHeader("X-User-Id");

      if (xUserId != null && !xUserId.isBlank()) {
        try {
          authenticate(UUID.fromString(xUserId));
        } catch (IllegalArgumentException ignored) {
          // No autenticar si el header no es UUID válido.
        }
      }
    }

    filterChain.doFilter(request, response);
  }

  private void authenticate(UUID userId) {
    var authentication = new UsernamePasswordAuthenticationToken(
            userId,
            null,
            List.of(new SimpleGrantedAuthority("ROLE_USER"))
    );

    SecurityContextHolder.getContext().setAuthentication(authentication);
  }
}