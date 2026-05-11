package com.hogaria.controller;

import com.hogaria.dto.AuthDtos.*;
import com.hogaria.security.CurrentUserResolver;
import com.hogaria.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
  private final AuthService authService;
  private final CurrentUserResolver currentUserResolver;

  public AuthController(AuthService authService, CurrentUserResolver currentUserResolver) {
    this.authService = authService;
    this.currentUserResolver = currentUserResolver;
  }

  @PostMapping("/login")
  public LoginResponse login(@Valid @RequestBody LoginRequest request) { return authService.login(request); }

  @GetMapping("/me")
  public MeResponse me() { return authService.me(currentUserResolver.currentUserId()); }
}
